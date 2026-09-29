package com.gis.logistics.domain.payment;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway gateway;

    @Transactional
    public Payment initiate(Long orderId, Long userId, Payment.Channel channel) {
        Order order = ownedOrder(orderId, userId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BizException(ErrorCode.ORDER_INVALID_TRANSITION,
                    "only PENDING order can be paid, current=" + order.getStatus());
        }
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setChannel(channel);
        payment.setTxnNo(gateway.newTxnNo());
        payment.setAmount(order.getAmount());
        payment.setStatus(Payment.Status.PENDING);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment handleCallback(String txnNo, Long orderId, int amountFen, boolean signedOk) {
        Payment payment = paymentRepository.findByOrderIdAndTxnNo(orderId, txnNo).orElseThrow(
                () -> new BizException(ErrorCode.PAYMENT_NOT_FOUND, "txn not found: " + txnNo));
        if (!signedOk) {
            throw new BizException(ErrorCode.PAYMENT_SIGNATURE_INVALID, "callback signature invalid");
        }
        if (payment.getAmount() != amountFen) {
            throw new BizException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "amount mismatch expected=" + payment.getAmount() + " got=" + amountFen);
        }
        if (payment.getStatus() == Payment.Status.PAID) {
            return payment;
        }
        payment.setStatus(Payment.Status.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        paymentLogRepository.save(new PaymentLog(payment.getId(), "PENDING", "PAID",
                "{\"txn\":\"" + txnNo + "\"}"));
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND, "order missing"));
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        return payment;
    }

    private Order ownedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND, "order not found: " + orderId));
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ErrorCode.ORDER_NOT_OWNED, "not your order");
        }
        return order;
    }
}