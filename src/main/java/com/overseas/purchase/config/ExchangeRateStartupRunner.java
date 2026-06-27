package com.overseas.purchase.config;

import com.overseas.purchase.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeRateStartupRunner {

    private final ExchangeRateService exchangeRateService;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshRatesAtStartup() {
        exchangeRateService.refreshOnStartup();
    }
}
