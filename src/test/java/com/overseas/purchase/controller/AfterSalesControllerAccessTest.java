package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.AfterSalesAuditLog;
import com.overseas.purchase.entity.AfterSalesOrder;
import com.overseas.purchase.service.AfterSalesAuditLogService;
import com.overseas.purchase.service.AfterSalesService;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSalesControllerAccessTest {

    @Test
    void buyerSellerAndAdminCanReadTheirAfterSalesDetail() {
        AfterSalesService afterSalesService = mock(AfterSalesService.class);
        AfterSalesController controller = new AfterSalesController(afterSalesService, mock(AfterSalesAuditLogService.class));
        AfterSalesOrder apply = afterSalesApply();
        when(afterSalesService.getDetail(1L)).thenReturn(apply);

        assertThat(controller.getDetail(1L, request(10L, "USER")).getCode()).isEqualTo(200);
        assertThat(controller.getDetail(1L, request(20L, "SELLER")).getCode()).isEqualTo(200);
        assertThat(controller.getDetail(1L, request(99L, "ADMIN")).getCode()).isEqualTo(200);
    }

    @Test
    void unrelatedUserCannotReadAfterSalesDetail() {
        AfterSalesService afterSalesService = mock(AfterSalesService.class);
        AfterSalesController controller = new AfterSalesController(afterSalesService, mock(AfterSalesAuditLogService.class));
        when(afterSalesService.getDetail(1L)).thenReturn(afterSalesApply());

        Result<AfterSalesOrder> result = controller.getDetail(1L, request(30L, "USER"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("No permission");
        assertThat(result.getData()).isNull();
    }

    @Test
    void unrelatedUserCannotReadAfterSalesLogs() {
        AfterSalesService afterSalesService = mock(AfterSalesService.class);
        AfterSalesAuditLogService auditLogService = mock(AfterSalesAuditLogService.class);
        AfterSalesController controller = new AfterSalesController(afterSalesService, auditLogService);
        when(afterSalesService.getDetail(1L)).thenReturn(afterSalesApply());

        Result<List<AfterSalesAuditLog>> result = controller.getLogs(1L, request(30L, "USER"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("No permission");
        verify(auditLogService, never()).listByAfterSalesId(1L);
    }

    @Test
    void sellerCannotUseAdminAuditEndpoint() {
        AfterSalesService afterSalesService = mock(AfterSalesService.class);
        AfterSalesController controller = new AfterSalesController(afterSalesService, mock(AfterSalesAuditLogService.class));

        Result<Void> result = controller.audit(auditParams(), request(20L, "SELLER"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("No permission");
        verify(afterSalesService, never()).audit(1L, "APPROVED", "ok");
    }

    @Test
    void adminCanUseAdminAuditEndpoint() {
        AfterSalesService afterSalesService = mock(AfterSalesService.class);
        AfterSalesController controller = new AfterSalesController(afterSalesService, mock(AfterSalesAuditLogService.class));

        Result<Void> result = controller.audit(auditParams(), request(99L, "ADMIN"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(afterSalesService).audit(1L, "APPROVED", "ok");
    }

    private static AfterSalesOrder afterSalesApply() {
        AfterSalesOrder apply = new AfterSalesOrder();
        apply.setId(1L);
        apply.setUserId(10L);
        apply.setSellerId(20L);
        apply.setDeleted(0);
        return apply;
    }

    private static Map<String, Object> auditParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("status", "APPROVED");
        params.put("remark", "ok");
        return params;
    }

    private static HttpServletRequest request(Long userId, String role) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(userId);
        when(request.getAttribute("role")).thenReturn(role);
        return request;
    }
}
