package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.entity.OrderEvidence;
import com.overseas.purchase.mapper.OrderEvidenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单证据链服务
 */
@Service
@RequiredArgsConstructor
public class OrderEvidenceService {

    private final OrderEvidenceMapper orderEvidenceMapper;

    @Transactional
    public void addEvidence(OrderEvidence evidence, Long userId) {
        evidence.setCreatedBy(userId);
        evidence.setCreateTime(LocalDateTime.now());
        evidence.setUpdateTime(LocalDateTime.now());
        evidence.setDeleted(0);
        orderEvidenceMapper.insert(evidence);
    }

    public List<OrderEvidence> listByOrderId(Long orderId) {
        return orderEvidenceMapper.selectList(
                new LambdaQueryWrapper<OrderEvidence>()
                        .eq(OrderEvidence::getOrderId, orderId)
                        .orderByAsc(OrderEvidence::getCreateTime)
        );
    }
}

