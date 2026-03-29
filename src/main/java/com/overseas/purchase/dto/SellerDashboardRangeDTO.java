package com.overseas.purchase.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SellerDashboardRangeDTO {
    private Integer days;
    private Boolean allTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
