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
            orderEvidenceService.addEvidence(evidence, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<OrderEvidence>> list(@RequestParam Long orderId) {
        return Result.success(orderEvidenceService.listByOrderId(orderId));
    }
}

