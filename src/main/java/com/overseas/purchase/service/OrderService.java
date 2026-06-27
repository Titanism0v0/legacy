package com.overseas.purchase.service;

import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.dto.OrderEstimateDTO;
import com.overseas.purchase.entity.Address;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.AddressMapper;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final CrossBorderPricingService pricingService;
    private final CrossBorderComplianceService complianceService;
    private final AdminAuditModerationService adminAuditModerationService;
    private final OrderFulfillmentService orderFulfillmentService;

    @Transactional
    public Order createOrder(Long buyerId, Long productId, Long addressId, Integer quantity,
                             String settlementCurrency,
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
        validateBuyerAddress(buyerId, addressId);

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

        CrossBorderPricingService.PriceQuote quote = pricingService.calculateQuote(product, quantity, settlementCurrency);

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
        order.setPaymentCurrencySnapshot(quote.getPaymentCurrency());
        order.setInternationalShippingFeeSnapshot(quote.getInternationalShippingFee());
        order.setInsuranceFeeSnapshot(quote.getInsuranceFee());
        order.setTariffAmountSnapshot(quote.getTariffAmount());
        order.setVatAmountSnapshot(quote.getVatAmount());
        order.setConsumptionTaxAmountSnapshot(quote.getConsumptionTaxAmount());
        order.setTaxModeSnapshot(quote.getTaxMode());
        order.setOriginZoneSnapshot(quote.getOriginZone());
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

    private void validateBuyerAddress(Long buyerId, Long addressId) {
        if (addressId == null) {
            throw new RuntimeException("Address does not exist");
        }
        Address address = addressMapper.selectById(addressId);
        if (address == null || Integer.valueOf(1).equals(address.getDeleted())) {
            throw new RuntimeException("Address does not exist");
        }
        if (buyerId == null || !buyerId.equals(address.getUserId())) {
            throw new RuntimeException("No permission");
        }
    }

    public OrderEstimateDTO estimateOrder(Long productId, Integer quantity, String settlementCurrency) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("Product does not exist");
        }
        CrossBorderComplianceService.ComplianceResult compliance = complianceService.validateCategory(product.getCategoryId());
        CrossBorderPricingService.PriceQuote quote = pricingService.calculateQuote(product, quantity, settlementCurrency);

        OrderEstimateDTO result = new OrderEstimateDTO();
        result.setProductId(productId);
        result.setQuantity(quantity == null || quantity <= 0 ? 1 : quantity);
        result.setSubtotalPrice(quote.convertForDisplay(pricingService, quote.getSubtotalPrice()));
        result.setTaxEstimatedAmount(quote.convertForDisplay(pricingService, quote.getTaxAmount()));
        result.setShippingFeeSnapshot(quote.convertForDisplay(pricingService, quote.getShippingFee()));
        result.setTotalPrice(quote.convertForDisplay(pricingService, quote.getTotalPrice()));
        result.setPaymentSubtotalPrice(quote.getSubtotalPrice());
        result.setPaymentTaxEstimatedAmount(quote.getTaxAmount());
        result.setPaymentShippingFeeSnapshot(quote.getShippingFee());
        result.setPaymentTotalPrice(quote.getTotalPrice());
        result.setDutiablePrice(quote.convertForDisplay(pricingService, quote.getDutiablePrice()));
        result.setInternationalShippingFee(quote.convertForDisplay(pricingService, quote.getInternationalShippingFee()));
        result.setInsuranceFee(quote.convertForDisplay(pricingService, quote.getInsuranceFee()));
        result.setTariffAmount(quote.convertForDisplay(pricingService, quote.getTariffAmount()));
        result.setVatAmount(quote.convertForDisplay(pricingService, quote.getVatAmount()));
        result.setConsumptionTaxAmount(quote.convertForDisplay(pricingService, quote.getConsumptionTaxAmount()));
        result.setTaxRateSnapshot(quote.getTaxRate());
        result.setExchangeRateSnapshot(quote.getExchangeRate());
        result.setTaxIncludedFlag(quote.getTaxIncludedFlag());
        result.setCustomsClearanceStatus(quote.getCustomsClearanceStatus());
        result.setRiskLevel(compliance.getRiskLevel());
        result.setRestrictedFlag(compliance.getRestrictedFlag());
        result.setProductCurrency(product.getCurrency());
        result.setDisplayCurrency(quote.getDisplayCurrency());
        result.setPaymentCurrency(quote.getPaymentCurrency());
        result.setPaymentFallbackApplied(quote.isPaymentFallbackApplied());
        result.setTaxMode(quote.getTaxMode());
        result.setOriginZone(quote.getOriginZone());
        result.setRuleVersion(quote.getRuleVersion());
        result.setRuleSummary(quote.getRuleSummary());
        return result;
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
        Product product = productMapper.selectById(order.getProductId());
        Map<String, String> moderationFields = new LinkedHashMap<>();
        moderationFields.put("orderNo", order.getOrderNo());
        moderationFields.put("productTitle", product == null ? null : product.getTitle());
        moderationFields.put("paymentChannel", channel);
        moderationFields.put("paymentProof", paymentProof);
        moderationFields.put("buyerSeller", "buyerId=" + order.getBuyerId() + ",sellerId=" + order.getSellerId());
        AdminAuditModerationService.ModerationResult moderation = adminAuditModerationService
                .moderate("ORDER_PAYMENT_PROOF", moderationFields);

        order.setStatus("PENDING_AUDIT");
        order.setPaymentStatus("SUBMITTED");
        order.setPaymentChannel(channel);
        order.setPaymentProof(paymentProof);
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentSubmittedTime(now);
        order.setPaymentTime(now);
        order.setAuditStatus("PENDING");
        String existingRemark = StringUtils.hasText(order.getRemark()) ? order.getRemark().trim() : null;
        order.setAuditRemark(adminAuditModerationService.buildAuditRemark(moderation, existingRemark));
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

    public Map<String, Object> getOrderInsight(Long id) {
        OrderDTO order = getOrderById(id);
        if (order == null) {
            throw new RuntimeException("Order does not exist");
        }
        Product product = productMapper.selectById(order.getProductId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("fulfillment", orderFulfillmentService.describe(order.getStatus()));
        result.put("feeBreakdown", buildFeeBreakdown(order));
        result.put("taxSummary", buildTaxSummary(order));
        result.put("riskSummary", buildRiskSummary(order, product));
        return result;
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

    private List<Map<String, Object>> buildFeeBreakdown(OrderDTO order) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(feeItem("商品小计", order.getSubtotalPrice(), order.getPaymentCurrencySnapshot(), "商品价格按下单时汇率折算并快照留存"));
        list.add(feeItem("国际运费", order.getInternationalShippingFeeSnapshot(), order.getPaymentCurrencySnapshot(), "根据发货地区分区和件数估算"));
        list.add(feeItem("保险费", order.getInsuranceFeeSnapshot(), order.getPaymentCurrencySnapshot(), "按商品小计与国际运费计算最低保险费"));
        list.add(feeItem("关税", order.getTariffAmountSnapshot(), order.getPaymentCurrencySnapshot(), "根据跨境优惠或一般贸易口径估算"));
        list.add(feeItem("进口增值税", order.getVatAmountSnapshot(), order.getPaymentCurrencySnapshot(), "根据类目税率和计税口径估算"));
        list.add(feeItem("进口消费税", order.getConsumptionTaxAmountSnapshot(), order.getPaymentCurrencySnapshot(), "适用于美妆等可能涉及消费税的类目"));
        list.add(feeItem("税费合计", order.getTaxEstimatedAmount(), order.getPaymentCurrencySnapshot(), "页面估算值，最终以海关审核为准"));
        list.add(feeItem("订单合计", order.getTotalPrice(), order.getPaymentCurrencySnapshot(), "订单支付金额快照"));
        return list;
    }

    private Map<String, Object> feeItem(String label, BigDecimal amount, String currency, String note) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("amount", amount == null ? BigDecimal.ZERO : amount);
        item.put("currency", StringUtils.hasText(currency) ? currency : CrossBorderPricingService.PAYMENT_CURRENCY);
        item.put("note", note);
        return item;
    }

    private Map<String, Object> buildTaxSummary(OrderDTO order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taxMode", order.getTaxModeSnapshot());
        map.put("taxModeLabel", "CBEC_PREFERENTIAL".equals(order.getTaxModeSnapshot())
                ? "跨境零售进口优惠口径" : "一般贸易估算口径");
        map.put("originZone", order.getOriginZoneSnapshot());
        map.put("originZoneLabel", resolveOriginZoneLabel(order.getOriginZoneSnapshot()));
        map.put("exchangeRate", order.getExchangeRateSnapshot());
        map.put("effectiveTaxRate", order.getTaxRateSnapshot());
        map.put("customsClearanceStatus", order.getCustomsClearanceStatus());
        map.put("notice", "税费、清关和物流状态均为平台估算或业务节点记录，最终以实际申报和物流结果为准。");
        return map;
    }

    private Map<String, Object> buildRiskSummary(OrderDTO order, Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        String riskLevel = product == null || !StringUtils.hasText(product.getRiskLevel()) ? "LOW" : product.getRiskLevel();
        Integer restrictedFlag = product == null || product.getRestrictedFlag() == null ? 0 : product.getRestrictedFlag();
        map.put("riskLevel", riskLevel);
        map.put("restrictedFlag", restrictedFlag);
        map.put("taxDeclarationAccepted", order.getTaxDeclarationAccepted());
        map.put("restrictedDeclarationAccepted", order.getRestrictedDeclarationAccepted());
        map.put("riskNotice", buildRiskNotice(riskLevel, restrictedFlag));
        map.put("auditRemark", product == null ? null : product.getAuditRemark());
        return map;
    }

    private String resolveOriginZoneLabel(String zone) {
        if ("ASIA_NEAR".equals(zone)) {
            return "亚洲近邻";
        }
        if ("PACIFIC".equals(zone)) {
            return "太平洋区域";
        }
        if ("EUROPE".equals(zone)) {
            return "欧洲区域";
        }
        return "全球其他区域";
    }

    private String buildRiskNotice(String riskLevel, Integer restrictedFlag) {
        if (restrictedFlag != null && restrictedFlag == 1) {
            return "该商品命中禁限售或高风险规则，需平台人工复核后才可继续交易。";
        }
        if ("HIGH".equalsIgnoreCase(riskLevel)) {
            return "该订单属于高风险类目，建议保留采购凭证、物流凭证和验货记录。";
        }
        if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
            return "该订单涉及跨境常见敏感类目，平台会提示税费、清关和售后边界。";
        }
        return "该订单当前风险较低，仍需按跨境规则保留交易和物流证据。";
    }
}
