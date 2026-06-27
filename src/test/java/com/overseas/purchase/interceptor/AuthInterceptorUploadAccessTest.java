package com.overseas.purchase.interceptor;

import com.overseas.purchase.common.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthInterceptorUploadAccessTest {

    private final AuthInterceptor authInterceptor = new AuthInterceptor(mock(JwtUtil.class));

    @Test
    void unauthenticatedUploadPostRequiresLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload/avatar");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void unauthenticatedUploadGetRemainsPublicForStoredFiles() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/upload/avatar/not-found.png");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void unauthenticatedUploadsHeadRemainsPublicForStoredFiles() {
        MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/api/uploads/avatar/not-found.png");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
