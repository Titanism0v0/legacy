package com.overseas.purchase.service;

import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.OrderEvidence;
import com.overseas.purchase.mapper.OrderEvidenceMapper;
import com.overseas.purchase.mapper.OrderMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderEvidenceServiceAccessTest {

    @Test
    void unrelatedUserCannotAddEvidenceToAnotherOrder() {
        OrderEvidenceMapper evidenceMapper = mock(OrderEvidenceMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        when(orderMapper.selectById(1L)).thenReturn(order());
        OrderEvidenceService service = new OrderEvidenceService(evidenceMapper, orderMapper);
        OrderEvidence evidence = new OrderEvidence();
        evidence.setOrderId(1L);
        evidence.setCreatedBy(999L);

        assertThatThrownBy(() -> service.addEvidence(evidence, 30L, "USER"))
                .hasMessage("No permission");
        verify(evidenceMapper, never()).insert(any());
    }

    @Test
    void buyerCanAddEvidenceAndCreatedByUsesCurrentUser() {
        OrderEvidenceMapper evidenceMapper = mock(OrderEvidenceMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        when(orderMapper.selectById(1L)).thenReturn(order());
        OrderEvidenceService service = new OrderEvidenceService(evidenceMapper, orderMapper);
        OrderEvidence evidence = new OrderEvidence();
        evidence.setOrderId(1L);
        evidence.setCreatedBy(999L);

        service.addEvidence(evidence, 10L, "USER");

        assertThat(evidence.getCreatedBy()).isEqualTo(10L);
        verify(evidenceMapper).insert(evidence);
    }

    @Test
    void sellerAndAdminCanListEvidenceForOrder() {
        OrderEvidenceMapper evidenceMapper = mock(OrderEvidenceMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        when(orderMapper.selectById(1L)).thenReturn(order());
        when(evidenceMapper.selectList(any())).thenReturn(Collections.singletonList(new OrderEvidence()));
        OrderEvidenceService service = new OrderEvidenceService(evidenceMapper, orderMapper);

        List<OrderEvidence> sellerResult = service.listByOrderId(1L, 20L, "SELLER");
        List<OrderEvidence> adminResult = service.listByOrderId(1L, 99L, "ADMIN");

        assertThat(sellerResult).hasSize(1);
        assertThat(adminResult).hasSize(1);
    }

    @Test
    void unrelatedUserCannotListEvidenceForAnotherOrder() {
        OrderEvidenceMapper evidenceMapper = mock(OrderEvidenceMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        when(orderMapper.selectById(1L)).thenReturn(order());
        OrderEvidenceService service = new OrderEvidenceService(evidenceMapper, orderMapper);

        assertThatThrownBy(() -> service.listByOrderId(1L, 30L, "USER"))
                .hasMessage("No permission");
        verify(evidenceMapper, never()).selectList(any());
    }

    @Test
    void missingOrderCannotBeUsedForEvidence() {
        OrderEvidenceMapper evidenceMapper = mock(OrderEvidenceMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderEvidenceService service = new OrderEvidenceService(evidenceMapper, orderMapper);
        OrderEvidence evidence = new OrderEvidence();
        evidence.setOrderId(404L);

        assertThatThrownBy(() -> service.addEvidence(evidence, 10L, "USER"))
                .hasMessage("Order does not exist");
    }

    private static Order order() {
        Order order = new Order();
        order.setId(1L);
        order.setBuyerId(10L);
        order.setSellerId(20L);
        order.setDeleted(0);
        return order;
    }
}
