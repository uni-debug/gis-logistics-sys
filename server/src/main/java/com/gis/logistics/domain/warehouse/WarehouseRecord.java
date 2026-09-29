package com.gis.logistics.domain.warehouse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_records")
@Getter
@Setter
public class WarehouseRecord {

    public enum Action { IN, OUT, HELD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 16)
    private Action action;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public WarehouseRecord() {
    }

    public WarehouseRecord(Long siteId, Long orderId, Action action, int quantity) {
        this.siteId = siteId;
        this.orderId = orderId;
        this.action = action;
        this.quantity = quantity;
        this.occurredAt = LocalDateTime.now();
    }
}
