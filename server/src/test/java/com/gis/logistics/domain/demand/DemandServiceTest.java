package com.gis.logistics.domain.demand;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

    @Mock
    private DemandRepository demandRepository;
    @Mock
    private OrderRepository orderRepository;

    private DemandService service;

    @BeforeEach
    void setUp() {
        service = new DemandService(demandRepository, orderRepository);
    }

    @Test
    void publishSucceeds() {
        when(demandRepository.save(any())).thenAnswer(inv -> {
            Demand d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        Demand d = service.publish(10L, new DemandService.PublishCommand(
                "寄件", 500, 1000, false, "110000", "北京朝阳", "310000", "上海浦东"));
        assertEquals(1L, d.getId());
        assertEquals(Demand.Status.PENDING, d.getStatus());
        assertEquals(10L, d.getUserId());
    }

    @Test
    void publishRejectsNonPositiveWeight() {
        assertThrows(BizException.class,
                () -> service.publish(10L, new DemandService.PublishCommand("x", 0, 100, false, "a", "b", "c", "d")));
    }

    @Test
    void quoteMovesToQuotedWithPrice() {
        Demand d = demand(1L, Demand.Status.PENDING);
        when(demandRepository.findById(1L)).thenReturn(Optional.of(d));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Demand after = service.quote(1L, 99L, 8000);

        assertEquals(Demand.Status.QUOTED, after.getStatus());
        assertEquals(99L, after.getQuotedBy());
        assertEquals(8000, after.getQuotedPrice());
    }

    @Test
    void quoteRejectsAlreadyQuoted() {
        Demand d = demand(1L, Demand.Status.QUOTED);
        when(demandRepository.findById(1L)).thenReturn(Optional.of(d));
        assertThrows(BizException.class, () -> service.quote(1L, 99L, 100));
    }

    @Test
    void quoteRejectsNonPositivePrice() {
        Demand d = demand(1L, Demand.Status.PENDING);
        when(demandRepository.findById(1L)).thenReturn(Optional.of(d));
        BizException ex = assertThrows(BizException.class, () -> service.quote(1L, 99L, 0));
        assertEquals(ErrorCode.GENERIC_BAD_REQUEST, ex.getCode());
    }

    @Test
    void confirmCreatesOrderFromQuotedPrice() {
        Demand d = demand(1L, Demand.Status.QUOTED);
        d.setQuotedBy(99L);
        d.setQuotedPrice(8000);
        d.setUserId(10L);
        when(demandRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(d));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(7L);
            return o;
        });

        DemandService.ConfirmResult result = service.confirm(1L, 10L);

        assertEquals(Demand.Status.ACCEPTED, result.demand().getStatus());
        assertEquals(7L, result.order().getId());
        assertEquals(8000, result.order().getAmount());
        assertEquals(OrderStatus.PENDING, result.order().getStatus());
        assertEquals(99L, result.order().getStaffId());
        assertEquals(10L, result.order().getUserId());
        assertEquals(1L, result.order().getDemandId());
    }

    @Test
    void confirmRejectsNotQuoted() {
        Demand d = demand(1L, Demand.Status.PENDING);
        d.setUserId(10L);
        when(demandRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(d));
        assertThrows(BizException.class, () -> service.confirm(1L, 10L));
    }

    @Test
    void confirmRejectsForeignUser() {
        Demand d = demand(1L, Demand.Status.QUOTED);
        d.setUserId(10L);
        d.setQuotedBy(99L);
        d.setQuotedPrice(1000);
        when(demandRepository.findByIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.confirm(1L, 20L));
    }

    @Test
    void closeSetsClosedStatus() {
        Demand d = demand(1L, Demand.Status.PENDING);
        when(demandRepository.findById(1L)).thenReturn(Optional.of(d));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Demand after = service.close(1L, "违规");
        assertEquals(Demand.Status.CLOSED, after.getStatus());
    }

    @Test
    void closeMissingThrows() {
        when(demandRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.close(404L, "x"));
    }

    @Test
    void auditPendingReturnsPage() {
        when(demandRepository.findByStatus(Demand.Status.PENDING, org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        assertEquals(0, service.auditPending(org.springframework.data.domain.PageRequest.of(0, 20)).getTotalElements());
    }

    private Demand demand(Long id, Demand.Status status) {
        Demand d = new Demand();
        d.setId(id);
        d.setStatus(status);
        d.setUserId(10L);
        return d;
    }
}
