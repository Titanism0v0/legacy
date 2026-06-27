package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedExceptionReturnsGenericPublicMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<Void> result = handler.handleException(new RuntimeException(
                "Connection refused: http://127.0.0.1:11434 model=secret-provider"));

        assertThat(result.getMessage()).isEqualTo("请求处理失败，请稍后重试");
        assertThat(result.getMessage()).doesNotContain("127.0.0.1", "11434", "model", "provider");
    }
}
