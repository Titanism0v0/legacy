package com.overseas.purchase.config;

import com.overseas.purchase.common.JwtUtil;
import com.overseas.purchase.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebConfigUploadInterceptorTest {

    @Test
    void uploadPathsAreNotExcludedFromAuthenticationInterceptor() throws Exception {
        InterceptorRegistry registry = new InterceptorRegistry();
        WebConfig webConfig = new WebConfig(new AuthInterceptor(mock(JwtUtil.class)));

        webConfig.addInterceptors(registry);

        InterceptorRegistration registration = registrations(registry).get(0);
        assertThat(excludePatterns(registration))
                .doesNotContain("/upload/**", "/uploads/**");
    }

    @SuppressWarnings("unchecked")
    private List<InterceptorRegistration> registrations(InterceptorRegistry registry) throws Exception {
        Field field = InterceptorRegistry.class.getDeclaredField("registrations");
        field.setAccessible(true);
        return (List<InterceptorRegistration>) field.get(registry);
    }

    @SuppressWarnings("unchecked")
    private List<String> excludePatterns(InterceptorRegistration registration) throws Exception {
        Field field = InterceptorRegistration.class.getDeclaredField("excludePatterns");
        field.setAccessible(true);
        return (List<String>) field.get(registration);
    }
}
