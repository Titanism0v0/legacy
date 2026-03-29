package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private Long addressId;
    private Integer quantity;

    private BigDecimal subtotalPrice;
    private BigDecimal taxEstimatedAmount;
    private BigDecimal shippingFeeSnapshot;
    private BigDecimal totalPrice;

    private BigDecimal taxRateSnapshot;
    private BigDecimal exchangeRateSnapshot;
    private Integer taxIncludedFlag;

    private String status;
    private String customsClearanceStatus;

    private String trackingNumber;
    private String crossborderTrackingNumber;
    private String domesticTrackingNumber;

    private Integer taxDeclarationAccepted;
    private Integer restrictedDeclarationAccepted;
    private String auditStatus;
    private String auditRemark;
    private LocalDateTime auditTime;

    private String remark;
    private String paymentProof;
    private LocalDateTime paymentTime;
    private String paymentStatus;
    private String paymentChannel;
    private LocalDateTime paymentSubmittedTime;
    private LocalDateTime paymentVerifiedTime;

    private String refundStatus;
    private BigDecimal refundAmount;
    private LocalDateTime refundTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
