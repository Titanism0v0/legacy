package com.overseas.purchase.service;

import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CrossBorderPricingService pricingService;
    private final CrossBorderComplianceService complianceService;

    @Transactional
    public Order createOrder(Long buyerId, Long productId, Long addressId, Integer quantity,
                             BigDecimal taxEstimatedAmount,
                             Integer taxDeclarationAccepted,
                             Integer restrictedDeclarationAccepted) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("Product does not exist");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Invalid quantity");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new RuntimeException("Product is not available");
        }
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足，当前仅剩" + product.getStock() + "件");
        }
        if (product.getRestrictedFlag() != null && product.getRestrictedFlag() == 1
                && (restrictedDeclarationAccepted == null || restrictedDeclarationAccepted != 1)) {
            throw new RuntimeException("Restricted declaration must be accepted");
        }

        User seller = userMapper.selectById(product.getSellerId());
        if (seller == null || seller.getDeleted() == 1) {
            throw new RuntimeException("Seller does not exist");
        }
        if ("SELLER".equalsIgnoreCase(seller.getRole()) && !"APPROVED".equalsIgnoreCase(seller.getKycStatus())) {
            throw new RuntimeException("Seller KYC is not approved");
        }

        CrossBorderComplianceService.ComplianceResult compliance = complianceService.validateCategory(product.getCategoryId());
        if (!compliance.isAllowed()) {
            throw new RuntimeException(compliance.getReason());
        }

        CrossBorderPricingService.PriceQuote quote = pricingService.calculateQuote(product, quantity);
        if (taxEstimatedAmount != null) {
            quote.setTaxAmount(taxEstimatedAmount.setScale(2, RoundingMode.HALF_UP));
            quote.setTotalPrice(quote.getSubtotalPrice().add(quote.getShippingFee()).add(quote.getTaxAmount())
                    .setScale(2, RoundingMode.HALF_UP));
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(productId);
        order.setAddressId(addressId);
        order.setQuantity(quantity);
        order.setSubtotalPrice(quote.getSubtotalPrice());
        order.setTaxEstimatedAmount(quote.getTaxAmount());
        order.setShippingFeeSnapshot(quote.getShippingFee());
        order.setTaxRateSnapshot(quote.getTaxRate());
        order.setExchangeRateSnapshot(quote.getExchangeRate());
        order.setTaxIncludedFlag(quote.getTaxIncludedFlag());
        order.setTotalPrice(quote.getTotalPrice());
        order.setCustomsClearanceStatus(quote.getCustomsClearanceStatus());
        order.setStatus("PENDING_PAYMENT");
        order.setPaymentStatus("UNPAID");
        order.setTaxDeclarationAccepted(taxDeclarationAccepted == null ? 1 : taxDeclarationAccepted);
        order.setRestrictedDeclarationAccepted(restrictedDeclarationAccepted == null ? 0 : restrictedDeclarationAccepted);
        order.setRefundStatus("NONE");
        orderMapper.insert(order);

        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);
        return order;
    }

    public Map<String, Object> estimateOrder(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("Product does not exist");
        }
        CrossBorderComplianceService.ComplianceResult compliance = complianceService.validateCategory(product.getCategoryId());
        CrossBorderPricingService.PriceQuote quote = pricingService.calculateQuote(product, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantity", quantity == null || quantity <= 0 ? 1 : quantity);
        result.put("subtotalPrice", quote.getSubtotalPrice());
        result.put("taxEstimatedAmount", quote.getTaxAmount());
        result.put("shippingFeeSnapshot", quote.getShippingFee());
        result.put("totalPrice", quote.getTotalPrice());
        result.put("taxRateSnapshot", quote.getTaxRate());
        result.put("exchangeRateSnapshot", quote.getExchangeRate());
        result.put("taxIncludedFlag", quote.getTaxIncludedFlag());
        result.put("customsClearanceStatus", quote.getCustomsClearanceStatus());
        result.put("riskLevel", compliance.getRiskLevel());
        result.put("restrictedFlag", compliance.getRestrictedFlag());
        result.put("currency", product.getCurrency());
        return result;
    }

    @Transactional
    public void payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status");
        }
        order.setStatus("PAYMENT_PROCESSING");
        order.setPaymentStatus("PROCESSING");
        order.setPaymentChannel("MANUAL_QR");
        order.setPaymentSubmittedTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    public void shipOrder(Long orderId, String trackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status");
        }
        order.setStatus("SHIPPED");
        order.setCustomsClearanceStatus("IN_TRANSIT");
        order.setTrackingNumber(trackingNumber);
        if (order.getDomesticTrackingNumber() == null || order.getDomesticTrackingNumber().isEmpty()) {
            order.setDomesticTrackingNumber(trackingNumber);
        }
        orderMapper.updateById(order);
    }

    public void confirmReceipt(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"SHIPPED".equals(order.getStatus()) && !"DOMESTIC_SHIPPING".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status");
        }
        order.setStatus("COMPLETED");
        order.setCustomsClearanceStatus("CLEARED");
        orderMapper.updateById(order);
    }

    public void auditOrder(Long orderId, boolean approved, String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"PENDING_AUDIT".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for audit");
        }

        if (approved) {
            order.setAuditStatus("APPROVED");
            order.setStatus("PENDING_SHIPMENT");
            order.setPaymentStatus("PAID");
            order.setPaymentVerifiedTime(LocalDateTime.now());
        } else {
            order.setAuditStatus("REJECTED");
            order.setStatus("PENDING_PAYMENT");
            order.setPaymentStatus("REJECTED");
        }
        order.setAuditRemark(remark);
        order.setAuditTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    public void updateTrackingNumbers(Long orderId, String crossborderTrackingNumber, String domesticTrackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (crossborderTrackingNumber != null) {
            order.setCrossborderTrackingNumber(crossborderTrackingNumber);
        }
        if (domesticTrackingNumber != null) {
            order.setDomesticTrackingNumber(domesticTrackingNumber);
            if (!domesticTrackingNumber.isEmpty()) {
                order.setTrackingNumber(domesticTrackingNumber);
            }
        }
        orderMapper.updateById(order);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())
                && !"PAYMENT_PROCESSING".equals(order.getStatus())
                && !"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for cancellation");
        }

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() + order.getQuantity());
            productMapper.updateById(product);
        }

        order.setStatus("CANCELLED");
        if ("PAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("REFUND_PENDING");
        } else {
            order.setPaymentStatus("CANCELLED");
        }
        orderMapper.updateById(order);
    }

    public void markPaymentProcessing(Long orderId, String channel) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PAYMENT_PROCESSING".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for payment processing");
        }
        order.setStatus("PAYMENT_PROCESSING");
        order.setPaymentStatus("PROCESSING");
        order.setPaymentChannel(channel);
        order.setPaymentSubmittedTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    public void markPaymentSubmitted(Long orderId, String channel, String paymentProof) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PAYMENT_PROCESSING".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for payment submission");
        }
        order.setStatus("PENDING_AUDIT");
        order.setPaymentStatus("SUBMITTED");
        order.setPaymentChannel(channel);
        order.setPaymentProof(paymentProof);
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentSubmittedTime(now);
        order.setPaymentTime(now);
        order.setAuditStatus("PENDING");
        order.setAuditRemark(null);
        orderMapper.updateById(order);
    }

    public void markPaid(Long orderId, String channel) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus()) && "PENDING_SHIPMENT".equals(order.getStatus())) {
            return;
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PAYMENT_PROCESSING".equals(order.getStatus())) {
            throw new RuntimeException("Invalid order status for paid update");
        }
        order.setStatus("PENDING_SHIPMENT");
        order.setPaymentStatus("PAID");
        if (channel != null) {
            order.setPaymentChannel(channel);
        }
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentTime(now);
        order.setPaymentVerifiedTime(now);
        orderMapper.updateById(order);
    }

    public List<OrderDTO> getOrderList(Long buyerId, Long sellerId, String status) {
        return orderMapper.selectOrderList(buyerId, sellerId, status);
    }

    public OrderDTO getOrderById(Long id) {
        return orderMapper.selectOrderById(id);
    }

    public void deleteOrder(Long id) {
        orderMapper.deleteById(id);
    }

    public void updateRefundSnapshot(Long orderId, String refundStatus, BigDecimal refundAmount) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            return;
        }
        order.setRefundStatus(refundStatus);
        order.setRefundAmount(refundAmount);
        order.setRefundTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "ORD" + date + uuid.toUpperCase();
    }
}
