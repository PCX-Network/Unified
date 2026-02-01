/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Central service for managing and evaluating conditions.
 *
 * <p>The ConditionService provides methods for evaluating conditions against
 * players and contexts, managing conditional groups, handling temporary conditions,
 * and registering custom condition types.</p>
 *
 * <h2>Basic Usage:</h2>
 * <pre>{@code
 * @Inject
 * private ConditionService conditions;
 *
 * // Evaluate a condition
 * Condition vipCheck = Condition.permission("group.vip");
 * boolean isVip = conditions.evaluate(player, vipCheck);
 *
 * // Evaluate with context
 * ConditionContext context = ConditionContext.builder()
 *     .player(player)
 *     .metadata("event", "boss_fight")
 *     .build();
 * ConditionResult result = conditions.evaluateWithResult(context, vipCheck);
 *
 * // Async evaluation for expensive checks
 * conditions.evaluateAsync(player, expensiveCondition)
 *     .thenAccept(result -> {
 *         if (result.passed()) {
 *             // Apply effect
 *         }
 *     });
 * }</pre>
 *
 * <h2>Conditional Groups:</h2>
 * <pre>{@code
 * ConditionalGroup nightVip = ConditionalGroup.builder()
 *     .name("night_vip")
 *     .condition(Condition.all(
 *         Condition.permission("group.vip"),
 *         Condition.timeRange(LocalTime.of(20, 0), LocalTime.of(6, 0))
 *     ))
 *     .priority(100)
 *     .onEnter(player -> player.sendMessage("Night VIP bonuses active!"))
 *     .onExit(player -> player.sendMessage("Night VIP bonuses ended."))
 *     .build();
 *
 * conditions.registerGroup(nightVip);
 * Set<ConditionalGroup> active = conditions.getActiveGroups(player);
 * }</pre>
 *
 * <h2>Temporary Conditions:</h2>
 * <pre>{@code
 * TemporaryCondition temp = conditions.applyTemporary(player, "double_xp")
 *     .duration(Duration.ofHours(1))
 *     .onExpire(p -> p.sendMessage("Double XP expired!"))
 *     .apply();
 *
 * temp.extend(Duration.ofMinutes(30));
 * Duration remaining = temp.getRemaining();
 * temp.cancel();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 * @see ConditionContext
 * @see ConditionResult
 * @see ConditionalGroup
 * @see TemporaryCondition
 */
public interface ConditionService extends Service {

    // ==================== Condition Evaluation ====================

    /**
     * Evaluates a condition for a player.
     *
     * @param player    the player to evaluate for
     * @param condition the condition to evaluate
     * @return true if the condition passed
     * @since 1.0.0
     */
    boolean evaluate(@NotNull UnifiedPlayer player, @NotNull Condition condition);

    /**
     * Evaluates a condition against a context.
     *
     * @param context   the context to evaluate against
     * @param condition the condition to evaluate
     * @return true if the condition passed
     * @since 1.0.0
     */
    boolean evaluate(@NotNull ConditionContext context, @NotNull Condition condition);

    /**
     * Evaluates a condition for a player and returns the full result.
     *
     * @param player    the player to evaluate for
     * @param condition the condition to evaluate
     * @return the evaluation result
     * @since 1.0.0
     */
    @NotNull
    ConditionResult evaluateWithResult(@NotNull UnifiedPlayer player, @NotNull Condition condition);

    /**
     * Evaluates a condition against a context and returns the full result.
     *
     * @param context   the context to evaluate against
     * @param condition the condition to evaluate
     * @return the evaluation result
     * @since 1.0.0
     */
    @NotNull
    ConditionResult evaluateWithResult(@NotNull ConditionContext context, @NotNull Condition condition);

    /**
     * Evaluates a condition asynchronously.
     *
     * <p>This is useful for conditions that may be expensive to evaluate,
     * such as those involving database lookups or external API calls.</p>
     *
     * @param player    the player to evaluate for
     * @param condition the condition to evaluate
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ConditionResult> evaluateAsync(@NotNull UnifiedPlayer player, @NotNull Condition condition);

    /**
     * Evaluates a condition asynchronously against a context.
     *
     * @param context   the context to evaluate against
     * @param condition the condition to evaluate
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ConditionResult> evaluateAsync(@NotNull ConditionContext context, @NotNull Condition condition);

    /**
     * Evaluates multiple conditions and returns all results.
     *
     * @param context    the context to evaluate against
     * @param conditions the conditions to evaluate
     * @return the results keyed by condition name
     * @since 1.0.0
     */
    @NotNull
    java.util.Map<String, ConditionResult> evaluateAll(
            @NotNull ConditionContext context,
            @NotNull Collection<Condition> conditions
    );

    // ==================== Cached Evaluation ====================

    /**
     * Evaluates a condition with caching.
     *
     * <p>If the condition specifies a cache TTL, results are cached for
     * that duration. Otherwise, no caching is applied.</p>
     *
     * @param player    the player to evaluate for
     * @param condition the condition to evaluate
     * @return the evaluation result (possibly cached)
     * @since 1.0.0
     */
    @NotNull
    ConditionResult evaluateCached(@NotNull UnifiedPlayer player, @NotNull Condition condition);

    /**
     * Evaluates a condition with explicit cache TTL.
     *
     * @param player    the player to evaluate for
     * @param condition the condition to evaluate
     * @param cacheTtl  the cache time-to-live
     * @return the evaluation result (possibly cached)
     * @since 1.0.0
     */
    @NotNull
    ConditionResult evaluateCached(@NotNull UnifiedPlayer player, @NotNull Condition condition, @NotNull Duration cacheTtl);

    /**
     * Invalidates cached results for a player.
     *
     * @param player the player to invalidate cache for
     * @since 1.0.0
     */
    void invalidateCache(@NotNull UnifiedPlayer player);

    /**
     * Invalidates cached results for a specific condition.
     *
     * @param player    the player
     * @param condition the condition to invalidate
     * @since 1.0.0
     */
    void invalidateCache(@NotNull UnifiedPlayer player, @NotNull Condition condition);

    /**
     * Clears all cached condition results.
     *
     * @since 1.0.0
     */
    void clearCache();

    // ==================== Conditional Groups ====================

    /**
     * Registers a conditional group.
     *
     * @param group the group to register
     * @since 1.0.0
     */
    void registerGroup(@NotNull ConditionalGroup group);

    /**
     * Unregisters a conditional group.
     *
     * @param name the group name
     * @return the unregistered group, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<ConditionalGroup> unregisterGroup(@NotNull String name);

    /**
     * Gets a registered conditional group by name.
     *
     * @param name the group name
     * @return the group, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<ConditionalGroup> getGroup(@NotNull String name);

    /**
     * Gets all registered conditional groups.
     *
     * @return an unmodifiable collection of groups
     * @since 1.0.0
     */
    @NotNull
    Collection<ConditionalGroup> getGroups();

    /**
     * Gets the active conditional groups for a player.
     *
     * <p>A group is active if its condition currently evaluates to true.</p>
     *
     * @param player the player to check
     * @return a set of active groups
     * @since 1.0.0
     */
    @NotNull
    Set<ConditionalGroup> getActiveGroups(@NotNull UnifiedPlayer player);

    /**
     * Checks if a player is in a conditional group.
     *
     * @param player    the player to check
     * @param groupName the group name
     * @return true if the player is in the group
     * @since 1.0.0
     */
    boolean isInGroup(@NotNull UnifiedPlayer player, @NotNull String groupName);

    /**
     * Forces re-evaluation of conditional groups for a player.
     *
     * @param player the player to re-evaluate
     * @since 1.0.0
     */
    void reevaluateGroups(@NotNull UnifiedPlayer player);

    /**
     * Forces re-evaluation of all conditional groups for all players.
     *
     * @since 1.0.0
     */
    void reevaluateAllGroups();

    // ==================== Temporary Conditions ====================

    /**
     * Starts building a temporary condition for a player.
     *
     * @param player the player to apply the condition to
     * @param name   the unique name for this temporary condition
     * @return a builder for the temporary condition
     * @since 1.0.0
     */
    @NotNull
    TemporaryCondition.Builder applyTemporary(@NotNull UnifiedPlayer player, @NotNull String name);

    /**
     * Gets an active temporary condition for a player.
     *
     * @param playerId the player's UUID
     * @param name     the condition name
     * @return the temporary condition, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<TemporaryCondition> getTemporary(@NotNull UUID playerId, @NotNull String name);

    /**
     * Gets all active temporary conditions for a player.
     *
     * @param playerId the player's UUID
     * @return a collection of active temporary conditions
     * @since 1.0.0
     */
    @NotNull
    Collection<TemporaryCondition> getTemporaryConditions(@NotNull UUID playerId);

    /**
     * Checks if a player has an active temporary condition.
     *
     * @param playerId the player's UUID
     * @param name     the condition name
     * @return true if the condition is active
     * @since 1.0.0
     */
    boolean hasTemporary(@NotNull UUID playerId, @NotNull String name);

    /**
     * Cancels a temporary condition.
     *
     * @param playerId the player's UUID
     * @param name     the condition name
     * @return true if the condition was cancelled
     * @since 1.0.0
     */
    boolean cancelTemporary(@NotNull UUID playerId, @NotNull String name);

    /**
     * Cancels all temporary conditions for a player.
     *
     * @param playerId the player's UUID
     * @return the number of conditions cancelled
     * @since 1.0.0
     */
    int cancelAllTemporary(@NotNull UUID playerId);

    // ==================== Condition Registration ====================

    /**
     * Registers a custom condition type.
     *
     * @param type     the condition type identifier
     * @param factory  the factory for creating conditions of this type
     * @since 1.0.0
     */
    void registerConditionType(@NotNull String type, @NotNull ConditionFactory factory);

    /**
     * Unregisters a condition type.
     *
     * @param type the condition type identifier
     * @return the unregistered factory, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<ConditionFactory> unregisterConditionType(@NotNull String type);

    /**
     * Gets a registered condition factory by type.
     *
     * @param type the condition type identifier
     * @return the factory, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<ConditionFactory> getConditionFactory(@NotNull String type);

    /**
     * Gets all registered condition types.
     *
     * @return a set of registered type identifiers
     * @since 1.0.0
     */
    @NotNull
    Set<String> getRegisteredTypes();

    // ==================== Condition Watching ====================

    /**
     * Watches a condition for changes for a specific player.
     *
     * <p>When the condition result changes, the callback is invoked.
     * This is useful for implementing reactive behavior based on conditions.</p>
     *
     * @param player    the player to watch
     * @param condition the condition to watch
     * @param callback  the callback when the condition changes
     * @return a handle that can be used to stop watching
     * @since 1.0.0
     */
    @NotNull
    ConditionWatch watch(
            @NotNull UnifiedPlayer player,
            @NotNull Condition condition,
            @NotNull Consumer<ConditionResult> callback
    );

    /**
     * Watches a condition for changes for a specific player.
     *
     * @param player    the player to watch
     * @param condition the condition to watch
     * @param interval  the check interval
     * @param callback  the callback when the condition changes
     * @return a handle that can be used to stop watching
     * @since 1.0.0
     */
    @NotNull
    ConditionWatch watch(
            @NotNull UnifiedPlayer player,
            @NotNull Condition condition,
            @NotNull Duration interval,
            @NotNull Consumer<ConditionResult> callback
    );

    /**
     * Stops all condition watches for a player.
     *
     * @param player the player
     * @return the number of watches stopped
     * @since 1.0.0
     */
    int stopWatching(@NotNull UnifiedPlayer player);

    // ==================== Parsing ====================

    /**
     * Parses a condition from a configuration string.
     *
     * @param expression the condition expression
     * @return the parsed condition
     * @throws ConditionParseException if parsing fails
     * @since 1.0.0
     */
    @NotNull
    Condition parse(@NotNull String expression);

    /**
     * Parses a condition from a configuration map.
     *
     * @param config the configuration map
     * @return the parsed condition
     * @throws ConditionParseException if parsing fails
     * @since 1.0.0
     */
    @NotNull
    Condition parse(@NotNull java.util.Map<String, Object> config);

    /**
     * Serializes a condition to a configuration map.
     *
     * @param condition the condition to serialize
     * @return the configuration map
     * @since 1.0.0
     */
    @NotNull
    java.util.Map<String, Object> serialize(@NotNull Condition condition);

    // ==================== Statistics ====================

    /**
     * Gets statistics about condition evaluations.
     *
     * @return the condition statistics
     * @since 1.0.0
     */
    @NotNull
    ConditionStatistics getStatistics();

    /**
     * Resets condition evaluation statistics.
     *
     * @since 1.0.0
     */
    void resetStatistics();

    /**
     * A handle for stopping a condition watch.
     *
     * @since 1.0.0
     */
    interface ConditionWatch {

        /**
         * Gets the condition being watched.
         *
         * @return the condition
         * @since 1.0.0
         */
        @NotNull
        Condition getCondition();

        /**
         * Gets the player UUID being watched.
         *
         * @return the player UUID
         * @since 1.0.0
         */
        @NotNull
        UUID getPlayerId();

        /**
         * Gets the current result.
         *
         * @return the current result
         * @since 1.0.0
         */
        @NotNull
        ConditionResult getCurrentResult();

        /**
         * Checks if this watch is still active.
         *
         * @return true if active
         * @since 1.0.0
         */
        boolean isActive();

        /**
         * Stops this watch.
         *
         * @since 1.0.0
         */
        void stop();
    }

    /**
     * Statistics about condition evaluations.
     *
     * @since 1.0.0
     */
    interface ConditionStatistics {

        /**
         * Gets the total number of evaluations.
         *
         * @return the total evaluation count
         * @since 1.0.0
         */
        long getTotalEvaluations();

        /**
         * Gets the number of cache hits.
         *
         * @return the cache hit count
         * @since 1.0.0
         */
        long getCacheHits();

        /**
         * Gets the number of cache misses.
         *
         * @return the cache miss count
         * @since 1.0.0
         */
        long getCacheMisses();

        /**
         * Gets the cache hit rate (0.0 to 1.0).
         *
         * @return the cache hit rate
         * @since 1.0.0
         */
        double getCacheHitRate();

        /**
         * Gets the average evaluation time in nanoseconds.
         *
         * @return the average evaluation time
         * @since 1.0.0
         */
        double getAverageEvaluationTimeNanos();

        /**
         * Gets statistics by condition type.
         *
         * @param type the condition type
         * @return evaluation count for the type
         * @since 1.0.0
         */
        long getEvaluationsByType(@NotNull String type);
    }
}
