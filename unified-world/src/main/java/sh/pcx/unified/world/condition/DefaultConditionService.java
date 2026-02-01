/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.condition;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import sh.pcx.unified.condition.*;
import sh.pcx.unified.event.EventBus;
import sh.pcx.unified.event.condition.*;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Default implementation of {@link ConditionService}.
 *
 * <p>This implementation provides caching, condition watching, group management,
 * and temporary condition handling.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultConditionService implements ConditionService {

    private final EventBus eventBus;
    private final ScheduledExecutorService scheduler;

    // Caching
    private final Cache<CacheKey, ConditionResult> resultCache;

    // Groups
    private final Map<String, ConditionalGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerGroups = new ConcurrentHashMap<>();

    // Temporary conditions
    private final Map<UUID, Map<String, TemporaryConditionImpl>> temporaryConditions = new ConcurrentHashMap<>();

    // Watches
    private final Map<UUID, List<ConditionWatchImpl>> watches = new ConcurrentHashMap<>();

    // Condition factories
    private final Map<String, ConditionFactory> conditionFactories = new ConcurrentHashMap<>();

    // Statistics
    private final AtomicLong totalEvaluations = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final Map<String, AtomicLong> evaluationsByType = new ConcurrentHashMap<>();
    private final AtomicLong totalEvaluationTimeNanos = new AtomicLong();

    // Default check interval
    private static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(30);

    /**
     * Creates a new DefaultConditionService.
     *
     * @param eventBus the event bus for firing events
     */
    public DefaultConditionService(@NotNull EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ConditionService-Scheduler");
            t.setDaemon(true);
            return t;
        });

        this.resultCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();

        // Register built-in condition factories
        registerBuiltInFactories();

        // Start background tasks
        startBackgroundTasks();
    }

    // ==================== Condition Evaluation ====================

    @Override
    public boolean evaluate(@NotNull UnifiedPlayer player, @NotNull Condition condition) {
        return evaluateWithResult(player, condition).passed();
    }

    @Override
    public boolean evaluate(@NotNull ConditionContext context, @NotNull Condition condition) {
        return evaluateWithResult(context, condition).passed();
    }

    @Override
    public @NotNull ConditionResult evaluateWithResult(@NotNull UnifiedPlayer player, @NotNull Condition condition) {
        return evaluateWithResult(ConditionContext.of(player), condition);
    }

    @Override
    public @NotNull ConditionResult evaluateWithResult(@NotNull ConditionContext context, @NotNull Condition condition) {
        long startTime = System.nanoTime();
        try {
            ConditionResult result = condition.evaluate(context);
            recordEvaluation(condition.getType(), System.nanoTime() - startTime);
            return result.withType(condition.getType());
        } catch (Exception e) {
            recordEvaluation(condition.getType(), System.nanoTime() - startTime);
            return ConditionResult.failure("Evaluation error: " + e.getMessage());
        }
    }

    @Override
    public @NotNull CompletableFuture<ConditionResult> evaluateAsync(@NotNull UnifiedPlayer player, @NotNull Condition condition) {
        return evaluateAsync(ConditionContext.of(player), condition);
    }

    @Override
    public @NotNull CompletableFuture<ConditionResult> evaluateAsync(@NotNull ConditionContext context, @NotNull Condition condition) {
        return CompletableFuture.supplyAsync(() -> evaluateWithResult(context, condition), scheduler);
    }

    @Override
    public @NotNull Map<String, ConditionResult> evaluateAll(
            @NotNull ConditionContext context,
            @NotNull Collection<Condition> conditions
    ) {
        Map<String, ConditionResult> results = new LinkedHashMap<>();
        for (Condition condition : conditions) {
            results.put(condition.getName(), evaluateWithResult(context, condition));
        }
        return Collections.unmodifiableMap(results);
    }

    // ==================== Cached Evaluation ====================

    @Override
    public @NotNull ConditionResult evaluateCached(@NotNull UnifiedPlayer player, @NotNull Condition condition) {
        Optional<Duration> ttl = condition.getCacheTtl();
        if (ttl.isPresent()) {
            return evaluateCached(player, condition, ttl.get());
        }
        return evaluateWithResult(player, condition);
    }

    @Override
    public @NotNull ConditionResult evaluateCached(
            @NotNull UnifiedPlayer player,
            @NotNull Condition condition,
            @NotNull Duration cacheTtl
    ) {
        CacheKey key = new CacheKey(player.getUniqueId(), condition.getName());

        ConditionResult cached = resultCache.getIfPresent(key);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        cacheMisses.incrementAndGet();
        ConditionResult result = evaluateWithResult(player, condition);
        resultCache.put(key, result);
        return result;
    }

    @Override
    public void invalidateCache(@NotNull UnifiedPlayer player) {
        resultCache.asMap().keySet().removeIf(key -> key.playerId().equals(player.getUniqueId()));
    }

    @Override
    public void invalidateCache(@NotNull UnifiedPlayer player, @NotNull Condition condition) {
        resultCache.invalidate(new CacheKey(player.getUniqueId(), condition.getName()));
    }

    @Override
    public void clearCache() {
        resultCache.invalidateAll();
    }

    // ==================== Conditional Groups ====================

    @Override
    public void registerGroup(@NotNull ConditionalGroup group) {
        groups.put(group.getName(), group);
    }

    @Override
    public @NotNull Optional<ConditionalGroup> unregisterGroup(@NotNull String name) {
        ConditionalGroup removed = groups.remove(name);
        if (removed != null) {
            // Remove all players from the group
            playerGroups.values().forEach(set -> set.remove(name));
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public @NotNull Optional<ConditionalGroup> getGroup(@NotNull String name) {
        return Optional.ofNullable(groups.get(name));
    }

    @Override
    public @NotNull Collection<ConditionalGroup> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    @Override
    public @NotNull Set<ConditionalGroup> getActiveGroups(@NotNull UnifiedPlayer player) {
        Set<String> groupNames = playerGroups.getOrDefault(player.getUniqueId(), Collections.emptySet());
        Set<ConditionalGroup> active = new LinkedHashSet<>();
        for (String name : groupNames) {
            ConditionalGroup group = groups.get(name);
            if (group != null) {
                active.add(group);
            }
        }
        return active;
    }

    @Override
    public boolean isInGroup(@NotNull UnifiedPlayer player, @NotNull String groupName) {
        Set<String> groupNames = playerGroups.get(player.getUniqueId());
        return groupNames != null && groupNames.contains(groupName);
    }

    @Override
    public void reevaluateGroups(@NotNull UnifiedPlayer player) {
        ConditionContext context = ConditionContext.of(player);
        UUID playerId = player.getUniqueId();

        for (ConditionalGroup group : groups.values()) {
            boolean inGroup = isInGroup(player, group.getName());
            boolean shouldBeInGroup = evaluate(context, group.getCondition());

            if (shouldBeInGroup && !inGroup) {
                // Enter group
                ConditionalGroupEnterEvent event = new ConditionalGroupEnterEvent(player, group);
                eventBus.fire(event);

                if (!event.isCancelled()) {
                    playerGroups.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(group.getName());
                    group.getOnEnter().ifPresent(action -> action.accept(player));
                }
            } else if (!shouldBeInGroup && inGroup) {
                // Exit group
                Set<String> groups = playerGroups.get(playerId);
                if (groups != null) {
                    groups.remove(group.getName());
                }
                group.getOnExit().ifPresent(action -> action.accept(player));

                ConditionalGroupExitEvent event = new ConditionalGroupExitEvent(
                        player, group, ConditionalGroupExitEvent.ExitReason.CONDITION_NO_LONGER_MET
                );
                eventBus.fire(event);
            }
        }
    }

    @Override
    public void reevaluateAllGroups() {
        // This would iterate all online players - implementation depends on server access
        // For now, this is a placeholder
    }

    // ==================== Temporary Conditions ====================

    @Override
    public TemporaryCondition.@NotNull Builder applyTemporary(@NotNull UnifiedPlayer player, @NotNull String name) {
        return new TemporaryConditionBuilderImpl(this, player, name);
    }

    @Override
    public @NotNull Optional<TemporaryCondition> getTemporary(@NotNull UUID playerId, @NotNull String name) {
        Map<String, TemporaryConditionImpl> conditions = temporaryConditions.get(playerId);
        if (conditions == null) return Optional.empty();
        return Optional.ofNullable(conditions.get(name));
    }

    @Override
    public @NotNull Collection<TemporaryCondition> getTemporaryConditions(@NotNull UUID playerId) {
        Map<String, TemporaryConditionImpl> conditions = temporaryConditions.get(playerId);
        if (conditions == null) return Collections.emptyList();
        return Collections.unmodifiableCollection(new ArrayList<>(conditions.values()));
    }

    @Override
    public boolean hasTemporary(@NotNull UUID playerId, @NotNull String name) {
        return getTemporary(playerId, name).isPresent();
    }

    @Override
    public boolean cancelTemporary(@NotNull UUID playerId, @NotNull String name) {
        Map<String, TemporaryConditionImpl> conditions = temporaryConditions.get(playerId);
        if (conditions == null) return false;
        TemporaryConditionImpl condition = conditions.remove(name);
        if (condition != null && condition.isActive()) {
            condition.cancel();
            return true;
        }
        return false;
    }

    @Override
    public int cancelAllTemporary(@NotNull UUID playerId) {
        Map<String, TemporaryConditionImpl> conditions = temporaryConditions.remove(playerId);
        if (conditions == null) return 0;
        int count = 0;
        for (TemporaryConditionImpl condition : conditions.values()) {
            if (condition.isActive()) {
                condition.cancelSilently();
                count++;
            }
        }
        return count;
    }

    TemporaryCondition registerTemporary(TemporaryConditionImpl condition) {
        temporaryConditions
                .computeIfAbsent(condition.getPlayerId(), k -> new ConcurrentHashMap<>())
                .put(condition.getName(), condition);

        // Fire event
        eventBus.fire(new TemporaryConditionAppliedEvent(condition));

        return condition;
    }

    void removeTemporary(TemporaryConditionImpl condition) {
        Map<String, TemporaryConditionImpl> conditions = temporaryConditions.get(condition.getPlayerId());
        if (conditions != null) {
            conditions.remove(condition.getName());
        }
    }

    EventBus getEventBus() {
        return eventBus;
    }

    ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    // ==================== Condition Registration ====================

    @Override
    public void registerConditionType(@NotNull String type, @NotNull ConditionFactory factory) {
        conditionFactories.put(type, factory);
    }

    @Override
    public @NotNull Optional<ConditionFactory> unregisterConditionType(@NotNull String type) {
        return Optional.ofNullable(conditionFactories.remove(type));
    }

    @Override
    public @NotNull Optional<ConditionFactory> getConditionFactory(@NotNull String type) {
        return Optional.ofNullable(conditionFactories.get(type));
    }

    @Override
    public @NotNull Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(conditionFactories.keySet());
    }

    // ==================== Condition Watching ====================

    @Override
    public @NotNull ConditionWatch watch(
            @NotNull UnifiedPlayer player,
            @NotNull Condition condition,
            @NotNull Consumer<ConditionResult> callback
    ) {
        return watch(player, condition, DEFAULT_CHECK_INTERVAL, callback);
    }

    @Override
    public @NotNull ConditionWatch watch(
            @NotNull UnifiedPlayer player,
            @NotNull Condition condition,
            @NotNull Duration interval,
            @NotNull Consumer<ConditionResult> callback
    ) {
        ConditionWatchImpl watch = new ConditionWatchImpl(this, player, condition, interval, callback);
        watches.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArrayList<>()).add(watch);
        watch.start();
        return watch;
    }

    @Override
    public int stopWatching(@NotNull UnifiedPlayer player) {
        List<ConditionWatchImpl> playerWatches = watches.remove(player.getUniqueId());
        if (playerWatches == null) return 0;
        for (ConditionWatchImpl watch : playerWatches) {
            watch.stop();
        }
        return playerWatches.size();
    }

    void removeWatch(UUID playerId, ConditionWatchImpl watch) {
        List<ConditionWatchImpl> playerWatches = watches.get(playerId);
        if (playerWatches != null) {
            playerWatches.remove(watch);
        }
    }

    // ==================== Parsing ====================

    @Override
    public @NotNull Condition parse(@NotNull String expression) {
        return ConditionParser.parse(expression);
    }

    @Override
    public @NotNull Condition parse(@NotNull Map<String, Object> config) {
        String type = (String) config.get("type");
        if (type == null) {
            throw new ConditionParseException("Missing 'type' in condition config");
        }

        ConditionFactory factory = conditionFactories.get(type);
        if (factory == null) {
            throw new ConditionParseException("Unknown condition type: " + type);
        }

        return factory.create(config);
    }

    @Override
    public @NotNull Map<String, Object> serialize(@NotNull Condition condition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", condition.getType());
        map.put("name", condition.getName());
        // Additional serialization can be added based on condition type
        return map;
    }

    // ==================== Statistics ====================

    @Override
    public @NotNull ConditionStatistics getStatistics() {
        return new ConditionStatisticsImpl();
    }

    @Override
    public void resetStatistics() {
        totalEvaluations.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        totalEvaluationTimeNanos.set(0);
        evaluationsByType.clear();
    }

    private void recordEvaluation(String type, long timeNanos) {
        totalEvaluations.incrementAndGet();
        totalEvaluationTimeNanos.addAndGet(timeNanos);
        evaluationsByType.computeIfAbsent(type, k -> new AtomicLong()).incrementAndGet();
    }

    // ==================== Private Methods ====================

    private void registerBuiltInFactories() {
        registerConditionType("permission", PermissionCondition::of);
        registerConditionType("world", value -> Condition.world(value.split(",")));
        registerConditionType("region", RegionCondition::of);
        registerConditionType("cron", CronCondition::of);
    }

    private void startBackgroundTasks() {
        // Check for expired temporary conditions every second
        scheduler.scheduleAtFixedRate(this::checkExpiredTemporaryConditions, 1, 1, TimeUnit.SECONDS);

        // Re-evaluate conditional groups periodically
        scheduler.scheduleAtFixedRate(this::checkConditionalGroups, 30, 30, TimeUnit.SECONDS);
    }

    private void checkExpiredTemporaryConditions() {
        Instant now = Instant.now();
        for (Map.Entry<UUID, Map<String, TemporaryConditionImpl>> entry : temporaryConditions.entrySet()) {
            for (TemporaryConditionImpl condition : entry.getValue().values()) {
                if (condition.isActive() && now.isAfter(condition.getExpiresAt())) {
                    condition.expire();
                }
            }
        }
    }

    private void checkConditionalGroups() {
        // Implementation would iterate online players and re-evaluate groups
    }

    /**
     * Shuts down the condition service.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ==================== Inner Classes ====================

    private record CacheKey(UUID playerId, String conditionName) {}

    private class ConditionStatisticsImpl implements ConditionStatistics {
        @Override
        public long getTotalEvaluations() {
            return totalEvaluations.get();
        }

        @Override
        public long getCacheHits() {
            return cacheHits.get();
        }

        @Override
        public long getCacheMisses() {
            return cacheMisses.get();
        }

        @Override
        public double getCacheHitRate() {
            long hits = cacheHits.get();
            long total = hits + cacheMisses.get();
            return total > 0 ? (double) hits / total : 0.0;
        }

        @Override
        public double getAverageEvaluationTimeNanos() {
            long total = totalEvaluations.get();
            return total > 0 ? (double) totalEvaluationTimeNanos.get() / total : 0.0;
        }

        @Override
        public long getEvaluationsByType(@NotNull String type) {
            AtomicLong count = evaluationsByType.get(type);
            return count != null ? count.get() : 0;
        }
    }
}
