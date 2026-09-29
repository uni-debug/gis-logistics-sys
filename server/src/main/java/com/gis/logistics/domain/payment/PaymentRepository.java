package com.gis.logistics.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.Optional<Payment> findByOrderId(Long orderId);
    java.util.Optional<Payment> findByOrderIdAndTxnNo(Long orderId, String txnNo);
}