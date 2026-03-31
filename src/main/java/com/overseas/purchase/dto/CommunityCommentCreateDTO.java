package com.overseas.purchase.dto;

import lombok.Data;

@Data
public class CommunityCommentCreateDTO {

    private Long postId;

    private Long parentId;

    private Long replyToUserId;

    private String content;
}
