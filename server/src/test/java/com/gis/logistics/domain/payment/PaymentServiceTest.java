package com.gis.logistics.domain.payment;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentLogRepository paymentLogRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentGateway gateway;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, paymentLogRepository, orderRepository, gateway);
    }

    @Test
    void initiateCreatesPendingPayment() {
        Order o = order(OrderStatus.PENDING, 10L, 1000);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(gateway.newTxnNo()).thenReturn("TXN-1");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment p = service.initiate(1L, 10L, Payment.Channel.ALIPAY);

        assertEquals(1000, p.getAmount());
        assertEquals(Payment.Status.PENDING, p.getStatus());
        assertEquals("TXN-1", p.getTxnNo());
    }

    @Test
    void initiateRejectsForeignUser() {
        Order o = order(OrderStatus.PENDING, 99L, 1000);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        BizException ex = assertThrows(BizException.class, () -> service.initiate(1L, 10L, Payment.Channel.ALIPAY));
        assertEquals(ErrorCode.ORDER_NOT_OWNED, ex.getCode());
    }

    @Test
    void initiateRejectsNonPendingOrder() {
        Order o = order(OrderStatus.PAID, 10L, 1000);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        BizException ex = assertThrows(BizException.class, () -> service.initiate(1L, 10L, Payment.Channel.ALIPAY));
        assertEquals(ErrorCode.ORDER_INVALID_TRANSITION, ex.getCode());
    }

    @Test
    void callbackPaysOrder() {
        Payment p = payment(5L, Payment.Status.PENDING, 1000);
        Order o = order(OrderStatus.PENDING, 10L, 1000);
        when(paymentRepository.findByOrderIdAndTxnNo(1L, "TXN-1")).thenReturn(Optional.of(p));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment after = service.handleCallback("TXN-1", 1L, 1000, true);

        assertEquals(Payment.Status.PAID, after.getStatus());
        assertNotNull(after.getPaidAt());
        assertEquals(OrderStatus.PAID, o.getStatus());
        verify(paymentLogRepository).save(any(PaymentLog.class));
    }

    @Test
    void callbackRejectsAmountMismatch() {
        Payment p = payment(5L, Payment.Status.PENDING, 1000);
        when(paymentRepository.findByOrderIdAndTxnNo(1L, "TXN-1")).thenReturn(Optional.of(p));

        BizException ex = assertThrows(BizException.class, () -> service.handleCallback("TXN-1", 1L, 999, true));
        assertEquals(ErrorCode.PAYMENT_AMOUNT_MISMATCH, ex.getCode());
    }

    @Test
    void callbackRejectsBadSignature() {
        Payment p = payment(5L, Payment.Status.PENDING, 1000);
        when(paymentRepository.findByOrderIdAndTxnNo(1L, "TXN-1")).thenReturn(Optional.of(p));

        BizException ex = assertThrows(BizException.class, () -> service.handleCallback("TXN-1", 1L, 1000, false));
        assertEquals(ErrorCode.PAYMENT_SIGNATURE_INVALID, ex.getCode());
    }

    @Test
    void callbackIdempotentWhenAlreadyPaid() {
        Payment p = payment(5L, Payment.Status.PAID, 1000);
        when(paymentRepository.findByOrderIdAndTxnNo(1L, "TXN-1")).thenReturn(Optional.of(p));

        Payment after = service.handleCallback("TXN-1", 1L, 1000, true);
        assertEquals(Payment.Status.PAID, after.getStatus());
        verify(paymentLogRepository, never()).save(any(PaymentLog.class));
    }

    private Order order(OrderStatus status, Long userId, int amount) {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(status);
        o.setUserId(userId);
        o.setAmount(amount);
        o.setStaffId(20L);
        o.setDemandId(30L);
        o.setVersion(0);
        return o;
    }

    private Payment payment(Long id, Payment.Status status, int amount) {
        Payment p = new Payment();
        p.setId(id);
        p.setOrderId(1L);
        p.setStatus(status);
        p.setAmount(amount);
        p.setTxnNo("TXN-1");
        return p;
    }
}