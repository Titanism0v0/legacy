package com.overseas.purchase.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityBeautifyResponseDTO {

    private String suggestedTitle;

    private String suggestedSubtitle;

    private List<String> highlights = new ArrayList<>();

    private List<String> recommendedEmoji = new ArrayList<>();

    private String recommendedTemplateId;

    private String recommendedBackgroundTag;

    private CommunityLayoutPayloadDTO renderPayload;
}
