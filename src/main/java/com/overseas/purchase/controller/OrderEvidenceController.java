package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.OrderEvidence;
import com.overseas.purchase.service.OrderEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 订单证据链接口
 */
@RestController
@RequestMapping("/order-evidence")
@RequiredArgsConstructor
public class OrderEvidenceController {

    private final OrderEvidenceService orderEvidenceService;

    @PostMapping("/add")
    public Result<Void> add(@RequestBody OrderEvidence evidence, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            orderEvidenceService.addEvidence(evidence, userId, role);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    @GetMapping("/list")
    public Result<List<OrderEvidence>> list(@RequestParam Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return Result.success(orderEvidenceService.listByOrderId(orderId, userId, role));
    }
}

