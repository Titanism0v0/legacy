package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Audit trail for after-sales decisions and arbitration.
 */
@Data
@TableName("after_sales_audit_log")
public class AfterSalesAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long afterSalesId;

    private Long orderId;

    private String operatorRole;

    private Long operatorId;

    private String action;

    private String detail;

    private LocalDateTime createTime;
}

