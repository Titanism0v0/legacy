package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.dto.ProductDTO;
import com.overseas.purchase.entity.Cart;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.CartMapper;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 8;
    private static final double CATEGORY_WEIGHT = 35D;
    private static final double PREFERENCE_WEIGHT = 30D;
    private static final double PRICE_WEIGHT = 20D;
    private static final double POPULARITY_WEIGHT = 15D;

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;

    public List<ProductDTO> recommendProducts(Long currentProductId, Long userId, Integer limit) {
        Product currentProduct = productMapper.selectById(currentProductId);
        if (currentProduct == null) {
            throw new RuntimeException("Product does not exist");
        }

        int recommendLimit = normalizeLimit(limit);
        List<Product> allCandidates = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SALE")
                .gt(Product::getStock, 0)
                .ne(Product::getId, currentProductId)
                .orderByDesc(Product::getCreateTime));
        if (allCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> userPreference = buildUserPreference(userId);
        boolean coldStart = userPreference.isEmpty();

        List<Product> primaryCandidates = allCandidates;
        List<Product> secondaryCandidates = Collections.emptyList();
        if (coldStart && currentProduct.getCategoryId() != null) {
            primaryCandidates = allCandidates.stream()
                    .filter(product -> Objects.equals(product.getCategoryId(), currentProduct.getCategoryId()))
                    .collect(Collectors.toList());
            if (primaryCandidates.isEmpty()) {
                primaryCandidates = allCandidates;
            } else {
                Set<Long> primaryIds = primaryCandidates.stream()
                        .map(Product::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                secondaryCandidates = allCandidates.stream()
                        .filter(product -> !primaryIds.contains(product.getId()))
                        .collect(Collectors.toList());
            }
        }

        List<ProductDTO> recommendations = new ArrayList<>(
                rankCandidates(primaryCandidates, currentProduct, userPreference, recommendLimit)
        );
        if (recommendations.size() < recommendLimit && !secondaryCandidates.isEmpty()) {
            Set<Long> chosenIds = recommendations.stream()
                    .map(ProductDTO::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<ProductDTO> fallback = rankCandidates(secondaryCandidates, currentProduct, userPreference, recommendLimit)
                    .stream()
                    .filter(item -> !chosenIds.contains(item.getId()))
                    .limit(recommendLimit - recommendations.size())
                    .collect(Collectors.toList());
            recommendations.addAll(fallback);
        }

        return recommendations.stream()
                .limit(recommendLimit)
                .collect(Collectors.toList());
    }

    private List<ProductDTO> rankCandidates(List<Product> candidates,
                                            Product currentProduct,
                                            Map<Long, Double> userPreference,
                                            int limit) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Integer> salesByProduct = loadSalesByProduct(candidates);
        int maxSales = candidates.stream()
                .mapToInt(product -> salesByProduct.getOrDefault(product.getId(), 0))
                .max()
                .orElse(0);
        int maxViews = candidates.stream()
                .mapToInt(product -> safeInt(product.getViewCount()))
                .max()
                .orElse(0);
        double maxPreference = userPreference.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0D);

        return candidates.stream()
                .map(candidate -> buildCandidate(candidate, currentProduct, userPreference, maxPreference, salesByProduct, maxSales, maxViews))
                .sorted(Comparator
                        .comparingDouble(ScoredCandidate::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(ScoredCandidate::getSalesCount).reversed())
                        .thenComparing(Comparator.comparingInt(ScoredCandidate::getViewCount).reversed())
                        .thenComparing(ScoredCandidate::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(ScoredCandidate::toProductDTO)
                .collect(Collectors.toList());
    }

    private ScoredCandidate buildCandidate(Product candidate,
                                           Product currentProduct,
                                           Map<Long, Double> userPreference,
                                           double maxPreference,
                                           Map<Long, Integer> salesByProduct,
                                           int maxSales,
                                           int maxViews) {
        double categoryScore = Objects.equals(candidate.getCategoryId(), currentProduct.getCategoryId()) ? CATEGORY_WEIGHT : 0D;
        double preferenceRatio = maxPreference > 0D
                ? userPreference.getOrDefault(candidate.getCategoryId(), 0D) / maxPreference
                : 0D;
        double preferenceScore = preferenceRatio * PREFERENCE_WEIGHT;
        double priceScore = calculatePriceCloseness(currentProduct, candidate) * PRICE_WEIGHT;
        double popularityScore = calculatePopularityScore(candidate, salesByProduct, maxSales, maxViews) * POPULARITY_WEIGHT;

        // MVP推荐评分 = 当前商品类别匹配 + 用户偏好类别匹配 + 价格接近度 + 商品热度
        double totalScore = roundScore(categoryScore + preferenceScore + priceScore + popularityScore);
        return new ScoredCandidate(candidate, totalScore, salesByProduct.getOrDefault(candidate.getId(), 0), safeInt(candidate.getViewCount()));
    }

    private Map<Long, Double> buildUserPreference(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }

        Map<Long, Double> categoryWeights = new HashMap<>();

        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .select(Order::getProductId, Order::getQuantity, Order::getStatus)
                .eq(Order::getBuyerId, userId));
        mergeCategoryWeight(categoryWeights, orders.stream()
                .filter(order -> order.getProductId() != null && isEffectiveOrder(order.getStatus()))
                .collect(Collectors.toMap(Order::getProductId, order -> order.getQuantity() == null ? 1D : order.getQuantity() * 2D, Double::sum)));

        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .select(Cart::getProductId, Cart::getQuantity)
                .eq(Cart::getUserId, userId));
        mergeCategoryWeight(categoryWeights, carts.stream()
                .filter(cart -> cart.getProductId() != null)
                .collect(Collectors.toMap(Cart::getProductId, cart -> cart.getQuantity() == null ? 1D : cart.getQuantity().doubleValue(), Double::sum)));

        return categoryWeights;
    }

    private void mergeCategoryWeight(Map<Long, Double> categoryWeights, Map<Long, Double> productWeights) {
        if (productWeights.isEmpty()) {
            return;
        }

        List<Product> behaviorProducts = productMapper.selectBatchIds(productWeights.keySet());
        for (Product product : behaviorProducts) {
            if (product == null || product.getCategoryId() == null) {
                continue;
            }
            double weight = productWeights.getOrDefault(product.getId(), 0D);
            categoryWeights.merge(product.getCategoryId(), weight, Double::sum);
        }
    }

    private Map<Long, Integer> loadSalesByProduct(List<Product> candidates) {
        List<Long> productIds = candidates.stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .select(Order::getProductId, Order::getQuantity, Order::getStatus)
                .in(Order::getProductId, productIds));
        Map<Long, Integer> salesByProduct = new HashMap<>();
        for (Order order : orders) {
            if (order.getProductId() == null || !isEffectiveOrder(order.getStatus())) {
                continue;
            }
            salesByProduct.merge(order.getProductId(), order.getQuantity() == null ? 1 : order.getQuantity(), Integer::sum);
        }
        return salesByProduct;
    }

    private boolean isEffectiveOrder(String status) {
        return !StringUtils.hasText(status) || !"CANCELLED".equalsIgnoreCase(status);
    }

    private double calculatePriceCloseness(Product currentProduct, Product candidate) {
        if (currentProduct.getPrice() == null || candidate.getPrice() == null || currentProduct.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return 0D;
        }
        if (!sameCurrency(currentProduct.getCurrency(), candidate.getCurrency())) {
            return 0.2D;
        }

        BigDecimal priceGap = currentProduct.getPrice().subtract(candidate.getPrice()).abs();
        BigDecimal ratio = BigDecimal.ONE.subtract(
                priceGap.divide(currentProduct.getPrice(), 4, RoundingMode.HALF_UP)
        );
        double closeness = ratio.doubleValue();
        if (closeness < 0D) {
            return 0D;
        }
        return Math.min(closeness, 1D);
    }

    private boolean sameCurrency(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equalsIgnoreCase(right);
    }

    private double calculatePopularityScore(Product candidate,
                                            Map<Long, Integer> salesByProduct,
                                            int maxSales,
                                            int maxViews) {
        double salesRatio = maxSales > 0 ? salesByProduct.getOrDefault(candidate.getId(), 0) / (double) maxSales : 0D;
        double viewRatio = maxViews > 0 ? safeInt(candidate.getViewCount()) / (double) maxViews : 0D;
        return salesRatio * 0.7D + viewRatio * 0.3D;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double roundScore(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static class ScoredCandidate {
        private final Product product;
        private final double score;
        private final int salesCount;
        private final int viewCount;

        private ScoredCandidate(Product product, double score, int salesCount, int viewCount) {
            this.product = product;
            this.score = score;
            this.salesCount = salesCount;
            this.viewCount = viewCount;
        }

        public double getScore() {
            return score;
        }

        public int getSalesCount() {
            return salesCount;
        }

        public int getViewCount() {
            return viewCount;
        }

        public java.time.LocalDateTime getCreateTime() {
            return product.getCreateTime();
        }

        public ProductDTO toProductDTO() {
            ProductDTO dto = new ProductDTO();
            BeanUtils.copyProperties(product, dto);
            return dto;
        }
    }
}
