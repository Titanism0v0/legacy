package com.overseas.purchase.service;

import com.overseas.purchase.entity.Order;
import com.overseas.purchase.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationService {

    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    private final ChatWebSocketHandler chatWebSocketHandler;

    public void notifyOrderStatusChanged(Order order, String statusLabel) {
        if (order == null) {
            return;
        }
        Map<String, Object> payload = buildOrderPayload(order, statusLabel);
        pushQuietly(order.getBuyerId(), payload);
        pushQuietly(order.getSellerId(), payload);
    }

    private Map<String, Object> buildOrderPayload(Order order, String statusLabel) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("status", order.getStatus());
        payload.put("statusLabel", statusLabel == null ? order.getStatus() : statusLabel);
        payload.put("crossborderTrackingNumber", order.getCrossborderTrackingNumber());
        payload.put("domesticTrackingNumber", order.getDomesticTrackingNumber());
        return payload;
    }

    private void pushQuietly(Long userId, Map<String, Object> payload) {
        try {
            chatWebSocketHandler.pushEvent(userId, ORDER_STATUS_CHANGED, payload);
        } catch (Exception e) {
            log.warn("Realtime order notification failed, userId={}, orderId={}", userId, payload.get("orderId"), e);
        }
    }
}
