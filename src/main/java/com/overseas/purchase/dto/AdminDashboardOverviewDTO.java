package com.overseas.purchase.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminDashboardOverviewDTO {
    private AdminDashboardSummaryDTO summary;
    private List<AdminDashboardStatusDTO> orderStatusBreakdown;
    private List<AdminDashboardTrendPointDTO> orderTrend;
    private AdminDashboardRangeDTO range;
}

