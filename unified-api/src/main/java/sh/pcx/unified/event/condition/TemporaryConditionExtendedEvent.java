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
 * Event fired when a temporary condition's duration is extended.
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onTemporaryExtended(TemporaryConditionExtendedEvent event) {
 *     Duration extension = event.getExtension();
 *     Instant newExpiry = event.getNewExpiresAt();
 *
 *     logger.info("Temporary condition '" + event.getConditionName() +
 *                 "' extended by " + extension.toMinutes() + " minutes");
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see TemporaryConditionEvent
 */
public class TemporaryConditionExtendedEvent extends TemporaryConditionEvent {

    private final Duration extension;
    private final Instant previousExpiresAt;
    private final Instant newExpiresAt;

    /**
     * Constructs a new extended event.
     *
     * @param condition         the temporary condition that was extended
     * @param extension         the extension duration
     * @param previousExpiresAt the previous expiration time
     * @param newExpiresAt      the new expiration time
     */
    public TemporaryConditionExtendedEvent(
            @NotNull TemporaryCondition condition,
            @NotNull Duration extension,
            @NotNull Instant previousExpiresAt,
            @NotNull Instant newExpiresAt
    ) {
        super(condition);
        this.extension = extension;
        this.previousExpiresAt = previousExpiresAt;
        this.newExpiresAt = newExpiresAt;
    }

    /**
     * Returns the extension duration.
     *
     * @return the extension
     * @since 1.0.0
     */
    @NotNull
    public Duration getExtension() {
        return extension;
    }

    /**
     * Returns the previous expiration time.
     *
     * @return the previous expiration time
     * @since 1.0.0
     */
    @NotNull
    public Instant getPreviousExpiresAt() {
        return previousExpiresAt;
    }

    /**
     * Returns the new expiration time.
     *
     * @return the new expiration time
     * @since 1.0.0
     */
    @NotNull
    public Instant getNewExpiresAt() {
        return newExpiresAt;
    }

    @Override
    public String toString() {
        return "TemporaryConditionExtendedEvent[player=" + getPlayerId() +
                ", name=" + getConditionName() +
                ", extension=" + extension +
                ", newExpiry=" + newExpiresAt + "]";
    }
}
