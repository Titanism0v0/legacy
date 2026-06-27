package com.overseas.purchase.service;

import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceModerationTest {

    @Mock private ProductMapper productMapper;
    @Mock private UserMapper userMapper;
    @Mock private CrossBorderComplianceService complianceService;
    @Mock private AdminAuditModerationService moderationService;

    @Test
    void editingAnOnSaleProductToProhibitedContentForcesItBackToReview() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setSellerId(9L);
        existing.setCategoryId(1L);
        existing.setStatus("ON_SALE");
        existing.setDeleted(0);
        existing.setRestrictedFlag(0);
        when(productMapper.selectById(1L)).thenReturn(existing);

        User seller = new User();
        seller.setId(9L);
        seller.setRole("SELLER");
        seller.setKycStatus("APPROVED");
        seller.setDeleted(0);
        when(userMapper.selectById(9L)).thenReturn(seller);
        when(complianceService.validateCategory(1L))
                .thenReturn(new CrossBorderComplianceService.ComplianceResult(true, "LOW", 0, "PASS"));
        when(moderationService.moderate(eq("PRODUCT_SUBMISSION"), any(Map.class)))
                .thenReturn(new AdminAuditModerationService.ModerationResult(
                        "PRODUCT_SUBMISSION", "BLOCK", BigDecimal.ONE, "HIGH", "blocked by rules", "provider", "model"));
        when(moderationService.buildAuditRemark(any(), any()))
                .thenReturn("[AUTO_MOD] decision=BLOCK");

        Product update = new Product();
        update.setId(1L);
        update.setTitle("海洛因现货");
        new ProductService(productMapper, userMapper, complianceService, moderationService).updateProduct(update);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("OFF_SALE");
        assertThat(saved.getAuditStatus()).isEqualTo("PENDING");
        assertThat(saved.getRestrictedFlag()).isEqualTo(1);
        assertThat(saved.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    void adminCannotApproveWhileMarkingProductAsRestricted() {
        Product existing = new Product();
        existing.setId(2L);
        existing.setDeleted(0);
        existing.setRestrictedFlag(0);
        existing.setStatus("OFF_SALE");
        when(productMapper.selectById(2L)).thenReturn(existing);

        ProductService service = new ProductService(productMapper, userMapper, complianceService, moderationService);

        assertThatThrownBy(() -> service.auditProduct(2L, "APPROVE", null, "HIGH", 1))
                .hasMessageContaining("Restricted product");
    }
}
