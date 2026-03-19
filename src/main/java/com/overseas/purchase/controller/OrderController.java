package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.service.OrderService;
import com.overseas.purchase.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * 
 * @author System
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    
    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<com.overseas.purchase.entity.Order> createOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Long productId = Long.valueOf(params.get("productId").toString());
            Long addressId = Long.valueOf(params.get("addressId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());

            java.math.BigDecimal taxEstimatedAmount = null;
            if (params.get("taxEstimatedAmount") != null && !params.get("taxEstimatedAmount").toString().isEmpty()) {
                taxEstimatedAmount = new java.math.BigDecimal(params.get("taxEstimatedAmount").toString());
            }
            Integer taxDeclarationAccepted = params.get("taxDeclarationAccepted") == null ? null : Integer.valueOf(params.get("taxDeclarationAccepted").toString());
            Integer restrictedDeclarationAccepted = params.get("restrictedDeclarationAccepted") == null ? null : Integer.valueOf(params.get("restrictedDeclarationAccepted").toString());

            com.overseas.purchase.entity.Order order = orderService.createOrder(userId, productId, addressId, quantity,
                    taxEstimatedAmount, taxDeclarationAccepted, restrictedDeclarationAccepted);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 支付订单（模拟支付，直接完成）
     */
    @PostMapping("/pay/{id}")
    public Result<Void> payOrder(@PathVariable Long id) {
        try {
            orderService.payOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成支付二维码（个人收款码方案）
     */
    @GetMapping("/payment-qrcode/{id}")
    public Result<Map<String, Object>> generatePaymentQRCode(@PathVariable Long id) {
        try {
            Map<String, Object> qrCodeInfo = paymentService.generatePaymentQRCode(id);
            return Result.success(qrCodeInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 确认支付（用户点击"我已支付"后调用）
     */
    @PostMapping("/confirm-payment/{id}")
    public Result<Void> confirmPayment(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        try {
            String paymentProof = null;
            if (params != null && params.containsKey("paymentProof")) {
                paymentProof = (String) params.get("paymentProof");
            }
            paymentService.confirmPayment(id, paymentProof);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员：审核订单（支付后进入待审核）
     */
    @PostMapping("/audit")
    public Result<Void> auditOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限访问");
        }
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            String action = params.get("action").toString();
            String remark = params.get("remark") == null ? null : params.get("remark").toString();

            boolean approved;
            if ("APPROVE".equalsIgnoreCase(action)) {
                approved = true;
            } else if ("REJECT".equalsIgnoreCase(action)) {
                approved = false;
            } else {
                return Result.error("无效的审核动作");
            }

            orderService.auditOrder(orderId, approved, remark);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 卖家/管理员：更新两段运单号
     */
    @PostMapping("/update-tracking")
    public Result<Void> updateTracking(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            String crossborderTrackingNumber = params.get("crossborderTrackingNumber") == null ? null : params.get("crossborderTrackingNumber").toString();
            String domesticTrackingNumber = params.get("domesticTrackingNumber") == null ? null : params.get("domesticTrackingNumber").toString();

            OrderDTO order = orderService.getOrderById(orderId);
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            if (!"ADMIN".equals(role) && (order == null || !order.getSellerId().equals(userId))) {
                return Result.error("无权限操作");
            }

            orderService.updateTrackingNumbers(orderId, crossborderTrackingNumber, domesticTrackingNumber);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 卖家：发货
     */
    @PostMapping("/ship")
    public Result<Void> shipOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            String trackingNumber = params.get("trackingNumber").toString();
            
            // 验证是否为卖家
            OrderDTO order = orderService.getOrderById(orderId);
            Long userId = (Long) request.getAttribute("userId");
            if (!order.getSellerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            orderService.shipOrder(orderId, trackingNumber);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 买家：确认收货
     */
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmReceipt(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 验证是否为买家
            OrderDTO order = orderService.getOrderById(id);
            Long userId = (Long) request.getAttribute("userId");
            if (!order.getBuyerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            orderService.confirmReceipt(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/cancel/{id}")
    public Result<Void> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 验证权限
            OrderDTO order = orderService.getOrderById(id);
            Long userId = (Long) request.getAttribute("userId");
            if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            orderService.cancelOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 查询订单列表
     */
    @GetMapping("/list")
public Result<List<OrderDTO>> getOrderList(@RequestParam(required = false) String status,
                                           HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    String role = (String) request.getAttribute("role");
    
    List<OrderDTO> orders;
    if ("ADMIN".equals(role)) {
        // 管理员查看所有订单
        orders = orderService.getOrderList(null, null, status);  // 两个参数为 null 表示不限买家或卖家
    } else if ("SELLER".equals(role)) {
        // 卖家查看自己的订单
        orders = orderService.getOrderList(null, userId, status);
    } else {
        // 买家查看自己的订单
        orders = orderService.getOrderList(userId, null, status);
    }
    
    return Result.success(orders);
}
    
    /**
     * 根据ID查询订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDTO> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }
    
    /**
     * 管理员：批量删除订单
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteOrders(@RequestBody List<Long> ids, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限访问");
        }
        
        try {
            for (Long id : ids) {
                orderService.deleteOrder(id);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员：删除订单
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限访问");
        }
        
        try {
            orderService.deleteOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
