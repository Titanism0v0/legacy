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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final Map<String, List<String>> BUILTIN_SENSITIVE_VARIANTS = initSensitiveVariants();

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

    @Value("${moderation.image.ai-enabled:false}")
    private boolean imageAiEnabled;

    @Value("${moderation.image.block-keywords:weapon,gun,drugs,porn,explosive}")
    private String imageBlockKeywords;

    @Value("${moderation.image.review-keywords:wechat,whatsapp,telegram,casino,gamble}")
    private String imageReviewKeywords;

    public ModerationDecision moderatePost(CommunityPostCreateDTO dto) {
        long start = System.currentTimeMillis();
        List<LayerResult> layers = new ArrayList<>();

        LayerResult layer1 = sensitiveWordLayer(dto);
        layers.add(layer1);
        if (DECISION_BLOCK.equals(layer1.getDecision())) {
            return finalizeDecision(layers, start, dto);
        }

        LayerResult layer2 = textAiLayer(dto);
        layers.add(layer2);
        if (DECISION_BLOCK.equals(layer2.getDecision())) {
            return finalizeDecision(layers, start, dto);
        }

        LayerResult layer3 = imageLayer(dto);
        layers.add(layer3);
        return finalizeDecision(layers, start, dto);
    }

    private LayerResult sensitiveWordLayer(CommunityPostCreateDTO dto) {
        String rawCorpus = buildTextCorpus(dto);
        String lowerCorpus = rawCorpus.toLowerCase(Locale.ROOT);
        String normalizedCorpus = normalizeForMatch(rawCorpus);
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
            return new LayerResult("L1-SENSITIVE", DECISION_BLOCK, BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP),
                    LEVEL_HIGH, "Sensitive blocked words hit: " + String.join(",", blockHits));
        }

        Set<String> reviewHits = new LinkedHashSet<>();
        for (String word : reviewWords) {
            if (containsKeyword(lowerCorpus, normalizedCorpus, word)) {
                reviewHits.add(word);
            }
        }
        if (!reviewHits.isEmpty()) {
            return new LayerResult("L1-SENSITIVE", DECISION_REVIEW, new BigDecimal("0.650"),
                    LEVEL_MEDIUM, "Sensitive review words hit: " + String.join(",", reviewHits));
        }
        return allowLayer("L1-SENSITIVE", "No sensitive words matched");
    }

    private LayerResult textAiLayer(CommunityPostCreateDTO dto) {
        if (!enabled) {
            return aiRequired
                    ? reviewLayer("L2-TEXT-AI", "AI moderation disabled")
                    : allowLayer("L2-TEXT-AI", "AI moderation disabled but aiRequired=false");
        }
        if (!StringUtils.hasText(apiKey)) {
            return aiRequired
                    ? reviewLayer("L2-TEXT-AI", "AI key missing")
                    : allowLayer("L2-TEXT-AI", "AI key missing but aiRequired=false");
        }

        try {
            ModerationPayload payload = requestTextModeration(dto);
            BigDecimal score = normalizeScore(payload.getRiskScore());
            String decision = decisionByScore(score);
            String level = normalizeRiskLevel(payload.getRiskLevel(), score);
            String reason = trimToLength(payload.getReason(), 500);
            if (!StringUtils.hasText(reason)) {
                reason = "AI text moderation result";
            }
            return new LayerResult("L2-TEXT-AI", decision, score, level, reason);
        } catch (Exception e) {
            log.warn("community text moderation failed; failureType={}", e.getClass().getName(), e);
            return aiRequired
                    ? reviewLayer("L2-TEXT-AI", "AI text moderation service unavailable; manual review required")
                    : allowLayer("L2-TEXT-AI", "AI text moderation failed but aiRequired=false");
        }
    }

    private LayerResult imageLayer(CommunityPostCreateDTO dto) {
        List<String> imageUrls = collectImageUrls(dto);
        if (imageUrls.isEmpty()) {
            return allowLayer("L3-IMAGE", "No images to moderate");
        }

        LayerResult urlRuleResult = imageUrlRuleLayer(imageUrls);
        if (!DECISION_ALLOW.equals(urlRuleResult.getDecision())) {
            return urlRuleResult;
        }

        if (!imageAiEnabled) {
            return allowLayer("L3-IMAGE", "Image AI disabled, URL rules passed");
        }
        if (!enabled) {
            return aiRequired
                    ? reviewLayer("L3-IMAGE-AI", "Image AI disabled")
                    : allowLayer("L3-IMAGE-AI", "Image AI disabled but aiRequired=false");
        }
        if (!StringUtils.hasText(apiKey)) {
            return aiRequired
                    ? reviewLayer("L3-IMAGE-AI", "Image AI key missing")
                    : allowLayer("L3-IMAGE-AI", "Image AI key missing but aiRequired=false");
        }

        try {
            ModerationPayload payload = requestImageModeration(dto, imageUrls);
            BigDecimal score = normalizeScore(payload.getRiskScore());
            String decision = decisionByScore(score);
            String level = normalizeRiskLevel(payload.getRiskLevel(), score);
            String reason = trimToLength(payload.getReason(), 500);
            if (!StringUtils.hasText(reason)) {
                reason = "AI image moderation result";
            }
            return new LayerResult("L3-IMAGE-AI", decision, score, level, reason);
        } catch (Exception e) {
            log.warn("community image moderation failed; failureType={}", e.getClass().getName(), e);
            return aiRequired
                    ? reviewLayer("L3-IMAGE-AI", "AI image moderation service unavailable; manual review required")
                    : allowLayer("L3-IMAGE-AI", "AI image moderation failed but aiRequired=false");
        }
    }

    private LayerResult imageUrlRuleLayer(List<String> imageUrls) {
        String corpus = String.join(" ", imageUrls).toLowerCase(Locale.ROOT);
        Set<String> blockWords = parseWords(imageBlockKeywords);
        Set<String> reviewWords = parseWords(imageReviewKeywords);

        Set<String> blockHits = new LinkedHashSet<>();
        for (String word : blockWords) {
            if (corpus.contains(word.toLowerCase(Locale.ROOT))) {
                blockHits.add(word);
            }
        }
        if (!blockHits.isEmpty()) {
            return new LayerResult("L3-IMAGE-RULE", DECISION_BLOCK, BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP),
                    LEVEL_HIGH, "Image URL blocked keywords hit: " + String.join(",", blockHits));
        }

        Set<String> reviewHits = new LinkedHashSet<>();
        for (String word : reviewWords) {
            if (corpus.contains(word.toLowerCase(Locale.ROOT))) {
                reviewHits.add(word);
            }
        }
        if (!reviewHits.isEmpty()) {
            return new LayerResult("L3-IMAGE-RULE", DECISION_REVIEW, new BigDecimal("0.650"),
                    LEVEL_MEDIUM, "Image URL review keywords hit: " + String.join(",", reviewHits));
        }
        return allowLayer("L3-IMAGE-RULE", "Image URL rules passed");
    }

    private ModerationDecision finalizeDecision(List<LayerResult> layers, long start, CommunityPostCreateDTO dto) {
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
        String finalReason = trimToLength(String.join(" | ", reasons), 500);
        if (!StringUtils.hasText(finalReason)) {
            finalReason = "Hybrid moderation result";
        }

        log.info("community moderation final decision={} score={} level={} costMs={} titleLen={} contentLen={} imageCount={}",
                finalDecision, finalScore, finalLevel, System.currentTimeMillis() - start,
                lengthOf(dto.getTitle()), lengthOf(dto.getContent()), estimateImageCount(dto));

        return new ModerationDecision(finalDecision, finalScore, finalLevel, finalReason, providerName(), modelName());
    }

    private ModerationPayload requestTextModeration(CommunityPostCreateDTO dto) throws Exception {
        List<Map<String, Object>> messages = Arrays.asList(
                buildMessage("system",
                        "You are a strict text moderation classifier for marketplace community posts. " +
                                "Return JSON only with fields: riskScore (0 to 1), riskLevel (LOW|MEDIUM|HIGH), reason (max 120 chars)."),
                buildMessage("user", buildTextPrompt(dto))
        );
        JsonNode root = invokeChatCompletion(messages);
        return parseModerationPayload(root);
    }

    private ModerationPayload requestImageModeration(CommunityPostCreateDTO dto, List<String> imageUrls) throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", buildImagePrompt(dto, imageUrls));
        content.add(textPart);

        for (String imageUrl : imageUrls) {
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> imageUrlObj = new HashMap<>();
            imageUrlObj.put("url", imageUrl);
            imagePart.put("image_url", imageUrlObj);
            content.add(imagePart);
        }

        List<Map<String, Object>> messages = Arrays.asList(
                buildMessage("system",
                        "You are a strict image moderation classifier for marketplace community posts. " +
                                "Inspect provided image URLs and return JSON only with: riskScore, riskLevel, reason."),
                buildMessage("user", content)
        );
        JsonNode root = invokeChatCompletion(messages);
        return parseModerationPayload(root);
    }

    private JsonNode invokeChatCompletion(List<Map<String, Object>> messages) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
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

    private Map<String, Object> buildMessage(String role, Object content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildTextPrompt(CommunityPostCreateDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("postType=").append(trimToLength(dto.getPostType(), 50)).append('\n');
        sb.append("title=").append(trimToLength(dto.getTitle(), 200)).append('\n');
        sb.append("content=").append(trimToLength(dto.getContent(), 1200)).append('\n');
        sb.append("contentMode=").append(trimToLength(dto.getContentMode(), 20)).append('\n');
        sb.append("renderPayload=").append(trimToLength(dto.getRenderPayload(), 1200)).append('\n');
        sb.append("Assess violations such as illegal goods, hate/violence, pornographic/explicit, scam/fraud, personal attacks, privacy abuse.");
        return sb.toString();
    }

    private String buildImagePrompt(CommunityPostCreateDTO dto, List<String> imageUrls) {
        StringBuilder sb = new StringBuilder();
        sb.append("postType=").append(trimToLength(dto.getPostType(), 50)).append('\n');
        sb.append("title=").append(trimToLength(dto.getTitle(), 200)).append('\n');
        sb.append("content=").append(trimToLength(dto.getContent(), 600)).append('\n');
        sb.append("imageCount=").append(imageUrls.size()).append('\n');
        sb.append("Evaluate image-related risks only (violence, explicit sexual content, drugs, weapons, fraud clues).");
        return sb.toString();
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

    private List<String> collectImageUrls(CommunityPostCreateDTO dto) {
        Set<String> result = new LinkedHashSet<>();
        appendUrls(result, dto.getImages());
        if (StringUtils.hasText(dto.getCoverImage())) {
            result.add(dto.getCoverImage().trim());
        }
        return new ArrayList<>(result);
    }

    private void appendUrls(Set<String> out, String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] arr = trimmed.split(",");
        for (String item : arr) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String url = item.trim();
            if (url.startsWith("\"")) {
                url = url.substring(1);
            }
            if (url.endsWith("\"")) {
                url = url.substring(0, url.length() - 1);
            }
            if (StringUtils.hasText(url)) {
                out.add(url.trim());
            }
        }
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

    private Set<String> expandWithBuiltinVariants(Set<String> configuredWords) {
        Set<String> result = new LinkedHashSet<>();
        if (configuredWords == null || configuredWords.isEmpty()) {
            return result;
        }
        for (String raw : configuredWords) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String[] explicitVariants = raw.split("\\|");
            for (String item : explicitVariants) {
                if (!StringUtils.hasText(item)) {
                    continue;
                }
                String word = item.trim();
                result.add(word);
                List<String> builtin = BUILTIN_SENSITIVE_VARIANTS.get(normalizeForMatch(word));
                if (builtin != null && !builtin.isEmpty()) {
                    result.addAll(builtin);
                }
            }
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

    private String buildTextCorpus(CommunityPostCreateDTO dto) {
        StringBuilder sb = new StringBuilder();
        appendNullable(sb, dto.getTitle(), 500);
        appendNullable(sb, dto.getContent(), 5000);
        appendNullable(sb, dto.getRenderPayload(), 5000);
        appendNullable(sb, dto.getImages(), 2000);
        appendNullable(sb, dto.getCoverImage(), 500);
        return sb.toString();
    }

    private void appendNullable(StringBuilder sb, String value, int maxLen) {
        String trimmed = trimToLength(value, maxLen);
        if (StringUtils.hasText(trimmed)) {
            sb.append(' ').append(trimmed);
        }
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
                LEVEL_LOW, trimToLength(reason, 500));
    }

    private LayerResult reviewLayer(String layer, String reason) {
        return new LayerResult(layer, DECISION_REVIEW, new BigDecimal("0.500"),
                LEVEL_MEDIUM, trimToLength(reason, 500));
    }

    private int lengthOf(String text) {
        return text == null ? 0 : text.length();
    }

    private int estimateImageCount(CommunityPostCreateDTO dto) {
        return collectImageUrls(dto).size();
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

    private static Map<String, List<String>> initSensitiveVariants() {
        Map<String, List<String>> variants = new HashMap<>();
        putVariants(variants, "毒品", "毒品", "dupin", "dp");
        putVariants(variants, "海洛因", "海洛因", "hailuoyin", "hly", "4号", "四号", "4hao");
        putVariants(variants, "冰毒", "冰毒", "bingdu", "甲基苯丙胺", "meth");
        putVariants(variants, "可卡因", "可卡因", "kekayin", "cocaine");
        putVariants(variants, "大麻", "大麻", "dama", "weed", "marijuana", "mj");
        putVariants(variants, "芬太尼", "芬太尼", "fentanyl", "ftn");
        putVariants(variants, "k粉", "k粉", "kfen", "ketamine", "氯胺酮");
        putVariants(variants, "枪支", "枪支", "手枪", "步枪", "qiangzhi");
        putVariants(variants, "炸药", "炸药", "zhayao", "tnt", "雷管", "手雷");
        putVariants(variants, "恐怖", "恐怖", "terror", "extremism");
        putVariants(variants, "儿童色情", "儿童色情", "幼童色情", "childporn", "cp");

        putVariants(variants, "加v", "加v", "加V", "+v", "加vx", "加v信", "加微", "加威");
        putVariants(variants, "私聊", "私聊", "私信", "pm");
        putVariants(variants, "引流", "引流", "导流", "拉群");
        putVariants(variants, "刷单", "刷单", "代刷");
        putVariants(variants, "博彩", "博彩", "赌博", "casino", "bet", "gamble");
        putVariants(variants, "wechat", "wechat", "wx", "vx", "vx", "v信", "微x", "微信");
        putVariants(variants, "telegram", "telegram", "tg", "电报");
        putVariants(variants, "whatsapp", "whatsapp", "wa");
        return variants;
    }

    private static void putVariants(Map<String, List<String>> target, String key, String... values) {
        String normalizedKey = normalizeForMatchStatic(key);
        List<String> list = target.computeIfAbsent(normalizedKey, k -> new ArrayList<>());
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                list.add(value.trim());
            }
        }
    }

    private static String normalizeForMatchStatic(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            char normalized = toHalfWidthStatic(ch);
            if (Character.isLetterOrDigit(normalized) || isCjkStatic(normalized)) {
                sb.append(Character.toLowerCase(normalized));
            }
        }
        return sb.toString();
    }

    private static char toHalfWidthStatic(char ch) {
        if (ch == 12288) {
            return ' ';
        }
        if (ch >= 65281 && ch <= 65374) {
            return (char) (ch - 65248);
        }
        return ch;
    }

    private static boolean isCjkStatic(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block)
                || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)
                || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B.equals(block)
                || Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block);
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
    public static class ModerationDecision {
        private String decision;
        private BigDecimal riskScore;
        private String riskLevel;
        private String reason;
        private String provider;
        private String model;
    }
}
