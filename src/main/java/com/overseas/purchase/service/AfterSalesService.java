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

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_SELLER_REJECTED = "SELLER_REJECTED";
    private static final String STATUS_SELLER_RESPONDED = "SELLER_RESPONDED";
    private static final String STATUS_ADMIN_ARBITRATING = "ADMIN_ARBITRATING";

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

        // 关联卖家ID，便于卖家侧处理与查询
        apply.setSellerId(order.getSellerId());

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

        apply.setStatus(STATUS_PENDING);
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
        // 如果是卖家，只能看自己作为卖家的售后
        if ("SELLER".equals(role)) {
            queryWrapper.eq(AfterSalesOrder::getSellerId, userId);
        }
        
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
     * 用户申请平台介入（仅在卖家拒绝后）
     */
    @Transactional
    public void requestArbitration(Long id, Long userId) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("申请不存在");
        }
        if (apply.getUserId() == null || !apply.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作");
        }
        if (!STATUS_SELLER_REJECTED.equals(apply.getStatus())) {
            throw new RuntimeException("当前状态不支持申请平台介入");
        }
        apply.setStatus(STATUS_ADMIN_ARBITRATING);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
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
        
        if (!STATUS_PENDING.equals(apply.getStatus())) {
            throw new RuntimeException("该申请已审核");
        }

        apply.setStatus(status);
        apply.setAuditRemark(remark);
        apply.setUpdateTime(LocalDateTime.now());
        
        afterSalesOrderMapper.updateById(apply);
    }

    /**
     * 卖家响应售后（简化：写入备注并推进状态）
     */
    @Transactional
    public void sellerRespond(Long id, Long sellerId, String response) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("申请不存在");
        }
        if (apply.getSellerId() == null || !apply.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权限操作");
        }
        if (!STATUS_PENDING.equals(apply.getStatus()) && !STATUS_ADMIN_ARBITRATING.equals(apply.getStatus())) {
            throw new RuntimeException("当前状态不支持卖家响应");
        }

        apply.setStatus(STATUS_SELLER_RESPONDED);
        apply.setAuditRemark(response);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
    }

    /**
     * 卖家决策：同意/拒绝售后（卖家先处理）
     */
    @Transactional
    public void sellerDecision(Long id, Long sellerId, String decision, String remark) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("申请不存在");
        }
        if (apply.getSellerId() == null || !apply.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权限操作");
        }
        if (!STATUS_PENDING.equals(apply.getStatus())) {
            throw new RuntimeException("当前状态不支持卖家处理");
        }
        if (decision == null || decision.isEmpty()) {
            throw new RuntimeException("decision不能为空");
        }

        if ("APPROVE".equalsIgnoreCase(decision)) {
            apply.setStatus(STATUS_APPROVED);
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            apply.setStatus(STATUS_SELLER_REJECTED);
        } else {
            throw new RuntimeException("无效的decision");
        }

        apply.setAuditRemark(remark);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
    }

    /**
     * 管理员仲裁
     */
    @Transactional
    public void arbitrate(Long id, String responsibility, String result, String finalStatus) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("申请不存在");
        }

        if (!STATUS_PENDING.equals(apply.getStatus())
                && !STATUS_SELLER_RESPONDED.equals(apply.getStatus())
                && !STATUS_SELLER_REJECTED.equals(apply.getStatus())
                && !STATUS_ADMIN_ARBITRATING.equals(apply.getStatus())) {
            throw new RuntimeException("当前状态不支持仲裁");
        }

        apply.setResponsibility(responsibility);
        apply.setArbitrationResult(result);
        if (finalStatus == null || finalStatus.isEmpty()) {
            apply.setStatus(STATUS_APPROVED);
        } else {
            apply.setStatus(finalStatus);
        }
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
    }
}
