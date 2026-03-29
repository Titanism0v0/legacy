package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.OrderEvidence;
import com.overseas.purchase.mapper.AfterSalesOrderMapper;
import com.overseas.purchase.mapper.OrderEvidenceMapper;
import com.overseas.purchase.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AfterSalesService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderEvidenceMapper orderEvidenceMapper;
    private final OrderService orderService;
    private final AfterSalesDecisionService decisionService;
    private final AfterSalesAuditLogService auditLogService;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_SELLER_REJECTED = "SELLER_REJECTED";
    private static final String STATUS_SELLER_RESPONDED = "SELLER_RESPONDED";
    private static final String STATUS_ADMIN_ARBITRATING = "ADMIN_ARBITRATING";

    @Transactional
    public void submitApply(AfterSalesOrder apply) {
        Order order = orderMapper.selectById(apply.getOrderId());
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("Order does not exist");
        }
        if (!order.getBuyerId().equals(apply.getUserId())) {
            throw new RuntimeException("No permission");
        }
        if ("PENDING_PAYMENT".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Current order status does not support after-sales");
        }
        if (apply.getAmount() == null || apply.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid refund amount");
        }
        if (apply.getAmount().compareTo(order.getTotalPrice()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed order total");
        }

        Long pendingCount = afterSalesOrderMapper.selectCount(new LambdaQueryWrapper<AfterSalesOrder>()
                .eq(AfterSalesOrder::getOrderId, apply.getOrderId())
                .in(AfterSalesOrder::getStatus, STATUS_PENDING, STATUS_ADMIN_ARBITRATING));
        if (pendingCount != null && pendingCount > 0) {
            throw new RuntimeException("This order already has an active after-sales request");
        }

        List<String> evidenceUrls = parseJsonArray(firstNonBlank(apply.getEvidenceUrls(), apply.getImages()));
        String evidenceText = firstNonBlank(apply.getEvidenceText(), apply.getDescription());
        if (evidenceUrls.isEmpty() && (!StringUtils.hasText(evidenceText) || evidenceText.trim().length() < 10)) {
            throw new RuntimeException("Please provide evidence: image/video links or detailed text");
        }

        apply.setSellerId(order.getSellerId());
        apply.setEvidenceUrls(toJsonArray(evidenceUrls));
        apply.setEvidenceType(resolveEvidenceType(evidenceUrls, evidenceText));
        apply.setEvidenceText(evidenceText);
        apply.setImages(toImageJson(evidenceUrls));

        AfterSalesDecisionService.DecisionResult decision =
                decisionService.evaluate(order, apply, evidenceUrls.size(), evidenceText == null ? 0 : evidenceText.length());
        apply.setRuleDecision(decision.getRuleDecision());
        apply.setRuleReason(decision.getRuleReason());
        apply.setAiScore(decision.getAiScore());
        apply.setAiSuggestion(decision.getAiSuggestion());
        apply.setAiReason(decision.getAiReason());
        apply.setStatus(decision.getFinalStatus());
        apply.setAuditRemark("AUTO_APPROVE".equals(decision.getRuleDecision()) || "AUTO_REJECT".equals(decision.getRuleDecision())
                ? decision.getRuleReason() : null);

        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        apply.setDeleted(0);
        afterSalesOrderMapper.insert(apply);

        persistEvidence(order.getId(), apply.getUserId(), apply.getEvidenceType(), evidenceUrls, evidenceText);
        syncRefundSnapshot(order.getId(), apply.getAmount(), apply.getStatus());

        auditLogService.record(apply.getId(), order.getId(), "SYSTEM", 0L, "APPLY_SUBMITTED",
                "rule=" + apply.getRuleDecision() + ", ai=" + apply.getAiScore() + ", status=" + apply.getStatus());
    }

    public Page<AfterSalesOrder> getList(Integer page, Integer size, Long userId, String role, String status) {
        Page<AfterSalesOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AfterSalesOrder> queryWrapper = new LambdaQueryWrapper<>();
        if ("USER".equals(role)) {
            queryWrapper.eq(AfterSalesOrder::getUserId, userId);
        }
        if ("SELLER".equals(role)) {
            queryWrapper.eq(AfterSalesOrder::getSellerId, userId);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(AfterSalesOrder::getStatus, status);
        }
        queryWrapper.orderByDesc(AfterSalesOrder::getCreateTime);
        return afterSalesOrderMapper.selectPage(pageParam, queryWrapper);
    }

    public AfterSalesOrder getDetail(Long id) {
        return afterSalesOrderMapper.selectById(id);
    }

    @Transactional
    public void requestArbitration(Long id, Long userId) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("Request does not exist");
        }
        if (!apply.getUserId().equals(userId)) {
            throw new RuntimeException("No permission");
        }
        if (!STATUS_SELLER_REJECTED.equals(apply.getStatus())) {
            throw new RuntimeException("Current status does not support arbitration request");
        }
        apply.setStatus(STATUS_ADMIN_ARBITRATING);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
        auditLogService.record(apply.getId(), apply.getOrderId(), "USER", userId, "REQUEST_ARBITRATION", "user requested admin arbitration");
    }

    @Transactional
    public void audit(Long id, String status, String remark) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("Request does not exist");
        }
        if (!STATUS_PENDING.equals(apply.getStatus()) && !STATUS_ADMIN_ARBITRATING.equals(apply.getStatus())) {
            throw new RuntimeException("Request already processed");
        }

        apply.setStatus(status);
        apply.setAuditRemark(remark);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
        syncRefundSnapshot(apply.getOrderId(), apply.getAmount(), status);
        auditLogService.record(apply.getId(), apply.getOrderId(), "ADMIN", 0L, "AUDIT_" + status, remark);
    }

    @Transactional
    public void sellerRespond(Long id, Long sellerId, String response) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("Request does not exist");
        }
        if (!apply.getSellerId().equals(sellerId)) {
            throw new RuntimeException("No permission");
        }
        if (!STATUS_PENDING.equals(apply.getStatus()) && !STATUS_ADMIN_ARBITRATING.equals(apply.getStatus())) {
            throw new RuntimeException("Current status does not support seller response");
        }

        apply.setStatus(STATUS_SELLER_RESPONDED);
        apply.setAuditRemark(response);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
        auditLogService.record(apply.getId(), apply.getOrderId(), "SELLER", sellerId, "SELLER_RESPOND", response);
    }

    @Transactional
    public void sellerDecision(Long id, Long sellerId, String decision, String remark) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("Request does not exist");
        }
        if (!apply.getSellerId().equals(sellerId)) {
            throw new RuntimeException("No permission");
        }
        if (!STATUS_PENDING.equals(apply.getStatus())) {
            throw new RuntimeException("Current status does not support seller decision");
        }
        if (!StringUtils.hasText(decision)) {
            throw new RuntimeException("Decision cannot be empty");
        }

        if ("APPROVE".equalsIgnoreCase(decision)) {
            apply.setStatus(STATUS_APPROVED);
            syncRefundSnapshot(apply.getOrderId(), apply.getAmount(), STATUS_APPROVED);
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            apply.setStatus(STATUS_SELLER_REJECTED);
        } else {
            throw new RuntimeException("Invalid decision");
        }

        apply.setAuditRemark(remark);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);
        auditLogService.record(apply.getId(), apply.getOrderId(), "SELLER", sellerId,
                "SELLER_" + decision.toUpperCase(), remark);
    }

    @Transactional
    public void arbitrate(Long id, String responsibility, String result, String finalStatus) {
        AfterSalesOrder apply = afterSalesOrderMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("Request does not exist");
        }
        if (!STATUS_PENDING.equals(apply.getStatus())
                && !STATUS_SELLER_RESPONDED.equals(apply.getStatus())
                && !STATUS_SELLER_REJECTED.equals(apply.getStatus())
                && !STATUS_ADMIN_ARBITRATING.equals(apply.getStatus())) {
            throw new RuntimeException("Current status does not support arbitration");
        }

        String resolvedFinalStatus = StringUtils.hasText(finalStatus) ? finalStatus : STATUS_APPROVED;
        apply.setResponsibility(responsibility);
        apply.setArbitrationResult(result);
        apply.setStatus(resolvedFinalStatus);
        apply.setUpdateTime(LocalDateTime.now());
        afterSalesOrderMapper.updateById(apply);

        syncRefundSnapshot(apply.getOrderId(), apply.getAmount(), resolvedFinalStatus);
        auditLogService.record(apply.getId(), apply.getOrderId(), "ADMIN", 0L,
                "ARBITRATE_" + resolvedFinalStatus, result);
    }

    private void syncRefundSnapshot(Long orderId, BigDecimal amount, String afterSalesStatus) {
        if (STATUS_APPROVED.equals(afterSalesStatus) || "COMPLETED".equals(afterSalesStatus)) {
            orderService.updateRefundSnapshot(orderId, "REFUND_APPROVED", amount);
        } else if (STATUS_REJECTED.equals(afterSalesStatus)) {
            orderService.updateRefundSnapshot(orderId, "REFUND_REJECTED", BigDecimal.ZERO);
        }
    }

    private void persistEvidence(Long orderId, Long userId, String evidenceType, List<String> evidenceUrls, String evidenceText) {
        if (evidenceUrls.isEmpty() && !StringUtils.hasText(evidenceText)) {
            return;
        }
        OrderEvidence evidence = new OrderEvidence();
        evidence.setOrderId(orderId);
        evidence.setType(evidenceType);
        evidence.setUrls(toJsonArray(evidenceUrls));
        evidence.setNote(evidenceText);
        evidence.setCreatedBy(userId);
        evidence.setCreateTime(LocalDateTime.now());
        evidence.setUpdateTime(LocalDateTime.now());
        evidence.setDeleted(0);
        orderEvidenceMapper.insert(evidence);
    }

    private String resolveEvidenceType(List<String> evidenceUrls, String evidenceText) {
        boolean hasImage = evidenceUrls.stream().anyMatch(this::isImageUrl);
        boolean hasVideo = evidenceUrls.stream().anyMatch(this::isVideoUrl);
        boolean hasText = StringUtils.hasText(evidenceText);
        if ((hasImage || hasVideo) && hasText) {
            return "MIXED";
        }
        if (hasVideo) {
            return hasImage ? "MIXED" : "VIDEO";
        }
        if (hasImage) {
            return "IMAGE";
        }
        return "TEXT";
    }

    private boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".webp") || lower.endsWith(".gif");
    }

    private boolean isVideoUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".mkv");
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return StringUtils.hasText(second) ? second : null;
    }

    private List<String> parseJsonArray(String json) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(json)) {
            return result;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String body = trimmed.substring(1, trimmed.length() - 1).trim();
            if (body.isEmpty()) {
                return result;
            }
            String[] arr = body.split(",");
            for (String item : arr) {
                String url = item.trim();
                if (url.startsWith("\"")) {
                    url = url.substring(1);
                }
                if (url.endsWith("\"")) {
                    url = url.substring(0, url.length() - 1);
                }
                if (StringUtils.hasText(url)) {
                    result.add(url);
                }
            }
            return result;
        }
        String[] arr = trimmed.split(",");
        for (String item : arr) {
            if (StringUtils.hasText(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private String toJsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(values.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String toImageJson(List<String> evidenceUrls) {
        List<String> images = new ArrayList<>();
        for (String url : evidenceUrls) {
            if (isImageUrl(url)) {
                images.add(url);
            }
        }
        return toJsonArray(images);
    }
}

