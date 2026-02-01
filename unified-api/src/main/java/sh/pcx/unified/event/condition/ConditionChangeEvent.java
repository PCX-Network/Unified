/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.Condition;
import sh.pcx.unified.condition.ConditionResult;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Event fired when a watched condition changes state for a player.
 *
 * <p>This event is only fired for conditions that are being watched
 * via {@link sh.pcx.unified.condition.ConditionService#watch}.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onConditionChange(ConditionChangeEvent event) {
 *     Player player = event.getPlayer();
 *     Condition condition = event.getCondition();
 *     boolean nowMet = event.isNowMet();
 *
 *     if (condition.getName().equals("vip_status")) {
 *         if (nowMet) {
 *             applyVipPerks(player);
 *         } else {
 *             removeVipPerks(player);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionEvent
 */
public class ConditionChangeEvent extends ConditionEvent {

    private final ConditionResult previousResult;
    private final ConditionResult currentResult;

    /**
     * Constructs a new condition change event.
     *
     * @param player         the player whose condition changed
     * @param condition      the condition that changed
     * @param previousResult the previous evaluation result
     * @param currentResult  the current evaluation result
     */
    public ConditionChangeEvent(
            @NotNull UnifiedPlayer player,
            @NotNull Condition condition,
            @NotNull ConditionResult previousResult,
            @NotNull ConditionResult currentResult
    ) {
        super(player, condition);
        this.previousResult = Objects.requireNonNull(previousResult, "previousResult cannot be null");
        this.currentResult = Objects.requireNonNull(currentResult, "currentResult cannot be null");
    }

    /**
     * Returns the previous evaluation result.
     *
     * @return the previous result
     * @since 1.0.0
     */
    @NotNull
    public ConditionResult getPreviousResult() {
        return previousResult;
    }

    /**
     * Returns the current evaluation result.
     *
     * @return the current result
     * @since 1.0.0
     */
    @NotNull
    public ConditionResult getCurrentResult() {
        return currentResult;
    }

    /**
     * Returns whether the condition was previously met.
     *
     * @return true if previously met
     * @since 1.0.0
     */
    public boolean wasPreviouslyMet() {
        return previousResult.passed();
    }

    /**
     * Returns whether the condition is now met.
     *
     * @return true if now met
     * @since 1.0.0
     */
    public boolean isNowMet() {
        return currentResult.passed();
    }

    /**
     * Returns whether the condition became true (was false, now true).
     *
     * @return true if the condition became true
     * @since 1.0.0
     */
    public boolean becameTrue() {
        return !previousResult.passed() && currentResult.passed();
    }

    /**
     * Returns whether the condition became false (was true, now false).
     *
     * @return true if the condition became false
     * @since 1.0.0
     */
    public boolean becameFalse() {
        return previousResult.passed() && !currentResult.passed();
    }

    @Override
    public String toString() {
        return "ConditionChangeEvent[player=" + getPlayer().getName() +
                ", condition=" + getCondition().getName() +
                ", was=" + previousResult.passed() +
                ", now=" + currentResult.passed() + "]";
    }
}
