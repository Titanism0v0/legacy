package com.overseas.purchase.service;

import com.overseas.purchase.dto.CommunityBeautifyRequestDTO;
import com.overseas.purchase.dto.CommunityBeautifyResponseDTO;
import com.overseas.purchase.dto.CommunityLayoutPayloadDTO;
import com.overseas.purchase.entity.Category;
import com.overseas.purchase.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RuleBasedCommunityBeautifyAdvisor implements CommunityBeautifyAdvisor {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[kKwW万千]?|\\d+\\s*(GB|kg|ml|cm|寸|件|盒|套))");
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("[\\n\\r]+|(?<=[。！？!?；;])");

    private final CategoryMapper categoryMapper;

    @Override
    public CommunityBeautifyResponseDTO beautify(CommunityBeautifyRequestDTO request) {
        String normalizedTitle = normalizeText(request.getTitle());
        String normalizedContent = normalizeText(request.getContent());
        List<String> normalizedTags = normalizeList(request.getTags(), 6, 12);
        List<String> imageUrls = normalizeList(request.getImageUrls(), 6, 500);
        String categoryName = loadCategoryName(request.getCategoryId());

        String displayTitle = buildDisplayTitle(normalizedTitle, normalizedContent);
        List<String> paragraphs = buildParagraphs(normalizedContent);
        List<String> keywords = extractKeywords(displayTitle, normalizedContent, categoryName, request.getPostType(), normalizedTags);
        List<String> highlights = buildHighlights(displayTitle, normalizedContent, keywords);
        String templateId = recommendTemplate(request.getPostType(), categoryName, keywords);
        String backgroundTag = recommendBackgroundTag(request.getPostType(), categoryName, keywords);
        List<String> recommendedEmoji = recommendEmoji(request.getPostType(), categoryName, keywords);
        String subtitle = buildSubtitle(categoryName, highlights, normalizedTags);

        CommunityLayoutPayloadDTO payload = new CommunityLayoutPayloadDTO();
        payload.setVersion(1);
        payload.setSource(buildSource(displayTitle, normalizedTitle, normalizedContent, normalizedTags, imageUrls));
        payload.setAnalysis(buildAnalysis(keywords, recommendedEmoji, templateId, backgroundTag));
        payload.setLayout(buildLayout(displayTitle, subtitle, paragraphs, highlights, normalizedTags));
        payload.setRender(buildRender(templateId, backgroundTag, imageUrls));

        CommunityBeautifyResponseDTO response = new CommunityBeautifyResponseDTO();
        response.setSuggestedTitle(displayTitle);
        response.setSuggestedSubtitle(subtitle);
        response.setHighlights(highlights);
        response.setRecommendedEmoji(recommendedEmoji);
        response.setRecommendedTemplateId(templateId);
        response.setRecommendedBackgroundTag(backgroundTag);
        response.setRenderPayload(payload);
        return response;
    }

    private CommunityLayoutPayloadDTO.Source buildSource(String displayTitle,
                                                         String rawTitle,
                                                         String rawContent,
                                                         List<String> tags,
                                                         List<String> imageUrls) {
        CommunityLayoutPayloadDTO.Source source = new CommunityLayoutPayloadDTO.Source();
        source.setRawTitle(StringUtils.hasText(rawTitle) ? rawTitle : displayTitle);
        source.setRawContent(rawContent == null ? "" : rawContent);
        source.setRawTags(tags);
        source.setSourceImageUrls(imageUrls);
        return source;
    }

    private CommunityLayoutPayloadDTO.Analysis buildAnalysis(List<String> keywords,
                                                             List<String> recommendedEmoji,
                                                             String templateId,
                                                             String backgroundTag) {
        CommunityLayoutPayloadDTO.Analysis analysis = new CommunityLayoutPayloadDTO.Analysis();
        analysis.setKeywords(keywords);
        analysis.setRecommendedEmoji(recommendedEmoji);
        analysis.setRecommendedTemplateId(templateId);
        analysis.setRecommendedBackgroundTag(backgroundTag);
        return analysis;
    }

    private CommunityLayoutPayloadDTO.Layout buildLayout(String displayTitle,
                                                         String subtitle,
                                                         List<String> paragraphs,
                                                         List<String> highlights,
                                                         List<String> tags) {
        CommunityLayoutPayloadDTO.Layout layout = new CommunityLayoutPayloadDTO.Layout();
        layout.setDisplayTitle(displayTitle);
        layout.setDisplaySubtitle(subtitle);
        layout.setParagraphs(paragraphs);
        layout.setHighlights(highlights);
        layout.setChips(tags);
        return layout;
    }

    private CommunityLayoutPayloadDTO.Render buildRender(String templateId,
                                                         String backgroundTag,
                                                         List<String> imageUrls) {
        CommunityLayoutPayloadDTO.Render render = new CommunityLayoutPayloadDTO.Render();
        render.setTemplateId(templateId);
        render.setBackgroundTag(backgroundTag);
        render.setBackgroundImage(null);
        render.setAlignment("pop".equalsIgnoreCase(templateId) ? "center" : "left");
        render.setCoverAspectRatio("4:5");
        return render;
    }

    private String buildDisplayTitle(String title, String content) {
        if (StringUtils.hasText(title)) {
            return limitWithEllipsis(title, 26);
        }
        if (StringUtils.hasText(content)) {
            String firstSentence = splitToSentences(content).stream().findFirst().orElse("社区分享");
            return limitWithEllipsis(firstSentence, 26);
        }
        return "社区分享";
    }

    private List<String> buildParagraphs(String content) {
        List<String> source = splitToSentences(content);
        if (source.isEmpty()) {
            return new ArrayList<>(Arrays.asList("系统将根据标题与分类自动整理更适合分享的内容展示结构。"));
        }
        List<String> paragraphs = new ArrayList<>();
        for (String sentence : source) {
            if (paragraphs.size() >= 5) {
                break;
            }
            if (sentence.length() <= 34) {
                paragraphs.add(sentence);
                continue;
            }
            int start = 0;
            while (start < sentence.length() && paragraphs.size() < 5) {
                int end = Math.min(start + 28, sentence.length());
                paragraphs.add(sentence.substring(start, end));
                start = end;
            }
        }
        return paragraphs;
    }

    private List<String> buildHighlights(String title, String content, List<String> keywords) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Matcher matcher = NUMBER_PATTERN.matcher((title == null ? "" : title) + " " + (content == null ? "" : content));
        while (matcher.find() && result.size() < 4) {
            result.add(matcher.group(1).replaceAll("\\s+", ""));
        }
        for (String keyword : keywords) {
            if (result.size() >= 4) {
                break;
            }
            result.add(keyword);
        }
        return new ArrayList<>(result);
    }

    private List<String> extractKeywords(String title,
                                         String content,
                                         String categoryName,
                                         String postType,
                                         List<String> tags) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addMatchedKeywords(keywords, title);
        addMatchedKeywords(keywords, content);
        addMatchedKeywords(keywords, categoryName);
        addMatchedKeywords(keywords, tags);

        if ("WANTED".equalsIgnoreCase(postType)) {
            keywords.add("求购");
        } else if ("FOR_SALE".equalsIgnoreCase(postType)) {
            keywords.add("出售");
        } else if ("DISCUSSION".equalsIgnoreCase(postType)) {
            keywords.add("交流");
        }
        return new ArrayList<>(keywords).subList(0, Math.min(keywords.size(), 6));
    }

    private void addMatchedKeywords(Set<String> target, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("预算")) target.add("预算");
        if (normalized.contains("轻薄")) target.add("轻薄");
        if (normalized.contains("续航")) target.add("续航");
        if (normalized.contains("二手")) target.add("二手");
        if (normalized.contains("全新")) target.add("全新");
        if (normalized.contains("拼单")) target.add("拼单");
        if (normalized.contains("闲置")) target.add("闲置");
        if (normalized.contains("开箱")) target.add("开箱");
        if (normalized.contains("测评")) target.add("测评");
        if (normalized.contains("护肤")) target.add("护肤");
        if (normalized.contains("彩妆")) target.add("彩妆");
        if (normalized.contains("鞋")) target.add("鞋服");
        if (normalized.contains("包")) target.add("鞋服");
        if (normalized.contains("数码")) target.add("数码");
        if (normalized.contains("手机")) target.add("手机");
        if (normalized.contains("电脑")) target.add("电脑");
        if (normalized.contains("母婴")) target.add("母婴");
        if (normalized.contains("食品")) target.add("食品");
        if (normalized.contains("保健")) target.add("保健");
    }

    private void addMatchedKeywords(Set<String> target, List<String> tags) {
        for (String tag : tags) {
            if (target.size() >= 6) {
                return;
            }
            if (StringUtils.hasText(tag)) {
                target.add(limitWithEllipsis(tag, 8));
            }
        }
    }

    private String recommendTemplate(String postType, String categoryName, List<String> keywords) {
        String category = categoryName == null ? "" : categoryName;
        if (containsAny(keywords, "手机", "电脑", "数码") || category.contains("电子")) {
            return "aurora";
        }
        if (containsAny(keywords, "护肤", "彩妆") || category.contains("美妆")) {
            return "paper";
        }
        if ("FOR_SALE".equalsIgnoreCase(postType)) {
            return "pop";
        }
        if (containsAny(keywords, "食品", "保健", "母婴") || category.contains("食品") || category.contains("母婴")) {
            return "forest";
        }
        return "paper";
    }

    private String recommendBackgroundTag(String postType, String categoryName, List<String> keywords) {
        String category = categoryName == null ? "" : categoryName;
        if (containsAny(keywords, "手机", "电脑", "数码") || category.contains("电子")) {
            return "digital-light";
        }
        if (containsAny(keywords, "护肤", "彩妆") || category.contains("美妆")) {
            return "soft-paper";
        }
        if ("FOR_SALE".equalsIgnoreCase(postType)) {
            return "bold-promo";
        }
        if (containsAny(keywords, "食品", "保健", "母婴") || category.contains("食品") || category.contains("母婴")) {
            return "natural-green";
        }
        return "clean-share";
    }

    private List<String> recommendEmoji(String postType, String categoryName, List<String> keywords) {
        LinkedHashSet<String> emoji = new LinkedHashSet<>();
        if ("WANTED".equalsIgnoreCase(postType)) {
            emoji.add("🔎");
            emoji.add("🛍️");
        } else if ("FOR_SALE".equalsIgnoreCase(postType)) {
            emoji.add("🛒");
            emoji.add("✨");
        } else {
            emoji.add("💬");
            emoji.add("🌟");
        }
        if (containsAny(keywords, "手机", "电脑", "数码") || safeContains(categoryName, "电子")) {
            emoji.add("💻");
        }
        if (containsAny(keywords, "护肤", "彩妆") || safeContains(categoryName, "美妆")) {
            emoji.add("💄");
        }
        if (containsAny(keywords, "食品", "保健") || safeContains(categoryName, "食品")) {
            emoji.add("🍵");
        }
        if (safeContains(categoryName, "母婴")) {
            emoji.add("🍼");
        }
        return new ArrayList<>(emoji).subList(0, Math.min(emoji.size(), 2));
    }

    private String buildSubtitle(String categoryName, List<String> highlights, List<String> tags) {
        if (!highlights.isEmpty()) {
            return highlights.get(0);
        }
        if (!tags.isEmpty()) {
            return "#" + tags.get(0);
        }
        if (StringUtils.hasText(categoryName)) {
            return categoryName;
        }
        return "社区智能美化";
    }

    private List<String> splitToSentences(String content) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return result;
        }
        String[] parts = SENTENCE_SPLIT_PATTERN.split(content.trim());
        for (String part : parts) {
            String cleaned = normalizeText(part);
            if (StringUtils.hasText(cleaned)) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private String loadCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? "" : normalizeText(category.getName());
    }

    private List<String> normalizeList(List<String> source, int maxItems, int maxLength) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String item : source) {
            if (result.size() >= maxItems) {
                break;
            }
            String normalized = normalizeText(item);
            if (StringUtils.hasText(normalized)) {
                result.add(limitWithEllipsis(normalized, maxLength));
            }
        }
        return result;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String limitWithEllipsis(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private boolean containsAny(List<String> values, String... targets) {
        for (String value : values) {
            for (String target : targets) {
                if (target.equalsIgnoreCase(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean safeContains(String value, String target) {
        return StringUtils.hasText(value) && value.contains(target);
    }
}
