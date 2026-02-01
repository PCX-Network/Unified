/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents a scheduled task that can be registered by a module.
 *
 * <p>This class provides a fluent builder API for defining scheduled tasks
 * with various configuration options including async execution, delays,
 * periods, and health awareness.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Repeating Task</h3>
 * <pre>{@code
 * ScheduledTask.builder("auto-save")
 *     .period(Duration.ofMinutes(5))
 *     .action(() -> saveAllData())
 *     .build();
 * }</pre>
 *
 * <h3>Async Task with Delay</h3>
 * <pre>{@code
 * ScheduledTask.builder("database-sync")
 *     .async(true)
 *     .delay(Duration.ofSeconds(30))
 *     .period(Duration.ofMinutes(1))
 *     .action(() -> syncWithDatabase())
 *     .build();
 * }</pre>
 *
 * <h3>Health-Aware Task</h3>
 * <pre>{@code
 * ScheduledTask.builder("particles")
 *     .period(Duration.ofMillis(50))
 *     .action(() -> spawnParticles())
 *     .pauseWhenUnhealthy(true)
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Schedulable
 */
public final class ScheduledTask {

    private final String name;
    private final Runnable action;
    private final Duration delay;
    private final Duration period;
    private final boolean async;
    private final boolean pauseWhenUnhealthy;

    /**
     * Constructs a scheduled task with the specified configuration.
     *
     * @param name               the unique name of this task
     * @param action             the action to execute
     * @param delay              the initial delay before first execution
     * @param period             the period between executions (null for one-shot)
     * @param async              whether to run asynchronously
     * @param pauseWhenUnhealthy whether to skip execution during low TPS
     */
    private ScheduledTask(
            String name,
            Runnable action,
            Duration delay,
            Duration period,
            boolean async,
            boolean pauseWhenUnhealthy
    ) {
        this.name = Objects.requireNonNull(name, "Task name cannot be null");
        this.action = Objects.requireNonNull(action, "Task action cannot be null");
        this.delay = delay != null ? delay : Duration.ZERO;
        this.period = period;
        this.async = async;
        this.pauseWhenUnhealthy = pauseWhenUnhealthy;
    }

    /**
     * Creates a new builder for a scheduled task.
     *
     * @param name the unique name for the task
     * @return a new builder instance
     */
    @NotNull
    public static Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Creates a simple one-shot task.
     *
     * @param name   the task name
     * @param delay  the delay before execution
     * @param action the action to execute
     * @return the scheduled task
     */
    @NotNull
    public static ScheduledTask delayed(@NotNull String name, @NotNull Duration delay, @NotNull Runnable action) {
        return builder(name).delay(delay).action(action).build();
    }

    /**
     * Creates a simple repeating task with no initial delay.
     *
     * @param name   the task name
     * @param period the period between executions
     * @param action the action to execute
     * @return the scheduled task
     */
    @NotNull
    public static ScheduledTask repeating(@NotNull String name, @NotNull Duration period, @NotNull Runnable action) {
        return builder(name).period(period).action(action).build();
    }

    /**
     * Returns the unique name of this task.
     *
     * @return the task name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the action to execute.
     *
     * @return the task action
     */
    @NotNull
    public Runnable getAction() {
        return action;
    }

    /**
     * Returns the initial delay before first execution.
     *
     * @return the delay duration
     */
    @NotNull
    public Duration getDelay() {
        return delay;
    }

    /**
     * Returns the period between executions.
     *
     * @return the period duration, or null for one-shot tasks
     */
    public Duration getPeriod() {
        return period;
    }

    /**
     * Returns whether this is a repeating task.
     *
     * @return {@code true} if this task repeats
     */
    public boolean isRepeating() {
        return period != null && !period.isZero() && !period.isNegative();
    }

    /**
     * Returns whether this task runs asynchronously.
     *
     * @return {@code true} for async execution
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Returns whether this task should pause during low TPS.
     *
     * @return {@code true} to pause when server is unhealthy
     */
    public boolean isPauseWhenUnhealthy() {
        return pauseWhenUnhealthy;
    }

    /**
     * Returns the delay in ticks (20 ticks = 1 second).
     *
     * @return the delay in ticks
     */
    public long getDelayTicks() {
        return delay.toMillis() / 50;
    }

    /**
     * Returns the period in ticks (20 ticks = 1 second).
     *
     * @return the period in ticks, or 0 for one-shot tasks
     */
    public long getPeriodTicks() {
        return period != null ? period.toMillis() / 50 : 0;
    }

    @Override
    public String toString() {
        return "ScheduledTask{" +
                "name='" + name + '\'' +
                ", delay=" + delay +
                ", period=" + period +
                ", async=" + async +
                ", pauseWhenUnhealthy=" + pauseWhenUnhealthy +
                '}';
    }

    /**
     * Builder for creating {@link ScheduledTask} instances.
     */
    public static final class Builder {
        private final String name;
        private Runnable action;
        private Duration delay = Duration.ZERO;
        private Duration period;
        private boolean async = false;
        private boolean pauseWhenUnhealthy = false;

        /**
         * Constructs a builder with the specified task name.
         *
         * @param name the task name
         */
        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "Task name cannot be null");
        }

        /**
         * Sets the action to execute.
         *
         * @param action the task action
         * @return this builder
         */
        @NotNull
        public Builder action(@NotNull Runnable action) {
            this.action = Objects.requireNonNull(action, "Action cannot be null");
            return this;
        }

        /**
         * Sets the initial delay before first execution.
         *
         * @param delay the delay duration
         * @return this builder
         */
        @NotNull
        public Builder delay(@NotNull Duration delay) {
            this.delay = Objects.requireNonNull(delay, "Delay cannot be null");
            return this;
        }

        /**
         * Sets the period between executions.
         *
         * <p>If not set or null, the task executes only once.
         *
         * @param period the period duration
         * @return this builder
         */
        @NotNull
        public Builder period(@NotNull Duration period) {
            this.period = Objects.requireNonNull(period, "Period cannot be null");
            return this;
        }

        /**
         * Sets whether the task should run asynchronously.
         *
         * @param async {@code true} for async execution
         * @return this builder
         */
        @NotNull
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets whether the task should pause during low TPS.
         *
         * @param pauseWhenUnhealthy {@code true} to pause when unhealthy
         * @return this builder
         */
        @NotNull
        public Builder pauseWhenUnhealthy(boolean pauseWhenUnhealthy) {
            this.pauseWhenUnhealthy = pauseWhenUnhealthy;
            return this;
        }

        /**
         * Builds the scheduled task.
         *
         * @return the configured scheduled task
         * @throws IllegalStateException if action is not set
         */
        @NotNull
        public ScheduledTask build() {
            if (action == null) {
                throw new IllegalStateException("Task action must be set");
            }
            return new ScheduledTask(name, action, delay, period, async, pauseWhenUnhealthy);
        }
    }
}
