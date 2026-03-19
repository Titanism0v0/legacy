package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.service.AfterSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 售后服务控制器
 * 
 * @author System
 */
@RestController
@RequestMapping("/after-sales")
@RequiredArgsConstructor
public class AfterSalesController {

    private final AfterSalesService afterSalesService;

    /**
     * 提交售后申请
     */
    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody AfterSalesOrder apply, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            apply.setUserId(userId);
            afterSalesService.submitApply(apply);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询售后列表
     */
    @GetMapping("/list")
    public Result<Page<AfterSalesOrder>> getList(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String status,
                                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        
        Page<AfterSalesOrder> result = afterSalesService.getList(page, size, userId, role, status);
        return Result.success(result);
    }

    /**
     * 获取售后详情
     */
    @GetMapping("/detail/{id}")
    public Result<AfterSalesOrder> getDetail(@PathVariable Long id) {
        AfterSalesOrder detail = afterSalesService.getDetail(id);
        if (detail == null) {
            return Result.error("记录不存在");
        }
        return Result.success(detail);
    }

    /**
     * 审核售后申请（管理员/卖家）
     */
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            // 简单权限校验：只有管理员和卖家可以审核
            // 实际业务中卖家只能审核关联自己店铺订单的售后，这里暂简化
            if (!"ADMIN".equals(role) && !"SELLER".equals(role)) {
                return Result.error("无权限操作");
            }

            Long id = Long.valueOf(params.get("id").toString());
            String status = (String) params.get("status");
            String remark = (String) params.get("remark");

            if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
                return Result.error("无效的审核状态");
            }

            afterSalesService.audit(id, status, remark);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 卖家：响应售后
     */
    @PostMapping("/respond")
    public Result<Void> respond(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"SELLER".equals(role)) {
                return Result.error("无权限操作");
            }
            Long sellerId = (Long) request.getAttribute("userId");
            Long id = Long.valueOf(params.get("id").toString());
            String response = params.get("response") == null ? null : params.get("response").toString();
            afterSalesService.sellerRespond(id, sellerId, response);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 卖家：同意/拒绝售后（卖家先处理）
     */
    @PostMapping("/seller-decision")
    public Result<Void> sellerDecision(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"SELLER".equals(role)) {
                return Result.error("无权限操作");
            }
            Long sellerId = (Long) request.getAttribute("userId");
            Long id = Long.valueOf(params.get("id").toString());
            String decision = params.get("decision") == null ? null : params.get("decision").toString();
            String remark = params.get("remark") == null ? null : params.get("remark").toString();
            afterSalesService.sellerDecision(id, sellerId, decision, remark);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户：申请平台介入（仅在卖家拒绝后）
     */
    @PostMapping("/request-arbitration")
    public Result<Void> requestArbitration(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"USER".equals(role)) {
                return Result.error("无权限操作");
            }
            Long userId = (Long) request.getAttribute("userId");
            Long id = Long.valueOf(params.get("id").toString());
            afterSalesService.requestArbitration(id, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员：仲裁售后
     */
    @PostMapping("/arbitrate")
    public Result<Void> arbitrate(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"ADMIN".equals(role)) {
                return Result.error(403, "无权限访问");
            }
            Long id = Long.valueOf(params.get("id").toString());
            String responsibility = params.get("responsibility") == null ? null : params.get("responsibility").toString();
            String result = params.get("result") == null ? null : params.get("result").toString();
            String finalStatus = params.get("finalStatus") == null ? null : params.get("finalStatus").toString();
            afterSalesService.arbitrate(id, responsibility, result, finalStatus);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
