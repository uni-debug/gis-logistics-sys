package com.gis.logistics.domain.staffapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffApplicationRepository extends JpaRepository<StaffApplication, Long> {
    Optional<StaffApplication> findByUserId(Long userId);
    List<StaffApplication> findByStatus(StaffApplication.Status status);
}
