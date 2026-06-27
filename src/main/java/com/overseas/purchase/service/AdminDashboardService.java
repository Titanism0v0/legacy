package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.dto.AdminDashboardOverviewDTO;
import com.overseas.purchase.dto.AdminDashboardRangeDTO;
import com.overseas.purchase.dto.AdminDashboardStatusDTO;
import com.overseas.purchase.dto.AdminDashboardSummaryDTO;
import com.overseas.purchase.dto.AdminDashboardTrendPointDTO;
import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.entity.CommunityPost;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.AfterSalesOrderMapper;
import com.overseas.purchase.mapper.CommunityPostMapper;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final List<String> AFTER_SALES_PENDING_STATUSES =
            Arrays.asList("PENDING", "SELLER_RESPONDED", "ADMIN_ARBITRATING");

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final CommunityPostMapper communityPostMapper;

    public AdminDashboardOverviewDTO getOverview(Integer days) {
        Integer normalizedDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = normalizedDays == null
                ? null
                : LocalDate.now().minusDays(normalizedDays - 1L).atStartOfDay();

        AdminDashboardSummaryDTO summary = orderMapper.selectAdminDashboardSummary(startTime);
        if (summary == null) {
            summary = new AdminDashboardSummaryDTO();
        }
        fillSummaryDefaults(summary);
        fillPendingCounters(summary);

        List<AdminDashboardStatusDTO> statusBreakdown = orderMapper.selectAdminDashboardStatusBreakdown(startTime);
        List<AdminDashboardTrendPointDTO> orderTrend = buildTrend(
                orderMapper.selectAdminDashboardDailyTrend(startTime),
                normalizedDays,
                startTime == null ? null : startTime.toLocalDate(),
                now.toLocalDate()
        );

        AdminDashboardRangeDTO range = new AdminDashboardRangeDTO();
        range.setDays(normalizedDays);
        range.setAllTime(normalizedDays == null);
        range.setStartTime(startTime);
        range.setEndTime(now);

        AdminDashboardOverviewDTO overview = new AdminDashboardOverviewDTO();
        overview.setSummary(summary);
        overview.setOrderStatusBreakdown(statusBreakdown == null ? Collections.emptyList() : statusBreakdown);
        overview.setOrderTrend(orderTrend);
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

    private void fillSummaryDefaults(AdminDashboardSummaryDTO summary) {
        if (summary.getOrderCount() == null) {
            summary.setOrderCount(0);
        }
        if (summary.getOrderAmount() == null) {
            summary.setOrderAmount(BigDecimal.ZERO);
        }
        if (summary.getPendingOrderAuditCount() == null) {
            summary.setPendingOrderAuditCount(0);
        }
        if (summary.getPendingProductAuditCount() == null) {
            summary.setPendingProductAuditCount(0);
        }
        if (summary.getPendingKycCount() == null) {
            summary.setPendingKycCount(0);
        }
        if (summary.getPendingAfterSalesCount() == null) {
            summary.setPendingAfterSalesCount(0);
        }
        if (summary.getPendingCommunityReviewCount() == null) {
            summary.setPendingCommunityReviewCount(0);
        }
        if (summary.getHighRiskProductCount() == null) {
            summary.setHighRiskProductCount(0);
        }
        if (summary.getActiveFulfillmentCount() == null) {
            summary.setActiveFulfillmentCount(0);
        }
        if (summary.getCustomsPendingCount() == null) {
            summary.setCustomsPendingCount(0);
        }
    }

    private void fillPendingCounters(AdminDashboardSummaryDTO summary) {
        summary.setPendingOrderAuditCount(safeCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING_AUDIT")
        )));

        summary.setPendingProductAuditCount(safeCount(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getAuditStatus, "PENDING")
        )));

        summary.setPendingKycCount(safeCount(userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "SELLER")
                        .eq(User::getKycStatus, "PENDING")
        )));

        summary.setPendingAfterSalesCount(safeCount(afterSalesOrderMapper.selectCount(
                new LambdaQueryWrapper<AfterSalesOrder>()
                        .in(AfterSalesOrder::getStatus, AFTER_SALES_PENDING_STATUSES)
        )));

        summary.setPendingCommunityReviewCount(safeCount(communityPostMapper.selectCount(
                new LambdaQueryWrapper<CommunityPost>().eq(CommunityPost::getStatus, "PENDING_REVIEW")
        )));

        summary.setHighRiskProductCount(safeCount(productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .and(w -> w.eq(Product::getRestrictedFlag, 1).or().eq(Product::getRiskLevel, "HIGH"))
        )));

        summary.setActiveFulfillmentCount(safeCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getStatus, "PURCHASING", "PURCHASED", "INTL_SHIPPING", "CUSTOMS_CLEARANCE", "WAREHOUSE_INSPECTION", "DOMESTIC_SHIPPING")
        )));

        summary.setCustomsPendingCount(safeCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getStatus, "INTL_SHIPPING", "CUSTOMS_CLEARANCE", "WAREHOUSE_INSPECTION")
        )));
    }

    private Integer safeCount(Long count) {
        if (count == null || count <= 0) {
            return 0;
        }
        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return count.intValue();
    }

    private List<AdminDashboardTrendPointDTO> buildTrend(List<AdminDashboardTrendPointDTO> rawTrend,
                                                         Integer normalizedDays,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        List<AdminDashboardTrendPointDTO> source = rawTrend == null ? Collections.emptyList() : rawTrend;
        fillTrendDefaults(source);

        if (normalizedDays == null || startDate == null || endDate == null) {
            return source;
        }

        Map<String, AdminDashboardTrendPointDTO> indexedTrend = new LinkedHashMap<>();
        for (AdminDashboardTrendPointDTO point : source) {
            indexedTrend.put(point.getDateValue(), point);
        }

        List<AdminDashboardTrendPointDTO> filledTrend = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String dateValue = cursor.toString();
            AdminDashboardTrendPointDTO existing = indexedTrend.get(dateValue);
            if (existing != null) {
                filledTrend.add(existing);
            } else {
                AdminDashboardTrendPointDTO point = new AdminDashboardTrendPointDTO();
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

    private void fillTrendDefaults(List<AdminDashboardTrendPointDTO> trend) {
        for (AdminDashboardTrendPointDTO point : trend) {
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
