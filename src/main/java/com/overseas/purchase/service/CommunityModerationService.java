package com.overseas.purchase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.dto.CommunityPostCreateDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityModerationService {

    private static final String DECISION_ALLOW = "ALLOW";
    private static final String DECISION_REVIEW = "REVIEW";
    private static final String DECISION_BLOCK = "BLOCK";
    private static final String LEVEL_LOW = "LOW";
    private static final String LEVEL_MEDIUM = "MEDIUM";
    private static final String LEVEL_HIGH = "HIGH";
    private static final BigDecimal DEFAULT_ALLOW_THRESHOLD = new BigDecimal("0.30");
    private static final BigDecimal DEFAULT_BLOCK_THRESHOLD = new BigDecimal("0.75");
    private static final BigDecimal DEFAULT_REVIEW_SCORE = new BigDecimal("0.500");

    private final ObjectMapper objectMapper;

    @Value("${moderation.enabled:false}")
    private boolean enabled;

    @Value("${moderation.provider:openai-compatible}")
    private String provider;

    @Value("${moderation.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    @Value("${moderation.api-key:}")
    private String apiKey;

    @Value("${moderation.model:gpt-4.1-mini}")
    private String model;

    @Value("${moderation.timeout-ms:4000}")
    private int timeoutMs;

    @Value("${moderation.threshold-allow:0.30}")
    private BigDecimal thresholdAllow;

    @Value("${moderation.threshold-block:0.75}")
    private BigDecimal thresholdBlock;

    public ModerationDecision moderatePost(CommunityPostCreateDTO dto) {
        long start = System.currentTimeMillis();
        if (!enabled) {
            return fallbackAllow("moderation disabled", start, dto);
        }
        if (!StringUtils.hasText(apiKey)) {
            return fallbackAllow("moderation api key missing", start, dto);
        }

        try {
            ModerationPayload payload = requestModeration(dto);
            BigDecimal score = normalizeScore(payload.getRiskScore());
            String decision = decisionByScore(score);
            String level = normalizeRiskLevel(payload.getRiskLevel(), score);
            String reason = trimToLength(payload.getReason(), 500);
            if (!StringUtils.hasText(reason)) {
                reason = "AI moderation result";
            }
            log.info("community moderation ok decision={} score={} level={} costMs={} titleLen={} contentLen={} imageCount={}",
                    decision, score, level, System.currentTimeMillis() - start,
                    lengthOf(dto.getTitle()), lengthOf(dto.getContent()), estimateImageCount(dto));
            return new ModerationDecision(decision, score, level, reason, providerName(), modelName());
        } catch (Exception e) {
            return fallbackAllow("moderation request failed: " + e.getMessage(), start, dto);
        }
    }

    private ModerationPayload requestModeration(CommunityPostCreateDTO dto) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName());
        requestBody.put("temperature", 0);
        requestBody.put("messages", Arrays.asList(
                buildMessage("system",
                        "You are a strict content moderation classifier for marketplace community posts. " +
                                "Return JSON only with fields: riskScore (0 to 1), riskLevel (LOW|MEDIUM|HIGH), reason (max 120 chars)."),
                buildMessage("user", buildUserPrompt(dto))
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = buildRestTemplate().postForEntity(baseUrl, entity, String.class);
        String body = response.getBody();
        if (!StringUtils.hasText(body)) {
            throw new RuntimeException("empty response");
        }
        return parseModerationPayload(body);
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int safeTimeout = timeoutMs <= 0 ? 4000 : timeoutMs;
        factory.setConnectTimeout(safeTimeout);
        factory.setReadTimeout(safeTimeout);
        return new RestTemplate(factory);
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildUserPrompt(CommunityPostCreateDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("postType=").append(trimToLength(dto.getPostType(), 50)).append('\n');
        sb.append("title=").append(trimToLength(dto.getTitle(), 200)).append('\n');
        sb.append("content=").append(trimToLength(dto.getContent(), 1200)).append('\n');
        sb.append("contentMode=").append(trimToLength(dto.getContentMode(), 20)).append('\n');
        sb.append("renderPayload=").append(trimToLength(dto.getRenderPayload(), 1200)).append('\n');
        sb.append("images=").append(trimToLength(dto.getImages(), 800)).append('\n');
        sb.append("coverImage=").append(trimToLength(dto.getCoverImage(), 500)).append('\n');
        sb.append("Assess violations such as illegal goods, hate/violence, pornographic/explicit, scam/fraud, personal attacks.");
        return sb.toString();
    }

    private ModerationPayload parseModerationPayload(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.has("riskScore")) {
            return toPayload(root);
        }
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual()) {
            throw new RuntimeException("missing choices[0].message.content");
        }
        String content = contentNode.asText();
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("empty model content");
        }
        String json = extractJsonObject(content);
        JsonNode parsed = objectMapper.readTree(json);
        return toPayload(parsed);
    }

    private ModerationPayload toPayload(JsonNode node) {
        BigDecimal score = parseScore(node.path("riskScore"));
        String level = trimToLength(node.path("riskLevel").asText(null), 20);
        String reason = trimToLength(node.path("reason").asText(null), 500);
        if (score == null) {
            score = DEFAULT_REVIEW_SCORE;
        }
        return new ModerationPayload(score, level, reason);
    }

    private BigDecimal parseScore(JsonNode scoreNode) {
        if (scoreNode == null || scoreNode.isNull()) {
            return null;
        }
        try {
            if (scoreNode.isNumber()) {
                return scoreNode.decimalValue();
            }
            if (scoreNode.isTextual()) {
                return new BigDecimal(scoreNode.asText().trim());
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String extractJsonObject(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RuntimeException("model content is not valid json");
        }
        return trimmed.substring(start, end + 1);
    }

    private ModerationDecision fallbackAllow(String reason, long start, CommunityPostCreateDTO dto) {
        String safeReason = trimToLength(reason, 500);
        log.warn("community moderation fallback reason={} costMs={} titleLen={} contentLen={} imageCount={}",
                safeReason,
                System.currentTimeMillis() - start,
                lengthOf(dto.getTitle()),
                lengthOf(dto.getContent()),
                estimateImageCount(dto));
        return new ModerationDecision(
                DECISION_ALLOW,
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                LEVEL_LOW,
                safeReason,
                providerName(),
                modelName()
        );
    }

    private String decisionByScore(BigDecimal score) {
        BigDecimal allow = safeAllowThreshold();
        BigDecimal block = safeBlockThreshold(allow);
        if (score.compareTo(allow) <= 0) {
            return DECISION_ALLOW;
        }
        if (score.compareTo(block) > 0) {
            return DECISION_BLOCK;
        }
        return DECISION_REVIEW;
    }

    private String normalizeRiskLevel(String level, BigDecimal score) {
        if (StringUtils.hasText(level)) {
            String normalized = level.trim().toUpperCase(Locale.ROOT);
            if (LEVEL_LOW.equals(normalized) || LEVEL_MEDIUM.equals(normalized) || LEVEL_HIGH.equals(normalized)) {
                return normalized;
            }
        }
        BigDecimal allow = safeAllowThreshold();
        BigDecimal block = safeBlockThreshold(allow);
        if (score.compareTo(allow) <= 0) {
            return LEVEL_LOW;
        }
        if (score.compareTo(block) > 0) {
            return LEVEL_HIGH;
        }
        return LEVEL_MEDIUM;
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        BigDecimal score = value == null ? DEFAULT_REVIEW_SCORE : value;
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ONE) > 0) {
            score = BigDecimal.ONE;
        }
        return score.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAllowThreshold() {
        if (thresholdAllow == null || thresholdAllow.compareTo(BigDecimal.ZERO) < 0 || thresholdAllow.compareTo(BigDecimal.ONE) > 0) {
            return DEFAULT_ALLOW_THRESHOLD;
        }
        return thresholdAllow;
    }

    private BigDecimal safeBlockThreshold(BigDecimal allow) {
        if (thresholdBlock == null || thresholdBlock.compareTo(BigDecimal.ZERO) < 0 || thresholdBlock.compareTo(BigDecimal.ONE) > 0) {
            return DEFAULT_BLOCK_THRESHOLD;
        }
        if (thresholdBlock.compareTo(allow) <= 0) {
            return DEFAULT_BLOCK_THRESHOLD;
        }
        return thresholdBlock;
    }

    private int lengthOf(String text) {
        return text == null ? 0 : text.length();
    }

    private int estimateImageCount(CommunityPostCreateDTO dto) {
        int count = 0;
        if (StringUtils.hasText(dto.getImages())) {
            String[] arr = dto.getImages().split(",");
            for (String item : arr) {
                if (StringUtils.hasText(item)) {
                    count++;
                }
            }
        }
        if (StringUtils.hasText(dto.getCoverImage())) {
            count++;
        }
        return count;
    }

    private String providerName() {
        return StringUtils.hasText(provider) ? provider.trim() : "openai-compatible";
    }

    private String modelName() {
        return StringUtils.hasText(model) ? model.trim() : "gpt-4.1-mini";
    }

    private String trimToLength(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    @Data
    @AllArgsConstructor
    private static class ModerationPayload {
        private BigDecimal riskScore;
        private String riskLevel;
        private String reason;
    }

    @Data
    @AllArgsConstructor
    public static class ModerationDecision {
        private String decision;
        private BigDecimal riskScore;
        private String riskLevel;
        private String reason;
        private String provider;
        private String model;
    }
}
