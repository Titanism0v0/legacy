package com.overseas.purchase.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityLayoutPayloadDTO {

    private Integer version;

    private Source source;

    private Analysis analysis;

    private Layout layout;

    private Render render;

    @Data
    public static class Source {
        private String rawTitle;
        private String rawContent;
        private List<String> rawTags = new ArrayList<>();
        private List<String> sourceImageUrls = new ArrayList<>();
    }

    @Data
    public static class Analysis {
        private List<String> keywords = new ArrayList<>();
        private List<String> recommendedEmoji = new ArrayList<>();
        private String recommendedTemplateId;
        private String recommendedBackgroundTag;
    }

    @Data
    public static class Layout {
        private String displayTitle;
        private String displaySubtitle;
        private List<String> paragraphs = new ArrayList<>();
        private List<String> highlights = new ArrayList<>();
        private List<String> chips = new ArrayList<>();
    }

    @Data
    public static class Render {
        private String templateId;
        private String backgroundTag;
        private String backgroundImage;
        private String alignment;
        private String coverAspectRatio;
    }
}
