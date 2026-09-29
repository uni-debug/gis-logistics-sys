package com.gis.logistics.domain.staff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

/**
 * 员工档案：绑定用户、站点、配送区域、执业资质。
 */
@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "license_no", nullable = false, unique = true, length = 64)
    private String licenseNo;

    @Column(name = "delivery_area")
    private Geometry deliveryArea;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    public StaffProfile() {
    }

    public StaffProfile(Long userId, Long siteId, String licenseNo) {
        this.userId = userId;
        this.siteId = siteId;
        this.licenseNo = licenseNo;
        this.status = 1;
    }
}

