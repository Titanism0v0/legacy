package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.ProductDTO;
import com.overseas.purchase.service.ProductService;
import com.overseas.purchase.service.RecommendationService;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerPublicDtoTest {

    @SuppressWarnings("unchecked")
    @Test
    void anonymousProductListForcesOnSaleAndOmitsBackOfficeAuditDetails() throws Exception {
        ProductService productService = mock(ProductService.class);
        RecommendationService recommendationService = mock(RecommendationService.class);
        ProductController controller = new ProductController(productService, recommendationService);
        HttpServletRequest request = mock(HttpServletRequest.class);

        ProductDTO product = new ProductDTO();
        product.setId(1L);
        product.setAuditStatus("PENDING");
        product.setAuditRemark("internal moderation reason");
        product.setRiskLevel("HIGH");
        product.setRestrictedFlag(1);
        Page<ProductDTO> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(product));
        when(productService.getProductList(1, 10, null, null, "ON_SALE", null)).thenReturn(page);

        Method endpoint = ProductController.class.getMethod("getProductList", Integer.class, Integer.class,
                Long.class, String.class, String.class, Long.class, HttpServletRequest.class);
        Result<Page<ProductDTO>> result = (Result<Page<ProductDTO>>) endpoint.invoke(
                controller, 1, 10, null, null, "OFF_SALE", null, request);

        ProductDTO publicProduct = result.getData().getRecords().get(0);
        assertThat(publicProduct.getAuditStatus()).isNull();
        assertThat(publicProduct.getAuditRemark()).isNull();
        assertThat(publicProduct.getRiskLevel()).isNull();
        assertThat(publicProduct.getRestrictedFlag()).isEqualTo(1);
        verify(productService).getProductList(1, 10, null, null, "ON_SALE", null);
    }

    @Test
    void anonymousProductDetailUsesPublicProductLookup() {
        ProductService productService = mock(ProductService.class);
        RecommendationService recommendationService = mock(RecommendationService.class);
        ProductController controller = new ProductController(productService, recommendationService);
        HttpServletRequest request = mock(HttpServletRequest.class);

        ProductDTO product = new ProductDTO();
        product.setId(1L);
        product.setAuditStatus("APPROVED");
        when(productService.getPublicProductById(1L)).thenReturn(product);

        Result<ProductDTO> result = controller.getProductById(1L, request);

        assertThat(result.getData().getAuditStatus()).isNull();
        verify(productService).getPublicProductById(1L);
    }

    @Test
    void anonymousProductDetailReturnsNotFoundForNonPublicProduct() {
        ProductService productService = mock(ProductService.class);
        RecommendationService recommendationService = mock(RecommendationService.class);
        ProductController controller = new ProductController(productService, recommendationService);
        HttpServletRequest request = mock(HttpServletRequest.class);

        Result<ProductDTO> result = controller.getProductById(1L, request);

        assertThat(result.getMessage()).isEqualTo("商品不存在");
    }
}
