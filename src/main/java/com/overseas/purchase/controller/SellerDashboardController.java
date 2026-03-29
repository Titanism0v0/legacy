package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.SellerDashboardOverviewDTO;
import com.overseas.purchase.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/seller/dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping("/overview")
    public Result<SellerDashboardOverviewDTO> getOverview(@RequestParam(required = false) Integer days,
                                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if (!"SELLER".equals(role) || userId == null) {
            return Result.error(403, "Forbidden");
        }
        return Result.success(sellerDashboardService.getOverview(userId, days));
    }
}
