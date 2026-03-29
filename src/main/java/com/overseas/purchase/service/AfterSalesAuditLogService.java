package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.entity.AfterSalesAuditLog;
import com.overseas.purchase.mapper.AfterSalesAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AfterSalesAuditLogService {

    private final AfterSalesAuditLogMapper auditLogMapper;

    public void record(Long afterSalesId, Long orderId, String operatorRole, Long operatorId, String action, String detail) {
        AfterSalesAuditLog log = new AfterSalesAuditLog();
        log.setAfterSalesId(afterSalesId);
        log.setOrderId(orderId);
        log.setOperatorRole(operatorRole);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    public List<AfterSalesAuditLog> listByAfterSalesId(Long afterSalesId) {
        return auditLogMapper.selectList(new LambdaQueryWrapper<AfterSalesAuditLog>()
                .eq(AfterSalesAuditLog::getAfterSalesId, afterSalesId)
                .orderByAsc(AfterSalesAuditLog::getCreateTime));
    }
}

