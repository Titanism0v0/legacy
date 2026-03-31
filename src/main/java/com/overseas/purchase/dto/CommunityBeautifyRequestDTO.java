package com.overseas.purchase.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommunityBeautifyRequestDTO {

    private String postType;

    private Long categoryId;

    private String title;

    private String content;

    private List<String> tags;

    private List<String> imageUrls;
}
