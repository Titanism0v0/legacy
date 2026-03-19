package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后订单实体类
 * 
 * @author System
 */
@Data
@TableName("after_sales_order")
public class AfterSalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId; // 关联订单ID

    private Long userId; // 申请用户ID

    private Long sellerId; // 卖家ID（便于卖家查询与处理）

    private String type; // REFUND_ONLY-仅退款，RETURN_GOODS-退货退款

    private String reason; // 申请原因

    private BigDecimal amount; // 退款金额

    private String description; // 问题描述

    private String images; // 凭证图片列表（JSON格式）

    private String status; // PENDING-待审核，APPROVED-已通过，REJECTED-已拒绝，COMPLETED-已完成，SELLER_REJECTED-卖家已拒绝，ADMIN_ARBITRATING-平台仲裁中，SELLER_RESPONDED-卖家已响应（兼容旧流程）

    private String responsibility; // BUYER/SELLER/LOGISTICS/PLATFORM/UNKNOWN

    private String auditRemark; // 审核备注

    private String arbitrationResult; // 仲裁结论

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
