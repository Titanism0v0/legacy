package com.overseas.purchase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAuditModerationService {

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

    @Value("${moderation.ai-required:true}")
    private boolean aiRequired;

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

    @Value("${moderation.sensitive.block-words:drugs,gun,explosive,terror,porn,child-sexual}")
    private String sensitiveBlockWords;

    @Value("${moderation.sensitive.review-words:wechat,telegram,whatsapp,casino,private-transfer}")
    private String sensitiveReviewWords;

    public ModerationResult moderate(String scene, Map<String, String> fields) {
        long start = System.currentTimeMillis();
        String safeScene = StringUtils.hasText(scene) ? scene.trim() : "UNKNOWN_SCENE";
        Map<String, String> safeFields = fields == null ? new LinkedHashMap<>() : fields;

        String textCorpus = buildCorpus(safeFields);
        LayerResult keywordLayer = keywordLayer(textCorpus);
        LayerResult aiLayer = aiLayer(safeScene, safeFields);

        ModerationResult result = merge(safeScene, keywordLayer, aiLayer);
        log.info("admin audit moderation scene={} decision={} score={} level={} costMs={}",
                safeScene, result.getDecision(), result.getRiskScore(), result.getRiskLevel(),
                System.currentTimeMillis() - start);
        return result;
    }

    public String buildAuditRemark(ModerationResult result, String extraRemark) {
        if (result == null) {
            return trimToLength(extraRemark, 500);
        }
        String reason = StringUtils.hasText(result.getReason()) ? result.getReason() : "-";
        String base = String.format(Locale.ROOT, "[AUTO_MOD] decision=%s score=%s level=%s reason=%s",
                result.getDecision(), result.getRiskScore(), result.getRiskLevel(), reason);
        if (!StringUtils.hasText(extraRemark)) {
            return trimToLength(base, 500);
        }
        return trimToLength(base + " | extra=" + extraRemark.trim(), 500);
    }

    private LayerResult keywordLayer(String corpus) {
        String lowerCorpus = corpus.toLowerCase(Locale.ROOT);
        String normalizedCorpus = normalizeForMatch(corpus);
        Set<String> blockWords = SensitiveContentRules.blockWords(sensitiveBlockWords);
        Set<String> reviewWords = SensitiveContentRules.reviewWords(sensitiveReviewWords);
        reviewWords.removeAll(blockWords);

        Set<String> blockHits = new LinkedHashSet<>();
        for (String word : blockWords) {
            if (containsKeyword(lowerCorpus, normalizedCorpus, word)) {
                blockHits.add(word);
            }
        }
        if (!blockHits.isEmpty()) {
            return new LayerResult("L1-KEYWORD", DECISION_BLOCK, BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP),
                    LEVEL_HIGH, "Sensitive blocked words hit: " + String.join(",", blockHits));
        }

        Set<String> reviewHits = new LinkedHashSet<>();
        for (String word : reviewWords) {
            if (containsKeyword(lowerCorpus, normalizedCorpus, word)) {
                reviewHits.add(word);
            }
        }
        if (!reviewHits.isEmpty()) {
            return new LayerResult("L1-KEYWORD", DECISION_REVIEW, new BigDecimal("0.650"),
                    LEVEL_MEDIUM, "Sensitive review words hit: " + String.join(",", reviewHits));
        }

        if (!StringUtils.hasText(corpus)) {
            return allowLayer("L1-KEYWORD", "No content");
        }
        return allowLayer("L1-KEYWORD", "No sensitive words matched");
    }

    private LayerResult aiLayer(String scene, Map<String, String> fields) {
        if (!enabled) {
            return aiRequired
                    ? reviewLayer("L2-AI", "AI moderation disabled")
                    : allowLayer("L2-AI", "AI moderation disabled but aiRequired=false");
        }
        if (!StringUtils.hasText(apiKey)) {
            return aiRequired
                    ? reviewLayer("L2-AI", "AI key missing")
                    : allowLayer("L2-AI", "AI key missing but aiRequired=false");
        }

        try {
            ModerationPayload payload = requestModeration(scene, fields);
            BigDecimal score = normalizeScore(payload.getRiskScore());
            String decision = decisionByScore(score);
            String level = normalizeRiskLevel(payload.getRiskLevel(), score);
            String reason = trimToLength(payload.getReason(), 300);
            if (!StringUtils.hasText(reason)) {
                reason = "AI moderation result";
            }
            return new LayerResult("L2-AI", decision, score, level, reason);
        } catch (Exception e) {
            log.warn("admin AI moderation failed; scene={} failureType={}", scene, e.getClass().getName(), e);
            return aiRequired
                    ? reviewLayer("L2-AI", "AI moderation service unavailable; manual review required")
                    : allowLayer("L2-AI", "AI moderation failed but aiRequired=false");
        }
    }

    private ModerationResult merge(String scene, LayerResult... layers) {
        String finalDecision = DECISION_ALLOW;
        BigDecimal finalScore = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        String finalLevel = LEVEL_LOW;
        List<String> reasons = new ArrayList<>();

        for (LayerResult layer : layers) {
            if (layer == null) {
                continue;
            }
            if (layer.getScore() != null && layer.getScore().compareTo(finalScore) > 0) {
                finalScore = layer.getScore();
            }
            finalLevel = mergeLevel(finalLevel, layer.getRiskLevel());
            if (DECISION_BLOCK.equals(layer.getDecision())) {
                finalDecision = DECISION_BLOCK;
            } else if (!DECISION_BLOCK.equals(finalDecision) && DECISION_REVIEW.equals(layer.getDecision())) {
                finalDecision = DECISION_REVIEW;
            }
            if (StringUtils.hasText(layer.getReason())) {
                reasons.add(layer.getLayer() + ": " + layer.getReason());
            }
        }

        if (DECISION_BLOCK.equals(finalDecision)) {
            finalLevel = LEVEL_HIGH;
            if (finalScore.compareTo(new BigDecimal("0.900")) < 0) {
                finalScore = new BigDecimal("0.900");
            }
        } else if (DECISION_REVIEW.equals(finalDecision)) {
            if (LEVEL_LOW.equals(finalLevel)) {
                finalLevel = LEVEL_MEDIUM;
            }
            if (finalScore.compareTo(new BigDecimal("0.500")) < 0) {
                finalScore = new BigDecimal("0.500");
            }
        }

        finalScore = normalizeScore(finalScore);
        String reason = trimToLength(String.join(" | ", reasons), 500);
        if (!StringUtils.hasText(reason)) {
            reason = "Moderation result";
        }

        return new ModerationResult(scene, finalDecision, finalScore, finalLevel, reason, providerName(), modelName());
    }

    private ModerationPayload requestModeration(String scene, Map<String, String> fields) throws Exception {
        List<Map<String, Object>> messages = Arrays.asList(
                buildMessage("system",
                        "You are a strict compliance moderation classifier for admin pre-review flows. " +
                                "Return JSON only with fields: riskScore (0 to 1), riskLevel (LOW|MEDIUM|HIGH), reason (max 120 chars)."),
                buildMessage("user", buildPrompt(scene, fields))
        );

        JsonNode root = invokeChatCompletion(messages);
        return parseModerationPayload(root);
    }

    private String buildPrompt(String scene, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("scene=").append(trimToLength(scene, 80)).append('\n');
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!StringUtils.hasText(entry.getValue())) {
                continue;
            }
            sb.append(entry.getKey()).append('=').append(trimToLength(entry.getValue(), 1200)).append('\n');
        }
        sb.append("Assess illegal goods, fraud/scam clues, explicit sexual content, violence, hate, and policy evasion.");
        return sb.toString();
    }

    private JsonNode invokeChatCompletion(List<Map<String, Object>> messages) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName());
        requestBody.put("temperature", 0);
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = buildRestTemplate().postForEntity(baseUrl, entity, String.class);
        String body = response.getBody();
        if (!StringUtils.hasText(body)) {
            throw new RuntimeException("empty response");
        }
        return objectMapper.readTree(body);
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int safeTimeout = timeoutMs <= 0 ? 4000 : timeoutMs;
        factory.setConnectTimeout(safeTimeout);
        factory.setReadTimeout(safeTimeout);
        return new RestTemplate(factory);
    }

    private ModerationPayload parseModerationPayload(JsonNode root) throws Exception {
        if (root.has("riskScore")) {
            return toPayload(root);
        }
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isArray() && contentNode.size() > 0) {
            for (JsonNode item : contentNode) {
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    String json = extractJsonObject(textNode.asText());
                    return toPayload(objectMapper.readTree(json));
                }
            }
        }
        if (!contentNode.isTextual()) {
            throw new RuntimeException("missing choices[0].message.content");
        }
        String content = contentNode.asText();
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("empty model content");
        }
        String json = extractJsonObject(content);
        return toPayload(objectMapper.readTree(json));
    }

    private ModerationPayload toPayload(JsonNode node) {
        BigDecimal score = parseScore(node.path("riskScore"));
        String level = trimToLength(node.path("riskLevel").asText(null), 20);
        String reason = trimToLength(node.path("reason").asText(null), 300);
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
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replace("```json", "").replace("```", "").trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                return trimmed;
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RuntimeException("model content is not valid json");
        }
        return trimmed.substring(start, end + 1);
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

    private String buildCorpus(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : fields.values()) {
            if (StringUtils.hasText(value)) {
                sb.append(' ').append(trimToLength(value, 2000));
            }
        }
        return sb.toString();
    }

    private Map<String, Object> buildMessage(String role, Object content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Set<String> parseWords(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(value)) {
            return result;
        }
        String[] arr = value.split("[,;|\\n\\r]");
        for (String item : arr) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            result.add(item.trim());
        }
        return result;
    }

    private boolean containsKeyword(String lowerCorpus, String normalizedCorpus, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        String lowered = keyword.trim().toLowerCase(Locale.ROOT);
        if (StringUtils.hasText(lowered) && lowerCorpus.contains(lowered)) {
            return true;
        }
        String normalizedKeyword = normalizeForMatch(keyword);
        return StringUtils.hasText(normalizedKeyword) && normalizedCorpus.contains(normalizedKeyword);
    }

    private String normalizeForMatch(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            char normalized = toHalfWidth(ch);
            if (Character.isLetterOrDigit(normalized) || isCjk(normalized)) {
                sb.append(Character.toLowerCase(normalized));
            }
        }
        return sb.toString();
    }

    private char toHalfWidth(char ch) {
        if (ch == 12288) {
            return ' ';
        }
        if (ch >= 65281 && ch <= 65374) {
            return (char) (ch - 65248);
        }
        return ch;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block)
                || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)
                || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B.equals(block)
                || Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block);
    }

    private String mergeLevel(String current, String next) {
        if (!StringUtils.hasText(next)) {
            return current;
        }
        if (LEVEL_HIGH.equals(next)) {
            return LEVEL_HIGH;
        }
        if (LEVEL_MEDIUM.equals(next) && LEVEL_LOW.equals(current)) {
            return LEVEL_MEDIUM;
        }
        return current;
    }

    private LayerResult allowLayer(String layer, String reason) {
        return new LayerResult(layer, DECISION_ALLOW, BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                LEVEL_LOW, trimToLength(reason, 300));
    }

    private LayerResult reviewLayer(String layer, String reason) {
        return new LayerResult(layer, DECISION_REVIEW, new BigDecimal("0.500"),
                LEVEL_MEDIUM, trimToLength(reason, 300));
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
    private static class LayerResult {
        private String layer;
        private String decision;
        private BigDecimal score;
        private String riskLevel;
        private String reason;
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
    public static class ModerationResult {
        private String scene;
        private String decision;
        private BigDecimal riskScore;
        private String riskLevel;
        private String reason;
        private String provider;
        private String model;
    }
}
