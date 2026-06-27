package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.SellerReviewDTO;
import com.overseas.purchase.service.SellerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 商家评价控制器
 */
@RestController
@RequestMapping("/seller-review")
@RequiredArgsConstructor
public class SellerReviewController {

    private final SellerReviewService sellerReviewService;

    /**
     * 查询商家的评价列表（公开接口，无需登录）
     */
    @GetMapping("/list")
    public Result<List<SellerReviewDTO>> getReviewList(@RequestParam Long sellerId) {
        List<SellerReviewDTO> list = sellerReviewService.getReviewsBySellerId(sellerId);
        return Result.success(list);
    }

    /**
     * 用户提交评价（需登录，且为订单买家）
     */
    @PostMapping("/add")
    public Result<Void> addReview(@RequestBody AddReviewRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            sellerReviewService.addReview(request.getOrderId(), userId, request.getRating(), request.getContent());
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    /**
     * 删除评价（仅发布者本人可删）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteReview(@PathVariable Long id, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            sellerReviewService.deleteReview(id, userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }

    /**
     * 检查订单是否已评价
     */
    @GetMapping("/has-reviewed")
    public Result<Boolean> hasReviewed(@RequestParam Long orderId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            return Result.success(false);
        }
        boolean has = sellerReviewService.hasReviewed(orderId);
        return Result.success(has);
    }

    @lombok.Data
    public static class AddReviewRequest {
        private Long orderId;
        private Integer rating;
        private String content;
    }
}
