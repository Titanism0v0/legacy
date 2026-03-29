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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CrossBorderComplianceService complianceService;

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

    public void addProduct(Product product) {
        assertSellerCanPublish(product.getSellerId());
        CrossBorderComplianceService.ComplianceResult compliance = complianceService.validateCategory(product.getCategoryId());
        if (!compliance.isAllowed()) {
            throw new RuntimeException(compliance.getReason());
        }
        product.setStatus("ON_SALE");
        product.setAuditStatus("PENDING");
        product.setRiskLevel(compliance.getRiskLevel());
        product.setRestrictedFlag(compliance.getRestrictedFlag());
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
        if (product.getRiskLevel() == null) {
            product.setRiskLevel(compliance.getRiskLevel());
        }
        if (product.getRestrictedFlag() == null) {
            product.setRestrictedFlag(compliance.getRestrictedFlag());
        }
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

        if ("APPROVE".equalsIgnoreCase(action)) {
            if (product.getRestrictedFlag() != null && product.getRestrictedFlag() == 1) {
                throw new RuntimeException("Restricted product cannot be approved for sale");
            }
            product.setAuditStatus("APPROVED");
            if ("OFF_SALE".equals(product.getStatus()) || "OUT_OF_STOCK".equals(product.getStatus())) {
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
            product.setRestrictedFlag(restrictedFlag);
        }
        productMapper.updateById(product);
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

