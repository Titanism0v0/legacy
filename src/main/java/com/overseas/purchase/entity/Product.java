package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 
 * @author System
 */
@Data
@TableName("product")
public class Product {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sellerId;
    
    private Long categoryId;
    
    private String title;
    
    private String description;
    
    private BigDecimal price;
    
    private String currency; // 货币单位
    
    private Integer stock;
    
    private String image; // 主图
    
    private String images; // 图片列表（JSON格式）
    
    private String shippingAddress; // 发货地址
    
    private String status; // ON_SALE-在售，OFF_SALE-下架，OUT_OF_STOCK-缺货

    private String auditStatus; // 审核状态：PENDING/APPROVED/REJECTED

    private String auditRemark; // 审核/下架原因

    private String riskLevel; // 风险等级：LOW/MEDIUM/HIGH

    private Integer restrictedFlag; // 禁限售标记：0-否，1-是
    
    private Integer viewCount; // 浏览次数
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
