package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品DTO（包含卖家信息）
 * 
 * @author System
 */
@Data
public class ProductDTO {
    
    private Long id;
    
    private Long sellerId;
    
    private String sellerNickname;
    
    private String sellerAvatar;
    
    private Long categoryId;
    
    private String categoryName;
    
    private String title;
    
    private String description;
    
    private BigDecimal price;
    
    private String currency;
    
    private Integer stock;
    
    private String image;
    
    private String images;
    
    private String shippingAddress;
    
    private String status;

    private String auditStatus;

    private String auditRemark;

    private String riskLevel;

    private Integer restrictedFlag;
    
    private Integer viewCount;
    
    private LocalDateTime createTime;
}
