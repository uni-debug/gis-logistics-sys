package com.gis.logistics.domain.logistics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogisticsEventRepository extends JpaRepository<LogisticsEvent, Long> {
    List<LogisticsEvent> findByOrderIdOrderByOccurredAtAsc(Long orderId);
    Page<LogisticsEvent> findByOrderId(Long orderId, Pageable pageable);
}
