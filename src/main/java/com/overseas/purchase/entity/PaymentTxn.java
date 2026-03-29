package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_txn")
public class PaymentTxn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private String channel;
    private String outTradeNo;
    private String gatewayTradeNo;
    private BigDecimal amount;
    private String status;
    private String qrCodeUrl;
    private LocalDateTime expireTime;
    private String notifyRaw;
    private LocalDateTime notifyTime;
    private String refundStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

