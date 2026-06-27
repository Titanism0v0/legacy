package com.overseas.purchase.dto;

import lombok.Data;

@Data
public class OrderRuleSummaryDTO {
    private String categoryName;
    private String taxRuleNote;
    private String shippingRuleNote;
    private String policyNotice;
    private String annualLimitNotice;
    private String paymentNotice;
    private String originLabel;
}
