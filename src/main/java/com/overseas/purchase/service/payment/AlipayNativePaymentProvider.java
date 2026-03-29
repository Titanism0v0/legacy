package com.overseas.purchase.service.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AlipayNativePaymentProvider implements PaymentProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${payment.alipay.gateway:https://openapi-sandbox.dl.alipaydev.com/gateway.do}")
    private String gateway;
    @Value("${payment.alipay.app-id:}")
    private String appId;
    @Value("${payment.alipay.private-key:}")
    private String privateKey;
    @Value("${payment.alipay.private-key-path:}")
    private String privateKeyPath;
    @Value("${payment.alipay.public-key:}")
    private String publicKey;
    @Value("${payment.alipay.public-key-path:}")
    private String publicKeyPath;
    @Value("${payment.alipay.notify-url:}")
    private String notifyUrl;
    @Value("${payment.alipay.charset:UTF-8}")
    private String charset;
    @Value("${payment.alipay.sign-type:RSA2}")
    private String signType;
    @Value("${payment.alipay.timeout-minutes:15}")
    private Integer timeoutMinutes;

    private volatile AlipayClient alipayClient;

    @Override
    public String channel() {
        return "ALIPAY";
    }

    @Override
    public PrepayResult prepay(Order order, String outTradeNo) {
        validateBaseConfig();
        try {
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            if (StringUtils.hasText(notifyUrl)) {
                request.setNotifyUrl(notifyUrl);
            }

            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(outTradeNo);
            model.setTotalAmount(formatAmount(order.getTotalPrice()));
            model.setSubject(buildSubject(order));
            model.setTimeoutExpress(resolveTimeoutExpress());
            request.setBizModel(model);

            AlipayTradePrecreateResponse response = client().execute(request);
            if (response == null || !response.isSuccess() || !StringUtils.hasText(response.getQrCode())) {
                throw new RuntimeException(buildResponseError(response, "Alipay precreate failed"));
            }
            return new PrepayResult(
                    response.getQrCode(),
                    LocalDateTime.now().plusMinutes(resolveTimeoutMinutes()),
                    response.getBody()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Alipay prepay order: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryResult query(String outTradeNo) {
        validateBaseConfig();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(outTradeNo);
            request.setBizModel(model);

            AlipayTradeQueryResponse response = client().execute(request);
            if (response == null || !response.isSuccess()) {
                throw new RuntimeException(buildResponseError(response, "Alipay query failed"));
            }
            return new QueryResult(
                    mapTradeStatus(response.getTradeStatus()),
                    response.getTradeNo(),
                    toFen(response.getTotalAmount()),
                    response.getBody()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to query Alipay payment status: " + e.getMessage(), e);
        }
    }

    @Override
    public NotifyResult parseNotify(String body, Map<String, String> headers, Map<String, String> params) {
        try {
            if (params == null || params.isEmpty()) {
                return new NotifyResult(false, null, null, null, null, body, "Missing Alipay callback params");
            }
            Map<String, String> notifyParams = new HashMap<>(params);
            boolean verified = AlipaySignature.rsaCheckV1(
                    notifyParams,
                    readKey(publicKey, publicKeyPath),
                    charset,
                    signType
            );
            if (!verified) {
                return new NotifyResult(false, null, null, null, null, rawNotifyPayload(body, notifyParams),
                        "Invalid Alipay callback signature");
            }

            String outTradeNo = notifyParams.get("out_trade_no");
            String tradeNo = notifyParams.get("trade_no");
            String tradeStatus = mapTradeStatus(notifyParams.get("trade_status"));
            Integer paidAmountFen = toFen(notifyParams.get("total_amount"));
            if (!StringUtils.hasText(outTradeNo)) {
                return new NotifyResult(false, null, tradeNo, tradeStatus, paidAmountFen,
                        rawNotifyPayload(body, notifyParams), "Missing out_trade_no");
            }

            return new NotifyResult(
                    true,
                    outTradeNo,
                    tradeNo,
                    tradeStatus,
                    paidAmountFen,
                    rawNotifyPayload(body, notifyParams),
                    "OK"
            );
        } catch (Exception e) {
            return new NotifyResult(false, null, null, null, null, body,
                    "Failed to parse Alipay notify: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refund(Order order, String outTradeNo, String outRefundNo, String reason) {
        validateBaseConfig();
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(outTradeNo);
            model.setOutRequestNo(outRefundNo);
            model.setRefundAmount(formatAmount(order.getTotalPrice()));
            if (StringUtils.hasText(reason)) {
                model.setRefundReason(reason);
            }
            request.setBizModel(model);

            AlipayTradeRefundResponse response = client().execute(request);
            if (response == null || !response.isSuccess()) {
                throw new RuntimeException(buildResponseError(response, "Alipay refund failed"));
            }

            String status = "Y".equalsIgnoreCase(response.getFundChange()) ? "SUCCESS" : "PROCESSING";
            return new RefundResult(status, response.getTradeNo(), response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Alipay refund: " + e.getMessage(), e);
        }
    }

    private AlipayClient client() {
        if (alipayClient != null) {
            return alipayClient;
        }
        synchronized (this) {
            if (alipayClient == null) {
                alipayClient = new DefaultAlipayClient(
                        gateway,
                        appId,
                        readKey(privateKey, privateKeyPath),
                        "json",
                        charset,
                        readKey(publicKey, publicKeyPath),
                        signType
                );
            }
            return alipayClient;
        }
    }

    private String rawNotifyPayload(String body, Map<String, String> params) throws Exception {
        if (StringUtils.hasText(body)) {
            return body;
        }
        return objectMapper.writeValueAsString(params);
    }

    private String buildSubject(Order order) {
        String base = "Overseas order " + order.getOrderNo();
        return base.length() > 256 ? base.substring(0, 256) : base;
    }

    private String resolveTimeoutExpress() {
        return resolveTimeoutMinutes() + "m";
    }

    private int resolveTimeoutMinutes() {
        return timeoutMinutes == null || timeoutMinutes <= 0 ? 15 : timeoutMinutes;
    }

    private String mapTradeStatus(String tradeStatus) {
        if (!StringUtils.hasText(tradeStatus)) {
            return "UNKNOWN";
        }
        if ("WAIT_BUYER_PAY".equalsIgnoreCase(tradeStatus)) {
            return "NOTPAY";
        }
        if ("TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
            return "SUCCESS";
        }
        if ("TRADE_CLOSED".equalsIgnoreCase(tradeStatus)) {
            return "CLOSED";
        }
        return tradeStatus;
    }

    private Integer toFen(String amount) {
        if (!StringUtils.hasText(amount)) {
            return null;
        }
        return new BigDecimal(amount).multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        return normalized.toPlainString();
    }

    private String buildResponseError(com.alipay.api.AlipayResponse response, String fallback) {
        if (response == null) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder(fallback);
        if (StringUtils.hasText(response.getSubMsg())) {
            builder.append(": ").append(response.getSubMsg());
        } else if (StringUtils.hasText(response.getMsg())) {
            builder.append(": ").append(response.getMsg());
        }
        return builder.toString();
    }

    private void validateBaseConfig() {
        Map<String, String> missingItems = new HashMap<>();
        if (!StringUtils.hasText(appId)) {
            missingItems.put("app-id", "app-id");
        }
        if (!hasKey(privateKey, privateKeyPath)) {
            missingItems.put("private-key", "private-key/private-key-path");
        }
        if (!hasKey(publicKey, publicKeyPath)) {
            missingItems.put("public-key", "public-key/public-key-path");
        }
        if (!missingItems.isEmpty()) {
            throw new RuntimeException("Alipay config incomplete: missing "
                    + String.join(", ", missingItems.values()));
        }
    }

    private boolean hasKey(String directContent, String path) {
        return StringUtils.hasText(directContent) || StringUtils.hasText(path);
    }

    private String readKey(String directContent, String path) {
        try {
            if (StringUtils.hasText(directContent)) {
                return normalizeKey(directContent);
            }
            if (!StringUtils.hasText(path)) {
                return null;
            }
            String resourcePath = path.startsWith("classpath:") || path.startsWith("file:")
                    ? path : "file:" + path;
            Resource resource = resourceLoader.getResource(resourcePath);
            if (!resource.exists()) {
                throw new RuntimeException("Key file not found: " + path);
            }
            return normalizeKey(new String(readAllBytes(resource.getInputStream()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load key: " + e.getMessage(), e);
        }
    }

    private String normalizeKey(String content) {
        return content == null ? null : content.replace("\\n", "\n").trim();
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
}
