package com.overseas.purchase.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityCommentDTO {

    private Long id;

    private Long postId;

    private Long authorId;

    private String authorNickname;

    private String authorAvatar;

    private Long parentId;

    private Long replyToUserId;

    private String replyToNickname;

    private String content;

    private Boolean canDelete;

    private LocalDateTime createTime;

    private List<CommunityCommentDTO> replies;
}
