package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommunityPostDTO {

    private Long id;

    private Long authorId;

    private String authorNickname;

    private String authorAvatar;

    private String authorRole;

    private String postType;

    private String title;

    private String content;

    private Long categoryId;

    private String categoryName;

    private String contentMode;

    private String renderPayload;

    private String images;

    private String coverImage;

    private String coverTemplate;

    private String status;

    private BigDecimal aiScore;

    private String riskLevel;

    private String aiReason;

    private String auditRemark;

    private LocalDateTime moderatedAt;

    private String moderationProvider;

    private String moderationModel;

    private Integer commentCount;

    private Boolean canDelete;

    private Boolean canContact;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
