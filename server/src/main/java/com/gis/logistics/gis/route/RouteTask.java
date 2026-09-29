package com.gis.logistics.gis.route;

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
 * 路线规划结果落库。geometry 存 OSRM 返回的 LineString（SRID 4326）。
 */
@Entity
@Table(name = "route_tasks")
@Getter
@Setter
public class RouteTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 16)
    private RouteStrategy strategy;

    @Column(name = "from_point", nullable = false)
    private Geometry fromPoint;

    @Column(name = "to_point", nullable = false)
    private Geometry toPoint;

    @Column(name = "via_points")
    private Geometry viaPoints;

    @Column(name = "avoid_segments", length = 512)
    private String avoidSegments;

    @Column(name = "geometry")
    private Geometry geometry;

    @Column(name = "distance_m")
    private Double distanceM;

    @Column(name = "eta_s")
    private Integer etaS;

    @Column(name = "cost")
    private Integer cost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RouteTask() {
    }

    public RouteTask(Long orderId, RouteStrategy strategy, Geometry geometry,
                     double distanceM, double etaS, int cost) {
        this.orderId = orderId;
        this.strategy = strategy;
        this.geometry = geometry;
        this.distanceM = distanceM;
        this.etaS = (int) Math.round(etaS);
        this.cost = cost;
        this.createdAt = LocalDateTime.now();
    }
}