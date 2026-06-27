package com.overseas.purchase.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal aiScore;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String riskLevel;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String aiReason;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String auditRemark;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime moderatedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String moderationProvider;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String moderationModel;

    private Integer commentCount;

    private Boolean canDelete;

    private Boolean canContact;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
