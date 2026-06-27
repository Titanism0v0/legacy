package com.overseas.purchase.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class MyBatisPlusConfigTest {

    @Test
    void registersPaginationInterceptorSoPageTotalsAreCounted() throws Exception {
        Class<?> configClass;
        try {
            configClass = Class.forName("com.overseas.purchase.config.MyBatisPlusConfig");
        } catch (ClassNotFoundException e) {
            fail("MyBatis-Plus pagination config is missing");
            return;
        }

        Object config = configClass.getDeclaredConstructor().newInstance();
        Method method = configClass.getMethod("mybatisPlusInterceptor");
        MybatisPlusInterceptor interceptor = (MybatisPlusInterceptor) method.invoke(config);

        assertThat(interceptor.getInterceptors())
                .anyMatch(item -> item instanceof PaginationInnerInterceptor);
    }
}
