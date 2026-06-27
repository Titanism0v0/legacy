package com.overseas.purchase.service;

import com.overseas.purchase.dto.OrderRuleSummaryDTO;
import com.overseas.purchase.entity.Category;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.CategoryMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CrossBorderPricingService {

    public static final String PAYMENT_CURRENCY = "CNY";
    public static final String RULE_VERSION = "CBEC-IMPORT-V1";
    public static final BigDecimal SINGLE_TRANSACTION_LIMIT_CNY = new BigDecimal("5000.00");

    private final CrossBorderComplianceService complianceService;
    private final CategoryMapper categoryMapper;
    private final ExchangeRateService exchangeRateService;

    private static final BigDecimal MIN_INSURANCE_FEE = new BigDecimal("2.00");
    private static final BigDecimal PREFERENTIAL_FACTOR = new BigDecimal("0.70");
    private static final Map<String, ZoneProfile> ZONE_PROFILES = new HashMap<String, ZoneProfile>() {{
        put("ASIA_NEAR", new ZoneProfile(
                "ASIA_NEAR",
                "亚洲邻近地区",
                new BigDecimal("18.00"),
                new BigDecimal("5.00"),
                new BigDecimal("0.0100")
        ));
        put("PACIFIC", new ZoneProfile(
                "PACIFIC",
                "太平洋地区",
                new BigDecimal("32.00"),
                new BigDecimal("8.00"),
                new BigDecimal("0.0120")
        ));
        put("EUROPE", new ZoneProfile(
                "EUROPE",
                "欧洲地区",
                new BigDecimal("36.00"),
                new BigDecimal("10.00"),
                new BigDecimal("0.0120")
        ));
        put("GLOBAL_OTHER", new ZoneProfile(
                "GLOBAL_OTHER",
                "其他地区",
                new BigDecimal("42.00"),
                new BigDecimal("12.00"),
                new BigDecimal("0.0150")
        ));
    }};
    private static final List<String> ASIA_NEAR_KEYWORDS = Arrays.asList(
            "日本", "韩国", "香港", "澳门", "台湾", "新加坡", "泰国", "马来西亚"
    );
    private static final List<String> PACIFIC_KEYWORDS = Arrays.asList(
            "美国", "加拿大", "澳大利亚", "澳洲", "新西兰"
    );
    private static final List<String> EUROPE_KEYWORDS = Arrays.asList(
            "英国", "法国", "德国", "意大利", "西班牙", "荷兰", "瑞士", "瑞典", "挪威", "丹麦"
    );

    public PriceQuote calculateQuote(Product product, Integer quantity, String settlementCurrency) {
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;
        String productCurrency = normalizeCurrency(product == null ? null : product.getCurrency());
        String displayCurrency = normalizeCurrency(settlementCurrency);
        BigDecimal exchangeRate = resolveExchangeRate(productCurrency);

        BigDecimal subtotalSource = safeAmount(product == null ? null : product.getPrice())
                .multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotalCny = convertToCny(subtotalSource, productCurrency);

        ZoneProfile zoneProfile = resolveZoneProfile(product == null ? null : product.getShippingAddress());
        BigDecimal internationalShippingCny = zoneProfile.getBaseFee()
                .add(zoneProfile.getAdditionalItemFee().multiply(BigDecimal.valueOf(Math.max(qty - 1, 0L))))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal insuranceFeeCny = subtotalCny.add(internationalShippingCny)
                .multiply(zoneProfile.getInsuranceRate())
                .setScale(2, RoundingMode.HALF_UP);
        if (insuranceFeeCny.compareTo(MIN_INSURANCE_FEE) < 0) {
            insuranceFeeCny = MIN_INSURANCE_FEE;
        }
        BigDecimal totalShippingCny = internationalShippingCny.add(insuranceFeeCny).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dutiablePriceCny = subtotalCny.add(totalShippingCny).setScale(2, RoundingMode.HALF_UP);

        CategoryTaxProfile taxProfile = resolveCategoryTaxProfile(product == null ? null : product.getCategoryId());
        boolean preferential = taxProfile.getCbecEnabled() == 1
                && dutiablePriceCny.compareTo(SINGLE_TRANSACTION_LIMIT_CNY) <= 0;
        String taxMode = preferential ? "CBEC_PREFERENTIAL" : "GENERAL_IMPORT_ESTIMATE";

        BigDecimal tariffAmountCny = BigDecimal.ZERO;
        BigDecimal vatAmountCny;
        BigDecimal consumptionTaxAmountCny;
        BigDecimal totalTaxCny;

        if (preferential) {
            tariffAmountCny = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (taxProfile.getConsumptionTaxRate().compareTo(BigDecimal.ZERO) <= 0) {
                consumptionTaxAmountCny = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                vatAmountCny = dutiablePriceCny.multiply(taxProfile.getImportVatRate())
                        .setScale(2, RoundingMode.HALF_UP);
                totalTaxCny = vatAmountCny.multiply(PREFERENTIAL_FACTOR).setScale(2, RoundingMode.HALF_UP);
            } else {
                consumptionTaxAmountCny = dutiablePriceCny.multiply(taxProfile.getConsumptionTaxRate())
                        .divide(BigDecimal.ONE.subtract(taxProfile.getConsumptionTaxRate()), 2, RoundingMode.HALF_UP);
                vatAmountCny = dutiablePriceCny.add(consumptionTaxAmountCny)
                        .multiply(taxProfile.getImportVatRate())
                        .setScale(2, RoundingMode.HALF_UP);
                totalTaxCny = consumptionTaxAmountCny.add(vatAmountCny)
                        .multiply(PREFERENTIAL_FACTOR)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            tariffAmountCny = dutiablePriceCny.multiply(taxProfile.getGeneralTariffRate())
                    .setScale(2, RoundingMode.HALF_UP);
            if (taxProfile.getConsumptionTaxRate().compareTo(BigDecimal.ZERO) <= 0) {
                consumptionTaxAmountCny = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                vatAmountCny = dutiablePriceCny.add(tariffAmountCny)
                        .multiply(taxProfile.getImportVatRate())
                        .setScale(2, RoundingMode.HALF_UP);
                totalTaxCny = tariffAmountCny.add(vatAmountCny).setScale(2, RoundingMode.HALF_UP);
            } else {
                consumptionTaxAmountCny = dutiablePriceCny.add(tariffAmountCny)
                        .multiply(taxProfile.getConsumptionTaxRate())
                        .divide(BigDecimal.ONE.subtract(taxProfile.getConsumptionTaxRate()), 2, RoundingMode.HALF_UP);
                vatAmountCny = dutiablePriceCny.add(tariffAmountCny).add(consumptionTaxAmountCny)
                        .multiply(taxProfile.getImportVatRate())
                        .setScale(2, RoundingMode.HALF_UP);
                totalTaxCny = tariffAmountCny.add(consumptionTaxAmountCny).add(vatAmountCny)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalPriceCny = subtotalCny.add(totalShippingCny).add(totalTaxCny).setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveTaxRate = dutiablePriceCny.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : totalTaxCny.divide(dutiablePriceCny, 4, RoundingMode.HALF_UP);

        OrderRuleSummaryDTO ruleSummary = new OrderRuleSummaryDTO();
        ruleSummary.setCategoryName(taxProfile.getCategoryName());
        ruleSummary.setTaxRuleNote(taxProfile.getTaxRuleNote());
        ruleSummary.setShippingRuleNote(buildShippingRuleNote(zoneProfile));
        ruleSummary.setPolicyNotice("单次完税价格不高于5000元时，按跨境零售进口优惠口径预估；超限值时按一般贸易口径估算。");
        ruleSummary.setAnnualLimitNotice("年度26000元限值第一版仅做提示，不在下单时拦截，最终以海关审核与申报结果为准。");
        ruleSummary.setPaymentNotice("页面金额随当前币种展示，实际支付与订单快照统一按人民币结算。");
        ruleSummary.setOriginLabel(zoneProfile.getDisplayName());

        return new PriceQuote(
                subtotalCny,
                internationalShippingCny,
                insuranceFeeCny,
                totalShippingCny,
                dutiablePriceCny,
                tariffAmountCny,
                vatAmountCny,
                consumptionTaxAmountCny,
                totalTaxCny,
                totalPriceCny,
                effectiveTaxRate,
                exchangeRate,
                productCurrency,
                displayCurrency,
                PAYMENT_CURRENCY,
                !"CNY".equalsIgnoreCase(displayCurrency),
                0,
                "PENDING_DECLARATION",
                taxMode,
                zoneProfile.getCode(),
                RULE_VERSION,
                ruleSummary
        );
    }

    public PriceQuote calculateQuote(Product product, Integer quantity) {
        return calculateQuote(product, quantity, PAYMENT_CURRENCY);
    }

    public BigDecimal resolveExchangeRate(String currency) {
        return exchangeRateService.resolveRateToCny(currency);
    }

    public BigDecimal convertToCny(BigDecimal amount, String sourceCurrency) {
        return safeAmount(amount).multiply(resolveExchangeRate(sourceCurrency)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal convertFromCny(BigDecimal cnyAmount, String targetCurrency) {
        String normalized = normalizeCurrency(targetCurrency);
        BigDecimal rate = resolveExchangeRate(normalized);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return safeAmount(cnyAmount).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal raw = safeAmount(cnyAmount).divide(rate, 4, RoundingMode.HALF_UP);
        if ("JPY".equals(normalized) || "KRW".equals(normalized)) {
            return raw.setScale(0, RoundingMode.HALF_UP);
        }
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private ZoneProfile resolveZoneProfile(String shippingAddress) {
        String normalized = shippingAddress == null ? "" : shippingAddress.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, ASIA_NEAR_KEYWORDS)) {
            return ZONE_PROFILES.get("ASIA_NEAR");
        }
        if (containsAny(normalized, PACIFIC_KEYWORDS)) {
            return ZONE_PROFILES.get("PACIFIC");
        }
        if (containsAny(normalized, EUROPE_KEYWORDS)) {
            return ZONE_PROFILES.get("EUROPE");
        }
        return ZONE_PROFILES.get("GLOBAL_OTHER");
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private CategoryTaxProfile resolveCategoryTaxProfile(Long categoryId) {
        Long topId = complianceService.resolveTopCategoryId(categoryId);
        if (topId == null) {
            return defaultCategoryProfile(6L, null);
        }
        Category category = categoryMapper.selectById(topId);
        if (category == null || category.getDeleted() == 1) {
            return defaultCategoryProfile(topId, null);
        }
        return new CategoryTaxProfile(
                topId,
                category.getName(),
                category.getCbecEnabled() == null ? 1 : category.getCbecEnabled(),
                defaultDecimal(category.getImportVatRate(), defaultCategoryProfile(topId, category.getName()).getImportVatRate()),
                defaultDecimal(category.getConsumptionTaxRate(), defaultCategoryProfile(topId, category.getName()).getConsumptionTaxRate()),
                defaultDecimal(category.getGeneralTariffRate(), defaultCategoryProfile(topId, category.getName()).getGeneralTariffRate()),
                category.getTaxRuleNote() == null || category.getTaxRuleNote().trim().isEmpty()
                        ? defaultCategoryProfile(topId, category.getName()).getTaxRuleNote()
                        : category.getTaxRuleNote().trim()
        );
    }

    private CategoryTaxProfile defaultCategoryProfile(Long topId, String categoryName) {
        if (Long.valueOf(1L).equals(topId)) {
            return new CategoryTaxProfile(1L, safeCategoryName(categoryName, "电子数码"), 1,
                    new BigDecimal("0.1300"), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    new BigDecimal("0.1000"),
                    "电子数码类限值内关税暂按0估算，主要计征进口增值税，超限值按一般贸易税率估算。");
        }
        if (Long.valueOf(2L).equals(topId)) {
            return new CategoryTaxProfile(2L, safeCategoryName(categoryName, "美妆个护"), 1,
                    new BigDecimal("0.1300"), new BigDecimal("0.1500"),
                    new BigDecimal("0.0500"),
                    "美妆个护类可能涉及消费税，当前按平台类目税费档案估算，最终以海关税则归类为准。");
        }
        if (Long.valueOf(3L).equals(topId)) {
            return new CategoryTaxProfile(3L, safeCategoryName(categoryName, "服饰鞋帽"), 1,
                    new BigDecimal("0.1300"), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    new BigDecimal("0.1600"),
                    "服饰鞋帽类限值内按跨境零售进口优惠口径估算，超限值按一般贸易税率估算。");
        }
        if (Long.valueOf(4L).equals(topId)) {
            return new CategoryTaxProfile(4L, safeCategoryName(categoryName, "食品保健"), 1,
                    new BigDecimal("0.0900"), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    new BigDecimal("0.1200"),
                    "食品保健类通常适用较低进口增值税率，具体税费仍需以监管申报结果为准。");
        }
        if (Long.valueOf(5L).equals(topId)) {
            return new CategoryTaxProfile(5L, safeCategoryName(categoryName, "母婴用品"), 1,
                    new BigDecimal("0.1300"), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    new BigDecimal("0.1000"),
                    "母婴用品类限值内按跨境零售进口优惠口径估算，关税暂按0估算。");
        }
        return new CategoryTaxProfile(6L, safeCategoryName(categoryName, "其他"), 1,
                new BigDecimal("0.1300"), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                new BigDecimal("0.1500"),
                "其他类商品按平台类目税费档案估算；如不适用跨境零售进口政策，则按一般贸易口径估算。");
    }

    private String buildShippingRuleNote(ZoneProfile zoneProfile) {
        return zoneProfile.getDisplayName() + "按基础运费"
                + zoneProfile.getBaseFee().setScale(0, RoundingMode.HALF_UP).toPlainString()
                + "元 + 每件附加"
                + zoneProfile.getAdditionalItemFee().setScale(0, RoundingMode.HALF_UP).toPlainString()
                + "元估算，保险费按"
                + zoneProfile.getInsuranceRate().multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).toPlainString()
                + "%计取，最低2元。";
    }

    private String normalizeCurrency(String currency) {
        return exchangeRateService.normalizeCurrency(currency);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal actual, BigDecimal fallback) {
        return actual == null ? fallback : actual.setScale(4, RoundingMode.HALF_UP);
    }

    private String safeCategoryName(String categoryName, String fallback) {
        return categoryName == null || categoryName.trim().isEmpty() ? fallback : categoryName;
    }

    @Data
    @AllArgsConstructor
    public static class PriceQuote {
        private BigDecimal subtotalPrice;
        private BigDecimal internationalShippingFee;
        private BigDecimal insuranceFee;
        private BigDecimal shippingFee;
        private BigDecimal dutiablePrice;
        private BigDecimal tariffAmount;
        private BigDecimal vatAmount;
        private BigDecimal consumptionTaxAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalPrice;
        private BigDecimal taxRate;
        private BigDecimal exchangeRate;
        private String productCurrency;
        private String displayCurrency;
        private String paymentCurrency;
        private boolean paymentFallbackApplied;
        private Integer taxIncludedFlag;
        private String customsClearanceStatus;
        private String taxMode;
        private String originZone;
        private String ruleVersion;
        private OrderRuleSummaryDTO ruleSummary;

        public BigDecimal convertForDisplay(CrossBorderPricingService pricingService, BigDecimal cnyAmount) {
            return pricingService.convertFromCny(cnyAmount, displayCurrency);
        }
    }

    @Data
    @AllArgsConstructor
    private static class CategoryTaxProfile {
        private Long categoryId;
        private String categoryName;
        private Integer cbecEnabled;
        private BigDecimal importVatRate;
        private BigDecimal consumptionTaxRate;
        private BigDecimal generalTariffRate;
        private String taxRuleNote;
    }

    @Data
    @AllArgsConstructor
    private static class ZoneProfile {
        private String code;
        private String displayName;
        private BigDecimal baseFee;
        private BigDecimal additionalItemFee;
        private BigDecimal insuranceRate;
    }
}
