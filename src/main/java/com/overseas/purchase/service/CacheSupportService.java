package com.overseas.purchase.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheSupportService {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${performance.redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${performance.redis.key-prefix:overseas-purchase}")
    private String keyPrefix;

    public <T> T getOrLoad(String key, TypeReference<T> typeReference, Duration ttl, Supplier<T> loader) {
        if (!redisEnabled) {
            return loader.get();
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return loader.get();
        }

        String namespacedKey = namespaced(key);
        try {
            String cached = redisTemplate.opsForValue().get(namespacedKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, typeReference);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed. key={}, reason={}", namespacedKey, e.getMessage());
        }

        T value = loader.get();
        if (value == null) {
            return null;
        }

        try {
            String payload = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(namespacedKey, payload, ttl);
        } catch (Exception e) {
            log.warn("Redis cache write failed. key={}, reason={}", namespacedKey, e.getMessage());
        }
        return value;
    }

    public void evict(String key) {
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(namespaced(key));
        } catch (Exception e) {
            log.warn("Redis cache evict failed. key={}, reason={}", key, e.getMessage());
        }
    }

    public void evictByPrefix(String prefix) {
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        String pattern = namespaced(prefix) + "*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis cache prefix evict failed. pattern={}, reason={}", pattern, e.getMessage());
        }
    }

    private String namespaced(String key) {
        String safePrefix = StringUtils.hasText(keyPrefix) ? keyPrefix.trim() : "overseas-purchase";
        String safeKey = key == null ? "unknown" : key.trim();
        if (safeKey.startsWith(":")) {
            safeKey = safeKey.substring(1);
        }
        return safePrefix + ":" + safeKey;
    }
}
