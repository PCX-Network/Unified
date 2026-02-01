/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.TemporaryCondition;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

/**
 * Event fired when a temporary condition is applied to a player.
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onTemporaryApplied(TemporaryConditionAppliedEvent event) {
 *     TemporaryCondition condition = event.getTemporaryCondition();
 *     Duration duration = event.getDuration();
 *
 *     logger.info("Applied temporary condition '" + condition.getName() +
 *                 "' for " + duration.toMinutes() + " minutes");
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see TemporaryConditionEvent
 */
public class TemporaryConditionAppliedEvent extends TemporaryConditionEvent {

    /**
     * Constructs a new applied event.
     *
     * @param condition the temporary condition that was applied
     */
    public TemporaryConditionAppliedEvent(@NotNull TemporaryCondition condition) {
        super(condition);
    }

    /**
     * Returns the duration of the temporary condition.
     *
     * @return the duration
     * @since 1.0.0
     */
    @NotNull
    public Duration getDuration() {
        return getTemporaryCondition().getDuration();
    }

    /**
     * Returns when the condition will expire.
     *
     * @return the expiration time
     * @since 1.0.0
     */
    @NotNull
    public Instant getExpiresAt() {
        return getTemporaryCondition().getExpiresAt();
    }

    @Override
    public String toString() {
        return "TemporaryConditionAppliedEvent[player=" + getPlayerId() +
                ", name=" + getConditionName() +
                ", duration=" + getDuration() + "]";
    }
}
