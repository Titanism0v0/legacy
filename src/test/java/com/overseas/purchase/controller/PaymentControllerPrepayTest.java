package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerPrepayTest {

    @Test
    void prepayPassesAuthenticatedUserContextToService() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 12L);
        request.setAttribute("role", "BUYER");
        Map<String, Object> serviceResult = Collections.singletonMap("orderId", 88L);

        when(paymentService.prepay(88L, 12L, "BUYER")).thenReturn(serviceResult);

        Result<Map<String, Object>> result = controller.prepay(88L, request);

        assertThat(result.getData()).isSameAs(serviceResult);
        verify(paymentService).prepay(88L, 12L, "BUYER");
    }
}
