package com.overseas.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务类
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    
    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Long buyerId, Long productId, Long addressId, Integer quantity,
                             java.math.BigDecimal taxEstimatedAmount,
                             Integer taxDeclarationAccepted,
                             Integer restrictedDeclarationAccepted) {
        // 检查商品
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("商品不存在");
        }
        
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架或缺货");
        }
        
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }
        
        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(productId);
        order.setAddressId(addressId);
        order.setQuantity(quantity);
        order.setTotalPrice(product.getPrice().multiply(new java.math.BigDecimal(quantity)));
        order.setStatus("PENDING_PAYMENT");
        order.setTaxEstimatedAmount(taxEstimatedAmount);
        order.setTaxDeclarationAccepted(taxDeclarationAccepted == null ? 0 : taxDeclarationAccepted);
        order.setRestrictedDeclarationAccepted(restrictedDeclarationAccepted == null ? 0 : restrictedDeclarationAccepted);
        
        orderMapper.insert(order);
        
        // 减少库存
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);
        
        return order;
    }
    
    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }
        
        order.setStatus("PENDING_SHIPMENT");
        orderMapper.updateById(order);
    }
    
    /**
     * 发货
     */
    public void shipOrder(Long orderId, String trackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }
        
        order.setStatus("SHIPPED");
        order.setTrackingNumber(trackingNumber);
        // 兼容：新字段优先写入国内单号
        if (order.getDomesticTrackingNumber() == null || order.getDomesticTrackingNumber().isEmpty()) {
            order.setDomesticTrackingNumber(trackingNumber);
        }
        orderMapper.updateById(order);
    }
    
    /**
     * 确认收货
     */
    public void confirmReceipt(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!"SHIPPED".equals(order.getStatus()) && !"DOMESTIC_SHIPPING".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }
        
        order.setStatus("COMPLETED");
        orderMapper.updateById(order);
    }

    /**
     * 管理员：审核订单（支付后审核合规/禁限售/税费声明）
     */
    public void auditOrder(Long orderId, boolean approved, String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        if (!"PENDING_AUDIT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确，无法审核");
        }

        if (approved) {
            order.setAuditStatus("APPROVED");
            // MVP：审核通过后直接进入待发货，确保商家端可立即处理发货
            order.setStatus("PENDING_SHIPMENT");
        } else {
            order.setAuditStatus("REJECTED");
            order.setStatus("REJECTED");
        }
        order.setAuditRemark(remark);
        order.setAuditTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 卖家/管理员：更新两段运单号
     */
    public void updateTrackingNumbers(Long orderId, String crossborderTrackingNumber, String domesticTrackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }

        if (crossborderTrackingNumber != null) {
            order.setCrossborderTrackingNumber(crossborderTrackingNumber);
        }
        if (domesticTrackingNumber != null) {
            order.setDomesticTrackingNumber(domesticTrackingNumber);
            // 兼容旧字段
            if (domesticTrackingNumber != null && !domesticTrackingNumber.isEmpty()) {
                order.setTrackingNumber(domesticTrackingNumber);
            }
        }
        orderMapper.updateById(order);
    }
    
    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确，无法取消");
        }
        
        // 恢复库存
        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() + order.getQuantity());
            productMapper.updateById(product);
        }
        
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
    }
    
    /**
     * 查询订单列表
     */
    public List<OrderDTO> getOrderList(Long buyerId, Long sellerId, String status) {
        return orderMapper.selectOrderList(buyerId, sellerId, status);
    }
    
    /**
     * 根据ID查询订单详情
     */
    public OrderDTO getOrderById(Long id) {
        return orderMapper.selectOrderById(id);
    }
    
    /**
     * 删除订单（逻辑删除）
     */
    public void deleteOrder(Long id) {
        orderMapper.deleteById(id);
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "ORD" + date + uuid.toUpperCase();
    }
}
