package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("community_post")
public class CommunityPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;

    private String authorRole;

    private String postType;

    private String title;

    private String content;

    private Long categoryId;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
