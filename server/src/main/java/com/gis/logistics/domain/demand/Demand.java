package com.gis.logistics.domain.demand;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "demands")
@Getter
@Setter
public class Demand {

    public enum Status { PENDING, QUOTED, ACCEPTED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "weight_g", nullable = false)
    private Integer weightG = 0;

    @Column(name = "volume_cm3", nullable = false)
    private Integer volumeCm3 = 0;

    @Column(name = "fragile", nullable = false)
    private Boolean fragile = false;

    @Column(name = "origin_region", nullable = false, length = 64)
    private String originRegion;

    @Column(name = "origin_addr", nullable = false, length = 256)
    private String originAddr;

    @Column(name = "target_region", nullable = false, length = 64)
    private String targetRegion;

    @Column(name = "target_addr", nullable = false, length = 256)
    private String targetAddr;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "quoted_price", nullable = false)
    private Integer quotedPrice = 0;

    @Column(name = "quoted_by")
    private Long quotedBy;

    @Column(name = "quoted_at")
    private LocalDateTime quotedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
