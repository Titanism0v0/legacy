package com.overseas.purchase.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminDashboardRangeDTO {
    private Integer days;
    private Boolean allTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

