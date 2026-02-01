/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a time-limited condition applied to a player.
 *
 * <p>Temporary conditions are used to apply time-limited effects or states
 * to players. They automatically expire after the specified duration and
 * can trigger callbacks on application, expiration, or cancellation.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Apply a temporary condition
 * TemporaryCondition doubleXp = conditionService.applyTemporary(player, "double_xp")
 *     .duration(Duration.ofHours(1))
 *     .onApply(p -> {
 *         p.sendMessage(Component.text("Double XP activated for 1 hour!"));
 *         applyDoubleXpModifier(p);
 *     })
 *     .onExpire(p -> {
 *         p.sendMessage(Component.text("Double XP has expired!"));
 *         removeDoubleXpModifier(p);
 *     })
 *     .apply();
 *
 * // Check remaining time
 * Duration remaining = doubleXp.getRemaining();
 * System.out.println("Time left: " + remaining.toMinutes() + " minutes");
 *
 * // Extend the duration
 * doubleXp.extend(Duration.ofMinutes(30));
 *
 * // Cancel early
 * if (someCondition) {
 *     doubleXp.cancel();
 * }
 * }</pre>
 *
 * <h2>Persistence:</h2>
 * <p>By default, temporary conditions are not persisted across server restarts.
 * Use {@link Builder#persistent(boolean)} to enable persistence.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionService
 */
public interface TemporaryCondition {

    /**
     * Returns the unique name of this temporary condition.
     *
     * @return the condition name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the UUID of the player this condition applies to.
     *
     * @return the player UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getPlayerId();

    /**
     * Returns when this condition was applied.
     *
     * @return the application timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getAppliedAt();

    /**
     * Returns when this condition expires.
     *
     * @return the expiration timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getExpiresAt();

    /**
     * Returns the original duration of this condition.
     *
     * @return the original duration
     * @since 1.0.0
     */
    @NotNull
    Duration getDuration();

    /**
     * Returns the remaining duration until expiration.
     *
     * @return the remaining duration, or {@link Duration#ZERO} if expired
     * @since 1.0.0
     */
    @NotNull
    Duration getRemaining();

    /**
     * Returns the elapsed time since application.
     *
     * @return the elapsed duration
     * @since 1.0.0
     */
    @NotNull
    Duration getElapsed();

    /**
     * Returns the progress as a value from 0.0 (just started) to 1.0 (expired).
     *
     * @return the progress value
     * @since 1.0.0
     */
    double getProgress();

    /**
     * Checks if this condition is still active (not expired or cancelled).
     *
     * @return true if active
     * @since 1.0.0
     */
    boolean isActive();

    /**
     * Checks if this condition has expired.
     *
     * @return true if expired
     * @since 1.0.0
     */
    boolean isExpired();

    /**
     * Checks if this condition was cancelled.
     *
     * @return true if cancelled
     * @since 1.0.0
     */
    boolean isCancelled();

    /**
     * Checks if this condition is persistent across restarts.
     *
     * @return true if persistent
     * @since 1.0.0
     */
    boolean isPersistent();

    /**
     * Returns metadata associated with this condition.
     *
     * @param key the metadata key
     * @return the metadata value, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<Object> getMetadata(@NotNull String key);

    /**
     * Returns typed metadata associated with this condition.
     *
     * @param <T>  the expected type
     * @param key  the metadata key
     * @param type the expected class
     * @return the typed value, or empty if not found or wrong type
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getMetadata(@NotNull String key, @NotNull Class<T> type);

    /**
     * Extends the duration of this condition.
     *
     * @param extension the duration to add
     * @return the new expiration time
     * @throws IllegalStateException if the condition is not active
     * @since 1.0.0
     */
    @NotNull
    Instant extend(@NotNull Duration extension);

    /**
     * Sets the expiration time directly.
     *
     * @param expiresAt the new expiration time
     * @throws IllegalStateException if the condition is not active
     * @throws IllegalArgumentException if the time is in the past
     * @since 1.0.0
     */
    void setExpiresAt(@NotNull Instant expiresAt);

    /**
     * Cancels this condition before it expires.
     *
     * <p>This will trigger the onCancel callback if set, but not the onExpire callback.</p>
     *
     * @throws IllegalStateException if already cancelled or expired
     * @since 1.0.0
     */
    void cancel();

    /**
     * Cancels this condition silently without triggering callbacks.
     *
     * @throws IllegalStateException if already cancelled or expired
     * @since 1.0.0
     */
    void cancelSilently();

    /**
     * Builder for creating temporary conditions.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the duration of the temporary condition.
         *
         * @param duration the duration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder duration(@NotNull Duration duration);

        /**
         * Sets the action to execute when the condition is applied.
         *
         * @param action the on-apply action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder onApply(@Nullable Consumer<UnifiedPlayer> action);

        /**
         * Sets the action to execute when the condition expires.
         *
         * @param action the on-expire action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder onExpire(@Nullable Consumer<UnifiedPlayer> action);

        /**
         * Sets the action to execute when the condition is cancelled.
         *
         * @param action the on-cancel action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder onCancel(@Nullable Consumer<UnifiedPlayer> action);

        /**
         * Sets whether the condition should persist across restarts.
         *
         * @param persistent true for persistence
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder persistent(boolean persistent);

        /**
         * Adds metadata to the condition.
         *
         * @param key   the metadata key
         * @param value the metadata value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder metadata(@NotNull String key, @NotNull Object value);

        /**
         * Sets whether to replace an existing condition with the same name.
         *
         * @param replace true to replace
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder replaceExisting(boolean replace);

        /**
         * Sets whether to extend an existing condition with the same name.
         *
         * <p>If true and a condition exists, adds the duration to it instead
         * of creating a new one.</p>
         *
         * @param extend true to extend existing
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder extendExisting(boolean extend);

        /**
         * Applies the temporary condition.
         *
         * @return the created temporary condition
         * @throws IllegalStateException if duration is not set
         * @throws IllegalArgumentException if a condition with this name already exists
         *         and neither replace nor extend is set
         * @since 1.0.0
         */
        @NotNull
        TemporaryCondition apply();

        /**
         * Applies the temporary condition if no condition with the same name exists.
         *
         * @return the temporary condition, or the existing one if present
         * @since 1.0.0
         */
        @NotNull
        TemporaryCondition applyIfAbsent();
    }
}
