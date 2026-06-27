package com.overseas.purchase.service;

import com.overseas.purchase.entity.Cart;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.CartMapper;
import com.overseas.purchase.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceAccessTest {

    @Test
    void userCanUpdateOwnCartItem() {
        CartMapper cartMapper = mock(CartMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        when(cartMapper.selectById(1L)).thenReturn(cart(1L, 10L, 100L));
        when(productMapper.selectById(100L)).thenReturn(product());
        CartService service = new CartService(cartMapper, productMapper);

        service.updateCartQuantity(1L, 3, 10L);

        verify(cartMapper).updateById(any(Cart.class));
    }

    @Test
    void userCannotUpdateAnotherUsersCartItem() {
        CartMapper cartMapper = mock(CartMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        when(cartMapper.selectById(1L)).thenReturn(cart(1L, 20L, 100L));
        CartService service = new CartService(cartMapper, productMapper);

        assertThatThrownBy(() -> service.updateCartQuantity(1L, 3, 10L))
                .hasMessage("No permission");
        verify(productMapper, never()).selectById(any());
        verify(cartMapper, never()).updateById(any());
    }

    @Test
    void invalidQuantityIsStillRejectedBeforeOwnershipWork() {
        CartService service = new CartService(mock(CartMapper.class), mock(ProductMapper.class));

        assertThatThrownBy(() -> service.updateCartQuantity(1L, 0, 10L))
                .hasMessage("商品数量必须大于0");
    }

    @Test
    void userCanDeleteOwnCartItem() {
        CartMapper cartMapper = mock(CartMapper.class);
        when(cartMapper.selectById(1L)).thenReturn(cart(1L, 10L, 100L));
        CartService service = new CartService(cartMapper, mock(ProductMapper.class));

        service.deleteCartItem(1L, 10L);

        verify(cartMapper).deleteById(1L);
    }

    @Test
    void userCannotDeleteAnotherUsersCartItem() {
        CartMapper cartMapper = mock(CartMapper.class);
        when(cartMapper.selectById(1L)).thenReturn(cart(1L, 20L, 100L));
        CartService service = new CartService(cartMapper, mock(ProductMapper.class));

        assertThatThrownBy(() -> service.deleteCartItem(1L, 10L))
                .hasMessage("No permission");
        verify(cartMapper, never()).deleteById(1L);
    }

    private static Cart cart(Long id, Long userId, Long productId) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(1);
        cart.setDeleted(0);
        return cart;
    }

    private static Product product() {
        Product product = new Product();
        product.setId(100L);
        product.setStatus("ON_SALE");
        product.setStock(10);
        product.setDeleted(0);
        return product;
    }
}
