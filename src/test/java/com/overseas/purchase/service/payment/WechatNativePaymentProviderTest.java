package com.overseas.purchase.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WechatNativePaymentProviderTest {

    @Test
    void invalidNotificationJsonReturnsSafePublicMessage() {
        WechatNativePaymentProvider provider = new WechatNativePaymentProvider(
                new ObjectMapper(), mock(ResourceLoader.class));
        ReflectionTestUtils.setField(provider, "notifySkipVerify", true);

        PaymentProvider.NotifyResult result = provider.parseNotify(
                "secret-request-content", Collections.emptyMap(), null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Unable to process Wechat payment notification");
        assertThat(result.getMessage()).doesNotContain("secret-request-content", "Unrecognized token");
    }
}
