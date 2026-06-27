package com.overseas.purchase.service;

import com.overseas.purchase.dto.ProductDTO;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServicePublicLookupTest {

    @Test
    void publicProductLookupReturnsNullWithoutIncrementingViewCountWhenMapperFindsNothing() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductService service = new ProductService(
                productMapper,
                mock(UserMapper.class),
                mock(CrossBorderComplianceService.class),
                mock(AdminAuditModerationService.class));

        ProductDTO result = service.getPublicProductById(1L);

        assertThat(result).isNull();
        verify(productMapper).selectPublicProductById(1L);
        verify(productMapper, never()).selectById(1L);
    }

    @Test
    void publicProductLookupIncrementsViewCountOnlyForPublicProduct() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        Product entity = new Product();
        entity.setId(1L);
        entity.setViewCount(7);
        when(productMapper.selectPublicProductById(1L)).thenReturn(dto);
        when(productMapper.selectById(1L)).thenReturn(entity);
        ProductService service = new ProductService(
                productMapper,
                mock(UserMapper.class),
                mock(CrossBorderComplianceService.class),
                mock(AdminAuditModerationService.class));

        ProductDTO result = service.getPublicProductById(1L);

        assertThat(result).isSameAs(dto);
        assertThat(entity.getViewCount()).isEqualTo(8);
        verify(productMapper).updateById(entity);
    }
}
