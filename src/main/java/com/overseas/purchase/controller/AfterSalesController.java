package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.AfterSalesAuditLog;
import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.service.AfterSalesAuditLogService;
import com.overseas.purchase.service.AfterSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/after-sales")
@RequiredArgsConstructor
public class AfterSalesController {

    private final AfterSalesService afterSalesService;
    private final AfterSalesAuditLogService auditLogService;

    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody AfterSalesOrder apply, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            apply.setUserId(userId);
            afterSalesService.submitApply(apply);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    @GetMapping("/list")
    public Result<Page<AfterSalesOrder>> getList(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String status,
                                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return Result.success(afterSalesService.getList(page, size, userId, role, status));
    }

    @GetMapping("/detail/{id}")
    public Result<AfterSalesOrder> getDetail(@PathVariable Long id, HttpServletRequest request) {
        AfterSalesOrder detail = afterSalesService.getDetail(id);
        if (detail == null) {
            return Result.error("Record does not exist");
        }
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if (!canAccessAfterSales(detail, userId, role)) {
            return Result.error("No permission");
        }
        return Result.success(detail);
    }

    @GetMapping("/logs")
    public Result<List<AfterSalesAuditLog>> getLogs(@RequestParam Long afterSalesId, HttpServletRequest request) {
        AfterSalesOrder detail = afterSalesService.getDetail(afterSalesId);
        if (detail == null) {
            return Result.error("Record does not exist");
        }
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if (!canAccessAfterSales(detail, userId, role)) {
            return Result.error("No permission");
        }
        return Result.success(auditLogService.listByAfterSalesId(afterSalesId));
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"ADMIN".equals(role)) {
                return Result.error("No permission");
            }
            Long id = Long.valueOf(params.get("id").toString());
            String status = String.valueOf(params.get("status"));
            String remark = params.get("remark") == null ? null : String.valueOf(params.get("remark"));
            if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
                return Result.error("Invalid status");
            }
            afterSalesService.audit(id, status, remark);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    @PostMapping("/respond")
    public Result<Void> respond(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"SELLER".equals(role)) {
                return Result.error("No permission");
            }
            Long sellerId = (Long) request.getAttribute("userId");
            Long id = Long.valueOf(params.get("id").toString());
            String response = params.get("response") == null ? null : String.valueOf(params.get("response"));
            afterSalesService.sellerRespond(id, sellerId, response);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    @PostMapping("/seller-decision")
    public Result<Void> sellerDecision(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"SELLER".equals(role)) {
                return Result.error("No permission");
            }
            Long sellerId = (Long) request.getAttribute("userId");
            Long id = Long.valueOf(params.get("id").toString());
            String decision = params.get("decision") == null ? null : String.valueOf(params.get("decision"));
            String remark = params.get("remark") == null ? null : String.valueOf(params.get("remark"));
            afterSalesService.sellerDecision(id, sellerId, decision, remark);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    @PostMapping("/request-arbitration")
    public Result<Void> requestArbitration(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"USER".equals(role)) {
                return Result.error("No permission");
            }
            Long userId = (Long) request.getAttribute("userId");
            Long id = Long.valueOf(params.get("id").toString());
            afterSalesService.requestArbitration(id, userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    @PostMapping("/arbitrate")
    public Result<Void> arbitrate(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"ADMIN".equals(role)) {
                return Result.error(403, "Forbidden");
            }
            Long id = Long.valueOf(params.get("id").toString());
            String responsibility = params.get("responsibility") == null ? null : String.valueOf(params.get("responsibility"));
            String result = params.get("result") == null ? null : String.valueOf(params.get("result"));
            String finalStatus = params.get("finalStatus") == null ? null : String.valueOf(params.get("finalStatus"));
            afterSalesService.arbitrate(id, responsibility, result, finalStatus);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    private boolean canAccessAfterSales(AfterSalesOrder apply, Long userId, String role) {
        if (apply == null) {
            return false;
        }
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return userId.equals(apply.getUserId()) || userId.equals(apply.getSellerId());
    }
}
