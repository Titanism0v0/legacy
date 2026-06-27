package com.overseas.purchase.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuditModerationServiceTest {

    private AdminAuditModerationService service;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        service = new AdminAuditModerationService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "aiRequired", false);
        ReflectionTestUtils.setField(service, "provider", "openai-compatible");
        ReflectionTestUtils.setField(service, "model", "internal-model-name");
        ReflectionTestUtils.setField(service, "timeoutMs", 100);
        ReflectionTestUtils.setField(service, "thresholdAllow", new java.math.BigDecimal("0.30"));
        ReflectionTestUtils.setField(service, "thresholdBlock", new java.math.BigDecimal("0.75"));
        ReflectionTestUtils.setField(service, "sensitiveBlockWords", "drugs,gun,explosive,terror,porn,child-sexual");
        ReflectionTestUtils.setField(service, "sensitiveReviewWords", "wechat,telegram,whatsapp,casino,private-transfer");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void blocksChineseExplicitViolationEvenWithEnglishOnlyConfiguration() {
        AdminAuditModerationService.ModerationResult result = service.moderate("PRODUCT_SUBMISSION", fields("出售冰毒和枪支"));

        assertThat(result.getDecision()).isEqualTo("BLOCK");
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    void connectionFailureFallsBackToReviewWithoutLeakingEndpoint() {
        enableAi("http://127.0.0.1:1/v1/chat/completions", 80);

        AdminAuditModerationService.ModerationResult result = service.moderate("PRODUCT_SUBMISSION", fields("普通旅行水杯"));

        assertThat(result.getDecision()).isEqualTo("REVIEW");
        assertThat(result.getReason()).doesNotContain("127.0.0.1", "Connection refused", "internal-model-name");
    }

    @Test
    void timeoutFallsBackToReviewWithoutRawException() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (Exception ignored) {
                // Client timeout closes the exchange before the delayed response is written.
            } finally {
                exchange.close();
            }
        });
        enableAi(serverUrl(), 50);

        AdminAuditModerationService.ModerationResult result = service.moderate("PRODUCT_SUBMISSION", fields("普通旅行水杯"));

        assertThat(result.getDecision()).isEqualTo("REVIEW");
        assertThat(result.getReason()).doesNotContain("timed out", "localhost", "internal-model-name");
    }

    @Test
    void invalidJsonFallsBackToReviewWithoutReturningParserDetails() throws Exception {
        startServer(exchange -> {
            byte[] body = "not-json".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        enableAi(serverUrl(), 500);

        AdminAuditModerationService.ModerationResult result = service.moderate("PRODUCT_SUBMISSION", fields("普通旅行水杯"));

        assertThat(result.getDecision()).isEqualTo("REVIEW");
        assertThat(result.getReason()).doesNotContain("Unrecognized token", "not-json", serverUrl());
    }

    private Map<String, String> fields(String title) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("title", title);
        return result;
    }

    private void enableAi(String baseUrl, int timeoutMs) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "aiRequired", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-only-key");
        ReflectionTestUtils.setField(service, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(service, "timeoutMs", timeoutMs);
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", handler);
        server.start();
    }

    private String serverUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
    }
}
