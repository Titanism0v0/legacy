package com.overseas.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单DTO（包含商品和地址信息）
 * 
 * @author System
 */
@Data
public class OrderDTO {
    
    private Long id;
    
    private String orderNo;
    
    private Long buyerId;
    
    private String buyerNickname;
    
    private Long sellerId;
    
    private String sellerNickname;
    
    private Long productId;
    
    private String productTitle;
    
    private String productImage;
    
    private Long addressId;
    
    private String receiverName;
    
    private String receiverPhone;
    
    private String fullAddress;
    
    private Integer quantity;
    
    private BigDecimal totalPrice;
    
    private String status;
    
    private String trackingNumber;
    
    private String remark;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
