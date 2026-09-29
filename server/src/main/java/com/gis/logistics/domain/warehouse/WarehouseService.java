package com.gis.logistics.domain.warehouse;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓储管理：站点分布、库存（IN-OUT 净量）、出入库记录、容量校验。
 */
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final SiteRepository siteRepository;
    private final WarehouseRecordRepository recordRepository;

    @Transactional
    public Site addSite(String code, String name, int capacity) {
        if (siteRepository.findByCode(code).isPresent()) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "site code exists: " + code);
        }
        return siteRepository.save(new Site(code, name, capacity));
    }

    @Transactional
    public WarehouseRecord inbound(Long siteId, Long orderId, int quantity) {
        return record(siteId, orderId, WarehouseRecord.Action.IN, quantity);
    }

    @Transactional
    public WarehouseRecord outbound(Long siteId, Long orderId, int quantity) {
        if (quantity <= 0) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "quantity must be positive");
        }
        if (quantity > stockOf(siteId)) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "insufficient stock at site " + siteId);
        }
        return record(siteId, orderId, WarehouseRecord.Action.OUT, quantity);
    }

    private WarehouseRecord record(Long siteId, Long orderId, WarehouseRecord.Action action, int quantity) {
        if (siteRepository.findById(siteId).isEmpty()) {
            throw new BizException(ErrorCode.GENERIC_NOT_FOUND, "site not found: " + siteId);
        }
        return recordRepository.save(new WarehouseRecord(siteId, orderId, action, quantity));
    }

    /** 手动出入库（无订单关联）：orderId 传 null */
    @Transactional
    public WarehouseRecord manualInbound(Long siteId, int quantity) { return record(siteId, null, WarehouseRecord.Action.IN, quantity); }

    @Transactional
    public WarehouseRecord manualOutbound(Long siteId, int quantity) {
        if (quantity > stockOf(siteId)) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "insufficient stock at site " + siteId);
        }
        return record(siteId, null, WarehouseRecord.Action.OUT, quantity);
    }

    public long stockOf(Long siteId) {
        long in = recordRepository.countBySiteIdAndAction(siteId, WarehouseRecord.Action.IN);
        long out = recordRepository.countBySiteIdAndAction(siteId, WarehouseRecord.Action.OUT);
        return in - out;
    }

    public List<Site> activeSites() {
        return siteRepository.findByStatus(1);
    }

    public List<WarehouseRecord> recordsOf(Long siteId) {
        return recordRepository.findBySiteIdOrderByOccurredAtDesc(siteId);
    }
}
