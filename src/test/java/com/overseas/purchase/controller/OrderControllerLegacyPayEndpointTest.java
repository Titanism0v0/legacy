package com.overseas.purchase.controller;

import com.overseas.purchase.service.OrderFulfillmentService;
import com.overseas.purchase.service.OrderService;
import com.overseas.purchase.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerLegacyPayEndpointTest {

    @Test
    void legacyPayEndpointIsNotMapped() throws Exception {
        OrderController controller = new OrderController(
                mock(OrderService.class),
                mock(PaymentService.class),
                mock(OrderFulfillmentService.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/order/pay/1"))
                .andExpect(status().isNotFound());
    }
}
