package com.overseas.purchase.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheSupportServiceTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheSupportService cacheSupportService;

    @BeforeEach
    void setUp() {
        cacheSupportService = new CacheSupportService(redisTemplateProvider, new ObjectMapper());
        ReflectionTestUtils.setField(cacheSupportService, "keyPrefix", "test");
    }

    @Test
    void getOrLoadUsesLoaderWhenRedisDisabled() {
        ReflectionTestUtils.setField(cacheSupportService, "redisEnabled", false);
        AtomicInteger loaderCalls = new AtomicInteger();

        String value = cacheSupportService.getOrLoad(
                "category:list",
                new TypeReference<String>() {},
                Duration.ofMinutes(1),
                () -> {
                    loaderCalls.incrementAndGet();
                    return "db-value";
                }
        );

        assertThat(value).isEqualTo("db-value");
        assertThat(loaderCalls).hasValue(1);
        verify(redisTemplateProvider, never()).getIfAvailable();
    }

    @Test
    void getOrLoadReturnsCacheHitWithoutCallingLoader() {
        ReflectionTestUtils.setField(cacheSupportService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:stats:summary")).thenReturn("{\"count\":3}");
        AtomicInteger loaderCalls = new AtomicInteger();

        Map<String, Integer> value = cacheSupportService.getOrLoad(
                "stats:summary",
                new TypeReference<Map<String, Integer>>() {},
                Duration.ofMinutes(5),
                () -> {
                    loaderCalls.incrementAndGet();
                    return new HashMap<String, Integer>();
                }
        );

        assertThat(value).containsEntry("count", 3);
        assertThat(loaderCalls).hasValue(0);
    }

    @Test
    void getOrLoadFallsBackAndWritesCacheOnMiss() {
        ReflectionTestUtils.setField(cacheSupportService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:exchange-rate:current")).thenReturn(null);

        Map<String, Integer> source = new HashMap<String, Integer>();
        source.put("rate", 7);
        Map<String, Integer> value = cacheSupportService.getOrLoad(
                "exchange-rate:current",
                new TypeReference<Map<String, Integer>>() {},
                Duration.ofMinutes(10),
                () -> source
        );

        assertThat(value).containsEntry("rate", 7);
        verify(valueOperations).set(eq("test:exchange-rate:current"), eq("{\"rate\":7}"), any(Duration.class));
    }
}
