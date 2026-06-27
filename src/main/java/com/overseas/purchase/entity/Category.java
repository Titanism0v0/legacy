package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体类
 * 
 * @author System
 */
@Data
@TableName("category")
public class Category {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private Long parentId; // 父分类ID，0表示顶级分类
    
    private Integer sortOrder;

    private Integer cbecEnabled;

    private java.math.BigDecimal importVatRate;

    private java.math.BigDecimal consumptionTaxRate;

    private java.math.BigDecimal generalTariffRate;

    private String taxRuleNote;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
