package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerDashboardTopProductDTO {
    private Long productId;
    private String productTitle;
    private String productImage;
    private Integer orderCount;
    private Integer quantitySold;
    private BigDecimal orderAmount;
    private Integer currentStock;
}
