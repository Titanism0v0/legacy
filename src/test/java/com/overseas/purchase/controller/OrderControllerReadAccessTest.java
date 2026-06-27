package com.overseas.purchase.controller;

import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.service.OrderFulfillmentService;
import com.overseas.purchase.service.OrderService;
import com.overseas.purchase.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerReadAccessTest {

    private OrderService orderService;
    private OrderFulfillmentService orderFulfillmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        PaymentService paymentService = mock(PaymentService.class);
        orderFulfillmentService = mock(OrderFulfillmentService.class);
        OrderController controller = new OrderController(orderService, paymentService, orderFulfillmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void buyerCanReadOwnOrderDetail() throws Exception {
        when(orderService.getOrderById(88L)).thenReturn(order(88L, 12L, 20L));

        mockMvc.perform(get("/order/88").requestAttr("userId", 12L).requestAttr("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(88));
    }

    @Test
    void sellerCanReadOwnOrderDetail() throws Exception {
        when(orderService.getOrderById(88L)).thenReturn(order(88L, 12L, 20L));

        mockMvc.perform(get("/order/88").requestAttr("userId", 20L).requestAttr("role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sellerId").value(20));
    }

    @Test
    void adminCanReadAnyOrderDetail() throws Exception {
        when(orderService.getOrderById(88L)).thenReturn(order(88L, 12L, 20L));

        mockMvc.perform(get("/order/88").requestAttr("userId", 99L).requestAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.buyerId").value(12));
    }

    @Test
    void unrelatedUserCannotReadOrderDetail() throws Exception {
        when(orderService.getOrderById(88L)).thenReturn(order(88L, 12L, 20L));

        mockMvc.perform(get("/order/88").requestAttr("userId", 77L).requestAttr("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("No permission"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void unrelatedUserCannotReadOrderStatusFlow() throws Exception {
        when(orderService.getOrderById(88L)).thenReturn(order(88L, 12L, 20L));

        mockMvc.perform(get("/order/88/status-flow").requestAttr("userId", 77L).requestAttr("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("No permission"));

        verify(orderFulfillmentService, never()).describe(anyString());
    }

    @Test
    void unrelatedUserCannotReadOrderInsight() throws Exception {
        when(orderService.getOrderById(88L)).thenReturn(order(88L, 12L, 20L));

        mockMvc.perform(get("/order/88/insight").requestAttr("userId", 77L).requestAttr("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("No permission"));

        verify(orderService, never()).getOrderInsight(88L);
    }

    @Test
    void publicStatusFlowDescriptionDoesNotRequireOrderOwnership() throws Exception {
        when(orderFulfillmentService.describe("PENDING_PAYMENT")).thenReturn(Collections.singletonMap("status", "PENDING_PAYMENT"));

        mockMvc.perform(get("/order/status-flow").param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    private OrderDTO order(Long id, Long buyerId, Long sellerId) {
        OrderDTO order = new OrderDTO();
        order.setId(id);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setStatus("PENDING_PAYMENT");
        return order;
    }
}
