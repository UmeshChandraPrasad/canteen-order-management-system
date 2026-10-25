package com.canteen.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle. The legal transitions form a small state machine;
 * OrderService rejects anything not listed here (kitchen screen shows a
 * friendly error instead of corrupting the order).
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(PLACED,          EnumSet.of(CONFIRMED, CANCELLED));
        ALLOWED.put(CONFIRMED,       EnumSet.of(PREPARING, CANCELLED));
        ALLOWED.put(PREPARING,       EnumSet.of(READY_FOR_PICKUP));
        ALLOWED.put(READY_FOR_PICKUP, EnumSet.of(COMPLETED));
        ALLOWED.put(COMPLETED,       EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CANCELLED,       EnumSet.noneOf(OrderStatus.class));
    }

    /** True when this status may move directly to {@code next}. */
    public boolean canTransitionTo(OrderStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }

    /** All statuses this status may move to (for the kitchen dropdown). */
    public Set<OrderStatus> allowedNext() {
        return EnumSet.copyOf(ALLOWED.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)));
    }

    /** "READY_FOR_PICKUP" -> "READY FOR PICKUP" for display. */
    public String label() {
        return name().replace('_', ' ');
    }
}
