package com.overseas.purchase.service;

import com.overseas.purchase.entity.Order;
import com.overseas.purchase.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RealtimeNotificationService realtimeNotificationService;

    private OrderFulfillmentService orderFulfillmentService;

    @BeforeEach
    void setUp() {
        orderFulfillmentService = new OrderFulfillmentService(orderMapper, realtimeNotificationService);
    }

    @Test
    void advanceRequiresCrossBorderTrackingForInternationalShipping() {
        Order order = baseOrder("PENDING_SHIPMENT");
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> orderFulfillmentService.advance(
                1L, "INTL_SHIPPING", 20L, "SELLER", null, null, null))
                .hasMessageContaining("Cross-border tracking number");

        verify(orderMapper).selectById(1L);
        verifyNoMoreInteractions(realtimeNotificationService);
    }

    @Test
    void sellerCanAdvanceOwnOrderAndPushRealtimeNotification() {
        Order order = baseOrder("PENDING_SHIPMENT");
        when(orderMapper.selectById(1L)).thenReturn(order);

        Order result = orderFulfillmentService.advance(
                1L,
                "INTL_SHIPPING",
                20L,
                "SELLER",
                "CB123",
                null,
                "handed to cross-border carrier"
        );

        assertThat(result.getStatus()).isEqualTo("INTL_SHIPPING");
        assertThat(result.getCrossborderTrackingNumber()).isEqualTo("CB123");
        assertThat(result.getCustomsClearanceStatus()).isEqualTo("IN_TRANSIT");
        assertThat(result.getRemark()).contains("[INTL_SHIPPING]");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(orderCaptor.capture());
        verify(realtimeNotificationService).notifyOrderStatusChanged(eq(orderCaptor.getValue()), anyString());
    }

    @Test
    void advanceRejectsBackwardTransition() {
        Order order = baseOrder("CUSTOMS_CLEARANCE");
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> orderFulfillmentService.advance(
                1L, "PURCHASING", 20L, "SELLER", null, null, null))
                .hasMessageContaining("only move forward");

        verify(orderMapper).selectById(1L);
        verifyNoMoreInteractions(realtimeNotificationService);
    }

    @Test
    void sellerCannotAdvanceAnotherSellerOrder() {
        Order order = baseOrder("PENDING_SHIPMENT");
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> orderFulfillmentService.advance(
                1L, "PURCHASING", 21L, "SELLER", null, null, null))
                .hasMessageContaining("No permission");

        verify(orderMapper).selectById(1L);
        verifyNoMoreInteractions(realtimeNotificationService);
    }

    private Order baseOrder(String status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("O202605060001");
        order.setBuyerId(10L);
        order.setSellerId(20L);
        order.setStatus(status);
        order.setDeleted(0);
        return order;
    }
}
