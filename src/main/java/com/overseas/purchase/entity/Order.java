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
    
    private String status; // PENDING_PAYMENT-待付款，PENDING_AUDIT-待审核，PURCHASING-采购中，WAREHOUSE_CHECK-验货中，CROSSBORDER_SHIPPING-跨境运输，CUSTOMS_CLEARANCE-清关中，DOMESTIC_SHIPPING-国内运输，COMPLETED-交易成功，CANCELLED-已取消，REJECTED-审核拒绝
    
    private String trackingNumber; // 运单号（兼容旧字段，默认作为国内运单号）

    private String crossborderTrackingNumber; // 跨境运单号

    private String domesticTrackingNumber; // 国内运单号（新字段）

    private BigDecimal taxEstimatedAmount; // 预估税费

    private Integer taxDeclarationAccepted; // 税费声明是否已确认：0-否，1-是

    private Integer restrictedDeclarationAccepted; // 禁限售声明是否已确认：0-否，1-是

    private String auditStatus; // 审核状态：PENDING/APPROVED/REJECTED

    private String auditRemark; // 审核备注

    private LocalDateTime auditTime; // 审核时间
    
    private String remark; // 备注
    
    private String paymentProof; // 支付凭证（转账截图URL）
    
    private LocalDateTime paymentTime; // 支付时间
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
