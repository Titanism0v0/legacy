package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.PaymentTxn;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.PaymentTxnMapper;
import com.overseas.purchase.mapper.UserMapper;
import com.overseas.purchase.service.payment.PaymentProvider;
import com.overseas.purchase.service.payment.PaymentProviderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CHANNEL_LEGACY_QR = "LEGACY_QR";
    private static final String CHANNEL_ALIPAY = "ALIPAY";
    private static final String WECHAT_NOTIFY_SUCCESS = "SUCCESS";
    private static final String ALIPAY_NOTIFY_SUCCESS = "success";
    private static final String VERIFY_FAILED_STATUS = "VERIFY_FAILED";
    private static final int QR_CODE_SIZE = 320;

    private final OrderMapper orderMapper;
    private final PaymentTxnMapper paymentTxnMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;
    private final PaymentProviderFactory paymentProviderFactory;
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

        PaymentProvider provider = paymentProviderFactory.currentProvider();
        PaymentTxn txn = findReusableTxn(orderId, provider.channel());
        if (txn == null) {
            if (requiresPaymentProof(provider.channel())) {
                txn = buildLegacyTxn(order);
            } else {
                String outTradeNo = buildOutTradeNo(order.getOrderNo());
                PaymentProvider.PrepayResult prepayResult = provider.prepay(order, outTradeNo);
                txn = buildTxn(order, provider.channel(), outTradeNo, prepayResult);
            }
            paymentTxnMapper.insert(txn);
        }

        orderService.markPaymentProcessing(order.getId(), provider.channel());
        return buildPrepayResult(order, txn, provider.channel());
    }

    @Transactional
    public Map<String, Object> getPaymentStatus(Long orderId, Long userId, String role) {
        Order order = getOrder(orderId);
        if (!hasOrderAccess(order, userId, role)) {
            throw new RuntimeException("No permission");
        }

        syncOrderPaymentState(order);
        order = getOrder(orderId);
        PaymentTxn txn = resolveDisplayTxn(orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderStatus", order.getStatus());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("verifiedTime", order.getPaymentVerifiedTime());
        result.put("paid", "PAID".equalsIgnoreCase(order.getPaymentStatus()));
        if (txn != null) {
            result.put("txnStatus", txn.getStatus());
            result.put("channel", txn.getChannel());
            result.put("expireTime", txn.getExpireTime());
            result.put("outTradeNo", txn.getOutTradeNo());
            result.put("gatewayTradeNo", txn.getGatewayTradeNo());
            result.put("requiresPaymentProof", requiresPaymentProof(txn.getChannel()));
        } else {
            result.put("txnStatus", "UNPAID");
            result.put("requiresPaymentProof", false);
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

        PaymentTxn txn = latestTxnByOrderId(orderId);
        String channel = txn == null ? paymentProviderFactory.currentProvider().channel() : txn.getChannel();
        if (!requiresPaymentProof(channel)) {
            throw new RuntimeException("Current payment channel does not require manual payment proof");
        }
        if (!StringUtils.hasText(paymentProof)) {
            throw new RuntimeException("Payment proof is required");
        }

        if (txn == null) {
            User seller = userMapper.selectById(order.getSellerId());
            String paymentQrUrl = seller == null ? null : readKycField(seller.getKycFiles(), "paymentQrUrl");
            txn = new PaymentTxn();
            txn.setOrderId(order.getId());
            txn.setChannel(CHANNEL_LEGACY_QR);
            txn.setOutTradeNo(buildOutTradeNo(order.getOrderNo()));
            txn.setAmount(order.getTotalPrice());
            txn.setCurrency(resolveCurrency(order));
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

        orderService.markPaymentSubmitted(orderId, CHANNEL_LEGACY_QR, paymentProof);
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
        if (txn == null || requiresPaymentProof(txn.getChannel())) {
            return markManualRefund(orderId, order, txn, reason);
        }

        PaymentProvider provider = paymentProviderFactory.providerFor(txn.getChannel());
        PaymentProvider.RefundResult refundResult = provider.refund(
                order,
                txn.getOutTradeNo(),
                buildOutRefundNo(order.getOrderNo()),
                reason
        );
        txn.setRefundStatus(refundResult.getStatus());
        txn.setUpdateTime(LocalDateTime.now());
        paymentTxnMapper.updateById(txn);

        String snapshotStatus = isRefundSuccess(refundResult.getStatus()) ? "REFUND_APPROVED" : "REFUND_PROCESSING";
        orderService.updateRefundSnapshot(orderId, snapshotStatus, order.getTotalPrice());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("refundStatus", refundResult.getStatus());
        result.put("reason", StringUtils.hasText(reason) ? reason : "after-sales refund");
        result.put("channel", txn.getChannel());
        return result;
    }

    @Transactional
    public Map<String, Object> generatePaymentQRCode(Long orderId, Long userId, String role) {
        Order order = getOrder(orderId);
        if (hasOrderAccess(order, userId, role)) {
            syncOrderPaymentState(order);
        }
        return prepay(orderId, userId, role);
    }

    @Transactional
    public Map<String, String> handleWechatNotify(String body, Map<String, String> headers) {
        PaymentProvider provider = paymentProviderFactory.providerFor("WECHAT_NATIVE");
        PaymentProvider.NotifyResult notifyResult = provider.parseNotify(body, headers, null);
        if (!notifyResult.isValid()) {
            return failNotifyResponse(notifyResult.getMessage());
        }
        return processNotifyResult(CHANNEL_LEGACY_QR.equals(provider.channel()) ? null : provider.channel(), notifyResult)
                ? successWechatNotifyResponse()
                : failNotifyResponse("Notify process failed");
    }

    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        PaymentProvider provider = paymentProviderFactory.providerFor(CHANNEL_ALIPAY);
        PaymentProvider.NotifyResult notifyResult = provider.parseNotify(null, null, params);
        if (!notifyResult.isValid()) {
            return "failure";
        }
        return processNotifyResult(provider.channel(), notifyResult) ? ALIPAY_NOTIFY_SUCCESS : "failure";
    }

    private Map<String, Object> buildPrepayResult(Order order, PaymentTxn txn, String channel) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getTotalPrice());
        result.put("currency", resolveCurrency(order));
        result.put("channel", channel);
        result.put("status", txn.getStatus());
        result.put("txnStatus", txn.getStatus());
        result.put("expireTime", txn.getExpireTime());
        result.put("outTradeNo", txn.getOutTradeNo());
        result.put("requiresPaymentProof", requiresPaymentProof(channel));
        result.put("paymentTip", resolvePaymentTip(channel));

        if (requiresPaymentProof(channel)) {
            User seller = userMapper.selectById(order.getSellerId());
            String paymentQrUrl = txn.getQrCodeUrl();
            result.put("receiverName", resolveSellerName(seller));
            result.put("sellerPaymentQrUrl", paymentQrUrl);
            result.put("qrCodeImage", paymentQrUrl);
            result.put("qrCodeUrl", paymentQrUrl);
        } else {
            result.put("qrCodeUrl", txn.getQrCodeUrl());
            result.put("qrCodeImage", renderQrCodeImage(txn.getQrCodeUrl()));
        }
        return result;
    }

    private PaymentTxn buildTxn(Order order, String channel, String outTradeNo, PaymentProvider.PrepayResult prepayResult) {
        PaymentTxn txn = new PaymentTxn();
        txn.setOrderId(order.getId());
        txn.setChannel(channel);
        txn.setOutTradeNo(outTradeNo);
        txn.setAmount(order.getTotalPrice());
        txn.setCurrency(resolveCurrency(order));
        txn.setStatus(defaultTxnStatus(channel));
        txn.setQrCodeUrl(prepayResult.getQrCodeUrl());
        txn.setExpireTime(prepayResult.getExpireTime());
        txn.setCreateTime(LocalDateTime.now());
        txn.setUpdateTime(LocalDateTime.now());
        txn.setDeleted(0);
        return txn;
    }

    private PaymentTxn buildLegacyTxn(Order order) {
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
        txn.setChannel(CHANNEL_LEGACY_QR);
        txn.setOutTradeNo(buildOutTradeNo(order.getOrderNo()));
        txn.setAmount(order.getTotalPrice());
        txn.setCurrency(resolveCurrency(order));
        txn.setStatus("AWAIT_BUYER_TRANSFER");
        txn.setQrCodeUrl(paymentQrUrl);
        txn.setExpireTime(LocalDateTime.now().plusMinutes(30));
        txn.setCreateTime(LocalDateTime.now());
        txn.setUpdateTime(LocalDateTime.now());
        txn.setDeleted(0);
        return txn;
    }

    private boolean processNotifyResult(String channel, PaymentProvider.NotifyResult notifyResult) {
        PaymentTxn txn = findTxnByOutTradeNo(notifyResult.getOutTradeNo());
        if (txn == null) {
            return false;
        }

        Order order = getOrder(txn.getOrderId());
        String effectiveChannel = channel == null ? txn.getChannel() : channel;
        String normalizedStatus = normalizeTxnStatus(notifyResult.getStatus(), effectiveChannel);
        txn.setGatewayTradeNo(StringUtils.hasText(notifyResult.getGatewayTradeNo())
                ? notifyResult.getGatewayTradeNo() : txn.getGatewayTradeNo());
        txn.setNotifyRaw(notifyResult.getRawPayload());
        txn.setNotifyTime(LocalDateTime.now());
        txn.setUpdateTime(LocalDateTime.now());

        if (isSuccessStatus(normalizedStatus)
                && !paymentResultMatches(order, txn, notifyResult.getPaidAmountFen(), notifyResult.getCurrency())) {
            txn.setStatus(VERIFY_FAILED_STATUS);
            paymentTxnMapper.updateById(txn);
            return false;
        }

        txn.setStatus(normalizedStatus);
        paymentTxnMapper.updateById(txn);

        if (isSuccessStatus(normalizedStatus)) {
            orderService.markPaid(order.getId(), txn.getChannel());
        }
        return true;
    }

    private void syncTxnStatus(Order order, PaymentTxn txn) {
        PaymentProvider provider = paymentProviderFactory.providerFor(txn.getChannel());
        PaymentProvider.QueryResult queryResult = provider.query(txn.getOutTradeNo());
        String normalizedStatus = normalizeTxnStatus(queryResult.getStatus(), txn.getChannel());

        txn.setGatewayTradeNo(StringUtils.hasText(queryResult.getGatewayTradeNo())
                ? queryResult.getGatewayTradeNo() : txn.getGatewayTradeNo());
        txn.setUpdateTime(LocalDateTime.now());

        if (isSuccessStatus(normalizedStatus) && !amountMatches(order.getTotalPrice(), queryResult.getPaidAmountFen())) {
            txn.setStatus(VERIFY_FAILED_STATUS);
            paymentTxnMapper.updateById(txn);
            return;
        }

        txn.setStatus(normalizedStatus);
        paymentTxnMapper.updateById(txn);

        if (isSuccessStatus(normalizedStatus) && amountMatches(order.getTotalPrice(), queryResult.getPaidAmountFen())) {
            orderService.markPaid(order.getId(), txn.getChannel());
        }
    }

    private void syncOrderPaymentState(Order order) {
        if (order == null || "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return;
        }
        List<PaymentTxn> txns = recentTxnsByOrderId(order.getId());
        for (PaymentTxn txn : txns) {
            if (txn == null || !supportsGatewaySync(txn.getChannel()) || !shouldSyncTxn(txn, order)) {
                continue;
            }
            try {
                syncTxnStatus(order, txn);
                Order refreshed = getOrder(order.getId());
                if ("PAID".equalsIgnoreCase(refreshed.getPaymentStatus())) {
                    return;
                }
            } catch (RuntimeException e) {
                if (looksLikeTradeNotFound(e)) {
                    continue;
                }
                throw e;
            }
        }
    }

    private PaymentTxn findReusableTxn(Long orderId, String channel) {
        List<PaymentTxn> txns = recentTxnsByOrderId(orderId);
        for (PaymentTxn txn : txns) {
            if (!channel.equalsIgnoreCase(txn.getChannel())) {
                continue;
            }
            if (!StringUtils.hasText(txn.getQrCodeUrl())) {
                continue;
            }
            if (!isPendingStatus(txn.getStatus())) {
                continue;
            }
            if (txn.getExpireTime() != null && txn.getExpireTime().isBefore(LocalDateTime.now())) {
                continue;
            }
            return txn;
        }
        return null;
    }

    private boolean shouldSyncTxn(PaymentTxn txn, Order order) {
        if (txn == null || !StringUtils.hasText(txn.getOutTradeNo())) {
            return false;
        }
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return false;
        }
        return isPendingStatus(txn.getStatus());
    }

    private boolean supportsGatewaySync(String channel) {
        return StringUtils.hasText(channel) && !CHANNEL_LEGACY_QR.equalsIgnoreCase(channel);
    }

    private String defaultTxnStatus(String channel) {
        if (CHANNEL_ALIPAY.equalsIgnoreCase(channel)) {
            return "WAIT_BUYER_PAY";
        }
        if ("WECHAT_NATIVE".equalsIgnoreCase(channel)) {
            return "NOTPAY";
        }
        return "AWAIT_BUYER_TRANSFER";
    }

    private String normalizeTxnStatus(String status, String channel) {
        if (!StringUtils.hasText(status)) {
            return "UNKNOWN";
        }
        String normalized = status.trim().toUpperCase();
        if (CHANNEL_ALIPAY.equalsIgnoreCase(channel)) {
            if ("TRADE_SUCCESS".equals(normalized) || "TRADE_FINISHED".equals(normalized)) {
                return "SUCCESS";
            }
            if ("WAIT_BUYER_PAY".equals(normalized)) {
                return "WAIT_BUYER_PAY";
            }
            if ("TRADE_CLOSED".equals(normalized)) {
                return "CLOSED";
            }
        }
        if ("WECHAT_NATIVE".equalsIgnoreCase(channel)) {
            if ("SUCCESS".equals(normalized)) {
                return "SUCCESS";
            }
            if ("NOTPAY".equals(normalized) || "USERPAYING".equals(normalized)) {
                return normalized;
            }
            if ("CLOSED".equals(normalized) || "PAYERROR".equals(normalized) || "REVOKED".equals(normalized)) {
                return "CLOSED";
            }
        }
        return normalized;
    }

    private boolean isPendingStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "WAIT_BUYER_PAY".equals(normalized)
                || "NOTPAY".equals(normalized)
                || "USERPAYING".equals(normalized)
                || "AWAIT_BUYER_TRANSFER".equals(normalized)
                || "SUBMITTED".equals(normalized)
                || "PROCESSING".equals(normalized);
    }

    private boolean isSuccessStatus(String status) {
        return StringUtils.hasText(status) && "SUCCESS".equalsIgnoreCase(status);
    }

    private boolean isRefundSuccess(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "SUCCESS".equals(normalized) || "REFUND_SUCCESS".equals(normalized);
    }

    private boolean requiresPaymentProof(String channel) {
        return CHANNEL_LEGACY_QR.equalsIgnoreCase(channel);
    }

    private String resolvePaymentTip(String channel) {
        if (CHANNEL_ALIPAY.equalsIgnoreCase(channel)) {
            return "Open the Alipay sandbox app and scan the QR code to complete payment.";
        }
        if ("WECHAT_NATIVE".equalsIgnoreCase(channel)) {
            return "Open WeChat and scan the QR code to complete payment.";
        }
        return "Scan the seller QR code, complete transfer, then upload payment proof.";
    }

    private String renderQrCodeImage(String qrContent) {
        if (!StringUtils.hasText(qrContent)) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean amountMatches(BigDecimal amount, Integer paidAmountFen) {
        if (paidAmountFen == null) {
            return false;
        }
        return amountToFen(amount) == paidAmountFen;
    }

    private boolean paymentResultMatches(Order order, PaymentTxn txn, Integer paidAmountFen, String currency) {
        return amountMatches(order.getTotalPrice(), paidAmountFen)
                && currencyMatches(resolveTxnCurrency(order, txn), currency);
    }

    private boolean currencyMatches(String expectedCurrency, String actualCurrency) {
        return StringUtils.hasText(expectedCurrency)
                && StringUtils.hasText(actualCurrency)
                && expectedCurrency.trim().equalsIgnoreCase(actualCurrency.trim());
    }

    private int amountToFen(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount;
        return normalized.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
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

    private List<PaymentTxn> recentTxnsByOrderId(Long orderId) {
        List<PaymentTxn> txns = paymentTxnMapper.selectList(new LambdaQueryWrapper<PaymentTxn>()
                .eq(PaymentTxn::getOrderId, orderId)
                .eq(PaymentTxn::getDeleted, 0)
                .orderByDesc(PaymentTxn::getId)
                .last("LIMIT 10"));
        return txns == null ? new ArrayList<>() : txns;
    }

    private PaymentTxn resolveDisplayTxn(Long orderId) {
        List<PaymentTxn> txns = recentTxnsByOrderId(orderId);
        if (txns.isEmpty()) {
            return null;
        }
        for (PaymentTxn txn : txns) {
            if (txn != null && (isSuccessStatus(txn.getStatus()) || StringUtils.hasText(txn.getGatewayTradeNo()))) {
                return txn;
            }
        }
        return txns.get(0);
    }

    private boolean looksLikeTradeNotFound(RuntimeException e) {
        if (e == null || !StringUtils.hasText(e.getMessage())) {
            return false;
        }
        String message = e.getMessage();
        return message.contains("交易不存在") || message.toLowerCase().contains("trade not exist");
    }

    private PaymentTxn findTxnByOutTradeNo(String outTradeNo) {
        if (!StringUtils.hasText(outTradeNo)) {
            return null;
        }
        return paymentTxnMapper.selectOne(new LambdaQueryWrapper<PaymentTxn>()
                .eq(PaymentTxn::getOutTradeNo, outTradeNo)
                .eq(PaymentTxn::getDeleted, 0)
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

    private String buildOutRefundNo(String orderNo) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String raw = "R" + orderNo + suffix;
        return raw.length() > 64 ? raw.substring(raw.length() - 64) : raw;
    }

    private String resolveCurrency(Order order) {
        return order.getPaymentCurrencySnapshot() == null ? "CNY" : order.getPaymentCurrencySnapshot();
    }

    private String resolveTxnCurrency(Order order, PaymentTxn txn) {
        return StringUtils.hasText(txn.getCurrency()) ? txn.getCurrency() : resolveCurrency(order);
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

    private Map<String, Object> markManualRefund(Long orderId, Order order, PaymentTxn txn, String reason) {
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
        result.put("channel", txn == null ? CHANNEL_LEGACY_QR : txn.getChannel());
        return result;
    }

    private Map<String, String> successWechatNotifyResponse() {
        Map<String, String> map = new HashMap<>();
        map.put("code", WECHAT_NOTIFY_SUCCESS);
        map.put("message", "成功");
        return map;
    }

    private Map<String, String> failNotifyResponse(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("code", "FAIL");
        map.put("message", message == null ? "Failed" : message);
        return map;
    }
}
