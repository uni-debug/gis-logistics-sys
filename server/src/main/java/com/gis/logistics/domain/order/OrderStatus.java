package com.gis.logistics.domain.order;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：PENDING -> PAID -> PICKED -> IN_TRANSIT -> ARRIVED -> DELIVERED
 * 分支：CANCELLED（任意非终态可取消）、REFUNDING -> REFUNDED（仅 PAID 后可发起）
 */
public enum OrderStatus {
    PENDING, PAID, PICKED, IN_TRANSIT, ARRIVED, DELIVERED, CANCELLED, REFUNDING, REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS;

    static {
        Map<OrderStatus, Set<OrderStatus>> m = new HashMap<>();
        m.put(PENDING, Set.of(PAID, CANCELLED));
        m.put(PAID, Set.of(PICKED, CANCELLED, REFUNDING));
        m.put(PICKED, Set.of(IN_TRANSIT, CANCELLED));
        m.put(IN_TRANSIT, Set.of(ARRIVED));
        m.put(ARRIVED, Set.of(DELIVERED));
        m.put(REFUNDING, Set.of(REFUNDED, PENDING));
        m.put(DELIVERED, Set.of());
        m.put(CANCELLED, Set.of());
        m.put(REFUNDED, Set.of());
        TRANSITIONS = Collections.unmodifiableMap(m);
    }

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }
}
