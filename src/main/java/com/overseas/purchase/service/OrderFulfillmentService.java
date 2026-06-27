package com.overseas.purchase.service;

import com.overseas.purchase.dto.OrderStatusStepDTO;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.mapper.OrderMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {

    private static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String PAYMENT_PROCESSING = "PAYMENT_PROCESSING";
    private static final String PENDING_AUDIT = "PENDING_AUDIT";
    private static final String PENDING_SHIPMENT = "PENDING_SHIPMENT";
    private static final String PURCHASING = "PURCHASING";
    private static final String PURCHASED = "PURCHASED";
    private static final String INTL_SHIPPING = "INTL_SHIPPING";
    private static final String CUSTOMS_CLEARANCE = "CUSTOMS_CLEARANCE";
    private static final String WAREHOUSE_INSPECTION = "WAREHOUSE_INSPECTION";
    private static final String DOMESTIC_SHIPPING = "DOMESTIC_SHIPPING";
    private static final String SHIPPED = "SHIPPED";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private static final List<StatusDefinition> FLOW = Collections.unmodifiableList(Arrays.asList(
            new StatusDefinition(PENDING_PAYMENT, "待支付", "支付", "用户提交订单后等待支付"),
            new StatusDefinition(PAYMENT_PROCESSING, "支付处理中", "支付", "平台或支付渠道确认交易状态"),
            new StatusDefinition(PENDING_AUDIT, "支付待审核", "审核", "平台审核付款凭证和订单风险"),
            new StatusDefinition(PENDING_SHIPMENT, "待代购", "履约", "支付确认后等待商家开始采购"),
            new StatusDefinition(PURCHASING, "代购采购中", "履约", "商家在海外渠道采购商品"),
            new StatusDefinition(PURCHASED, "已采购", "履约", "商家已上传或确认采购凭证"),
            new StatusDefinition(INTL_SHIPPING, "跨境运输中", "物流", "商品进入国际运输链路"),
            new StatusDefinition(CUSTOMS_CLEARANCE, "清关处理中", "物流", "商品等待或正在清关"),
            new StatusDefinition(WAREHOUSE_INSPECTION, "入库验货中", "物流", "商品到达国内仓并进行验货"),
            new StatusDefinition(DOMESTIC_SHIPPING, "国内配送中", "物流", "商品通过国内快递派送给用户"),
            new StatusDefinition(SHIPPED, "已发货", "物流", "兼容旧版已发货状态"),
            new StatusDefinition(COMPLETED, "交易完成", "完成", "用户确认收货或平台完成交易")
    ));

    private static final Map<String, StatusDefinition> STATUS_INDEX = buildStatusIndex();
    private static final Map<String, Integer> STATUS_ORDER = buildStatusOrder();

    private final OrderMapper orderMapper;
    private final RealtimeNotificationService realtimeNotificationService;

    public List<OrderStatusStepDTO> buildFlow(String currentStatus) {
        String normalizedStatus = normalizeStatus(currentStatus);
        int currentIndex = STATUS_ORDER.getOrDefault(normalizedStatus, -1);
        List<OrderStatusStepDTO> result = new ArrayList<>();
        for (int i = 0; i < FLOW.size(); i++) {
            StatusDefinition item = FLOW.get(i);
            result.add(new OrderStatusStepDTO(
                    item.getCode(),
                    item.getLabel(),
                    item.getStage(),
                    item.getDescription(),
                    currentIndex >= 0 && i < currentIndex,
                    item.getCode().equals(normalizedStatus)
            ));
        }
        if (CANCELLED.equals(normalizedStatus)) {
            result.add(new OrderStatusStepDTO(CANCELLED, "已取消", "关闭", "订单已取消，履约链路终止", false, true));
        }
        return result;
    }

    public Map<String, Object> describe(String currentStatus) {
        String normalizedStatus = normalizeStatus(currentStatus);
        StatusDefinition definition = STATUS_INDEX.get(normalizedStatus);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentStatus", normalizedStatus);
        result.put("currentLabel", definition == null ? normalizedStatus : definition.getLabel());
        result.put("currentStage", definition == null ? "UNKNOWN" : definition.getStage());
        result.put("flow", buildFlow(normalizedStatus));
        return result;
    }

    @Transactional
    public Order advance(Long orderId,
                         String targetStatus,
                         Long operatorId,
                         String role,
                         String crossborderTrackingNumber,
                         String domesticTrackingNumber,
                         String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        ensureOperatorCanAdvance(order, operatorId, role);

        String target = normalizeStatus(targetStatus);
        if (!STATUS_ORDER.containsKey(target) || CANCELLED.equals(target) || COMPLETED.equals(target)
                || PENDING_PAYMENT.equals(target) || PAYMENT_PROCESSING.equals(target) || PENDING_AUDIT.equals(target)) {
            throw new RuntimeException("Unsupported fulfillment target status");
        }

        String current = normalizeStatus(order.getStatus());
        ensureForwardTransition(current, target);

        if (INTL_SHIPPING.equals(target) && !StringUtils.hasText(crossborderTrackingNumber)
                && !StringUtils.hasText(order.getCrossborderTrackingNumber())) {
            throw new RuntimeException("Cross-border tracking number is required for international shipping");
        }
        if ((DOMESTIC_SHIPPING.equals(target) || SHIPPED.equals(target)) && !StringUtils.hasText(domesticTrackingNumber)
                && !StringUtils.hasText(order.getDomesticTrackingNumber()) && !StringUtils.hasText(order.getTrackingNumber())) {
            throw new RuntimeException("Domestic tracking number is required for domestic shipping");
        }

        if (StringUtils.hasText(crossborderTrackingNumber)) {
            order.setCrossborderTrackingNumber(crossborderTrackingNumber.trim());
        }
        if (StringUtils.hasText(domesticTrackingNumber)) {
            String normalizedDomestic = domesticTrackingNumber.trim();
            order.setDomesticTrackingNumber(normalizedDomestic);
            order.setTrackingNumber(normalizedDomestic);
        }
        if (StringUtils.hasText(remark)) {
            order.setRemark(appendRemark(order.getRemark(), target, remark));
        }

        order.setStatus(target);
        order.setCustomsClearanceStatus(resolveCustomsStatus(target, order.getCustomsClearanceStatus()));
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        realtimeNotificationService.notifyOrderStatusChanged(order, resolveStatusLabel(target));
        return order;
    }

    private void ensureOperatorCanAdvance(Order order, Long operatorId, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if (!"SELLER".equalsIgnoreCase(role) || operatorId == null || !operatorId.equals(order.getSellerId())) {
            throw new RuntimeException("No permission");
        }
    }

    private void ensureForwardTransition(String current, String target) {
        if (CANCELLED.equals(current) || COMPLETED.equals(current)) {
            throw new RuntimeException("Closed order cannot be advanced");
        }
        Integer currentIndex = STATUS_ORDER.get(current);
        Integer targetIndex = STATUS_ORDER.get(target);
        if (currentIndex == null || targetIndex == null) {
            throw new RuntimeException("Unknown order status");
        }
        if (targetIndex <= currentIndex) {
            throw new RuntimeException("Order status can only move forward");
        }
        if (targetIndex - currentIndex > 2 && !PENDING_SHIPMENT.equals(current)) {
            throw new RuntimeException("Target status skips too many fulfillment steps");
        }
    }

    private String resolveCustomsStatus(String target, String fallback) {
        if (INTL_SHIPPING.equals(target)) {
            return "IN_TRANSIT";
        }
        if (CUSTOMS_CLEARANCE.equals(target)) {
            return "CLEARING";
        }
        if (WAREHOUSE_INSPECTION.equals(target) || DOMESTIC_SHIPPING.equals(target) || SHIPPED.equals(target) || COMPLETED.equals(target)) {
            return "CLEARED";
        }
        return StringUtils.hasText(fallback) ? fallback : "PENDING_DECLARATION";
    }

    private String appendRemark(String oldRemark, String status, String remark) {
        String prefix = "[" + status + "] ";
        if (!StringUtils.hasText(oldRemark)) {
            return prefix + remark.trim();
        }
        return oldRemark.trim() + " | " + prefix + remark.trim();
    }

    private String resolveStatusLabel(String status) {
        StatusDefinition definition = STATUS_INDEX.get(normalizeStatus(status));
        return definition == null ? status : definition.getLabel();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return PENDING_PAYMENT;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("PENDING_SHIPMENT".equals(normalized)) {
            return PENDING_SHIPMENT;
        }
        return normalized;
    }

    private static Map<String, StatusDefinition> buildStatusIndex() {
        Map<String, StatusDefinition> map = new LinkedHashMap<>();
        for (StatusDefinition item : FLOW) {
            map.put(item.getCode(), item);
        }
        map.put(CANCELLED, new StatusDefinition(CANCELLED, "已取消", "关闭", "订单已取消"));
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Integer> buildStatusOrder() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < FLOW.size(); i++) {
            map.put(FLOW.get(i).getCode(), i);
        }
        return Collections.unmodifiableMap(map);
    }

    @Data
    @AllArgsConstructor
    private static class StatusDefinition {
        private String code;
        private String label;
        private String stage;
        private String description;
    }
}
