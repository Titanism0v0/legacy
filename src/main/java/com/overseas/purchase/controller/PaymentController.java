package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepay/{orderId}")
    public Result<Map<String, Object>> prepay(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            return Result.success(paymentService.prepay(orderId, userId, role));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/status/{orderId}")
    public Result<Map<String, Object>> status(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            return Result.success(paymentService.getPaymentStatus(orderId, userId, role));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/refund/{orderId}")
    public Result<Map<String, Object>> refund(@PathVariable Long orderId,
                                              @RequestBody(required = false) Map<String, Object> params,
                                              HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            String reason = params == null || params.get("reason") == null
                    ? "after-sales refund" : String.valueOf(params.get("reason"));
            return Result.success(paymentService.refund(orderId, reason, userId, role));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/notify/wechat")
    public ResponseEntity<Map<String, String>> wechatNotify(@RequestBody String body, HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        Map<String, String> response = paymentService.handleWechatNotify(body, headers);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        return paymentService.handleAlipayNotify(extractParams(request));
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}
