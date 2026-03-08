package com.overseas.purchase.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家评价 DTO（含评价人昵称、头像、星级）
 */
@Data
public class SellerReviewDTO {

    private Long id;

    private Long orderId;

    private Long buyerId;

    private String buyerNickname;

    private String buyerAvatar;

    private Long sellerId;

    /** 评价星级 1-5 */
    private Integer rating;

    private String content;

    private LocalDateTime createTime;
}
