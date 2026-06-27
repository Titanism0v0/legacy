package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO {

    private Long id;
    private String orderNo;

    private Long buyerId;
    private String buyerNickname;
    private Long sellerId;
    private String sellerNickname;

    private Long productId;
    private String productTitle;
    private String productImage;

    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String fullAddress;

    private Integer quantity;
    private BigDecimal subtotalPrice;
    private BigDecimal taxEstimatedAmount;
    private BigDecimal shippingFeeSnapshot;
    private BigDecimal totalPrice;
    private String paymentCurrencySnapshot;
    private BigDecimal internationalShippingFeeSnapshot;
    private BigDecimal insuranceFeeSnapshot;
    private BigDecimal tariffAmountSnapshot;
    private BigDecimal vatAmountSnapshot;
    private BigDecimal consumptionTaxAmountSnapshot;
    private String taxModeSnapshot;
    private String originZoneSnapshot;

    private BigDecimal taxRateSnapshot;
    private BigDecimal exchangeRateSnapshot;
    private Integer taxIncludedFlag;
    private String customsClearanceStatus;

    private String status;
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
}
