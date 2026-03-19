package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单证据链实体
 */
@Data
@TableName("order_evidence")
public class OrderEvidence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String type;

    private String urls; // JSON数组字符串

    private String note;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

