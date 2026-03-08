package com.overseas.purchase.service;

import com.overseas.purchase.dto.SellerReviewDTO;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.SellerReview;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.SellerReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商家评价服务
 */
@Service
@RequiredArgsConstructor
public class SellerReviewService {

    private final SellerReviewMapper sellerReviewMapper;
    private final OrderMapper orderMapper;

    /**
     * 查询商家的评价列表
     */
    public List<SellerReviewDTO> getReviewsBySellerId(Long sellerId) {
        return sellerReviewMapper.selectBySellerId(sellerId);
    }

    /**
     * 用户对商家进行评价（订单完成后）
     */
    public void addReview(Long orderId, Long buyerId, Integer rating, String content) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("只能评价自己购买的订单");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("只有交易成功的订单才能评价");
        }

        Long count = sellerReviewMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SellerReview>()
                .eq(SellerReview::getOrderId, orderId)
        );
        if (count != null && count > 0) {
            throw new RuntimeException("该订单已评价过");
        }

        int stars = (rating == null || rating < 1 || rating > 5) ? 5 : rating;

        SellerReview review = new SellerReview();
        review.setOrderId(orderId);
        review.setBuyerId(buyerId);
        review.setSellerId(order.getSellerId());
        review.setRating(stars);
        review.setContent(content);
        sellerReviewMapper.insert(review);
    }

    /**
     * 删除评价（仅发布者本人可删）
     */
    public void deleteReview(Long reviewId, Long userId) {
        SellerReview review = sellerReviewMapper.selectById(reviewId);
        if (review == null || review.getDeleted() == 1) {
            throw new RuntimeException("评价不存在");
        }
        if (!review.getBuyerId().equals(userId)) {
            throw new RuntimeException("只能删除自己的评价");
        }
        sellerReviewMapper.deleteById(reviewId);
    }

    /**
     * 检查订单是否已评价
     */
    public boolean hasReviewed(Long orderId) {
        Long count = sellerReviewMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SellerReview>()
                .eq(SellerReview::getOrderId, orderId)
        );
        return count != null && count > 0;
    }
}
