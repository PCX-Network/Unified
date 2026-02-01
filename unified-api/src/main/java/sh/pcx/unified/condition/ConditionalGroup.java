/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a conditional group that players can be dynamically assigned to.
 *
 * <p>Conditional groups are similar to permission groups, but membership is
 * determined dynamically by evaluating conditions. When a condition becomes
 * true, the player enters the group; when it becomes false, they exit.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ConditionalGroup nightVip = ConditionalGroup.builder()
 *     .name("night_vip")
 *     .condition(Condition.all(
 *         Condition.permission("group.vip"),
 *         Condition.timeRange(LocalTime.of(20, 0), LocalTime.of(6, 0))
 *     ))
 *     .priority(100)
 *     .onEnter(player -> {
 *         player.sendMessage(Component.text("Night VIP bonuses active!"));
 *         applyNightVipPerks(player);
 *     })
 *     .onExit(player -> {
 *         player.sendMessage(Component.text("Night VIP bonuses ended."));
 *         removeNightVipPerks(player);
 *     })
 *     .build();
 *
 * conditionService.registerGroup(nightVip);
 * }</pre>
 *
 * <h2>Group Priority:</h2>
 * <p>When a player is in multiple groups, priority determines the order
 * of evaluation and which effects take precedence. Higher priority groups
 * are evaluated first and may override lower priority groups.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 * @see ConditionService
 */
public sealed interface ConditionalGroup permits ConditionalGroup.Impl {

    /**
     * Returns the unique name of this group.
     *
     * @return the group name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the condition that determines group membership.
     *
     * @return the membership condition
     * @since 1.0.0
     */
    @NotNull
    Condition getCondition();

    /**
     * Returns the priority of this group.
     *
     * <p>Higher values indicate higher priority.</p>
     *
     * @return the priority (default: 0)
     * @since 1.0.0
     */
    int getPriority();

    /**
     * Returns the description of this group.
     *
     * @return the description, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getDescription();

    /**
     * Returns the check interval for this group.
     *
     * <p>This determines how often the condition is re-evaluated
     * for players. If empty, uses the service default.</p>
     *
     * @return the check interval, or empty for default
     * @since 1.0.0
     */
    @NotNull
    Optional<Duration> getCheckInterval();

    /**
     * Returns the action to execute when a player enters this group.
     *
     * @return the on-enter action, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<Consumer<UnifiedPlayer>> getOnEnter();

    /**
     * Returns the action to execute when a player exits this group.
     *
     * @return the on-exit action, or empty if none
     * @since 1.0.0
     */
    @NotNull
    Optional<Consumer<UnifiedPlayer>> getOnExit();

    /**
     * Checks if this group is transient (not persisted across restarts).
     *
     * @return true if transient
     * @since 1.0.0
     */
    boolean isTransient();

    /**
     * Creates a new builder for constructing conditional groups.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple conditional group with just a name and condition.
     *
     * @param name      the group name
     * @param condition the membership condition
     * @return the conditional group
     * @since 1.0.0
     */
    @NotNull
    static ConditionalGroup of(@NotNull String name, @NotNull Condition condition) {
        return builder().name(name).condition(condition).build();
    }

    // ==================== Implementation ====================

    /**
     * Internal sealed implementation of ConditionalGroup.
     */
    record Impl(
            @NotNull String name,
            @NotNull Condition condition,
            int priority,
            @Nullable String description,
            @Nullable Duration checkInterval,
            @Nullable Consumer<UnifiedPlayer> onEnter,
            @Nullable Consumer<UnifiedPlayer> onExit,
            boolean isTransient
    ) implements ConditionalGroup {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull Condition getCondition() {
            return condition;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public @NotNull Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public @NotNull Optional<Duration> getCheckInterval() {
            return Optional.ofNullable(checkInterval);
        }

        @Override
        public @NotNull Optional<Consumer<UnifiedPlayer>> getOnEnter() {
            return Optional.ofNullable(onEnter);
        }

        @Override
        public @NotNull Optional<Consumer<UnifiedPlayer>> getOnExit() {
            return Optional.ofNullable(onExit);
        }

        @Override
        public boolean isTransient() {
            return isTransient;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Impl impl = (Impl) o;
            return Objects.equals(name, impl.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "ConditionalGroup{" +
                    "name='" + name + '\'' +
                    ", priority=" + priority +
                    ", transient=" + isTransient +
                    '}';
        }
    }

    /**
     * Builder for creating {@link ConditionalGroup} instances.
     *
     * @since 1.0.0
     */
    final class Builder {
        private String name;
        private Condition condition;
        private int priority = 0;
        private String description;
        private Duration checkInterval;
        private Consumer<UnifiedPlayer> onEnter;
        private Consumer<UnifiedPlayer> onExit;
        private boolean isTransient = false;

        Builder() {}

        /**
         * Sets the group name.
         *
         * @param name the group name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            return this;
        }

        /**
         * Sets the membership condition.
         *
         * @param condition the condition
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder condition(@NotNull Condition condition) {
            this.condition = Objects.requireNonNull(condition, "condition cannot be null");
            return this;
        }

        /**
         * Sets the group priority.
         *
         * @param priority the priority
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the group description.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the condition check interval.
         *
         * @param interval the check interval
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder checkInterval(@Nullable Duration interval) {
            this.checkInterval = interval;
            return this;
        }

        /**
         * Sets the action to execute when a player enters the group.
         *
         * @param action the on-enter action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder onEnter(@Nullable Consumer<UnifiedPlayer> action) {
            this.onEnter = action;
            return this;
        }

        /**
         * Sets the action to execute when a player exits the group.
         *
         * @param action the on-exit action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder onExit(@Nullable Consumer<UnifiedPlayer> action) {
            this.onExit = action;
            return this;
        }

        /**
         * Sets whether the group is transient (not persisted).
         *
         * @param isTransient true for transient groups
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder transientGroup(boolean isTransient) {
            this.isTransient = isTransient;
            return this;
        }

        /**
         * Builds the conditional group.
         *
         * @return the built group
         * @throws IllegalStateException if name or condition is not set
         * @since 1.0.0
         */
        @NotNull
        public ConditionalGroup build() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("Group name must be set");
            }
            if (condition == null) {
                throw new IllegalStateException("Group condition must be set");
            }
            return new Impl(
                    name,
                    condition,
                    priority,
                    description,
                    checkInterval,
                    onEnter,
                    onExit,
                    isTransient
            );
        }
    }
}
