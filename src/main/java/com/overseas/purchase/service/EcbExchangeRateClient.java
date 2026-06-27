package com.overseas.purchase.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EcbExchangeRateClient {

    private static final Pattern TIME_PATTERN = Pattern.compile("time=['\"](\\d{4}-\\d{2}-\\d{2})['\"]");
    private static final Pattern RATE_PATTERN = Pattern.compile("currency=['\"]([A-Z]{3})['\"]\\s+rate=['\"]([0-9.]+)['\"]");

    private final RestTemplate restTemplate;

    @Value("${exchange-rate.ecb-url:https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml}")
    private String ecbUrl;

    public EcbExchangeRateClient(
            @Value("${exchange-rate.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${exchange-rate.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public EcbDailyRate fetchDailyRate() {
        ResponseEntity<String> response = restTemplate.getForEntity(ecbUrl, String.class);
        String body = response.getBody();
        if (body == null || body.trim().isEmpty()) {
            throw new RuntimeException("Empty ECB exchange-rate response");
        }
        LocalDate quoteDate = parseQuoteDate(body);
        Map<String, BigDecimal> eurBaseRates = parseEurBaseRates(body);
        eurBaseRates.put("EUR", BigDecimal.ONE);
        if (!eurBaseRates.containsKey("CNY")) {
            throw new RuntimeException("ECB response does not include CNY rate");
        }
        return new EcbDailyRate(quoteDate, eurBaseRates);
    }

    private LocalDate parseQuoteDate(String xml) {
        Matcher matcher = TIME_PATTERN.matcher(xml);
        if (!matcher.find()) {
            throw new RuntimeException("Failed to parse ECB quote date");
        }
        return LocalDate.parse(matcher.group(1));
    }

    private Map<String, BigDecimal> parseEurBaseRates(String xml) {
        Map<String, BigDecimal> rates = new HashMap<>();
        Matcher matcher = RATE_PATTERN.matcher(xml);
        while (matcher.find()) {
            rates.put(matcher.group(1), new BigDecimal(matcher.group(2)));
        }
        if (rates.isEmpty()) {
            throw new RuntimeException("Failed to parse ECB rates");
        }
        return rates;
    }

    @Data
    @AllArgsConstructor
    public static class EcbDailyRate {
        private LocalDate quoteDate;
        private Map<String, BigDecimal> eurBaseRates;
    }
}
