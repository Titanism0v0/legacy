package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.CommunityCommentDTO;
import com.overseas.purchase.dto.CommunityPostDTO;
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
import java.util.Map;

@RestController
@RequestMapping("/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public Result<Page<CommunityPostDTO>> listPosts(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    @RequestParam(required = false) String postType,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    HttpServletRequest request) {
        ensureAdmin(request);
        return Result.success(communityService.listAdminPosts(page, size, postType, keyword, status));
    }

    @PostMapping("/posts/{id}/audit")
    public Result<Void> auditPost(@PathVariable Long id,
                                  @RequestBody Map<String, Object> params,
                                  HttpServletRequest request) {
        ensureAdmin(request);
        String action = params.get("action") == null ? null : params.get("action").toString();
        String remark = params.get("remark") == null ? null : params.get("remark").toString();
        communityService.adminAuditPost(id, action, remark);
        return Result.success();
    }

    @GetMapping("/comments")
    public Result<Page<CommunityCommentDTO>> listComments(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size,
                                                          @RequestParam(required = false) Long postId,
                                                          @RequestParam(required = false) String keyword,
                                                          HttpServletRequest request) {
        ensureAdmin(request);
        return Result.success(communityService.listAdminComments(page, size, postId, keyword));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        communityService.adminDeletePost(id);
        return Result.success();
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        communityService.adminDeleteComment(id);
        return Result.success();
    }

    private void ensureAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"ADMIN".equals(role.toString())) {
            throw new RuntimeException("No permission");
        }
    }
}
