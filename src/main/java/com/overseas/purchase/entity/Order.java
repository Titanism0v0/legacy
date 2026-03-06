package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 
 * @author System
 */
@Data
@TableName("`order`")
public class Order {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo; // 订单号
    
    private Long buyerId; // 买家ID
    
    private Long sellerId; // 卖家ID
    
    private Long productId; // 商品ID
    
    private Long addressId; // 收货地址ID
    
    private Integer quantity; // 商品数量
    
    private BigDecimal totalPrice; // 订单总价
    
    private String status; // PENDING_PAYMENT-待付款，PENDING_SHIPMENT-待发货，SHIPPED-已发货，COMPLETED-交易成功，CANCELLED-已取消
    
    private String trackingNumber; // 运单号
    
    private String remark; // 备注
    
    @TableField(exist = false) // 如果数据库中没有此字段，设置为false
    private String paymentProof; // 支付凭证（转账截图URL）
    
    @TableField(exist = false) // 如果数据库中没有此字段，设置为false
    private LocalDateTime paymentTime; // 支付时间
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
