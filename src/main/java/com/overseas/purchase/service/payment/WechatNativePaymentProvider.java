package com.overseas.purchase.service.payment;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WechatNativePaymentProvider implements PaymentProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${payment.wechat.app-id:}")
    private String appId;
    @Value("${payment.wechat.mch-id:}")
    private String mchId;
    @Value("${payment.wechat.serial-no:}")
    private String serialNo;
    @Value("${payment.wechat.private-key:}")
    private String privateKeyContent;
    @Value("${payment.wechat.private-key-path:}")
    private String privateKeyPath;
    @Value("${payment.wechat.platform-public-key:}")
    private String platformPublicKeyContent;
    @Value("${payment.wechat.platform-public-key-path:}")
    private String platformPublicKeyPath;
    @Value("${payment.wechat.api-v3-key:}")
    private String apiV3Key;
    @Value("${payment.wechat.notify-url:}")
    private String notifyUrl;
    @Value("${payment.wechat.gateway:https://api.mch.weixin.qq.com}")
    private String gateway;
    @Value("${payment.wechat.timeout-seconds:900}")
    private Integer timeoutSeconds;
    @Value("${payment.wechat.notify-skip-verify:false}")
    private boolean notifySkipVerify;

    private volatile PrivateKey privateKey;
    private volatile PublicKey platformPublicKey;

    @Override
    public String channel() {
        return "WECHAT_NATIVE";
    }

    @Override
    public PrepayResult prepay(Order order, String outTradeNo) {
        validateBaseConfig();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appid", appId);
            payload.put("mchid", mchId);
            payload.put("description", "Order " + order.getOrderNo());
            payload.put("out_trade_no", outTradeNo);
            payload.put("notify_url", notifyUrl);

            Map<String, Object> amount = new HashMap<>();
            amount.put("total", toFen(order.getTotalPrice().doubleValue()));
            amount.put("currency", "CNY");
            payload.put("amount", amount);

            String body = objectMapper.writeValueAsString(payload);
            String path = "/v3/pay/transactions/native";
            String responseText = doRequest("POST", path, body);
            JsonNode node = objectMapper.readTree(responseText);
            String codeUrl = node.path("code_url").asText(null);
            if (!StringUtils.hasText(codeUrl)) {
                throw new RuntimeException("Wechat prepay response missing code_url");
            }
            int ttl = timeoutSeconds == null || timeoutSeconds <= 0 ? 900 : timeoutSeconds;
            return new PrepayResult(codeUrl, LocalDateTime.now().plusSeconds(ttl), responseText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Wechat prepay order: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryResult query(String outTradeNo) {
        validateBaseConfig();
        try {
            String path = "/v3/pay/transactions/out-trade-no/" + outTradeNo + "?mchid=" + mchId;
            String responseText = doRequest("GET", path, "");
            JsonNode node = objectMapper.readTree(responseText);
            String tradeState = node.path("trade_state").asText("NOTPAY");
            String txnId = node.path("transaction_id").asText(null);
            Integer totalFen = null;
            JsonNode amount = node.path("amount");
            if (amount != null && amount.has("total")) {
                totalFen = amount.path("total").asInt();
            }
            return new QueryResult(tradeState, txnId, totalFen, responseText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to query Wechat payment status: " + e.getMessage(), e);
        }
    }

    @Override
    public NotifyResult parseNotify(String body, Map<String, String> headers, Map<String, String> params) {
        try {
            if (!notifySkipVerify && !verifySignature(body, headers)) {
                return new NotifyResult(false, null, null, null, null, null, body, "Invalid Wechat callback signature");
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode resource = root.path("resource");
            if (resource.isMissingNode()) {
                return new NotifyResult(false, null, null, null, null, null, body, "Missing encrypted resource");
            }
            String ciphertext = resource.path("ciphertext").asText("");
            String nonce = resource.path("nonce").asText("");
            String associatedData = resource.path("associated_data").asText("");
            String plainText = decryptResource(ciphertext, nonce, associatedData);
            JsonNode plain = objectMapper.readTree(plainText);
            String outTradeNo = plain.path("out_trade_no").asText(null);
            String transactionId = plain.path("transaction_id").asText(null);
            String tradeState = plain.path("trade_state").asText(null);
            Integer paidFen = plain.path("amount").path("total").isMissingNode()
                    ? null : plain.path("amount").path("total").asInt();
            String currency = plain.path("amount").path("currency").asText(null);
            if (!StringUtils.hasText(outTradeNo)) {
                return new NotifyResult(false, null, transactionId, tradeState, paidFen, currency, plainText, "Missing out_trade_no");
            }
            return new NotifyResult(true, outTradeNo, transactionId, tradeState, paidFen, currency, plainText, "OK");
        } catch (Exception e) {
            log.warn("Wechat payment notification parsing failed; failureType={}", e.getClass().getName());
            return new NotifyResult(false, null, null, null, null, null, body,
                    "Unable to process Wechat payment notification");
        }
    }

    @Override
    public RefundResult refund(Order order, String outTradeNo, String outRefundNo, String reason) {
        validateBaseConfig();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("out_trade_no", outTradeNo);
            payload.put("out_refund_no", outRefundNo);
            payload.put("reason", StringUtils.hasText(reason) ? reason : "after-sales refund");

            Map<String, Object> amount = new HashMap<>();
            amount.put("refund", toFen(order.getTotalPrice().doubleValue()));
            amount.put("total", toFen(order.getTotalPrice().doubleValue()));
            amount.put("currency", "CNY");
            payload.put("amount", amount);

            String body = objectMapper.writeValueAsString(payload);
            String path = "/v3/refund/domestic/refunds";
            String responseText = doRequest("POST", path, body);
            JsonNode node = objectMapper.readTree(responseText);
            String status = node.path("status").asText("PROCESSING");
            String refundId = node.path("refund_id").asText(null);
            return new RefundResult(status, refundId, responseText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Wechat refund: " + e.getMessage(), e);
        }
    }

    private int toFen(double amount) {
        return (int) Math.round(amount * 100.0d);
    }

    private String doRequest(String method, String pathWithQuery, String body) throws Exception {
        String requestBody = body == null ? "" : body;
        String authorization = buildAuthorization(method, pathWithQuery, requestBody);
        String url = gateway + pathWithQuery;

        HttpRequest request;
        if ("GET".equalsIgnoreCase(method)) {
            request = HttpRequest.get(url);
        } else {
            request = HttpRequest.post(url).body(requestBody);
        }

        HttpResponse response = request
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "overseas-purchase-system")
                .timeout(10000)
                .execute();

        int statusCode = response.getStatus();
        String responseText = response.body();
        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException("Wechat API request failed, status=" + statusCode + ", body=" + responseText);
        }
        return responseText;
    }

    private String buildAuthorization(String method, String canonicalUrl, String body) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String message = method.toUpperCase() + "\n"
                + canonicalUrl + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + body + "\n";

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(loadPrivateKey());
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        String sign = Base64.getEncoder().encodeToString(signer.sign());

        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + mchId + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "signature=\"" + sign + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo + "\"";
    }

    private boolean verifySignature(String body, Map<String, String> headers) throws Exception {
        String timestamp = firstHeader(headers, "Wechatpay-Timestamp");
        String nonce = firstHeader(headers, "Wechatpay-Nonce");
        String signature = firstHeader(headers, "Wechatpay-Signature");
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            return false;
        }
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(loadPlatformPublicKey());
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(signature));
    }

    private String decryptResource(String ciphertext, String nonce, String associatedData) throws Exception {
        if (!StringUtils.hasText(apiV3Key) || apiV3Key.length() != 32) {
            throw new RuntimeException("Invalid Wechat APIv3 key. It must be 32 characters");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec params = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
        if (associatedData != null) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private PrivateKey loadPrivateKey() throws Exception {
        if (privateKey != null) {
            return privateKey;
        }
        synchronized (this) {
            if (privateKey == null) {
                String pem = readPem(privateKeyContent, privateKeyPath);
                if (!StringUtils.hasText(pem)) {
                    throw new RuntimeException("Missing Wechat private key");
                }
                String normalized = normalizePem(pem)
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
                byte[] keyBytes = Base64.getDecoder().decode(normalized);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            }
            return privateKey;
        }
    }

    private PublicKey loadPlatformPublicKey() throws Exception {
        if (platformPublicKey != null) {
            return platformPublicKey;
        }
        synchronized (this) {
            if (platformPublicKey == null) {
                String pem = readPem(platformPublicKeyContent, platformPublicKeyPath);
                if (!StringUtils.hasText(pem)) {
                    throw new RuntimeException("Missing Wechat platform public key");
                }
                String normalized = normalizePem(pem)
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
                byte[] keyBytes = Base64.getDecoder().decode(normalized);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                platformPublicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            }
            return platformPublicKey;
        }
    }

    private String normalizePem(String pem) {
        return pem.replace("\\n", "\n").trim();
    }

    private String readPem(String directContent, String path) throws Exception {
        if (StringUtils.hasText(directContent)) {
            return directContent;
        }
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String resourcePath = path.startsWith("classpath:") || path.startsWith("file:") ? path : "file:" + path;
        Resource resource = resourceLoader.getResource(resourcePath);
        if (!resource.exists()) {
            return null;
        }
        byte[] bytes = readAllBytes(resource.getInputStream());
        return new String(bytes, StandardCharsets.UTF_8);
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

    private String firstHeader(Map<String, String> headers, String key) {
        String value = headers.get(key);
        if (value != null) {
            return value;
        }
        value = headers.get(key.toLowerCase());
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void validateBaseConfig() {
        List<String> missingItems = new ArrayList<>();
        if (!StringUtils.hasText(appId)) {
            missingItems.add("app-id");
        }
        if (!StringUtils.hasText(mchId)) {
            missingItems.add("mch-id");
        }
        if (!StringUtils.hasText(serialNo)) {
            missingItems.add("serial-no");
        }
        if (!StringUtils.hasText(notifyUrl)) {
            missingItems.add("notify-url");
        }
        if (!missingItems.isEmpty()) {
            throw new RuntimeException("Wechat pay config incomplete: missing " + String.join(", ", missingItems));
        }
    }
}
