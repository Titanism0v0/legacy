package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerDashboardSummaryDTO {
    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer pendingShipmentCount;
    private Integer refundOrderCount;
}
