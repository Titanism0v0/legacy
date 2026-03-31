package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.service.LegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
public class LegalController {

    private final LegalService legalService;

    @GetMapping("/current")
    public Result<Map<String, Object>> current() {
        return Result.success(legalService.getCurrentLegal());
    }
}
