package com.gis.logistics.domain.staffapp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

/**
 * 员工入职申请：用户端发起，管理端审批。
 * 一人一条有效申请（user_id 唯一）。
 */
@Entity
@Table(name = "staff_applications")
@Getter
@Setter
public class StaffApplication {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "license_no", nullable = false, length = 64)
    private String licenseNo;

    @Column(name = "delivery_area")
    private Geometry deliveryArea;

    @Column(name = "reason", length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
