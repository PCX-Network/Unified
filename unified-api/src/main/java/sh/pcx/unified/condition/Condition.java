/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Base interface for all conditions in the conditional system.
 *
 * <p>A Condition represents a boolean expression that can be evaluated against
 * a {@link ConditionContext}. Conditions are used throughout the API to control
 * access, execution, and behavior based on various criteria such as permissions,
 * time, location, weather, and more.</p>
 *
 * <h2>Built-in Condition Factories:</h2>
 * <ul>
 *   <li>{@link #permission(String)} - Check player permissions</li>
 *   <li>{@link #world(String...)} - Match world names</li>
 *   <li>{@link #region(String)} - Check region membership</li>
 *   <li>{@link #cron(String)} - Cron expression time matching</li>
 *   <li>{@link #timeRange(LocalTime, LocalTime)} - Time range matching</li>
 *   <li>{@link #placeholder(String)} - Placeholder value comparisons</li>
 *   <li>{@link #expression(String)} - Parse condition expression strings</li>
 * </ul>
 *
 * <h2>Combinators:</h2>
 * <ul>
 *   <li>{@link #and(Condition)} / {@link #all(Condition...)} - All conditions must pass</li>
 *   <li>{@link #or(Condition)} / {@link #any(Condition...)} - Any condition passes</li>
 *   <li>{@link #negate()} / {@link #not(Condition)} - Negate a condition</li>
 *   <li>{@link #xor(Condition, Condition)} - Exactly one passes</li>
 * </ul>
 *
 * <h2>Implementation Example:</h2>
 * <pre>{@code
 * public class LevelCondition implements Condition {
 *     private final int requiredLevel;
 *
 *     public LevelCondition(int requiredLevel) {
 *         this.requiredLevel = requiredLevel;
 *     }
 *
 *     @Override
 *     public String getName() {
 *         return "level:" + requiredLevel;
 *     }
 *
 *     @Override
 *     public ConditionResult evaluate(ConditionContext context) {
 *         return context.getPlayer()
 *             .map(player -> {
 *                 int level = player.getLevel();
 *                 if (level >= requiredLevel) {
 *                     return ConditionResult.success();
 *                 }
 *                 return ConditionResult.failure("Level " + requiredLevel + " required, you have " + level);
 *             })
 *             .orElse(ConditionResult.failure("No player in context"));
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * Condition vipCheck = Condition.permission("group.vip")
 *     .and(Condition.world("survival"))
 *     .and(Condition.not(Condition.region("pvp_arena")));
 *
 * ConditionContext context = ConditionContext.of(player);
 * ConditionResult result = vipCheck.evaluate(context);
 *
 * if (result.passed()) {
 *     // Apply VIP benefits
 * } else {
 *     player.sendMessage(result.getReason().orElse("Condition not met"));
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionContext
 * @see ConditionResult
 * @see ConditionService
 */
@FunctionalInterface
public interface Condition {

    /**
     * Evaluates this condition against the provided context.
     *
     * <p>The evaluation should be deterministic - given the same context,
     * it should return the same result. Conditions should not modify
     * the context or have side effects.</p>
     *
     * @param context the context to evaluate against
     * @return the result of the evaluation
     * @since 1.0.0
     */
    @NotNull
    ConditionResult evaluate(@NotNull ConditionContext context);

    /**
     * Returns the unique name/identifier for this condition.
     *
     * <p>The name is used for serialization, debugging, configuration
     * parsing, and event tracking. Built-in conditions use identifiers
     * like "permission:admin.use", "world:survival", "cron:0 0 * * SAT,SUN".</p>
     *
     * @return the condition name/identifier
     * @since 1.0.0
     */
    @NotNull
    default String getName() {
        return getType() + ":" + getClass().getSimpleName().toLowerCase();
    }

    /**
     * Returns the type identifier for this condition.
     *
     * <p>The type categorizes conditions (e.g., "permission", "time", "world").
     * Custom conditions should use namespaced identifiers to avoid conflicts.</p>
     *
     * @return the condition type identifier
     * @since 1.0.0
     */
    @NotNull
    default String getType() {
        String name = getClass().getSimpleName();
        if (name.endsWith("Condition")) {
            name = name.substring(0, name.length() - 9);
        }
        return name.toLowerCase();
    }

    /**
     * Returns a human-readable description of this condition.
     *
     * <p>This is used for debugging, logging, and displaying
     * condition requirements to users.</p>
     *
     * @return a description of the condition
     * @since 1.0.0
     */
    @NotNull
    default String getDescription() {
        return getName();
    }

    /**
     * Returns the optional cache TTL for this condition's results.
     *
     * <p>If present, results of this condition can be cached for the
     * specified duration. Conditions that depend on frequently changing
     * state should return empty or a short duration.</p>
     *
     * @return optional cache duration
     * @since 1.0.0
     */
    @NotNull
    default Optional<Duration> getCacheTtl() {
        return Optional.empty();
    }

    /**
     * Combines this condition with another using AND logic.
     *
     * <p>The resulting condition passes only if both conditions pass.</p>
     *
     * @param other the other condition
     * @return a new condition representing this AND other
     * @since 1.0.0
     */
    @NotNull
    default Condition and(@NotNull Condition other) {
        return all(this, other);
    }

    /**
     * Combines this condition with another using OR logic.
     *
     * <p>The resulting condition passes if either condition passes.</p>
     *
     * @param other the other condition
     * @return a new condition representing this OR other
     * @since 1.0.0
     */
    @NotNull
    default Condition or(@NotNull Condition other) {
        return any(this, other);
    }

    /**
     * Negates this condition.
     *
     * <p>The resulting condition passes if this condition fails.</p>
     *
     * @return a new condition representing NOT this
     * @since 1.0.0
     */
    @NotNull
    default Condition negate() {
        return not(this);
    }

    // ==================== Static Factory Methods ====================

    /**
     * Creates a condition that always passes.
     *
     * @return a condition that always returns success
     * @since 1.0.0
     */
    @NotNull
    static Condition alwaysTrue() {
        return new Condition() {
            @Override
            public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
                return ConditionResult.success();
            }

            @Override
            public @NotNull String getName() {
                return "always:true";
            }

            @Override
            public @NotNull String getType() {
                return "always";
            }
        };
    }

    /**
     * Creates a condition that always fails.
     *
     * @param reason the failure reason
     * @return a condition that always returns failure
     * @since 1.0.0
     */
    @NotNull
    static Condition alwaysFalse(@NotNull String reason) {
        return new Condition() {
            @Override
            public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
                return ConditionResult.failure(reason);
            }

            @Override
            public @NotNull String getName() {
                return "always:false";
            }

            @Override
            public @NotNull String getType() {
                return "always";
            }
        };
    }

    /**
     * Creates a permission-based condition.
     *
     * @param permission the permission node to check
     * @return a condition that checks for the permission
     * @since 1.0.0
     */
    @NotNull
    static PermissionCondition permission(@NotNull String permission) {
        return PermissionCondition.of(permission);
    }

    /**
     * Creates a world-based condition.
     *
     * @param worlds the world names to match (any of them)
     * @return a condition that checks if in one of the specified worlds
     * @since 1.0.0
     */
    @NotNull
    static WorldCondition world(@NotNull String... worlds) {
        return WorldCondition.of(worlds);
    }

    /**
     * Creates a region-based condition.
     *
     * @param region the region name to check
     * @return a condition that checks region membership
     * @since 1.0.0
     */
    @NotNull
    static RegionCondition region(@NotNull String region) {
        return RegionCondition.of(region);
    }

    /**
     * Creates a cron-based time condition.
     *
     * @param cronExpression the cron expression (e.g., "0 0 * * SAT,SUN")
     * @return a condition that matches the cron schedule
     * @since 1.0.0
     */
    @NotNull
    static CronCondition cron(@NotNull String cronExpression) {
        return CronCondition.of(cronExpression);
    }

    /**
     * Creates a time range condition.
     *
     * @param start the start time (inclusive)
     * @param end   the end time (inclusive)
     * @return a condition that matches within the time range
     * @since 1.0.0
     */
    @NotNull
    static TimeRangeCondition timeRange(@NotNull LocalTime start, @NotNull LocalTime end) {
        return TimeRangeCondition.of(start, end);
    }

    /**
     * Creates a placeholder-based condition builder.
     *
     * @param placeholder the placeholder string (e.g., "%player_level%")
     * @return a builder for configuring the placeholder comparison
     * @since 1.0.0
     */
    @NotNull
    static PlaceholderCondition.Builder placeholder(@NotNull String placeholder) {
        return PlaceholderCondition.builder(placeholder);
    }

    /**
     * Parses a condition expression string.
     *
     * <p>Expression syntax supports:
     * <ul>
     *   <li>{@code permission:node} - Permission check</li>
     *   <li>{@code world:name} - World check</li>
     *   <li>{@code region:name} - Region check</li>
     *   <li>{@code placeholder:%name%>value} - Placeholder comparison</li>
     *   <li>{@code AND, OR, NOT} - Logical operators</li>
     *   <li>Parentheses for grouping</li>
     * </ul>
     *
     * <p>Example: {@code "(permission:vip OR placeholder:%level%>50) AND world:survival"}
     *
     * @param expression the condition expression
     * @return the parsed condition
     * @throws ConditionParseException if the expression is invalid
     * @since 1.0.0
     */
    @NotNull
    static Condition expression(@NotNull String expression) {
        return ConditionParser.parse(expression);
    }

    /**
     * Creates a condition that passes if all provided conditions pass.
     *
     * @param conditions the conditions to combine
     * @return a combined AND condition
     * @since 1.0.0
     */
    @NotNull
    static Condition all(@NotNull Condition... conditions) {
        return all(Arrays.asList(conditions));
    }

    /**
     * Creates a condition that passes if all provided conditions pass.
     *
     * @param conditions the conditions to combine
     * @return a combined AND condition
     * @since 1.0.0
     */
    @NotNull
    static Condition all(@NotNull Collection<Condition> conditions) {
        List<Condition> list = List.copyOf(conditions);
        if (list.isEmpty()) {
            return alwaysTrue();
        }
        if (list.size() == 1) {
            return list.getFirst();
        }
        return new CompositeCondition.And(list);
    }

    /**
     * Creates a condition that passes if any provided condition passes.
     *
     * @param conditions the conditions to combine
     * @return a combined OR condition
     * @since 1.0.0
     */
    @NotNull
    static Condition any(@NotNull Condition... conditions) {
        return any(Arrays.asList(conditions));
    }

    /**
     * Creates a condition that passes if any provided condition passes.
     *
     * @param conditions the conditions to combine
     * @return a combined OR condition
     * @since 1.0.0
     */
    @NotNull
    static Condition any(@NotNull Collection<Condition> conditions) {
        List<Condition> list = List.copyOf(conditions);
        if (list.isEmpty()) {
            return alwaysFalse("No conditions provided");
        }
        if (list.size() == 1) {
            return list.getFirst();
        }
        return new CompositeCondition.Or(list);
    }

    /**
     * Negates a condition.
     *
     * @param condition the condition to negate
     * @return a negated condition
     * @since 1.0.0
     */
    @NotNull
    static Condition not(@NotNull Condition condition) {
        return new CompositeCondition.Not(condition);
    }

    /**
     * Creates a condition that passes if exactly one of two conditions passes.
     *
     * @param first  the first condition
     * @param second the second condition
     * @return an XOR condition
     * @since 1.0.0
     */
    @NotNull
    static Condition xor(@NotNull Condition first, @NotNull Condition second) {
        return new CompositeCondition.Xor(first, second);
    }

    /**
     * Creates a new condition builder.
     *
     * @return a new condition builder
     * @since 1.0.0
     */
    @NotNull
    static ConditionBuilder builder() {
        return new ConditionBuilder();
    }
}
