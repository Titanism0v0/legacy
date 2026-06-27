package com.overseas.purchase.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.PaymentTxn;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.PaymentTxnMapper;
import com.overseas.purchase.mapper.UserMapper;
import com.overseas.purchase.service.payment.PaymentProvider;
import com.overseas.purchase.service.payment.PaymentProviderFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceGatewayValidationTest {

    @Test
    void notifyWithMissingPaidAmountDoesNotWriteSuccessOrMarkPaid() {
        Fixtures fixtures = new Fixtures();
        when(fixtures.provider.parseNotify(null, null, Collections.emptyMap()))
                .thenReturn(new PaymentProvider.NotifyResult(
                        true, "OUT123", "GW123", "TRADE_SUCCESS", null, "CNY", "{}", "OK"));

        String result = fixtures.service.handleAlipayNotify(Collections.emptyMap());

        assertThat(result).isEqualTo("failure");
        assertThat(fixtures.txn.getStatus()).isEqualTo("VERIFY_FAILED");
        verify(fixtures.paymentTxnMapper).updateById(fixtures.txn);
        verify(fixtures.orderService, never()).markPaid(any(), any());
    }

    @Test
    void notifyWithMismatchedAmountDoesNotWriteSuccessOrMarkPaid() {
        Fixtures fixtures = new Fixtures();
        when(fixtures.provider.parseNotify(null, null, Collections.emptyMap()))
                .thenReturn(new PaymentProvider.NotifyResult(
                        true, "OUT123", "GW123", "TRADE_SUCCESS", 9900, "CNY", "{}", "OK"));

        String result = fixtures.service.handleAlipayNotify(Collections.emptyMap());

        assertThat(result).isEqualTo("failure");
        assertThat(fixtures.txn.getStatus()).isEqualTo("VERIFY_FAILED");
        verify(fixtures.orderService, never()).markPaid(any(), any());
    }

    @Test
    void notifyWithMismatchedCurrencyDoesNotWriteSuccessOrMarkPaid() {
        Fixtures fixtures = new Fixtures();
        when(fixtures.provider.parseNotify(null, null, Collections.emptyMap()))
                .thenReturn(new PaymentProvider.NotifyResult(
                        true, "OUT123", "GW123", "TRADE_SUCCESS", 12345, "USD", "{}", "OK"));

        String result = fixtures.service.handleAlipayNotify(Collections.emptyMap());

        assertThat(result).isEqualTo("failure");
        assertThat(fixtures.txn.getStatus()).isEqualTo("VERIFY_FAILED");
        verify(fixtures.orderService, never()).markPaid(any(), any());
    }

    @Test
    void notifyWithMatchingAmountAndCurrencyMarksOrderPaid() {
        Fixtures fixtures = new Fixtures();
        when(fixtures.provider.parseNotify(null, null, Collections.emptyMap()))
                .thenReturn(new PaymentProvider.NotifyResult(
                        true, "OUT123", "GW123", "TRADE_SUCCESS", 12345, "CNY", "{}", "OK"));

        String result = fixtures.service.handleAlipayNotify(Collections.emptyMap());

        assertThat(result).isEqualTo("success");
        assertThat(fixtures.txn.getStatus()).isEqualTo("SUCCESS");
        verify(fixtures.orderService).markPaid(10L, "ALIPAY");
    }

    @Test
    void querySyncWithMissingAmountDoesNotWriteSuccessOrMarkPaid() {
        Fixtures fixtures = new Fixtures();
        fixtures.txn.setStatus("WAIT_BUYER_PAY");
        when(fixtures.paymentTxnMapper.selectList(any())).thenReturn(Collections.singletonList(fixtures.txn));
        when(fixtures.provider.query("OUT123"))
                .thenReturn(new PaymentProvider.QueryResult("TRADE_SUCCESS", "GW123", null, "{}"));

        Map<String, Object> result = fixtures.service.getPaymentStatus(10L, 20L, "BUYER");

        assertThat(result.get("paid")).isEqualTo(false);
        assertThat(fixtures.txn.getStatus()).isEqualTo("VERIFY_FAILED");
        verify(fixtures.orderService, never()).markPaid(any(), any());
    }

    private static class Fixtures {
        private final OrderMapper orderMapper = mock(OrderMapper.class);
        private final PaymentTxnMapper paymentTxnMapper = mock(PaymentTxnMapper.class);
        private final UserMapper userMapper = mock(UserMapper.class);
        private final OrderService orderService = mock(OrderService.class);
        private final PaymentProviderFactory paymentProviderFactory = mock(PaymentProviderFactory.class);
        private final PaymentProvider provider = mock(PaymentProvider.class);
        private final Order order = new Order();
        private final PaymentTxn txn = new PaymentTxn();
        private final PaymentService service = new PaymentService(
                orderMapper, paymentTxnMapper, userMapper, orderService, paymentProviderFactory, new ObjectMapper());

        private Fixtures() {
            order.setId(10L);
            order.setBuyerId(20L);
            order.setSellerId(30L);
            order.setTotalPrice(new BigDecimal("123.45"));
            order.setPaymentCurrencySnapshot("CNY");
            order.setPaymentStatus("UNPAID");
            order.setDeleted(0);

            txn.setId(100L);
            txn.setOrderId(10L);
            txn.setChannel("ALIPAY");
            txn.setOutTradeNo("OUT123");
            txn.setAmount(new BigDecimal("123.45"));
            txn.setCurrency("CNY");
            txn.setStatus("WAIT_BUYER_PAY");
            txn.setDeleted(0);

            when(orderMapper.selectById(10L)).thenReturn(order);
            when(paymentTxnMapper.selectOne(any())).thenReturn(txn);
            when(paymentProviderFactory.providerFor("ALIPAY")).thenReturn(provider);
            when(provider.channel()).thenReturn("ALIPAY");
        }
    }
}
