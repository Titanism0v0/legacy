package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.PaymentTxn;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.PaymentTxnMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CHANNEL_MANUAL_QR = "MANUAL_QR";

    private final OrderMapper orderMapper;
    private final PaymentTxnMapper paymentTxnMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> prepay(Long orderId, Long userId, String role) {
        Order order = getOrder(orderId);
        if (!hasOrderAccess(order, userId, role)) {
            throw new RuntimeException("No permission");
        }
        if ("PENDING_SHIPMENT".equals(order.getStatus())
                || "SHIPPED".equals(order.getStatus())
                || "COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("Current order status does not support payment");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PAYMENT_PROCESSING".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for payment");
        }

        User seller = userMapper.selectById(order.getSellerId());
        if (seller == null || seller.getDeleted() == 1) {
            throw new RuntimeException("Seller does not exist");
        }
        String paymentQrUrl = readKycField(seller.getKycFiles(), "paymentQrUrl");
        if (!StringUtils.hasText(paymentQrUrl)) {
            throw new RuntimeException("Seller payment QR code is not configured");
        }

        PaymentTxn txn = new PaymentTxn();
        txn.setOrderId(order.getId());
        txn.setChannel(CHANNEL_MANUAL_QR);
        txn.setOutTradeNo(buildOutTradeNo(order.getOrderNo()));
        txn.setAmount(order.getTotalPrice());
        txn.setStatus("AWAIT_BUYER_TRANSFER");
        txn.setQrCodeUrl(paymentQrUrl);
        txn.setExpireTime(LocalDateTime.now().plusMinutes(30));
        txn.setCreateTime(LocalDateTime.now());
        txn.setUpdateTime(LocalDateTime.now());
        txn.setDeleted(0);
        paymentTxnMapper.insert(txn);

        orderService.markPaymentProcessing(order.getId(), CHANNEL_MANUAL_QR);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getTotalPrice());
        result.put("channel", CHANNEL_MANUAL_QR);
        result.put("status", txn.getStatus());
        result.put("expireTime", txn.getExpireTime());
        result.put("receiverName", resolveSellerName(seller));
        result.put("sellerPaymentQrUrl", paymentQrUrl);
        result.put("qrCodeImage", paymentQrUrl);
        result.put("paymentTip", "Scan the seller QR code, complete transfer, then upload payment proof.");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatus(Long orderId, Long userId, String role) {
        Order order = getOrder(orderId);
        if (!hasOrderAccess(order, userId, role)) {
            throw new RuntimeException("No permission");
        }

        PaymentTxn txn = latestTxnByOrderId(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderStatus", order.getStatus());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("verifiedTime", order.getPaymentVerifiedTime());
        if (txn != null) {
            result.put("txnStatus", txn.getStatus());
            result.put("channel", txn.getChannel());
            result.put("expireTime", txn.getExpireTime());
            result.put("outTradeNo", txn.getOutTradeNo());
        } else {
            result.put("txnStatus", "UNPAID");
        }
        return result;
    }

    @Transactional
    public void confirmPayment(Long orderId, String paymentProof, Long userId, String role) {
        Order order = getOrder(orderId);
        if (!hasOrderAccess(order, userId, role)) {
            throw new RuntimeException("No permission");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PAYMENT_PROCESSING".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for payment confirmation");
        }
        if (!StringUtils.hasText(paymentProof)) {
            throw new RuntimeException("Payment proof is required");
        }

        PaymentTxn txn = latestTxnByOrderId(orderId);
        if (txn == null) {
            User seller = userMapper.selectById(order.getSellerId());
            String paymentQrUrl = seller == null ? null : readKycField(seller.getKycFiles(), "paymentQrUrl");
            txn = new PaymentTxn();
            txn.setOrderId(order.getId());
            txn.setChannel(CHANNEL_MANUAL_QR);
            txn.setOutTradeNo(buildOutTradeNo(order.getOrderNo()));
            txn.setAmount(order.getTotalPrice());
            txn.setStatus("SUBMITTED");
            txn.setQrCodeUrl(paymentQrUrl);
            txn.setCreateTime(LocalDateTime.now());
            txn.setUpdateTime(LocalDateTime.now());
            txn.setDeleted(0);
            paymentTxnMapper.insert(txn);
        } else {
            txn.setStatus("SUBMITTED");
            txn.setNotifyRaw(paymentProof);
            txn.setNotifyTime(LocalDateTime.now());
            txn.setUpdateTime(LocalDateTime.now());
            paymentTxnMapper.updateById(txn);
        }

        orderService.markPaymentSubmitted(orderId, CHANNEL_MANUAL_QR, paymentProof);
    }

    @Transactional
    public Map<String, Object> refund(Long orderId, String reason, Long operatorId, String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("No permission");
        }
        Order order = getOrder(orderId);
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new RuntimeException("Order has not been approved as paid");
        }
        PaymentTxn txn = latestTxnByOrderId(orderId);
        if (txn != null) {
            txn.setRefundStatus("MANUAL_REVIEW");
            txn.setUpdateTime(LocalDateTime.now());
            paymentTxnMapper.updateById(txn);
        }
        orderService.updateRefundSnapshot(orderId, "REFUND_PROCESSING", order.getTotalPrice());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("refundStatus", "MANUAL_REVIEW");
        result.put("reason", StringUtils.hasText(reason) ? reason : "manual refund review");
        result.put("channel", CHANNEL_MANUAL_QR);
        return result;
    }

    @Transactional
    public Map<String, Object> generatePaymentQRCode(Long orderId, Long userId, String role) {
        return prepay(orderId, userId, role);
    }

    public Map<String, String> handleWechatNotify(String body, Map<String, String> headers) {
        return failNotifyResponse("Official notify is disabled in manual QR mode");
    }

    public String handleAlipayNotify(Map<String, String> params) {
        return "failure";
    }

    private Order getOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        return order;
    }

    private PaymentTxn latestTxnByOrderId(Long orderId) {
        return paymentTxnMapper.selectOne(new LambdaQueryWrapper<PaymentTxn>()
                .eq(PaymentTxn::getOrderId, orderId)
                .eq(PaymentTxn::getDeleted, 0)
                .orderByDesc(PaymentTxn::getId)
                .last("LIMIT 1"));
    }

    private boolean hasOrderAccess(Order order, Long userId, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return userId.equals(order.getBuyerId());
    }

    private String buildOutTradeNo(String orderNo) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String raw = orderNo + suffix;
        return raw.length() > 32 ? raw.substring(raw.length() - 32) : raw;
    }

    private String resolveSellerName(User seller) {
        if (seller == null) {
            return "Seller";
        }
        if (StringUtils.hasText(seller.getNickname())) {
            return seller.getNickname();
        }
        if (StringUtils.hasText(seller.getUsername())) {
            return seller.getUsername();
        }
        return "Seller";
    }

    private String readKycField(String kycFiles, String fieldName) {
        if (!StringUtils.hasText(kycFiles)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(kycFiles);
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                return null;
            }
            String value = field.asText();
            return StringUtils.hasText(value) ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> failNotifyResponse(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("code", "FAIL");
        map.put("message", message == null ? "Failed" : message);
        return map;
    }
}
