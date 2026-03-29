package com.overseas.purchase.service;

import com.overseas.purchase.entity.Category;
import com.overseas.purchase.mapper.CategoryMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CrossBorderComplianceService {

    private final CategoryMapper categoryMapper;

    private static final Set<Long> TOP_WHITELIST = new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));
    private static final Set<String> BLACK_KEYWORDS = new HashSet<>(Arrays.asList(
            "electronic cigarette", "vape", "drug", "weapon", "explosive", "counterfeit",
            "电子烟", "处方药", "武器", "爆炸", "仿牌", "假货", "管制刀具"
    ));

    public ComplianceResult validateCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDeleted() == 1) {
            throw new RuntimeException("Category does not exist");
        }

        Long topId = resolveTopCategoryId(category);
        if (topId == null || !TOP_WHITELIST.contains(topId)) {
            return new ComplianceResult(false, "HIGH", 1, "Category is outside cross-border whitelist");
        }

        String text = normalize(category.getName()) + " " + normalize(category.getDescription());
        for (String keyword : BLACK_KEYWORDS) {
            if (text.contains(keyword)) {
                return new ComplianceResult(false, "HIGH", 1, "Category contains restricted keyword: " + keyword);
            }
        }

        String riskLevel = "LOW";
        if (topId == 1L || topId == 2L || topId == 4L) {
            riskLevel = "MEDIUM";
        }
        return new ComplianceResult(true, riskLevel, 0, "PASS");
    }

    public Long resolveTopCategoryId(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDeleted() == 1) {
            return null;
        }
        return resolveTopCategoryId(category);
    }

    private Long resolveTopCategoryId(Category category) {
        Long cursor = category.getId();
        Long parent = category.getParentId();
        int guard = 0;
        while (parent != null && parent > 0 && guard < 10) {
            Category parentCategory = categoryMapper.selectById(parent);
            if (parentCategory == null || parentCategory.getDeleted() == 1) {
                break;
            }
            cursor = parentCategory.getId();
            parent = parentCategory.getParentId();
            guard++;
        }
        return cursor;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    @Data
    @AllArgsConstructor
    public static class ComplianceResult {
        private boolean allowed;
        private String riskLevel;
        private Integer restrictedFlag;
        private String reason;
    }
}

