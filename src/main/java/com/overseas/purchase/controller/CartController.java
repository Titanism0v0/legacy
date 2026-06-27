package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.Cart;
import com.overseas.purchase.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 * 
 * @author System
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<Void> addToCart(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Long productId = Long.valueOf(params.get("productId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());
            
            cartService.addToCart(userId, productId, quantity);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
    
    /**
     * 查询购物车列表
     */
    @GetMapping("/list")
    public Result<List<Cart>> getCartList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Cart> carts = cartService.getCartList(userId);
        return Result.success(carts);
    }
    
    /**
     * 更新购物车商品数量
     */
    @PutMapping("/update")
    public Result<Void> updateCartQuantity(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Long cartId = Long.valueOf(params.get("cartId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());
            
            cartService.updateCartQuantity(cartId, quantity, userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
    
    /**
     * 删除购物车商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCartItem(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            cartService.deleteCartItem(id, userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
    
    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public Result<Void> clearCart(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            cartService.clearCart(userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
}
