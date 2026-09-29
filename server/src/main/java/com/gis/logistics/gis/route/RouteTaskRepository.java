package com.gis.logistics.gis.route;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteTaskRepository extends JpaRepository<RouteTask, Long> {
    List<RouteTask> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<RouteTask> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    List<RouteTask> findTop10ByOrderByDistanceMDesc();

}
