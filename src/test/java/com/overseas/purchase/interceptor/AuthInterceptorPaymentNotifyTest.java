package com.overseas.purchase.interceptor;

import com.overseas.purchase.common.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthInterceptorPaymentNotifyTest {

    private final AuthInterceptor authInterceptor = new AuthInterceptor(mock(JwtUtil.class));

    @Test
    void unauthenticatedAlipayNotifyIsPublicForGatewayCallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment/notify/alipay");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void unauthenticatedContactPasswordResetIsNotPublic() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/reset-password-by-contact");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
