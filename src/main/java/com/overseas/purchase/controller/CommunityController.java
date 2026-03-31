package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.CommunityCommentCreateDTO;
import com.overseas.purchase.dto.CommunityCommentDTO;
import com.overseas.purchase.dto.CommunityBeautifyRequestDTO;
import com.overseas.purchase.dto.CommunityBeautifyResponseDTO;
import com.overseas.purchase.dto.CommunityPostCreateDTO;
import com.overseas.purchase.dto.CommunityPostDTO;
import com.overseas.purchase.service.CommunityBeautifyService;
import com.overseas.purchase.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityBeautifyService communityBeautifyService;

    @GetMapping("/posts")
    public Result<Page<CommunityPostDTO>> listPosts(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    @RequestParam(required = false) String postType,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String keyword,
                                                    HttpServletRequest request) {
        Page<CommunityPostDTO> result = communityService.listPosts(
                page,
                size,
                postType,
                categoryId,
                keyword,
                getCurrentUserId(request),
                getCurrentRole(request)
        );
        return Result.success(result);
    }

    @GetMapping("/posts/{id}")
    public Result<CommunityPostDTO> getPost(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(communityService.getPostDetail(id, getCurrentUserId(request), getCurrentRole(request)));
    }

    @GetMapping("/posts/{id}/comments")
    public Result<List<CommunityCommentDTO>> listComments(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(communityService.listComments(id, getCurrentUserId(request), getCurrentRole(request)));
    }

    @PostMapping("/posts")
    public Result<CommunityPostDTO> createPost(@RequestBody CommunityPostCreateDTO dto, HttpServletRequest request) {
        Long userId = requireUserId(request);
        String role = requireRole(request);
        return Result.success(communityService.createPost(dto, userId, role));
    }

    @PostMapping("/posts/beautify")
    public Result<CommunityBeautifyResponseDTO> beautifyPost(@RequestBody CommunityBeautifyRequestDTO dto,
                                                             HttpServletRequest request) {
        requireUserId(request);
        requireRole(request);
        return Result.success(communityBeautifyService.beautify(dto));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        communityService.deletePost(id, requireUserId(request), requireRole(request));
        return Result.success();
    }

    @PostMapping("/comments")
    public Result<CommunityCommentDTO> createComment(@RequestBody CommunityCommentCreateDTO dto, HttpServletRequest request) {
        return Result.success(communityService.createComment(dto, requireUserId(request), requireRole(request)));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        communityService.deleteComment(id, requireUserId(request), requireRole(request));
        return Result.success();
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getCurrentRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role == null ? null : role.toString();
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            throw new RuntimeException("Please login first");
        }
        return userId;
    }

    private String requireRole(HttpServletRequest request) {
        String role = getCurrentRole(request);
        if (role == null) {
            throw new RuntimeException("Please login first");
        }
        return role;
    }
}
