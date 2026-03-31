package com.overseas.purchase.service;

import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.dto.SellerDashboardOverviewDTO;
import com.overseas.purchase.dto.SellerDashboardRangeDTO;
import com.overseas.purchase.dto.SellerDashboardStatusDTO;
import com.overseas.purchase.dto.SellerDashboardSummaryDTO;
import com.overseas.purchase.dto.SellerDashboardTrendPointDTO;
import com.overseas.purchase.dto.SellerDashboardTopProductDTO;
import com.overseas.purchase.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private static final int TOP_PRODUCT_LIMIT = 5;
    private static final int RECENT_ORDER_LIMIT = 6;
    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final OrderMapper orderMapper;

    public SellerDashboardOverviewDTO getOverview(Long sellerId, Integer days) {
        Integer normalizedDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = normalizedDays == null
                ? null
                : LocalDate.now().minusDays(normalizedDays - 1L).atStartOfDay();

        SellerDashboardSummaryDTO summary = orderMapper.selectSellerDashboardSummary(sellerId, startTime);
        if (summary == null) {
            summary = emptySummary();
        } else {
            fillSummaryDefaults(summary);
        }

        List<SellerDashboardStatusDTO> statusBreakdown = orderMapper.selectSellerDashboardStatusBreakdown(sellerId, startTime);
        List<SellerDashboardTrendPointDTO> dailyTrend = buildTrend(
                orderMapper.selectSellerDashboardDailyTrend(sellerId, startTime),
                normalizedDays,
                startTime == null ? null : startTime.toLocalDate(),
                now.toLocalDate()
        );
        List<SellerDashboardTopProductDTO> topProducts = orderMapper.selectSellerDashboardTopProducts(sellerId, startTime, TOP_PRODUCT_LIMIT);
        List<OrderDTO> recentOrders = orderMapper.selectSellerDashboardRecentOrders(sellerId, startTime, RECENT_ORDER_LIMIT);

        SellerDashboardRangeDTO range = new SellerDashboardRangeDTO();
        range.setDays(normalizedDays);
        range.setAllTime(normalizedDays == null);
        range.setStartTime(startTime);
        range.setEndTime(now);

        SellerDashboardOverviewDTO overview = new SellerDashboardOverviewDTO();
        overview.setSummary(summary);
        overview.setStatusBreakdown(statusBreakdown == null ? Collections.emptyList() : statusBreakdown);
        overview.setDailyTrend(dailyTrend);
        overview.setTopProducts(topProducts == null ? Collections.emptyList() : topProducts);
        overview.setRecentOrders(recentOrders == null ? Collections.emptyList() : recentOrders);
        overview.setRange(range);
        return overview;
    }

    private Integer normalizeDays(Integer days) {
        if (days == null) {
            return 7;
        }
        if (days <= 0) {
            return null;
        }
        return days;
    }

    private SellerDashboardSummaryDTO emptySummary() {
        SellerDashboardSummaryDTO summary = new SellerDashboardSummaryDTO();
        fillSummaryDefaults(summary);
        return summary;
    }

    private void fillSummaryDefaults(SellerDashboardSummaryDTO summary) {
        if (summary.getOrderCount() == null) {
            summary.setOrderCount(0);
        }
        if (summary.getOrderAmount() == null) {
            summary.setOrderAmount(BigDecimal.ZERO);
        }
        if (summary.getPendingShipmentCount() == null) {
            summary.setPendingShipmentCount(0);
        }
        if (summary.getRefundOrderCount() == null) {
            summary.setRefundOrderCount(0);
        }
    }

    private List<SellerDashboardTrendPointDTO> buildTrend(List<SellerDashboardTrendPointDTO> rawTrend,
                                                          Integer normalizedDays,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        List<SellerDashboardTrendPointDTO> source = rawTrend == null ? Collections.emptyList() : rawTrend;
        fillTrendDefaults(source);

        if (normalizedDays == null || startDate == null || endDate == null) {
            return source;
        }

        Map<String, SellerDashboardTrendPointDTO> indexedTrend = new LinkedHashMap<>();
        for (SellerDashboardTrendPointDTO point : source) {
            indexedTrend.put(point.getDateValue(), point);
        }

        List<SellerDashboardTrendPointDTO> filledTrend = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String dateValue = cursor.toString();
            SellerDashboardTrendPointDTO existing = indexedTrend.get(dateValue);
            if (existing != null) {
                filledTrend.add(existing);
            } else {
                SellerDashboardTrendPointDTO point = new SellerDashboardTrendPointDTO();
                point.setDateValue(dateValue);
                point.setDateLabel(cursor.format(TREND_LABEL_FORMATTER));
                point.setOrderCount(0);
                point.setOrderAmount(BigDecimal.ZERO);
                filledTrend.add(point);
            }
            cursor = cursor.plusDays(1);
        }
        return filledTrend;
    }

    private void fillTrendDefaults(List<SellerDashboardTrendPointDTO> trend) {
        for (SellerDashboardTrendPointDTO point : trend) {
            if (point.getOrderCount() == null) {
                point.setOrderCount(0);
            }
            if (point.getOrderAmount() == null) {
                point.setOrderAmount(BigDecimal.ZERO);
            }
            if (point.getDateLabel() == null && point.getDateValue() != null) {
                point.setDateLabel(LocalDate.parse(point.getDateValue()).format(TREND_LABEL_FORMATTER));
            }
        }
    }
}
