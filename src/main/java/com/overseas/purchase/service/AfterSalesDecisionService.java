package com.overseas.purchase.service;

import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AfterSalesDecisionService {

    public DecisionResult evaluate(Order order, AfterSalesOrder apply, int evidenceCount, int evidenceTextLen) {
        boolean evidenceComplete = evidenceCount > 0 || evidenceTextLen >= 15;
        if (!evidenceComplete) {
            return new DecisionResult(
                    "AUTO_REJECT",
                    "Evidence is not sufficient",
                    scoreByHeuristic(apply, evidenceCount, evidenceTextLen, order),
                    "REJECT",
                    "Insufficient evidence",
                    "REJECTED"
            );
        }

        LocalDateTime baseTime = order.getUpdateTime() == null ? order.getCreateTime() : order.getUpdateTime();
        if (baseTime != null) {
            long days = Duration.between(baseTime, LocalDateTime.now()).toDays();
            if (days > 15) {
                return new DecisionResult(
                        "AUTO_REJECT",
                        "After-sales request is out of 15-day window",
                        scoreByHeuristic(apply, evidenceCount, evidenceTextLen, order),
                        "REJECT",
                        "Request timeout",
                        "REJECTED"
                );
            }
        }

        BigDecimal score = scoreByHeuristic(apply, evidenceCount, evidenceTextLen, order);
        String suggestion = score.compareTo(new BigDecimal("0.75")) >= 0 ? "APPROVE"
                : (score.compareTo(new BigDecimal("0.35")) < 0 ? "REJECT" : "ARBITRATE");
        String suggestionReason = "Score-based suggestion";

        if ("APPROVE".equals(suggestion)
                && apply.getAmount() != null
                && order.getTotalPrice() != null
                && order.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = apply.getAmount().divide(order.getTotalPrice(), 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.85")) <= 0) {
                return new DecisionResult("AUTO_APPROVE", "High confidence with complete evidence", score,
                        suggestion, suggestionReason, "APPROVED");
            }
        }

        if ("REJECT".equals(suggestion)) {
            return new DecisionResult("AUTO_REJECT", "Low confidence request", score,
                    suggestion, suggestionReason, "REJECTED");
        }

        return new DecisionResult("NEED_MANUAL", "Requires manual review", score,
                suggestion, suggestionReason, "PENDING");
    }

    private BigDecimal scoreByHeuristic(AfterSalesOrder apply, int evidenceCount, int evidenceTextLen, Order order) {
        BigDecimal score = new BigDecimal("0.50");
        String reason = normalize(apply.getReason()) + " " + normalize(apply.getDescription());

        if (reason.contains("quality") || reason.contains("fake") || reason.contains("damage")
                || reason.contains("质量") || reason.contains("假货") || reason.contains("破损")) {
            score = score.add(new BigDecimal("0.20"));
        }
        if (evidenceCount > 0) {
            score = score.add(new BigDecimal("0.15"));
        }
        if (evidenceTextLen >= 40) {
            score = score.add(new BigDecimal("0.10"));
        }

        if (apply.getAmount() != null && order.getTotalPrice() != null
                && order.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = apply.getAmount().divide(order.getTotalPrice(), 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.90")) > 0) {
                score = score.subtract(new BigDecimal("0.20"));
            }
        }

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ONE) > 0) {
            score = BigDecimal.ONE;
        }
        return score.setScale(3, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @Data
    @AllArgsConstructor
    public static class DecisionResult {
        private String ruleDecision;
        private String ruleReason;
        private BigDecimal aiScore;
        private String aiSuggestion;
        private String aiReason;
        private String finalStatus;
    }
}

