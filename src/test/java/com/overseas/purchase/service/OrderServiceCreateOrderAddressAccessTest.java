package com.overseas.purchase.service;

import com.overseas.purchase.entity.Address;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.AddressMapper;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCreateOrderAddressAccessTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private CrossBorderPricingService pricingService;

    @Mock
    private CrossBorderComplianceService complianceService;

    @Mock
    private AdminAuditModerationService adminAuditModerationService;

    @Mock
    private OrderFulfillmentService orderFulfillmentService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void missingAddressRejectsOrderCreationBeforeSideEffects() {
        stubAvailableProduct();
        when(addressMapper.selectById(30L)).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder(10L, 1L, 30L, 2, "CNY", 1, 0))
                .hasMessageContaining("Address does not exist");

        verifyNoInteractions(orderMapper);
        verify(productMapper).selectById(1L);
    }

    @Test
    void deletedAddressRejectsOrderCreationBeforeSideEffects() {
        stubAvailableProduct();
        when(addressMapper.selectById(30L)).thenReturn(address(10L, 1));

        assertThatThrownBy(() -> orderService.createOrder(10L, 1L, 30L, 2, "CNY", 1, 0))
                .hasMessageContaining("Address does not exist");

        verifyNoInteractions(orderMapper);
        verify(productMapper).selectById(1L);
    }

    @Test
    void anotherBuyerAddressRejectsOrderCreationBeforeSideEffects() {
        stubAvailableProduct();
        when(addressMapper.selectById(30L)).thenReturn(address(11L, 0));

        assertThatThrownBy(() -> orderService.createOrder(10L, 1L, 30L, 2, "CNY", 1, 0))
                .hasMessageContaining("No permission");

        verifyNoInteractions(orderMapper);
        verify(productMapper).selectById(1L);
    }

    @Test
    void nullAddressIdRejectsOrderCreationBeforeSideEffects() {
        stubAvailableProduct();

        assertThatThrownBy(() -> orderService.createOrder(10L, 1L, null, 2, "CNY", 1, 0))
                .hasMessageContaining("Address does not exist");

        verifyNoInteractions(orderMapper);
        verify(productMapper).selectById(1L);
    }

    @Test
    void buyerOwnedAddressCreatesOrderWithRequestedAddress() {
        Product product = stubAvailableProduct();
        stubSellerComplianceAndQuote(product);
        when(addressMapper.selectById(30L)).thenReturn(address(10L, 0));

        Order result = orderService.createOrder(10L, 1L, 30L, 2, "CNY", 1, 0);

        assertThat(result.getAddressId()).isEqualTo(30L);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getBuyerId()).isEqualTo(10L);
        assertThat(orderCaptor.getValue().getAddressId()).isEqualTo(30L);
        verify(productMapper).updateById(productWithStock(3));
    }

    private Product stubAvailableProduct() {
        Product product = productWithStock(5);
        when(productMapper.selectById(1L)).thenReturn(product);
        return product;
    }

    private void stubSellerComplianceAndQuote(Product product) {
        User seller = new User();
        seller.setId(20L);
        seller.setRole("SELLER");
        seller.setKycStatus("APPROVED");
        seller.setDeleted(0);
        when(userMapper.selectById(20L)).thenReturn(seller);
        when(complianceService.validateCategory(3L))
                .thenReturn(new CrossBorderComplianceService.ComplianceResult(true, "LOW", 0, "PASS"));
        when(pricingService.calculateQuote(product, 2, "CNY")).thenReturn(priceQuote());
    }

    private Product productWithStock(int stock) {
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(20L);
        product.setCategoryId(3L);
        product.setPrice(new BigDecimal("100.00"));
        product.setCurrency("CNY");
        product.setStock(stock);
        product.setStatus("ON_SALE");
        product.setRestrictedFlag(0);
        product.setDeleted(0);
        return product;
    }

    private Address address(Long userId, int deleted) {
        Address address = new Address();
        address.setId(30L);
        address.setUserId(userId);
        address.setDeleted(deleted);
        return address;
    }

    private CrossBorderPricingService.PriceQuote priceQuote() {
        return new CrossBorderPricingService.PriceQuote(
                new BigDecimal("200.00"),
                new BigDecimal("18.00"),
                new BigDecimal("2.00"),
                new BigDecimal("20.00"),
                new BigDecimal("220.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("28.60"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("28.60"),
                new BigDecimal("248.60"),
                new BigDecimal("0.1300"),
                BigDecimal.ONE.setScale(2),
                "CNY",
                "CNY",
                "CNY",
                false,
                0,
                "PENDING_DECLARATION",
                "CBEC_PREFERENTIAL",
                "ASIA_NEAR",
                "CBEC-IMPORT-V1",
                null
        );
    }
}
