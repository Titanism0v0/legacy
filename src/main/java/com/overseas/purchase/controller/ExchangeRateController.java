package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/current")
    public Result<Map<String, Object>> current() {
        return Result.success(exchangeRateService.getCurrentSnapshot());
    }
}
