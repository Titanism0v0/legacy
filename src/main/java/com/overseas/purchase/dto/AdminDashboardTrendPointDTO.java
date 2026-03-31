package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardTrendPointDTO {
    private String dateValue;
    private String dateLabel;
    private Integer orderCount;
    private BigDecimal orderAmount;
}

