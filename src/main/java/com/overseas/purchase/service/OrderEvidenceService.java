package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.OrderEvidence;
import com.overseas.purchase.mapper.OrderEvidenceMapper;
import com.overseas.purchase.mapper.OrderMapper;
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
    private final OrderMapper orderMapper;

    @Transactional
    public void addEvidence(OrderEvidence evidence, Long userId, String role) {
        getAccessibleOrder(evidence.getOrderId(), userId, role);
        evidence.setCreatedBy(userId);
        evidence.setCreateTime(LocalDateTime.now());
        evidence.setUpdateTime(LocalDateTime.now());
        evidence.setDeleted(0);
        orderEvidenceMapper.insert(evidence);
    }

    public List<OrderEvidence> listByOrderId(Long orderId, Long userId, String role) {
        getAccessibleOrder(orderId, userId, role);
        return orderEvidenceMapper.selectList(
                new LambdaQueryWrapper<OrderEvidence>()
                        .eq(OrderEvidence::getOrderId, orderId)
                        .orderByAsc(OrderEvidence::getCreateTime)
        );
    }

    private Order getAccessibleOrder(Long orderId, Long userId, String role) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!canAccessOrder(order, userId, role)) {
            throw new RuntimeException("No permission");
        }
        return order;
    }

    private boolean canAccessOrder(Order order, Long userId, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return userId.equals(order.getBuyerId()) || userId.equals(order.getSellerId());
    }
}

