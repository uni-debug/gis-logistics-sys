package com.gis.logistics.domain.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUserId(Long userId);
    List<StaffProfile> findBySiteId(Long siteId);
    List<StaffProfile> findByStatus(Integer status);
}
