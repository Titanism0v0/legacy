package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.entity.Cart;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.CartMapper;
import com.overseas.purchase.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 购物车服务类
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    /**
     * 添加商品到购物车
     */
    public void addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new RuntimeException("商品不存在");
        }

        if (!"ON_SALE".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架或缺货");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("商品已售罄");
        }

        Cart existCart = cartMapper.selectOne(
            new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId)
        );

        if (existCart != null) {
            int nextQuantity = existCart.getQuantity() + quantity;
            if (nextQuantity > product.getStock()) {
                throw new RuntimeException("库存不足，当前仅剩" + product.getStock() + "件");
            }
            existCart.setQuantity(nextQuantity);
            cartMapper.updateById(existCart);
        } else {
            if (quantity > product.getStock()) {
                throw new RuntimeException("库存不足，当前仅剩" + product.getStock() + "件");
            }
            try {
                Cart cart = new Cart();
                cart.setUserId(userId);
                cart.setProductId(productId);
                cart.setQuantity(quantity);
                cartMapper.insert(cart);
            } catch (DuplicateKeyException e) {
                int updated = cartMapper.restoreAndUpdateQuantity(userId, productId, quantity);
                if (updated == 0) {
                    throw new RuntimeException("恢复购物车记录失败");
                }
            } catch (Exception e) {
                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof java.sql.SQLIntegrityConstraintViolationException) {
                        int updated = cartMapper.restoreAndUpdateQuantity(userId, productId, quantity);
                        if (updated == 0) {
                            throw new RuntimeException("恢复购物车记录失败");
                        }
                        return;
                    }
                    cause = cause.getCause();
                }
                throw new RuntimeException("添加购物车失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 查询用户的购物车列表（自动清理售罄商品）
     */
    public List<Cart> getCartList(Long userId) {

        // 查询购物车
        List<Cart> cartList = cartMapper.selectList(
            new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getDeleted, 0)
                .orderByDesc(Cart::getCreateTime)
        );

        // 自动清理售罄商品
        cartList.removeIf(cart -> {
            Product product = productMapper.selectById(cart.getProductId());

            if (product == null
                    || product.getDeleted() == 1
                    || product.getStock() == null
                    || product.getStock() <= 0) {

                cartMapper.deleteById(cart.getId());
                return true;
            }
            if (cart.getQuantity() > product.getStock()) {
                cart.setQuantity(product.getStock());
                cartMapper.updateById(cart);
            }
            return false;
        });

        return cartList;
    }

    /**
     * 更新购物车商品数量
     */
    public void updateCartQuantity(Long cartId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || cart.getDeleted() != 0) {
            throw new RuntimeException("购物车记录不存在");
        }

        Product product = productMapper.selectById(cart.getProductId());
        if (product == null || product.getDeleted() == 1 || !"ON_SALE".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("商品已售罄");
        }

        int safeQuantity = Math.min(quantity, product.getStock());
        cart.setQuantity(safeQuantity);
        cartMapper.updateById(cart);
    }

    /**
     * 删除购物车商品
     */
    public void deleteCartItem(Long cartId) {
        cartMapper.deleteById(cartId);
    }

    /**
     * 清空购物车
     */
    public void clearCart(Long userId) {
        List<Cart> carts = cartMapper.selectList(
            new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getDeleted, 0)
        );

        carts.forEach(cart -> cartMapper.deleteById(cart.getId()));
    }
}
