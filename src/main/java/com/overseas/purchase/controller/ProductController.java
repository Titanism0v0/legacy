package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.ProductDTO;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 商品控制器
 * 
 * @author System
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    public Result<Page<ProductDTO>> getProductList(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long sellerId) {
        Page<ProductDTO> result = productService.getProductList(page, size, categoryId, keyword, status, sellerId);
        return Result.success(result);
    }
    
    /**
     * 根据ID查询商品详情
     */
    @GetMapping("/detail/{id}")
    public Result<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }
    
    /**
     * 卖家：发布商品
     */
    @PostMapping("/add")
    public Result<Void> addProduct(@RequestBody Product product, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            if (!"SELLER".equals(role) && !"ADMIN".equals(role)) {
                return Result.error("无权限操作");
            }
            
            product.setSellerId(userId);
            productService.addProduct(product);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新商品
     */
    @PutMapping("/update")
    public Result<Void> updateProduct(@RequestBody Product product, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            // 打印接收到的商品信息，用于调试
            System.out.println("Updating product: " + product);
            System.out.println("Currency: " + product.getCurrency());
            
            // 检查权限：卖家只能更新自己的商品，管理员可以更新所有商品
            Product existProduct = productService.getProductEntityById(product.getId());
            if (existProduct == null || existProduct.getDeleted() == 1) {
                return Result.error("商品不存在");
            }
            
            if (!"ADMIN".equals(role) && !existProduct.getSellerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            productService.updateProduct(product);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员/卖家：下架商品
     */
    @PutMapping("/off-shelf/{id}")
    public Result<Void> offShelfProduct(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            Product product = productService.getProductEntityById(id);
            if (product == null || product.getDeleted() == 1) {
                return Result.error("商品不存在");
            }
            
            if (!"ADMIN".equals(role) && !product.getSellerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            productService.offShelfProduct(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员/卖家：标记缺货
     */
    @PutMapping("/out-of-stock/{id}")
    public Result<Void> markOutOfStock(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            Product product = productService.getProductEntityById(id);
            if (product == null || product.getDeleted() == 1) {
                return Result.error("商品不存在");
            }
            
            if (!"ADMIN".equals(role) && !product.getSellerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            productService.markOutOfStock(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员/卖家：恢复上架（取消缺货）
     */
    @PutMapping("/restore-on-sale/{id}")
    public Result<Void> restoreOnSale(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            Product product = productService.getProductEntityById(id);
            if (product == null || product.getDeleted() == 1) {
                return Result.error("商品不存在");
            }
            
            if (!"ADMIN".equals(role) && !product.getSellerId().equals(userId)) {
                return Result.error("无权限操作");
            }
            
            productService.restoreOnSale(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员：批量删除商品
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteProducts(@RequestBody List<Long> ids, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限访问");
        }
        
        try {
            for (Long id : ids) {
                productService.deleteProduct(id);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员/卖家：删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            Product product = productService.getProductEntityById(id);
            if (product == null) {
                return Result.success();
            }
            
            if ("ADMIN".equals(role)) {
                productService.deleteProduct(id);
                return Result.success();
            }
            
            if ("SELLER".equals(role)) {
                if (product.getSellerId().equals(userId)) {
                    productService.deleteProduct(id);
                    return Result.success();
                }
            }
            
            return Result.error(403, "无权限访问");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 卖家：查询我的商品列表
     */
    @GetMapping("/my-products")
    public Result<List<Product>> getMyProducts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            List<Product> products = productService.getSellerProducts(userId);
            return Result.success(products);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage() != null ? e.getMessage() : "加载商品失败");
        }
    }
}
