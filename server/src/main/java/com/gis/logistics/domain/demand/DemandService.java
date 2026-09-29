package com.gis.logistics.domain.demand;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 需求单服务：发布（用户）、报价（员工）、确认（用户）→ 生成订单。
 * 报价金额与确认建单属支付/一致性逻辑，需人工复核。
 */
@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Demand publish(Long userId, PublishCommand cmd) {
        if (cmd.weightG() <= 0 || cmd.volumeCm3() <= 0) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "weight/volume must be positive");
        }
        if (cmd.title() == null || cmd.title().isBlank()) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "title required");
        }
        Demand d = new Demand();
        d.setUserId(userId);
        d.setTitle(cmd.title().trim());
        d.setWeightG(cmd.weightG());
        d.setVolumeCm3(cmd.volumeCm3());
        d.setFragile(cmd.fragile());
        d.setOriginRegion(cmd.originRegion());
        d.setOriginAddr(cmd.originAddr());
        d.setTargetRegion(cmd.targetRegion());
        d.setTargetAddr(cmd.targetAddr());
        d.setStatus(Demand.Status.PENDING);
        return demandRepository.save(d);
    }

    @Transactional
    public Demand quote(Long demandId, Long staffId, int priceFen) {
        Demand d = demandRepository.findById(demandId)
                .orElseThrow(() -> new BizException(ErrorCode.DEMAND_NOT_FOUND, "demand not found: " + demandId));
        if (d.getStatus() != Demand.Status.PENDING) {
            throw new BizException(ErrorCode.DEMAND_ALREADY_QUOTED, "demand already quoted/accepted");
        }
        if (priceFen <= 0) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "price must be positive fen");
        }
        d.setStatus(Demand.Status.QUOTED);
        d.setQuotedBy(staffId);
        d.setQuotedPrice(priceFen);
        d.setQuotedAt(LocalDateTime.now());
        return demandRepository.save(d);
    }

    /**
     * 用户确认报价 → 需求 ACCEPTED + 生成正式订单（amount 回填报价、status PENDING）。
     * 接单员工取 demand.quotedBy（报价员工即接单员工），避免越权。
     */
    @Transactional
    public ConfirmResult confirm(Long demandId, Long userId) {
        Demand d = demandRepository.findByIdAndUserId(demandId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.DEMAND_NOT_FOUND, "demand not found"));
        if (d.getStatus() != Demand.Status.QUOTED) {
            throw new BizException(ErrorCode.DEMAND_ALREADY_QUOTED, "demand not in quoted state");
        }
        if (d.getQuotedBy() == null) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "demand has no quoting staff");
        }
        d.setStatus(Demand.Status.ACCEPTED);
        demandRepository.save(d);

        Order order = new Order();
        order.setDemandId(d.getId());
        order.setStaffId(d.getQuotedBy());
        order.setUserId(userId);
        order.setAmount(d.getQuotedPrice());
        order.setStatus(OrderStatus.PENDING);
        order.setVersion(0);
        order = orderRepository.save(order);
        return new ConfirmResult(d, order);
    }

    public record ConfirmResult(Demand demand, Order order) {}

    public Page<Demand> pageMine(Long userId, Demand.Status status, Pageable pageable) {
        if (status == null) {
            // 默认列出“需用户处理”的需求：待报价 + 已报价待确认
            java.util.List<Demand> merged = new java.util.ArrayList<>(
                    demandRepository.findByUserIdAndStatus(userId, Demand.Status.PENDING, pageable).getContent());
            merged.addAll(demandRepository.findByUserIdAndStatus(userId, Demand.Status.QUOTED, pageable).getContent());
            int from = (int) Math.max(0, pageable.getOffset());
            int to = Math.min(from + pageable.getPageSize(), merged.size());
            java.util.List<Demand> slice = from >= merged.size() ? java.util.List.of() : merged.subList(from, to);
            return new org.springframework.data.domain.PageImpl<>(new java.util.ArrayList<>(slice), pageable, merged.size());
        }
        return demandRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    public Page<Demand> pageByStatus(Demand.Status status, Pageable pageable) {
        return demandRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Demand close(Long demandId, String reason) {
        Demand d = demandRepository.findById(demandId)
                .orElseThrow(() -> new BizException(ErrorCode.DEMAND_NOT_FOUND, "demand not found: " + demandId));
        d.setStatus(Demand.Status.CLOSED);
        return demandRepository.save(d);
    }

    public Page<Demand> auditPending(Pageable pageable) {
        return demandRepository.findByStatus(Demand.Status.PENDING, pageable);
    }

    public record PublishCommand(
            String title, int weightG, int volumeCm3, boolean fragile,
            String originRegion, String originAddr,
            String targetRegion, String targetAddr) {}
}
