package com.gis.logistics.domain.order;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单状态机单一入口（物流数据一致性关键路径，需人工复核）。
 * 所有状态迁移经此校验合法性 + 乐观锁 + 时间戳落位。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order transition(Long orderId, OrderStatus target, Long operatorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND, "order not found: " + orderId));
        assertOwnership(order, operatorId);
        if (!order.getStatus().canTransitionTo(target)) {
            throw new BizException(ErrorCode.ORDER_INVALID_TRANSITION,
                    "invalid transition " + order.getStatus() + " -> " + target);
        }
        order.setStatus(target);
        LocalDateTime now = LocalDateTime.now();
        if (target == OrderStatus.PAID && order.getPaidAt() == null) {
            order.setPaidAt(now);
        }
        if (target == OrderStatus.DELIVERED) {
            order.setDeliveredAt(now);
        }
        try {
            return orderRepository.save(order);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BizException(ErrorCode.ORDER_VERSION_CONFLICT, "order version conflict, retry", null);
        }
    }

    /** 归属校验：仅订单责任员工本人或管理员（operatorId=0 哨兵）可迁移状态。 */
    private void assertOwnership(Order order, Long operatorId) {
        if (operatorId != null && operatorId == 0L) {
            return;
        }
        if (operatorId != null && operatorId.equals(order.getStaffId())) {
            return;
        }
        throw new BizException(ErrorCode.ORDER_NOT_OWNED,
                "operator " + operatorId + " does not own order " + order.getId() + " (staff " + order.getStaffId() + ")");
    }

    public Order getById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND, "order not found: " + orderId));
    }
}
