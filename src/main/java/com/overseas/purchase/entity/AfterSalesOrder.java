package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sales_order")
public class AfterSalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long userId;
    private Long sellerId;

    private String type;
    private String reason;
    private BigDecimal amount;
    private String description;

    // legacy field retained for compatibility
    private String images;

    private String evidenceType;
    private String evidenceUrls;
    private String evidenceText;

    private String ruleDecision;
    private String ruleReason;
    private BigDecimal aiScore;
    private String aiSuggestion;
    private String aiReason;

    private String status;
    private String responsibility;
    private String auditRemark;
    private String arbitrationResult;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

