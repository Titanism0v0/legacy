package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.mapper.AfterSalesOrderMapper;
import com.overseas.purchase.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后服务类
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class AfterSalesService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final OrderMapper orderMapper;

    /**
     * 提交售后申请
     */
    @Transactional
    public void submitApply(AfterSalesOrder apply) {
        // 校验订单是否存在
        Order order = orderMapper.selectById(apply.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 校验订单是否属于该用户
        if (!order.getBuyerId().equals(apply.getUserId())) {
            throw new RuntimeException("无权操作此订单");
        }

        // 校验订单状态
        if ("PENDING_PAYMENT".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不支持售后申请");
        }

        // 校验退款金额不能超过订单总价
        if (apply.getAmount().compareTo(order.getTotalPrice()) > 0) {
            throw new RuntimeException("退款金额不能超过订单总价");
        }
        
        // 简单处理：如果之前有申请但被拒绝或已完成，允许再次申请（这里简化为如果存在PENDING状态就不允许再次申请）
        Long count = afterSalesOrderMapper.selectCount(
                new LambdaQueryWrapper<AfterSalesOrder>()
                        .eq(AfterSalesOrder::getOrderId, apply.getOrderId())
                        .eq(AfterSalesOrder::getStatus, "PENDING")
        );
        if (count > 0) {
            throw new RuntimeException("该订单已有待审核的售后申请");
        }

        apply.setStatus("PENDING");
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        apply.setDeleted(0);
        
        afterSalesOrderMapper.insert(apply);
    }

    /**
     * 分页查询售后列表
     */
    public Page<AfterSalesOrder> getList(Integer page, Integer size, Long userId, String role, String status) {
        Page<AfterSalesOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AfterSalesOrder> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果是普通用户，只能看自己的
        if ("USER".equals(role)) {
            queryWrapper.eq(AfterSalesOrder::getUserId, userId);
        }
        // 如果是卖家，暂时无法通过简单查询关联（需要关联订单表的sellerId）。
        // 这里简化：如果是SELLER角色，暂且只能看到作为买家的售后（如果有的话），或者需要扩展表结构。
        // 为满足基本需求，假设管理员(ADMIN)可以看到所有。
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(AfterSalesOrder::getStatus, status);
        }
        
        queryWrapper.orderByDesc(AfterSalesOrder::getCreateTime);
        return afterSalesOrderMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 获取详情
     */
    public AfterSalesOrder getDetail(Long id) {
        return afterSalesOrderMapper.selectById(id);
    }

    /**
     * 审核售后申请
     */
    @Transactional
    public void audit(Long id, String status, String remark) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("申请不存在");
        }
        
        if (!"PENDING".equals(apply.getStatus())) {
            throw new RuntimeException("该申请已审核");
        }

        apply.setStatus(status);
        apply.setAuditRemark(remark);
        apply.setUpdateTime(LocalDateTime.now());
        
        afterSalesOrderMapper.updateById(apply);
    }
}
