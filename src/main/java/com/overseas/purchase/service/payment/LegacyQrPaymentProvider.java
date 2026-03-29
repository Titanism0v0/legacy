package com.overseas.purchase.service.payment;

import com.overseas.purchase.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class LegacyQrPaymentProvider implements PaymentProvider {

    @Value("${payment.qrcode.payment-domain:}")
    private String paymentDomain;

    @Override
    public String channel() {
        return "LEGACY_QR";
    }

    @Override
    public PrepayResult prepay(Order order, String outTradeNo) {
        String paymentUrl = String.format(
                "%s/payment?orderId=%d&orderNo=%s&amount=%.2f",
                resolvePaymentDomain(null),
                order.getId(),
                order.getOrderNo(),
                order.getTotalPrice().doubleValue()
        );
        return new PrepayResult(paymentUrl, LocalDateTime.now().plusMinutes(15), paymentUrl);
    }

    @Override
    public QueryResult query(String outTradeNo) {
        return new QueryResult("NOTPAY", null, null, "{}");
    }

    @Override
    public NotifyResult parseNotify(String body, Map<String, String> headers, Map<String, String> params) {
        return new NotifyResult(false, null, null, null, null, body, "Legacy provider does not support notify");
    }

    @Override
    public RefundResult refund(Order order, String outTradeNo, String outRefundNo, String reason) {
        return new RefundResult("NOT_SUPPORTED", null, "{}");
    }

    private String resolvePaymentDomain(HttpServletRequest request) {
        if (paymentDomain != null) {
            String configuredDomain = paymentDomain.trim();
            if (!configuredDomain.isEmpty()) {
                return configuredDomain;
            }
        }
        return request == null ? "http://localhost:8081" : request.getScheme() + "://" + request.getHeader("Host");
    }
}
