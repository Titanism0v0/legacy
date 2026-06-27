package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderEstimateDTO {
    private Long productId;
    private Integer quantity;

    private BigDecimal subtotalPrice;
    private BigDecimal taxEstimatedAmount;
    private BigDecimal shippingFeeSnapshot;
    private BigDecimal totalPrice;

    private BigDecimal paymentSubtotalPrice;
    private BigDecimal paymentTaxEstimatedAmount;
    private BigDecimal paymentShippingFeeSnapshot;
    private BigDecimal paymentTotalPrice;

    private BigDecimal dutiablePrice;
    private BigDecimal internationalShippingFee;
    private BigDecimal insuranceFee;
    private BigDecimal tariffAmount;
    private BigDecimal vatAmount;
    private BigDecimal consumptionTaxAmount;

    private BigDecimal taxRateSnapshot;
    private BigDecimal exchangeRateSnapshot;
    private Integer taxIncludedFlag;

    private String customsClearanceStatus;
    private String riskLevel;
    private Integer restrictedFlag;
    private String productCurrency;
    private String displayCurrency;
    private String paymentCurrency;
    private Boolean paymentFallbackApplied;
    private String taxMode;
    private String originZone;
    private String ruleVersion;
    private OrderRuleSummaryDTO ruleSummary;
}
