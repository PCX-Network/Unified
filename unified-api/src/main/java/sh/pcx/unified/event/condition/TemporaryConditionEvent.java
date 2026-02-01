/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.TemporaryCondition;
import sh.pcx.unified.event.UnifiedEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Base event for temporary condition lifecycle events.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see TemporaryConditionAppliedEvent
 * @see TemporaryConditionExpiredEvent
 * @see TemporaryConditionCancelledEvent
 * @see TemporaryConditionExtendedEvent
 */
public abstract class TemporaryConditionEvent extends UnifiedEvent {

    private final TemporaryCondition condition;

    /**
     * Constructs a new temporary condition event.
     *
     * @param condition the temporary condition involved
     */
    protected TemporaryConditionEvent(@NotNull TemporaryCondition condition) {
        super();
        this.condition = Objects.requireNonNull(condition, "condition cannot be null");
    }

    /**
     * Returns the temporary condition involved in this event.
     *
     * @return the temporary condition
     * @since 1.0.0
     */
    @NotNull
    public TemporaryCondition getTemporaryCondition() {
        return condition;
    }

    /**
     * Returns the name of the condition.
     *
     * @return the condition name
     * @since 1.0.0
     */
    @NotNull
    public String getConditionName() {
        return condition.getName();
    }

    /**
     * Returns the player UUID.
     *
     * @return the player UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return condition.getPlayerId();
    }
}
