package com.overseas.purchase.service;

import com.overseas.purchase.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CrossBorderPricingService {

    private final CrossBorderComplianceService complianceService;

    private static final BigDecimal BASE_SHIPPING = new BigDecimal("35.00");
    private static final BigDecimal PER_ITEM_SHIPPING = new BigDecimal("8.00");
    private static final Map<String, BigDecimal> CURRENCY_TO_CNY = new HashMap<String, BigDecimal>() {{
        put("CNY", new BigDecimal("1.0000"));
        put("CNH", new BigDecimal("1.0000"));
        put("USD", new BigDecimal("7.2000"));
        put("EUR", new BigDecimal("7.8500"));
        put("JPY", new BigDecimal("0.0470"));
        put("GBP", new BigDecimal("9.2000"));
        put("KRW", new BigDecimal("0.0054"));
        put("CAD", new BigDecimal("5.3000"));
        put("AUD", new BigDecimal("4.7000"));
        put("HKD", new BigDecimal("0.9200"));
    }};

    public PriceQuote calculateQuote(Product product, Integer quantity) {
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = resolveTaxRate(product.getCategoryId());
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = BASE_SHIPPING.add(PER_ITEM_SHIPPING.multiply(BigDecimal.valueOf(qty))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);
        BigDecimal exchangeRate = resolveExchangeRate(product.getCurrency());

        return new PriceQuote(subtotal, tax, shipping, total, taxRate, exchangeRate, 0, "PENDING_DECLARATION");
    }

    public BigDecimal resolveTaxRate(Long categoryId) {
        Long topId = complianceService.resolveTopCategoryId(categoryId);
        if (topId == null) {
            return new BigDecimal("0.10");
        }
        if (topId == 1L) {
            return new BigDecimal("0.15");
        }
        if (topId == 2L) {
            return new BigDecimal("0.20");
        }
        if (topId == 4L) {
            return new BigDecimal("0.13");
        }
        return new BigDecimal("0.10");
    }

    public BigDecimal resolveExchangeRate(String currency) {
        if (currency == null) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return CURRENCY_TO_CNY.getOrDefault(currency.toUpperCase(), BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
    }

    @Data
    @AllArgsConstructor
    public static class PriceQuote {
        private BigDecimal subtotalPrice;
        private BigDecimal taxAmount;
        private BigDecimal shippingFee;
        private BigDecimal totalPrice;
        private BigDecimal taxRate;
        private BigDecimal exchangeRate;
        private Integer taxIncludedFlag;
        private String customsClearanceStatus;
    }
}

