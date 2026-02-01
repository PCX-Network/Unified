/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.Condition;
import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Base class for all condition-related events.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionChangeEvent
 * @see ConditionalGroupEnterEvent
 * @see ConditionalGroupExitEvent
 * @see TemporaryConditionEvent
 */
public abstract class ConditionEvent extends UnifiedEvent {

    private final UnifiedPlayer player;
    private final Condition condition;

    /**
     * Constructs a new condition event.
     *
     * @param player    the player involved
     * @param condition the condition involved
     */
    protected ConditionEvent(@NotNull UnifiedPlayer player, @NotNull Condition condition) {
        super();
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.condition = Objects.requireNonNull(condition, "condition cannot be null");
    }

    /**
     * Returns the player involved in this event.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the condition involved in this event.
     *
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    public Condition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return getEventName() + "[player=" + player.getName() +
                ", condition=" + condition.getName() + "]";
    }
}
