package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.service.OrderService;
import com.overseas.purchase.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Long productId = Long.valueOf(params.get("productId").toString());
            Long addressId = Long.valueOf(params.get("addressId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());

            BigDecimal taxEstimatedAmount = null;
            if (params.get("taxEstimatedAmount") != null && !params.get("taxEstimatedAmount").toString().isEmpty()) {
                taxEstimatedAmount = new BigDecimal(params.get("taxEstimatedAmount").toString());
            }
            Integer taxDeclarationAccepted = params.get("taxDeclarationAccepted") == null
                    ? null : Integer.valueOf(params.get("taxDeclarationAccepted").toString());
            Integer restrictedDeclarationAccepted = params.get("restrictedDeclarationAccepted") == null
                    ? null : Integer.valueOf(params.get("restrictedDeclarationAccepted").toString());

            Order order = orderService.createOrder(userId, productId, addressId, quantity,
                    taxEstimatedAmount, taxDeclarationAccepted, restrictedDeclarationAccepted);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/estimate")
    public Result<Map<String, Object>> estimate(@RequestParam Long productId,
                                                @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            return Result.success(orderService.estimateOrder(productId, quantity));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/pay/{id}")
    public Result<Void> payOrder(@PathVariable Long id) {
        try {
            orderService.payOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/payment-qrcode/{id}")
    public Result<Map<String, Object>> generatePaymentQRCode(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            return Result.success(paymentService.generatePaymentQRCode(id, userId, role));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/confirm-payment/{id}")
    public Result<Void> confirmPayment(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, Object> params,
                                       HttpServletRequest request) {
        try {
            String paymentProof = params != null && params.containsKey("paymentProof")
                    ? String.valueOf(params.get("paymentProof")) : null;
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            paymentService.confirmPayment(id, paymentProof, userId, role);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/audit")
    public Result<Void> auditOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "Forbidden");
        }
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            String action = String.valueOf(params.get("action"));
            String remark = params.get("remark") == null ? null : String.valueOf(params.get("remark"));
            if (!"APPROVE".equalsIgnoreCase(action) && !"REJECT".equalsIgnoreCase(action)) {
                return Result.error("Invalid action");
            }
            orderService.auditOrder(orderId, "APPROVE".equalsIgnoreCase(action), remark);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update-tracking")
    public Result<Void> updateTracking(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            String crossborderTrackingNumber = params.get("crossborderTrackingNumber") == null
                    ? null : String.valueOf(params.get("crossborderTrackingNumber"));
            String domesticTrackingNumber = params.get("domesticTrackingNumber") == null
                    ? null : String.valueOf(params.get("domesticTrackingNumber"));

            OrderDTO order = orderService.getOrderById(orderId);
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            if (!"ADMIN".equals(role) && (order == null || !order.getSellerId().equals(userId))) {
                return Result.error("No permission");
            }

            orderService.updateTrackingNumbers(orderId, crossborderTrackingNumber, domesticTrackingNumber);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/ship")
    public Result<Void> shipOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            String trackingNumber = String.valueOf(params.get("trackingNumber"));
            OrderDTO order = orderService.getOrderById(orderId);
            Long userId = (Long) request.getAttribute("userId");
            if (order == null || !order.getSellerId().equals(userId)) {
                return Result.error("No permission");
            }

            orderService.shipOrder(orderId, trackingNumber);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/confirm/{id}")
    public Result<Void> confirmReceipt(@PathVariable Long id, HttpServletRequest request) {
        try {
            OrderDTO order = orderService.getOrderById(id);
            Long userId = (Long) request.getAttribute("userId");
            if (order == null || !order.getBuyerId().equals(userId)) {
                return Result.error("No permission");
            }
            orderService.confirmReceipt(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        try {
            OrderDTO order = orderService.getOrderById(id);
            Long userId = (Long) request.getAttribute("userId");
            if (order == null || (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))) {
                return Result.error("No permission");
            }

            orderService.cancelOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<OrderDTO>> getOrderList(@RequestParam(required = false) String status,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        List<OrderDTO> orders;
        if ("ADMIN".equals(role)) {
            orders = orderService.getOrderList(null, null, status);
        } else if ("SELLER".equals(role)) {
            orders = orderService.getOrderList(null, userId, status);
        } else {
            orders = orderService.getOrderList(userId, null, status);
        }
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    public Result<OrderDTO> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        if (order == null) {
            return Result.error("Order does not exist");
        }
        return Result.success(order);
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteOrders(@RequestBody List<Long> ids, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "Forbidden");
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

    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "Forbidden");
        }
        try {
            orderService.deleteOrder(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
