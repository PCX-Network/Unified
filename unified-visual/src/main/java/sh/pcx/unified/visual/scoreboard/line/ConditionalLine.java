/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.line;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * A conditional scoreboard line that is only shown when a condition is met.
 *
 * <p>Conditional lines wrap another line and add visibility logic based
 * on a predicate that evaluates per-player.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class ConditionalLine implements ScoreboardLine {

    private final Predicate<UnifiedPlayer> condition;
    private final ScoreboardLine delegate;

    private ConditionalLine(@NotNull Predicate<UnifiedPlayer> condition, @NotNull ScoreboardLine delegate) {
        this.condition = condition;
        this.delegate = delegate;
    }

    /**
     * Creates a conditional line with the given condition and delegate.
     *
     * @param condition the condition to check
     * @param delegate  the line to show when condition is true
     * @return a new conditional line
     * @since 1.0.0
     */
    @NotNull
    public static ConditionalLine of(@NotNull Predicate<UnifiedPlayer> condition, @NotNull ScoreboardLine delegate) {
        return new ConditionalLine(condition, delegate);
    }

    @Override
    public @NotNull Component render(@NotNull UnifiedPlayer player) {
        return delegate.render(player);
    }

    @Override
    public boolean isDynamic() {
        return true; // Conditional lines are always considered dynamic
    }

    @Override
    public boolean isConditional() {
        return true;
    }

    @Override
    public boolean isVisibleTo(@NotNull UnifiedPlayer player) {
        return condition.test(player);
    }

    /**
     * Returns the condition for this line.
     *
     * @return the condition predicate
     * @since 1.0.0
     */
    @NotNull
    public Predicate<UnifiedPlayer> getCondition() {
        return condition;
    }

    /**
     * Returns the delegate line.
     *
     * @return the delegate line
     * @since 1.0.0
     */
    @NotNull
    public ScoreboardLine getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "ConditionalLine{delegate=" + delegate + '}';
    }
}
