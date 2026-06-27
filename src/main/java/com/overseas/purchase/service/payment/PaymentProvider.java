package com.overseas.purchase.service.payment;

import com.overseas.purchase.entity.Order;

import java.time.LocalDateTime;
import java.util.Map;

public interface PaymentProvider {

    String channel();

    PrepayResult prepay(Order order, String outTradeNo);

    QueryResult query(String outTradeNo);

    NotifyResult parseNotify(String body, Map<String, String> headers, Map<String, String> params);

    RefundResult refund(Order order, String outTradeNo, String outRefundNo, String reason);

    class PrepayResult {
        private final String qrCodeUrl;
        private final LocalDateTime expireTime;
        private final String rawResponse;

        public PrepayResult(String qrCodeUrl, LocalDateTime expireTime, String rawResponse) {
            this.qrCodeUrl = qrCodeUrl;
            this.expireTime = expireTime;
            this.rawResponse = rawResponse;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public String getRawResponse() {
            return rawResponse;
        }
    }

    class QueryResult {
        private final String status;
        private final String gatewayTradeNo;
        private final Integer paidAmountFen;
        private final String rawResponse;

        public QueryResult(String status, String gatewayTradeNo, Integer paidAmountFen, String rawResponse) {
            this.status = status;
            this.gatewayTradeNo = gatewayTradeNo;
            this.paidAmountFen = paidAmountFen;
            this.rawResponse = rawResponse;
        }

        public String getStatus() {
            return status;
        }

        public String getGatewayTradeNo() {
            return gatewayTradeNo;
        }

        public Integer getPaidAmountFen() {
            return paidAmountFen;
        }

        public String getRawResponse() {
            return rawResponse;
        }
    }

    class NotifyResult {
        private final boolean valid;
        private final String outTradeNo;
        private final String gatewayTradeNo;
        private final String status;
        private final Integer paidAmountFen;
        private final String currency;
        private final String rawPayload;
        private final String message;

        public NotifyResult(boolean valid, String outTradeNo, String gatewayTradeNo, String status,
                            Integer paidAmountFen, String currency, String rawPayload, String message) {
            this.valid = valid;
            this.outTradeNo = outTradeNo;
            this.gatewayTradeNo = gatewayTradeNo;
            this.status = status;
            this.paidAmountFen = paidAmountFen;
            this.currency = currency;
            this.rawPayload = rawPayload;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public String getGatewayTradeNo() {
            return gatewayTradeNo;
        }

        public String getStatus() {
            return status;
        }

        public Integer getPaidAmountFen() {
            return paidAmountFen;
        }

        public String getCurrency() {
            return currency;
        }

        public String getRawPayload() {
            return rawPayload;
        }

        public String getMessage() {
            return message;
        }
    }

    class RefundResult {
        private final String status;
        private final String gatewayRefundNo;
        private final String rawResponse;

        public RefundResult(String status, String gatewayRefundNo, String rawResponse) {
            this.status = status;
            this.gatewayRefundNo = gatewayRefundNo;
            this.rawResponse = rawResponse;
        }

        public String getStatus() {
            return status;
        }

        public String getGatewayRefundNo() {
            return gatewayRefundNo;
        }

        public String getRawResponse() {
            return rawResponse;
        }
    }
}
