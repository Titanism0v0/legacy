package com.overseas.purchase.dto;

import lombok.Data;

import java.util.List;

@Data
public class SellerDashboardOverviewDTO {
    private SellerDashboardSummaryDTO summary;
    private List<SellerDashboardStatusDTO> statusBreakdown;
    private List<SellerDashboardTrendPointDTO> dailyTrend;
    private List<SellerDashboardTopProductDTO> topProducts;
    private List<OrderDTO> recentOrders;
    private SellerDashboardRangeDTO range;
}
