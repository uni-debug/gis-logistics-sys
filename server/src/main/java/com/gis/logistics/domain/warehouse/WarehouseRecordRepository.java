package com.gis.logistics.domain.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRecordRepository extends JpaRepository<WarehouseRecord, Long> {
    List<WarehouseRecord> findBySiteIdOrderByOccurredAtDesc(Long siteId);
    long countBySiteIdAndAction(Long siteId, WarehouseRecord.Action action);
}
