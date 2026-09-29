package com.gis.logistics.domain.logistics;

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
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

/**
 * 物流节点事件（揽件/在途/到达中转/签收），支撑 GIS 轨迹追踪。
 */
@Entity
@Table(name = "logistics_events")
@Getter
@Setter
public class LogisticsEvent {

    public enum EventType { PICKED, IN_TRANSIT, ARRIVED_DELIVERY, DELIVERED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private EventType type;

    @Column(name = "point")
    private Geometry point;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "operator_id")
    private Long operatorId;

    public LogisticsEvent() {
    }

    public LogisticsEvent(Long orderId, EventType type, Geometry point, Long operatorId) {
        this.orderId = orderId;
        this.type = type;
        this.point = point;
        this.occurredAt = LocalDateTime.now();
        this.operatorId = operatorId;
    }
}