package com.gis.logistics.domain.warehouse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(name = "sites")
@Getter
@Setter
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "point")
    private Geometry point;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    public Site() {
    }

    public Site(String code, String name, Integer capacity) {
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.status = 1;
    }
}
