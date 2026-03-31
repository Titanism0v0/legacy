package com.overseas.purchase.dto;

import lombok.Data;

@Data
public class CommunityPostCreateDTO {

    private String postType;

    private String title;

    private String content;

    private Long categoryId;

    private String contentMode;

    private String renderPayload;

    private String images;

    private String coverImage;

    private String coverTemplate;
}
