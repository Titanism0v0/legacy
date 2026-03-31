package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.dto.CommunityCommentCreateDTO;
import com.overseas.purchase.dto.CommunityCommentDTO;
import com.overseas.purchase.dto.CommunityPostCreateDTO;
import com.overseas.purchase.dto.CommunityPostDTO;
import com.overseas.purchase.entity.Category;
import com.overseas.purchase.entity.CommunityComment;
import com.overseas.purchase.entity.CommunityPost;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.CategoryMapper;
import com.overseas.purchase.mapper.CommunityCommentMapper;
import com.overseas.purchase.mapper.CommunityPostMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_SELLER = "SELLER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String POST_WANTED = "WANTED";
    private static final String POST_FOR_SALE = "FOR_SALE";
    private static final String POST_DISCUSSION = "DISCUSSION";
    private static final String CONTENT_MODE_STANDARD = "STANDARD";
    private static final String CONTENT_MODE_TEXT_IMAGE = "TEXT_IMAGE";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String DECISION_ALLOW = "ALLOW";
    private static final String DECISION_REVIEW = "REVIEW";
    private static final String DECISION_BLOCK = "BLOCK";

    private final CommunityPostMapper communityPostMapper;
    private final CommunityCommentMapper communityCommentMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final CommunityModerationService moderationService;

    public Page<CommunityPostDTO> listPosts(Integer page,
                                            Integer size,
                                            String postType,
                                            Long categoryId,
                                            String keyword,
                                            Long currentUserId,
                                            String currentRole) {
        Page<CommunityPost> postPage = new Page<>(page, size);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getDeleted, 0)
                .eq(CommunityPost::getStatus, STATUS_PUBLISHED)
                .orderByDesc(CommunityPost::getCreateTime)
                .orderByDesc(CommunityPost::getId);
        if (StringUtils.hasText(postType)) {
            wrapper.eq(CommunityPost::getPostType, postType);
        }
        if (categoryId != null) {
            wrapper.eq(CommunityPost::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CommunityPost::getTitle, keyword)
                    .or().like(CommunityPost::getContent, keyword));
        }

        Page<CommunityPost> result = communityPostMapper.selectPage(postPage, wrapper);
        return buildPostPage(result, currentUserId, currentRole);
    }

    public CommunityPostDTO getPostDetail(Long id, Long currentUserId, String currentRole) {
        CommunityPost post = getVisiblePost(id, currentUserId, currentRole);
        return toPostDTO(post, loadUsersMap(Collections.singleton(post.getAuthorId())), loadCategoriesMap(Collections.singleton(post.getCategoryId())), currentUserId, currentRole);
    }

    public List<CommunityCommentDTO> listComments(Long postId, Long currentUserId, String currentRole) {
        getVisiblePost(postId, currentUserId, currentRole);
        List<CommunityComment> comments = communityCommentMapper.selectList(
                new LambdaQueryWrapper<CommunityComment>()
                        .eq(CommunityComment::getPostId, postId)
                        .eq(CommunityComment::getDeleted, 0)
                        .orderByAsc(CommunityComment::getCreateTime)
                        .orderByAsc(CommunityComment::getId)
        );
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new HashSet<>();
        for (CommunityComment comment : comments) {
            userIds.add(comment.getAuthorId());
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
        }
        Map<Long, User> users = loadUsersMap(userIds);
        Map<Long, List<CommunityCommentDTO>> repliesByParentId = new HashMap<>();
        List<CommunityCommentDTO> roots = new ArrayList<>();

        for (CommunityComment comment : comments) {
            CommunityCommentDTO dto = toCommentDTO(comment, users, currentUserId, currentRole);
            if (comment.getParentId() == null || comment.getParentId() == 0L) {
                dto.setReplies(new ArrayList<>());
                roots.add(dto);
            } else {
                repliesByParentId.computeIfAbsent(comment.getParentId(), key -> new ArrayList<>()).add(dto);
            }
        }

        for (CommunityCommentDTO root : roots) {
            List<CommunityCommentDTO> replies = repliesByParentId.getOrDefault(root.getId(), new ArrayList<>());
            replies.sort(Comparator.comparing(CommunityCommentDTO::getCreateTime).thenComparing(CommunityCommentDTO::getId));
            root.setReplies(replies);
        }
        return roots;
    }

    @Transactional
    public CommunityPostDTO createPost(CommunityPostCreateDTO dto, Long userId, String role) {
        validatePostPermission(role, dto.getPostType());
        validateCategory(dto.getCategoryId());
        validatePostPayload(dto);
        String contentMode = resolveContentMode(dto.getContentMode(), dto.getRenderPayload());

        CommunityPost post = new CommunityPost();
        post.setAuthorId(userId);
        post.setAuthorRole(role);
        post.setPostType(dto.getPostType());
        post.setTitle(dto.getTitle().trim());
        post.setContent(normalizeContent(dto.getContent()));
        post.setCategoryId(dto.getCategoryId());
        post.setContentMode(contentMode);
        post.setRenderPayload(normalizeText(dto.getRenderPayload()));
        post.setImages(normalizeImages(dto));
        post.setCoverImage(normalizeText(dto.getCoverImage()));
        post.setCoverTemplate(normalizeText(dto.getCoverTemplate()));
        post.setCommentCount(0);

        CommunityModerationService.ModerationDecision moderationDecision = moderationService.moderatePost(dto);
        applyModerationResult(post, moderationDecision);
        post.setStatus(STATUS_PUBLISHED);
        communityPostMapper.insert(post);

        return toPostDTO(
                post,
                loadUsersMap(Collections.singleton(post.getAuthorId())),
                loadCategoriesMap(Collections.singleton(post.getCategoryId())),
                userId,
                role
        );
    }

    @Transactional
    public void deletePost(Long postId, Long userId, String role) {
        CommunityPost post = getExistingPost(postId);
        if (!ROLE_ADMIN.equals(role) && !userId.equals(post.getAuthorId())) {
            throw new RuntimeException("No permission to delete this post");
        }
        deletePostAndComments(postId);
    }

    @Transactional
    public CommunityCommentDTO createComment(CommunityCommentCreateDTO dto, Long userId, String role) {
        if (!ROLE_USER.equals(role) && !ROLE_SELLER.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new RuntimeException("No permission to comment");
        }
        CommunityPost post = getPublishedPost(dto.getPostId());
        if (!StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("Comment content cannot be empty");
        }

        CommunityComment comment = new CommunityComment();
        comment.setPostId(post.getId());
        comment.setAuthorId(userId);
        comment.setContent(dto.getContent().trim());

        if (dto.getParentId() != null && dto.getParentId() > 0) {
            CommunityComment parent = getExistingComment(dto.getParentId());
            if (!parent.getPostId().equals(post.getId())) {
                throw new RuntimeException("Comment parent mismatch");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                throw new RuntimeException("Only one-level replies are supported");
            }
            comment.setParentId(parent.getId());
            comment.setReplyToUserId(dto.getReplyToUserId() != null ? dto.getReplyToUserId() : parent.getAuthorId());
        } else {
            comment.setParentId(0L);
            comment.setReplyToUserId(null);
        }

        communityCommentMapper.insert(comment);
        incrementCommentCount(post.getId(), 1);

        Map<Long, User> users = loadUsersMap(buildCommentUserIds(comment));
        return toCommentDTO(comment, users, userId, role);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId, String role) {
        CommunityComment comment = getExistingComment(commentId);
        if (!ROLE_ADMIN.equals(role) && !userId.equals(comment.getAuthorId())) {
            throw new RuntimeException("No permission to delete this comment");
        }
        deleteCommentInternal(comment);
    }

    public Page<CommunityPostDTO> listAdminPosts(Integer page, Integer size, String postType, String keyword, String status) {
        Page<CommunityPost> postPage = new Page<>(page, size);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getDeleted, 0)
                .orderByDesc(CommunityPost::getCreateTime)
                .orderByDesc(CommunityPost::getId);
        if (StringUtils.hasText(postType)) {
            wrapper.eq(CommunityPost::getPostType, postType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CommunityPost::getTitle, keyword)
                    .or().like(CommunityPost::getContent, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommunityPost::getStatus, status.trim().toUpperCase());
        }
        return buildPostPage(communityPostMapper.selectPage(postPage, wrapper), null, ROLE_ADMIN);
    }

    public Page<CommunityCommentDTO> listAdminComments(Integer page, Integer size, Long postId, String keyword) {
        Page<CommunityComment> commentPage = new Page<>(page, size);
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getDeleted, 0)
                .orderByDesc(CommunityComment::getCreateTime)
                .orderByDesc(CommunityComment::getId);
        if (postId != null) {
            wrapper.eq(CommunityComment::getPostId, postId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CommunityComment::getContent, keyword);
        }
        Page<CommunityComment> result = communityCommentMapper.selectPage(commentPage, wrapper);
        Set<Long> userIds = new HashSet<>();
        for (CommunityComment comment : result.getRecords()) {
            userIds.add(comment.getAuthorId());
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
        }
        Map<Long, User> users = loadUsersMap(userIds);
        Page<CommunityCommentDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(comment -> toCommentDTO(comment, users, null, ROLE_ADMIN))
                .collect(Collectors.toList()));
        return dtoPage;
    }

    @Transactional
    public void adminDeletePost(Long postId) {
        deletePostAndComments(postId);
    }

    @Transactional
    public void adminAuditPost(Long postId, String action, String remark) {
        if (!StringUtils.hasText(action)) {
            throw new RuntimeException("Action is required");
        }
        CommunityPost post = getExistingPost(postId);
        if (!STATUS_PENDING_REVIEW.equals(post.getStatus())) {
            throw new RuntimeException("Only pending-review posts can be audited");
        }

        String normalizedAction = action.trim().toUpperCase();
        if ("APPROVE".equals(normalizedAction)) {
            post.setStatus(STATUS_PUBLISHED);
            post.setAuditRemark(trimToLength(remark, 500));
        } else if ("REJECT".equals(normalizedAction)) {
            post.setStatus(STATUS_REJECTED);
            String finalRemark = trimToLength(remark, 500);
            if (!StringUtils.hasText(finalRemark)) {
                finalRemark = trimToLength(post.getAiReason(), 500);
            }
            if (!StringUtils.hasText(finalRemark)) {
                finalRemark = "Rejected by admin";
            }
            post.setAuditRemark(finalRemark);
        } else {
            throw new RuntimeException("Invalid action");
        }
        post.setModeratedAt(LocalDateTime.now());
        communityPostMapper.updateById(post);
    }

    @Transactional
    public void adminDeleteComment(Long commentId) {
        deleteCommentInternal(getExistingComment(commentId));
    }

    private Page<CommunityPostDTO> buildPostPage(Page<CommunityPost> postPage, Long currentUserId, String currentRole) {
        Set<Long> userIds = postPage.getRecords().stream().map(CommunityPost::getAuthorId).collect(Collectors.toSet());
        Set<Long> categoryIds = postPage.getRecords().stream()
                .map(CommunityPost::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, User> users = loadUsersMap(userIds);
        Map<Long, Category> categories = loadCategoriesMap(categoryIds);

        Page<CommunityPostDTO> dtoPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        dtoPage.setRecords(postPage.getRecords().stream()
                .map(post -> toPostDTO(post, users, categories, currentUserId, currentRole))
                .collect(Collectors.toList()));
        return dtoPage;
    }

    private CommunityPostDTO toPostDTO(CommunityPost post,
                                       Map<Long, User> users,
                                       Map<Long, Category> categories,
                                       Long currentUserId,
                                       String currentRole) {
        CommunityPostDTO dto = new CommunityPostDTO();
        BeanUtils.copyProperties(post, dto);
        User author = users.get(post.getAuthorId());
        if (author != null) {
            dto.setAuthorNickname(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
            dto.setAuthorAvatar(author.getAvatar());
        }
        Category category = categories.get(post.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }
        dto.setCanDelete(currentUserId != null && (currentUserId.equals(post.getAuthorId()) || ROLE_ADMIN.equals(currentRole)));
        dto.setCanContact(currentUserId != null && !currentUserId.equals(post.getAuthorId()));
        dto.setCommentCount(post.getCommentCount() == null ? 0 : post.getCommentCount());
        dto.setContentMode(resolveContentMode(post.getContentMode(), post.getRenderPayload()));
        return dto;
    }

    private CommunityCommentDTO toCommentDTO(CommunityComment comment,
                                             Map<Long, User> users,
                                             Long currentUserId,
                                             String currentRole) {
        CommunityCommentDTO dto = new CommunityCommentDTO();
        BeanUtils.copyProperties(comment, dto);
        User author = users.get(comment.getAuthorId());
        if (author != null) {
            dto.setAuthorNickname(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
            dto.setAuthorAvatar(author.getAvatar());
        }
        if (comment.getReplyToUserId() != null) {
            User replyToUser = users.get(comment.getReplyToUserId());
            if (replyToUser != null) {
                dto.setReplyToNickname(StringUtils.hasText(replyToUser.getNickname()) ? replyToUser.getNickname() : replyToUser.getUsername());
            }
        }
        dto.setCanDelete(currentUserId != null && (currentUserId.equals(comment.getAuthorId()) || ROLE_ADMIN.equals(currentRole)));
        dto.setReplies(Collections.emptyList());
        return dto;
    }

    private void validatePostPermission(String role, String postType) {
        if (ROLE_ADMIN.equals(role)) {
            throw new RuntimeException("Admin cannot create community posts");
        }
        if (!StringUtils.hasText(postType)) {
            throw new RuntimeException("Post type is required");
        }
        if (ROLE_USER.equals(role)) {
            if (!POST_WANTED.equals(postType) && !POST_DISCUSSION.equals(postType)) {
                throw new RuntimeException("User can only publish wanted or discussion posts");
            }
            return;
        }
        if (ROLE_SELLER.equals(role)) {
            if (!POST_FOR_SALE.equals(postType) && !POST_DISCUSSION.equals(postType)) {
                throw new RuntimeException("Seller can only publish for-sale or discussion posts");
            }
            return;
        }
        throw new RuntimeException("Unsupported role");
    }

    private void validatePostPayload(CommunityPostCreateDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new RuntimeException("Title cannot be empty");
        }
        String contentMode = resolveContentMode(dto.getContentMode(), dto.getRenderPayload());
        String images = normalizeText(dto.getImages());
        String coverImage = normalizeText(dto.getCoverImage());
        if (CONTENT_MODE_TEXT_IMAGE.equals(contentMode)) {
            if (!StringUtils.hasText(coverImage)) {
                throw new RuntimeException("Text-image posts must include a generated cover image");
            }
            if (!StringUtils.hasText(normalizeText(dto.getRenderPayload()))) {
                throw new RuntimeException("Text-image post metadata is required");
            }
            return;
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("Content cannot be empty");
        }
        if (!StringUtils.hasText(images) && !StringUtils.hasText(coverImage)) {
            throw new RuntimeException("Post must contain images or a cover image");
        }
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            throw new RuntimeException("Category is required");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDeleted() == 1) {
            throw new RuntimeException("Category does not exist");
        }
    }

    private CommunityPost getPublishedPost(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1 || !STATUS_PUBLISHED.equals(post.getStatus())) {
            throw new RuntimeException("Post does not exist");
        }
        return post;
    }

    private CommunityPost getVisiblePost(Long postId, Long currentUserId, String currentRole) {
        CommunityPost post = getExistingPost(postId);
        if (STATUS_PUBLISHED.equals(post.getStatus())) {
            return post;
        }
        boolean isAuthor = currentUserId != null && currentUserId.equals(post.getAuthorId());
        boolean isAdmin = ROLE_ADMIN.equals(currentRole);
        if (isAuthor || isAdmin) {
            return post;
        }
        throw new RuntimeException("Post does not exist");
    }

    private CommunityPost getExistingPost(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new RuntimeException("Post does not exist");
        }
        return post;
    }

    private CommunityComment getExistingComment(Long commentId) {
        CommunityComment comment = communityCommentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new RuntimeException("Comment does not exist");
        }
        return comment;
    }

    private void deletePostAndComments(Long postId) {
        CommunityPost post = getExistingPost(postId);
        communityCommentMapper.delete(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getDeleted, 0));
        post.setCommentCount(0);
        communityPostMapper.updateById(post);
        communityPostMapper.deleteById(postId);
    }

    private void deleteCommentInternal(CommunityComment comment) {
        int affected;
        if (comment.getParentId() == null || comment.getParentId() == 0L) {
            affected = Math.toIntExact(communityCommentMapper.selectCount(
                    new LambdaQueryWrapper<CommunityComment>()
                            .eq(CommunityComment::getPostId, comment.getPostId())
                            .eq(CommunityComment::getDeleted, 0)
                            .and(w -> w.eq(CommunityComment::getId, comment.getId())
                                    .or().eq(CommunityComment::getParentId, comment.getId()))
            ));
            communityCommentMapper.delete(new LambdaQueryWrapper<CommunityComment>()
                    .eq(CommunityComment::getPostId, comment.getPostId())
                    .and(w -> w.eq(CommunityComment::getId, comment.getId())
                            .or().eq(CommunityComment::getParentId, comment.getId())));
        } else {
            affected = 1;
            communityCommentMapper.deleteById(comment.getId());
        }
        incrementCommentCount(comment.getPostId(), -affected);
    }

    private void incrementCommentCount(Long postId, int delta) {
        CommunityPost post = getExistingPost(postId);
        int current = post.getCommentCount() == null ? 0 : post.getCommentCount();
        int next = current + delta;
        if (next < 0) {
            next = 0;
        }
        communityPostMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .set(CommunityPost::getCommentCount, next));
    }

    private Map<Long, User> loadUsersMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> result = new HashMap<>();
        for (User user : users) {
            if (user != null && (user.getDeleted() == null || user.getDeleted() == 0)) {
                result.put(user.getId(), user);
            }
        }
        return result;
    }

    private Map<Long, Category> loadCategoriesMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, Category> result = new HashMap<>();
        for (Category category : categories) {
            if (category != null && (category.getDeleted() == null || category.getDeleted() == 0)) {
                result.put(category.getId(), category);
            }
        }
        return result;
    }

    private Set<Long> buildCommentUserIds(CommunityComment comment) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(comment.getAuthorId());
        if (comment.getReplyToUserId() != null) {
            userIds.add(comment.getReplyToUserId());
        }
        return userIds;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveContentMode(String value, String renderPayload) {
        if (StringUtils.hasText(renderPayload)) {
            return CONTENT_MODE_TEXT_IMAGE;
        }
        if (!StringUtils.hasText(value)) {
            return CONTENT_MODE_STANDARD;
        }
        String normalized = value.trim().toUpperCase();
        if (CONTENT_MODE_TEXT_IMAGE.equals(normalized)) {
            return CONTENT_MODE_TEXT_IMAGE;
        }
        return CONTENT_MODE_STANDARD;
    }

    private String normalizeImages(CommunityPostCreateDTO dto) {
        return normalizeText(dto.getImages());
    }

    private String normalizeContent(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private void applyModerationResult(CommunityPost post, CommunityModerationService.ModerationDecision decision) {
        String mappedStatus = mapDecisionToStatus(decision.getDecision());
        post.setStatus(mappedStatus);
        post.setAiScore(decision.getRiskScore());
        post.setRiskLevel(trimToLength(decision.getRiskLevel(), 20));
        post.setAiReason(trimToLength(decision.getReason(), 500));
        post.setModerationProvider(trimToLength(decision.getProvider(), 64));
        post.setModerationModel(trimToLength(decision.getModel(), 64));
        post.setModeratedAt(LocalDateTime.now());
        if (STATUS_REJECTED.equals(mappedStatus)) {
            post.setAuditRemark(trimToLength(decision.getReason(), 500));
        } else if (STATUS_PENDING_REVIEW.equals(mappedStatus)) {
            post.setAuditRemark(trimToLength("Auto-published; moderation suggested review: " + decision.getReason(), 500));
        } else {
            post.setAuditRemark(null);
        }
    }

    private String mapDecisionToStatus(String decision) {
        if (DECISION_ALLOW.equalsIgnoreCase(decision)) {
            return STATUS_PUBLISHED;
        }
        if (DECISION_BLOCK.equalsIgnoreCase(decision)) {
            return STATUS_REJECTED;
        }
        if (!DECISION_REVIEW.equalsIgnoreCase(decision)) {
            return STATUS_PENDING_REVIEW;
        }
        return STATUS_PENDING_REVIEW;
    }

    private String trimToLength(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }
}
