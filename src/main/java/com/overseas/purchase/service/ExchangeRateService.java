package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.overseas.purchase.entity.ExchangeRate;
import com.overseas.purchase.mapper.ExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String SOURCE_ECB = "ECB";
    private static final String SOURCE_DB = "DB_LAST_SUCCESS";
    private static final String SOURCE_DEFAULT = "BUILTIN_DEFAULT";

    private static final Set<String> SUPPORTED_CURRENCIES = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList("CNY", "CNH", "USD", "EUR", "JPY", "GBP", "KRW", "CAD", "AUD", "HKD", "CHF", "SEK")
    ));

    private static final Map<String, BigDecimal> DEFAULT_RATE_TO_CNY = new LinkedHashMap<String, BigDecimal>() {{
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
        put("CHF", new BigDecimal("8.9000"));
        put("SEK", new BigDecimal("0.6700"));
    }};

    private final ExchangeRateMapper exchangeRateMapper;
    private final EcbExchangeRateClient ecbExchangeRateClient;
    private final CacheSupportService cacheSupportService;

    private volatile Map<String, BigDecimal> cacheRateToCny = sanitizeRates(DEFAULT_RATE_TO_CNY);
    private volatile String cacheSource = SOURCE_DEFAULT;
    private volatile LocalDate cacheQuoteDate = null;
    private volatile boolean fallbackApplied = true;

    public synchronized void refreshOnStartup() {
        try {
            EcbExchangeRateClient.EcbDailyRate dailyRate = ecbExchangeRateClient.fetchDailyRate();
            Map<String, BigDecimal> converted = convertFromEcbToCny(dailyRate.getEurBaseRates());
            Map<String, BigDecimal> sanitized = sanitizeRates(converted);
            saveLatestRates(sanitized, SOURCE_ECB, dailyRate.getQuoteDate());
            applyCache(sanitized, SOURCE_ECB, dailyRate.getQuoteDate(), false);
            log.info("Exchange rates refreshed from ECB. quoteDate={}, currencyCount={}",
                    dailyRate.getQuoteDate(), sanitized.size());
            return;
        } catch (Exception e) {
            log.warn("Failed to refresh exchange rates from ECB on startup: {}", e.getMessage());
        }

        if (loadFromDatabase()) {
            fallbackApplied = true;
            log.warn("Exchange rates fallback to last successful database snapshot. quoteDate={}, currencyCount={}",
                    cacheQuoteDate, cacheRateToCny.size());
            return;
        }
        applyCache(DEFAULT_RATE_TO_CNY, SOURCE_DEFAULT, null, true);
        log.warn("Exchange rates fallback to builtin defaults. currencyCount={}", cacheRateToCny.size());
    }

    public Map<String, Object> getCurrentSnapshot() {
        return cacheSupportService.getOrLoad(
                "exchange-rate:current",
                new TypeReference<Map<String, Object>>() {},
                Duration.ofMinutes(10),
                this::buildCurrentSnapshot
        );
    }

    private Map<String, Object> buildCurrentSnapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", cacheSource);
        result.put("quoteDate", cacheQuoteDate == null ? null : cacheQuoteDate.toString());
        result.put("rates", new LinkedHashMap<>(cacheRateToCny));
        result.put("fallbackApplied", fallbackApplied);
        return result;
    }

    public BigDecimal resolveRateToCny(String currency) {
        String normalized = normalizeCurrency(currency);
        BigDecimal rate = cacheRateToCny.get(normalized);
        if (rate == null) {
            rate = DEFAULT_RATE_TO_CNY.get(normalized);
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    public boolean isSupportedCurrency(String currency) {
        return SUPPORTED_CURRENCIES.contains(normalizeCurrency(currency));
    }

    public String normalizeCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return "CNY";
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if ("RMB".equals(normalized)) {
            return "CNY";
        }
        return SUPPORTED_CURRENCIES.contains(normalized) ? normalized : "CNY";
    }

    public Set<String> supportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    private Map<String, BigDecimal> convertFromEcbToCny(Map<String, BigDecimal> eurBaseRates) {
        BigDecimal eurToCny = eurBaseRates.get("CNY");
        if (eurToCny == null || eurToCny.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("ECB CNY rate is invalid");
        }
        Map<String, BigDecimal> converted = new LinkedHashMap<>();
        converted.put("CNY", BigDecimal.ONE);
        converted.put("CNH", BigDecimal.ONE);
        converted.put("EUR", eurToCny);
        for (String currency : SUPPORTED_CURRENCIES) {
            if ("CNY".equals(currency) || "CNH".equals(currency) || "EUR".equals(currency)) {
                continue;
            }
            BigDecimal eurToCurrency = eurBaseRates.get(currency);
            if (eurToCurrency == null || eurToCurrency.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal rateToCny = eurToCny.divide(eurToCurrency, 8, RoundingMode.HALF_UP);
            converted.put(currency, rateToCny);
        }
        return converted;
    }

    private void saveLatestRates(Map<String, BigDecimal> ratesToCny, String source, LocalDate quoteDate) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, BigDecimal> entry : ratesToCny.entrySet()) {
            String currency = entry.getKey();
            BigDecimal rate = entry.getValue();
            QueryWrapper<ExchangeRate> wrapper = new QueryWrapper<>();
            wrapper.eq("currency", currency).eq("deleted", 0).last("LIMIT 1");
            ExchangeRate existing = exchangeRateMapper.selectOne(wrapper);

            if (existing == null) {
                ExchangeRate insert = new ExchangeRate();
                insert.setCurrency(currency);
                insert.setRateToCny(rate);
                insert.setSource(source);
                insert.setQuoteDate(quoteDate);
                insert.setFetchTime(now);
                insert.setStatus("ACTIVE");
                exchangeRateMapper.insert(insert);
            } else {
                existing.setRateToCny(rate);
                existing.setSource(source);
                existing.setQuoteDate(quoteDate);
                existing.setFetchTime(now);
                existing.setStatus("ACTIVE");
                exchangeRateMapper.updateById(existing);
            }
        }
    }

    private boolean loadFromDatabase() {
        QueryWrapper<ExchangeRate> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .in("currency", SUPPORTED_CURRENCIES)
                .orderByDesc("fetch_time");
        List<ExchangeRate> list = exchangeRateMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return false;
        }

        LocalDate quoteDate = null;
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (ExchangeRate row : list) {
            if (row.getCurrency() == null || row.getRateToCny() == null) {
                continue;
            }
            String currency = normalizeCurrency(row.getCurrency());
            if (!rates.containsKey(currency)) {
                rates.put(currency, row.getRateToCny());
            }
            if (quoteDate == null && row.getQuoteDate() != null) {
                quoteDate = row.getQuoteDate();
            }
        }
        if (rates.isEmpty()) {
            return false;
        }
        applyCache(rates, SOURCE_DB, quoteDate, true);
        return true;
    }

    private void applyCache(Map<String, BigDecimal> ratesToCny, String source, LocalDate quoteDate, boolean fallback) {
        this.cacheRateToCny = sanitizeRates(ratesToCny);
        this.cacheSource = source;
        this.cacheQuoteDate = quoteDate;
        this.fallbackApplied = fallback;
        cacheSupportService.evict("exchange-rate:current");
    }

    private Map<String, BigDecimal> sanitizeRates(Map<String, BigDecimal> inputRates) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String currency : SUPPORTED_CURRENCIES) {
            BigDecimal raw = inputRates == null ? null : inputRates.get(currency);
            if (raw == null || raw.compareTo(BigDecimal.ZERO) <= 0) {
                raw = DEFAULT_RATE_TO_CNY.get(currency);
            }
            if (raw == null || raw.compareTo(BigDecimal.ZERO) <= 0) {
                raw = BigDecimal.ONE;
            }
            result.put(currency, raw.setScale(4, RoundingMode.HALF_UP));
        }
        result.put("CNY", BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        result.put("CNH", BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        return result;
    }
}
