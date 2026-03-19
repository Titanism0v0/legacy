package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.dto.ProductDTO;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品服务类
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductMapper productMapper;
    
    /**
     * 分页查询商品列表（数据库分页，按页拉取）
     */
    public Page<ProductDTO> getProductList(Integer page, Integer size, Long categoryId, String keyword, String status, Long sellerId) {
        int offset = (page - 1) * size;
        List<ProductDTO> list = productMapper.selectProductList(categoryId, keyword, status, sellerId, offset, size);
        long total = productMapper.selectProductCount(categoryId, keyword, status, sellerId);

        Page<ProductDTO> result = new Page<>(page, size, total);
        result.setRecords(list);
        return result;
    }
    
    /**
     * 根据ID查询商品详情
     */
    public ProductDTO getProductById(Long id) {
        ProductDTO product = productMapper.selectProductById(id);
        if (product != null) {
            // 增加浏览次数
            Product entity = productMapper.selectById(id);
            if (entity != null) {
                entity.setViewCount(entity.getViewCount() + 1);
                productMapper.updateById(entity);
            }
        }
        return product;
    }
    
    /**
     * 添加商品
     */
    public void addProduct(Product product) {
        product.setStatus("ON_SALE");
        product.setViewCount(0);
        productMapper.insert(product);
    }
    
    /**
     * 更新商品
     */
    public void updateProduct(Product product) {
        productMapper.updateById(product);
    }
    
    /**
     * 删除商品（逻辑删除）
     */
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }
    
    /**
     * 下架商品
     */
    public void offShelfProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus("OFF_SALE");
            productMapper.updateById(product);
        }
    }
    
    /**
     * 标记缺货
     */
    public void markOutOfStock(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus("OUT_OF_STOCK");
            productMapper.updateById(product);
        }
    }
    
    /**
     * 恢复上架（取消缺货）
     */
    public void restoreOnSale(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus("ON_SALE");
            productMapper.updateById(product);
        }
    }
    
    /**
     * 查询卖家的商品列表
     */
    public List<Product> getSellerProducts(Long sellerId) {
        // 使用LambdaQueryWrapper查询，MyBatis-Plus会自动处理逻辑删除(@TableLogic)
        return productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .orderByDesc(Product::getCreateTime)
        );
    }
    
    /**
     * 根据ID查询商品实体（不包含关联信息）
     */
    public Product getProductEntityById(Long id) {
        return productMapper.selectById(id);
    }

    /**
     * 管理员：审核商品/违规下架（最小实现）
     */
    public void auditProduct(Long productId, String action, String remark, String riskLevel, Integer restrictedFlag) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("商品不存在");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            product.setAuditStatus("APPROVED");
        } else if ("REJECT".equalsIgnoreCase(action)) {
            product.setAuditStatus("REJECTED");
            product.setStatus("OFF_SALE");
        } else if ("TAKE_DOWN".equalsIgnoreCase(action)) {
            product.setAuditStatus("REJECTED");
            product.setStatus("OFF_SALE");
        } else {
            throw new RuntimeException("无效的审核动作");
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
}
