package com.gis.logistics.domain.demand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {

    Page<Demand> findByUserIdAndStatus(Long userId, Demand.Status status, Pageable pageable);

    Page<Demand> findByStatus(Demand.Status status, Pageable pageable);

    Optional<Demand> findByIdAndUserId(Long id, Long userId);
}
