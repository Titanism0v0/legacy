package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardSummaryDTO {
    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer pendingOrderAuditCount;
    private Integer pendingProductAuditCount;
    private Integer pendingKycCount;
    private Integer pendingAfterSalesCount;
    private Integer pendingCommunityReviewCount;
    private Integer highRiskProductCount;
    private Integer activeFulfillmentCount;
    private Integer customsPendingCount;
}
