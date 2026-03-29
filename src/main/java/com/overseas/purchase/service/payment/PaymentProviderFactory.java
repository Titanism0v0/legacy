package com.overseas.purchase.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final List<PaymentProvider> providers;

    @Value("${payment.provider:WECHAT_NATIVE}")
    private String providerType;

    private volatile Map<String, PaymentProvider> providerMap;

    public PaymentProvider currentProvider() {
        Map<String, PaymentProvider> map = getProviderMap();
        String key = StringUtils.hasText(providerType)
                ? providerType.trim().toUpperCase(Locale.ROOT)
                : "WECHAT_NATIVE";
        PaymentProvider provider = map.get(key);
        if (provider != null) {
            return provider;
        }
        PaymentProvider fallback = map.get("LEGACY_QR");
        if (fallback != null) {
            return fallback;
        }
        throw new RuntimeException("No payment provider available");
    }

    public PaymentProvider providerFor(String channel) {
        Map<String, PaymentProvider> map = getProviderMap();
        if (StringUtils.hasText(channel)) {
            PaymentProvider provider = map.get(channel.trim().toUpperCase(Locale.ROOT));
            if (provider != null) {
                return provider;
            }
        }
        return currentProvider();
    }

    private Map<String, PaymentProvider> getProviderMap() {
        if (providerMap != null) {
            return providerMap;
        }
        synchronized (this) {
            if (providerMap == null) {
                Map<String, PaymentProvider> map = new ConcurrentHashMap<>();
                for (PaymentProvider provider : providers) {
                    map.put(provider.channel().toUpperCase(Locale.ROOT), provider);
                }
                providerMap = map;
            }
            return providerMap;
        }
    }
}
