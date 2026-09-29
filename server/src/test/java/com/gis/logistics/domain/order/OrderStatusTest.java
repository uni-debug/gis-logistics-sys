package com.gis.logistics.domain.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void happyPathTransitions() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.PICKED));
        assertTrue(OrderStatus.PICKED.canTransitionTo(OrderStatus.IN_TRANSIT));
        assertTrue(OrderStatus.IN_TRANSIT.canTransitionTo(OrderStatus.ARRIVED));
        assertTrue(OrderStatus.ARRIVED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void cancelAllowedFromNonTerminal() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PICKED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void terminalStatesHaveNoOutgoing() {
        for (OrderStatus s : new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED}) {
            assertTrue(s.isTerminal());
            for (OrderStatus next : OrderStatus.values()) {
                assertFalse(s.canTransitionTo(next), s + " should be terminal");
            }
        }
    }

    @Test
    void refundOnlyFromPaid() {
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.REFUNDING));
        assertTrue(OrderStatus.REFUNDING.canTransitionTo(OrderStatus.REFUNDED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.REFUNDING));
    }

    @Test
    void invalidTransitionRejected() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PAID));
    }
}
