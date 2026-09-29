package com.gis.logistics.domain.order;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository);
    }

    @Test
    void paidTransitionSetsPaidAt() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order after = orderService.transition(1L, OrderStatus.PAID, 20L);

        assertEquals(OrderStatus.PAID, after.getStatus());
        assertNotNull(after.getPaidAt());
    }

    @Test
    void deliveredTransitionSetsDeliveredAt() {
        Order o = order(OrderStatus.ARRIVED);
        o.setPaidAt(java.time.LocalDateTime.now().minusHours(1));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order after = orderService.transition(1L, OrderStatus.DELIVERED, 20L);

        assertNotNull(after.getDeliveredAt());
    }

    @Test
    void invalidTransitionRejected() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.transition(1L, OrderStatus.DELIVERED, 20L));
        assertEquals(ErrorCode.ORDER_INVALID_TRANSITION, ex.getCode());
    }

    @Test
    void versionConflictMapped() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Order.class.getName(), 1L));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.transition(1L, OrderStatus.PAID, 20L));
        assertEquals(ErrorCode.ORDER_VERSION_CONFLICT, ex.getCode());
    }

    @Test
    void orderNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class,
                () -> orderService.transition(404L, OrderStatus.PAID, 9L));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getCode());
    }

    @Test
    void transitionAllowedForOwningStaff() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order after = orderService.transition(1L, OrderStatus.PAID, 20L);

        assertEquals(OrderStatus.PAID, after.getStatus());
    }

    @Test
    void transitionAllowedForAdminSentinel() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order after = orderService.transition(1L, OrderStatus.PAID, 0L);

        assertEquals(OrderStatus.PAID, after.getStatus());
    }

    @Test
    void transitionRejectedForOtherStaff() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.transition(1L, OrderStatus.PAID, 99L));
        assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.getCode());
    }

    @Test
    void transitionRejectedForNullOperator() {
        Order o = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.transition(1L, OrderStatus.PAID, null));
        assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.getCode());
    }

    private Order order(OrderStatus status) {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(status);
        o.setUserId(10L);
        o.setStaffId(20L);
        o.setDemandId(30L);
        o.setAmount(1000);
        return o;
    }
}
