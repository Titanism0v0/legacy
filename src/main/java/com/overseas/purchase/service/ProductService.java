package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.dto.ProductDTO;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CrossBorderComplianceService complianceService;
    private final AdminAuditModerationService adminAuditModerationService;

    public Page<ProductDTO> getProductList(Integer page, Integer size, Long categoryId, String keyword, String status, Long sellerId) {
        int offset = (page - 1) * size;
        List<ProductDTO> list = productMapper.selectProductList(categoryId, keyword, status, sellerId, offset, size);
        long total = productMapper.selectProductCount(categoryId, keyword, status, sellerId);
        Page<ProductDTO> result = new Page<>(page, size, total);
        result.setRecords(list);
        return result;
    }

    public ProductDTO getProductById(Long id) {
        ProductDTO product = productMapper.selectProductById(id);
        if (product != null) {
            Product entity = productMapper.selectById(id);
            if (entity != null) {
                entity.setViewCount(entity.getViewCount() + 1);
                productMapper.updateById(entity);
            }
        }
        return product;
    }

    public ProductDTO getPublicProductById(Long id) {
        ProductDTO product = productMapper.selectPublicProductById(id);
        if (product != null) {
            Product entity = productMapper.selectById(id);
            if (entity != null) {
                entity.setViewCount(entity.getViewCount() + 1);
                productMapper.updateById(entity);
            }
        }
        return product;
    }

    public void addProduct(Product product) {
        assertSellerCanPublish(product.getSellerId());
        CrossBorderComplianceService.ComplianceResult compliance = complianceService.validateCategory(product.getCategoryId());
        if (!compliance.isAllowed()) {
            throw new RuntimeException(compliance.getReason());
        }

        Map<String, String> moderationFields = new LinkedHashMap<>();
        moderationFields.put("title", product.getTitle());
        moderationFields.put("description", product.getDescription());
        moderationFields.put("shippingAddress", product.getShippingAddress());
        moderationFields.put("mainImage", product.getImage());
        moderationFields.put("images", product.getImages());

        AdminAuditModerationService.ModerationResult moderation = adminAuditModerationService
                .moderate("PRODUCT_SUBMISSION", moderationFields);

        product.setStatus("OFF_SALE");
        product.setAuditStatus("PENDING");
        product.setRiskLevel(mergeRiskLevel(compliance.getRiskLevel(), moderation.getRiskLevel()));
        product.setRestrictedFlag(DECISION_BLOCK.equals(moderation.getDecision()) ? 1 : compliance.getRestrictedFlag());
        String complianceRemark = "PASS".equalsIgnoreCase(compliance.getReason()) ? null : compliance.getReason();
        product.setAuditRemark(adminAuditModerationService.buildAuditRemark(moderation, complianceRemark));
        product.setViewCount(0);
        productMapper.insert(product);
    }

    public void updateProduct(Product product) {
        Product existing = productMapper.selectById(product.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new RuntimeException("Product does not exist");
        }

        assertSellerCanPublish(existing.getSellerId());
        Long categoryId = product.getCategoryId() == null ? existing.getCategoryId() : product.getCategoryId();
        CrossBorderComplianceService.ComplianceResult compliance = complianceService.validateCategory(categoryId);
        if (!compliance.isAllowed()) {
            throw new RuntimeException(compliance.getReason());
        }

        if (product.getCategoryId() == null) {
            product.setCategoryId(existing.getCategoryId());
        }

        Map<String, String> moderationFields = new LinkedHashMap<>();
        moderationFields.put("title", prefer(product.getTitle(), existing.getTitle()));
        moderationFields.put("description", prefer(product.getDescription(), existing.getDescription()));
        moderationFields.put("shippingAddress", prefer(product.getShippingAddress(), existing.getShippingAddress()));
        moderationFields.put("mainImage", prefer(product.getImage(), existing.getImage()));
        moderationFields.put("images", prefer(product.getImages(), existing.getImages()));
        AdminAuditModerationService.ModerationResult moderation = adminAuditModerationService
                .moderate("PRODUCT_SUBMISSION", moderationFields);

        product.setStatus("OFF_SALE");
        product.setAuditStatus("PENDING");
        product.setRiskLevel(mergeRiskLevel(compliance.getRiskLevel(), moderation.getRiskLevel()));
        product.setRestrictedFlag(DECISION_BLOCK.equals(moderation.getDecision()) ? 1 : compliance.getRestrictedFlag());
        String complianceRemark = "PASS".equalsIgnoreCase(compliance.getReason()) ? null : compliance.getReason();
        product.setAuditRemark(adminAuditModerationService.buildAuditRemark(moderation, complianceRemark));
        productMapper.updateById(product);
    }

    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    public void offShelfProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus("OFF_SALE");
            productMapper.updateById(product);
        }
    }

    public void markOutOfStock(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus("OUT_OF_STOCK");
            productMapper.updateById(product);
        }
    }

    public void restoreOnSale(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            assertSellerCanPublish(product.getSellerId());
            if (product.getRestrictedFlag() != null && product.getRestrictedFlag() == 1) {
                throw new RuntimeException("Restricted product cannot be put on sale");
            }
            product.setStatus("ON_SALE");
            productMapper.updateById(product);
        }
    }

    public List<Product> getSellerProducts(Long sellerId) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .orderByDesc(Product::getCreateTime));
    }

    public Product getProductEntityById(Long id) {
        return productMapper.selectById(id);
    }

    public void auditProduct(Long productId, String action, String remark, String riskLevel, Integer restrictedFlag) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("Product does not exist");
        }

        Integer effectiveRestrictedFlag = restrictedFlag == null ? product.getRestrictedFlag() : restrictedFlag;

        if ("APPROVE".equalsIgnoreCase(action)) {
            if (effectiveRestrictedFlag != null && effectiveRestrictedFlag == 1) {
                throw new RuntimeException("Restricted product cannot be approved for sale");
            }
            product.setAuditStatus("APPROVED");
            if ("OUT_OF_STOCK".equals(product.getStatus())) {
                // keep current status
            } else {
                product.setStatus("ON_SALE");
            }
        } else if ("REJECT".equalsIgnoreCase(action) || "TAKE_DOWN".equalsIgnoreCase(action)) {
            product.setAuditStatus("REJECTED");
            product.setStatus("OFF_SALE");
        } else {
            throw new RuntimeException("Invalid action");
        }

        if (remark != null) {
            product.setAuditRemark(remark);
        }
        if (riskLevel != null) {
            product.setRiskLevel(riskLevel);
        }
        if (restrictedFlag != null) {
            product.setRestrictedFlag(effectiveRestrictedFlag);
        }
        productMapper.updateById(product);
    }

    private static final String DECISION_BLOCK = "BLOCK";

    private String mergeRiskLevel(String first, String second) {
        return levelWeight(second) > levelWeight(first) ? normalizeRisk(second) : normalizeRisk(first);
    }

    private int levelWeight(String level) {
        String normalized = normalizeRisk(level);
        if ("HIGH".equals(normalized)) {
            return 3;
        }
        if ("MEDIUM".equals(normalized)) {
            return 2;
        }
        return 1;
    }

    private String normalizeRisk(String level) {
        if (!StringUtils.hasText(level)) {
            return "LOW";
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        if ("HIGH".equals(normalized) || "MEDIUM".equals(normalized)) {
            return normalized;
        }
        return "LOW";
    }

    private String prefer(String candidate, String fallback) {
        return candidate == null ? fallback : candidate;
    }

    private void assertSellerCanPublish(Long sellerId) {
        User seller = userMapper.selectById(sellerId);
        if (seller == null || seller.getDeleted() == 1) {
            throw new RuntimeException("Seller does not exist");
        }
        if ("ADMIN".equalsIgnoreCase(seller.getRole())) {
            return;
        }
        if (!"SELLER".equalsIgnoreCase(seller.getRole())) {
            throw new RuntimeException("Only sellers can publish products");
        }
        if (!"APPROVED".equalsIgnoreCase(seller.getKycStatus())) {
            throw new RuntimeException("Seller KYC is not approved yet");
        }
    }
}
