package com.overseas.purchase.service;

import com.overseas.purchase.dto.CommunityPostDTO;
import com.overseas.purchase.entity.CommunityPost;
import com.overseas.purchase.mapper.CategoryMapper;
import com.overseas.purchase.mapper.CommunityCommentMapper;
import com.overseas.purchase.mapper.CommunityPostMapper;
import com.overseas.purchase.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityServicePublicDtoTest {

    @Test
    void publicPostDtoDoesNotExposeModerationDiagnostics() {
        CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
        CommunityCommentMapper commentMapper = mock(CommunityCommentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        CommunityModerationService moderationService = mock(CommunityModerationService.class);
        CommunityService service = new CommunityService(postMapper, commentMapper, userMapper, categoryMapper, moderationService);

        CommunityPost post = new CommunityPost();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setCategoryId(3L);
        post.setTitle("公开帖子");
        post.setStatus("PUBLISHED");
        post.setDeleted(0);
        post.setAiScore(new BigDecimal("0.123"));
        post.setRiskLevel("LOW");
        post.setAiReason("internal rule details");
        post.setAuditRemark("internal audit remark");
        post.setModerationProvider("openai-compatible");
        post.setModerationModel("internal-model");
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectBatchIds(Collections.singleton(2L))).thenReturn(Collections.emptyList());
        when(categoryMapper.selectBatchIds(Collections.singleton(3L))).thenReturn(Collections.emptyList());

        CommunityPostDTO result = service.getPostDetail(1L, null, null);

        assertThat(result.getAiScore()).isNull();
        assertThat(result.getRiskLevel()).isNull();
        assertThat(result.getAiReason()).isNull();
        assertThat(result.getAuditRemark()).isNull();
        assertThat(result.getModerationProvider()).isNull();
        assertThat(result.getModerationModel()).isNull();

        CommunityPostDTO adminCallingPublicEndpoint = service.getPostDetail(1L, 99L, "ADMIN");
        assertThat(adminCallingPublicEndpoint.getAiReason()).isNull();
        assertThat(adminCallingPublicEndpoint.getModerationProvider()).isNull();
        assertThat(adminCallingPublicEndpoint.getModerationModel()).isNull();
    }
}
