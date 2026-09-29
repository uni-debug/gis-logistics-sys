package com.gis.logistics.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_logs")
@Getter
@Setter
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "from_status", nullable = false, length = 16)
    private String fromStatus = "";

    @Column(name = "to_status", nullable = false, length = 16)
    private String toStatus = "";

    @Column(name = "payload")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public PaymentLog() {
    }

    public PaymentLog(Long paymentId, String fromStatus, String toStatus, String payload) {
        this.paymentId = paymentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.payload = payload;
        this.occurredAt = LocalDateTime.now();
    }
}