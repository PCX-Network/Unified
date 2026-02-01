package sh.pcx.unified.util.time;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A global manager for tracking cooldowns across multiple keys.
 *
 * <p>This class provides a centralized way to manage cooldowns for various
 * entities or actions, supporting multiple cooldown categories and automatic
 * cleanup of expired entries.</p>
 *
 * <h2>Basic Usage:</h2>
 * <pre>{@code
 * // Create a manager for player ability cooldowns
 * CooldownManager<UUID> abilityCooldowns = new CooldownManager<>();
 *
 * // Check and set cooldowns
 * UUID playerId = player.getUniqueId();
 * Duration cooldownTime = Duration.ofSeconds(30);
 *
 * if (abilityCooldowns.isOnCooldown(playerId)) {
 *     Duration remaining = abilityCooldowns.getRemaining(playerId);
 *     player.sendMessage("Wait " + TimeUtils.formatDuration(remaining));
 * } else {
 *     abilityCooldowns.setCooldown(playerId, cooldownTime);
 *     useAbility(player);
 * }
 * }</pre>
 *
 * <h2>Named Cooldowns:</h2>
 * <pre>{@code
 * // Use named cooldowns for different abilities
 * CooldownManager<UUID> cooldowns = new CooldownManager<>();
 *
 * // Set cooldown for specific ability
 * cooldowns.setCooldown(playerId, "fireball", Duration.ofSeconds(10));
 * cooldowns.setCooldown(playerId, "heal", Duration.ofSeconds(30));
 *
 * // Check specific ability
 * if (!cooldowns.isOnCooldown(playerId, "fireball")) {
 *     castFireball(player);
 *     cooldowns.setCooldown(playerId, "fireball", Duration.ofSeconds(10));
 * }
 * }</pre>
 *
 * <h2>Automatic Cleanup:</h2>
 * <pre>{@code
 * // Periodically clean up expired cooldowns
 * scheduler.runRepeating(() -> {
 *     int removed = cooldowns.cleanup();
 *     if (removed > 0) {
 *         logger.debug("Cleaned up " + removed + " expired cooldowns");
 *     }
 * }, 5, TimeUnit.MINUTES);
 * }</pre>
 *
 * @param <K> the type of keys used to identify cooldown subjects
 * @author Supatuck
 * @since 1.0.0
 * @see Cooldown
 */
public class CooldownManager<K> {

    private final Map<K, Map<String, Cooldown>> cooldowns;
    private final String defaultCategory;

    /**
     * Creates a new CooldownManager with "default" as the default category.
     */
    public CooldownManager() {
        this("default");
    }

    /**
     * Creates a new CooldownManager with a custom default category.
     *
     * @param defaultCategory the default cooldown category
     */
    public CooldownManager(String defaultCategory) {
        this.cooldowns = new ConcurrentHashMap<>();
        this.defaultCategory = defaultCategory != null ? defaultCategory : "default";
    }

    // ==================== Basic Cooldown Operations ====================

    /**
     * Sets a cooldown for the specified key using the default category.
     *
     * @param key the key to set the cooldown for
     * @param duration the cooldown duration
     * @return the Cooldown object for further manipulation
     */
    public Cooldown setCooldown(K key, Duration duration) {
        return setCooldown(key, defaultCategory, duration);
    }

    /**
     * Sets a cooldown for the specified key and category.
     *
     * @param key the key to set the cooldown for
     * @param category the cooldown category
     * @param duration the cooldown duration
     * @return the Cooldown object for further manipulation
     */
    public Cooldown setCooldown(K key, String category, Duration duration) {
        if (key == null || duration == null) {
            throw new IllegalArgumentException("Key and duration cannot be null");
        }
        String cat = category != null ? category : defaultCategory;

        Cooldown cooldown = new Cooldown(duration);
        cooldown.start();

        cooldowns.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                 .put(cat, cooldown);

        return cooldown;
    }

    /**
     * Sets a cooldown using milliseconds.
     *
     * @param key the key to set the cooldown for
     * @param durationMillis the cooldown duration in milliseconds
     * @return the Cooldown object
     */
    public Cooldown setCooldown(K key, long durationMillis) {
        return setCooldown(key, Duration.ofMillis(durationMillis));
    }

    /**
     * Sets a cooldown using a time unit.
     *
     * @param key the key to set the cooldown for
     * @param duration the duration value
     * @param unit the time unit
     * @return the Cooldown object
     */
    public Cooldown setCooldown(K key, long duration, TimeUnit unit) {
        return setCooldown(key, Duration.ofMillis(unit.toMillis(duration)));
    }

    /**
     * Sets a cooldown by parsing a duration string.
     *
     * @param key the key to set the cooldown for
     * @param durationString the duration string (e.g., "30s", "5m")
     * @return the Cooldown object
     * @throws IllegalArgumentException if the duration string is invalid
     */
    public Cooldown setCooldown(K key, String durationString) {
        return setCooldown(key, TimeUtils.parseDuration(durationString));
    }

    // ==================== Cooldown Checks ====================

    /**
     * Checks if the key is on cooldown in the default category.
     *
     * @param key the key to check
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown(K key) {
        return isOnCooldown(key, defaultCategory);
    }

    /**
     * Checks if the key is on cooldown in the specified category.
     *
     * @param key the key to check
     * @param category the cooldown category
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown(K key, String category) {
        return getCooldown(key, category)
                .map(Cooldown::isOnCooldown)
                .orElse(false);
    }

    /**
     * Checks if the key is ready (not on cooldown) in the default category.
     *
     * @param key the key to check
     * @return true if ready, false if on cooldown
     */
    public boolean isReady(K key) {
        return !isOnCooldown(key);
    }

    /**
     * Checks if the key is ready in the specified category.
     *
     * @param key the key to check
     * @param category the cooldown category
     * @return true if ready, false if on cooldown
     */
    public boolean isReady(K key, String category) {
        return !isOnCooldown(key, category);
    }

    /**
     * Attempts to start a cooldown only if the key is not already on cooldown.
     *
     * @param key the key to set the cooldown for
     * @param duration the cooldown duration
     * @return true if the cooldown was started, false if already on cooldown
     */
    public boolean trySetCooldown(K key, Duration duration) {
        return trySetCooldown(key, defaultCategory, duration);
    }

    /**
     * Attempts to start a cooldown in a specific category.
     *
     * @param key the key to set the cooldown for
     * @param category the cooldown category
     * @param duration the cooldown duration
     * @return true if the cooldown was started, false if already on cooldown
     */
    public boolean trySetCooldown(K key, String category, Duration duration) {
        if (isOnCooldown(key, category)) {
            return false;
        }
        setCooldown(key, category, duration);
        return true;
    }

    // ==================== Cooldown Retrieval ====================

    /**
     * Gets the Cooldown object for the key in the default category.
     *
     * @param key the key
     * @return an Optional containing the Cooldown, or empty if none exists
     */
    public Optional<Cooldown> getCooldown(K key) {
        return getCooldown(key, defaultCategory);
    }

    /**
     * Gets the Cooldown object for the key in the specified category.
     *
     * @param key the key
     * @param category the cooldown category
     * @return an Optional containing the Cooldown, or empty if none exists
     */
    public Optional<Cooldown> getCooldown(K key, String category) {
        if (key == null) {
            return Optional.empty();
        }
        String cat = category != null ? category : defaultCategory;
        Map<String, Cooldown> keyCooldowns = cooldowns.get(key);
        if (keyCooldowns == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(keyCooldowns.get(cat));
    }

    /**
     * Gets the remaining time for a cooldown in the default category.
     *
     * @param key the key
     * @return the remaining Duration, or Duration.ZERO if not on cooldown
     */
    public Duration getRemaining(K key) {
        return getRemaining(key, defaultCategory);
    }

    /**
     * Gets the remaining time for a cooldown in the specified category.
     *
     * @param key the key
     * @param category the cooldown category
     * @return the remaining Duration, or Duration.ZERO if not on cooldown
     */
    public Duration getRemaining(K key, String category) {
        return getCooldown(key, category)
                .map(Cooldown::getRemaining)
                .orElse(Duration.ZERO);
    }

    /**
     * Gets the remaining time in milliseconds.
     *
     * @param key the key
     * @return the remaining milliseconds, or 0 if not on cooldown
     */
    public long getRemainingMillis(K key) {
        return getRemaining(key).toMillis();
    }

    /**
     * Gets the remaining time in the specified category in milliseconds.
     *
     * @param key the key
     * @param category the cooldown category
     * @return the remaining milliseconds, or 0 if not on cooldown
     */
    public long getRemainingMillis(K key, String category) {
        return getRemaining(key, category).toMillis();
    }

    /**
     * Gets the cooldown progress (0.0 to 1.0).
     *
     * @param key the key
     * @return the progress, or 1.0 if not on cooldown
     */
    public double getProgress(K key) {
        return getCooldown(key)
                .map(Cooldown::getProgress)
                .orElse(1.0);
    }

    /**
     * Gets the cooldown progress in the specified category.
     *
     * @param key the key
     * @param category the cooldown category
     * @return the progress, or 1.0 if not on cooldown
     */
    public double getProgress(K key, String category) {
        return getCooldown(key, category)
                .map(Cooldown::getProgress)
                .orElse(1.0);
    }

    // ==================== Cooldown Modification ====================

    /**
     * Resets the cooldown for the key in the default category.
     *
     * @param key the key
     * @return true if a cooldown was reset, false if none existed
     */
    public boolean resetCooldown(K key) {
        return resetCooldown(key, defaultCategory);
    }

    /**
     * Resets the cooldown for the key in the specified category.
     *
     * @param key the key
     * @param category the cooldown category
     * @return true if a cooldown was reset, false if none existed
     */
    public boolean resetCooldown(K key, String category) {
        if (key == null) {
            return false;
        }
        Map<String, Cooldown> keyCooldowns = cooldowns.get(key);
        if (keyCooldowns == null) {
            return false;
        }
        Cooldown cooldown = keyCooldowns.remove(category != null ? category : defaultCategory);
        if (cooldown != null) {
            cooldown.reset();
            return true;
        }
        return false;
    }

    /**
     * Resets all cooldowns for the specified key.
     *
     * @param key the key
     * @return the number of cooldowns reset
     */
    public int resetAllCooldowns(K key) {
        if (key == null) {
            return 0;
        }
        Map<String, Cooldown> keyCooldowns = cooldowns.remove(key);
        if (keyCooldowns == null) {
            return 0;
        }
        int count = keyCooldowns.size();
        keyCooldowns.values().forEach(Cooldown::reset);
        return count;
    }

    /**
     * Extends a cooldown by the specified duration.
     *
     * @param key the key
     * @param extension the duration to extend by
     * @return true if the cooldown was extended, false if none existed
     */
    public boolean extendCooldown(K key, Duration extension) {
        return extendCooldown(key, defaultCategory, extension);
    }

    /**
     * Extends a cooldown in the specified category.
     *
     * @param key the key
     * @param category the cooldown category
     * @param extension the duration to extend by
     * @return true if the cooldown was extended, false if none existed
     */
    public boolean extendCooldown(K key, String category, Duration extension) {
        return getCooldown(key, category)
                .map(c -> {
                    c.extend(extension);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Reduces a cooldown by the specified duration.
     *
     * @param key the key
     * @param reduction the duration to reduce by
     * @return true if the cooldown was reduced, false if none existed
     */
    public boolean reduceCooldown(K key, Duration reduction) {
        return reduceCooldown(key, defaultCategory, reduction);
    }

    /**
     * Reduces a cooldown in the specified category.
     *
     * @param key the key
     * @param category the cooldown category
     * @param reduction the duration to reduce by
     * @return true if the cooldown was reduced, false if none existed
     */
    public boolean reduceCooldown(K key, String category, Duration reduction) {
        return getCooldown(key, category)
                .map(c -> {
                    c.reduce(reduction);
                    return true;
                })
                .orElse(false);
    }

    // ==================== Bulk Operations ====================

    /**
     * Gets all keys currently tracked by this manager.
     *
     * @return a set of all keys
     */
    public Set<K> getKeys() {
        return Set.copyOf(cooldowns.keySet());
    }

    /**
     * Gets all categories for a specific key.
     *
     * @param key the key
     * @return a set of category names
     */
    public Set<String> getCategories(K key) {
        Map<String, Cooldown> keyCooldowns = cooldowns.get(key);
        return keyCooldowns != null ? Set.copyOf(keyCooldowns.keySet()) : Set.of();
    }

    /**
     * Gets the total number of tracked cooldowns.
     *
     * @return the total cooldown count
     */
    public int size() {
        return cooldowns.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    /**
     * Gets the number of active (not expired) cooldowns.
     *
     * @return the active cooldown count
     */
    public int activeCount() {
        return (int) cooldowns.values().stream()
                .flatMap(m -> m.values().stream())
                .filter(Cooldown::isOnCooldown)
                .count();
    }

    /**
     * Checks if this manager has any cooldowns for the specified key.
     *
     * @param key the key
     * @return true if any cooldowns exist for the key
     */
    public boolean hasKey(K key) {
        return cooldowns.containsKey(key);
    }

    /**
     * Iterates over all active cooldowns.
     *
     * @param action the action to perform for each key-cooldown pair
     */
    public void forEach(BiConsumer<K, Cooldown> action) {
        cooldowns.forEach((key, categories) ->
                categories.values().stream()
                        .filter(Cooldown::isOnCooldown)
                        .forEach(cooldown -> action.accept(key, cooldown))
        );
    }

    /**
     * Iterates over all cooldowns in a specific category.
     *
     * @param category the category to iterate
     * @param action the action to perform for each key-cooldown pair
     */
    public void forEachInCategory(String category, BiConsumer<K, Cooldown> action) {
        String cat = category != null ? category : defaultCategory;
        cooldowns.forEach((key, categories) -> {
            Cooldown cooldown = categories.get(cat);
            if (cooldown != null && cooldown.isOnCooldown()) {
                action.accept(key, cooldown);
            }
        });
    }

    // ==================== Cleanup ====================

    /**
     * Removes all expired cooldowns from the manager.
     *
     * @return the number of cooldowns removed
     */
    public int cleanup() {
        int removed = 0;
        var iterator = cooldowns.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            Map<String, Cooldown> categories = entry.getValue();

            // Remove expired cooldowns within each key
            categories.entrySet().removeIf(catEntry -> !catEntry.getValue().isOnCooldown());
            removed += categories.size();

            // Remove the key entirely if no cooldowns remain
            if (categories.isEmpty()) {
                iterator.remove();
            }
        }

        return removed;
    }

    /**
     * Clears all cooldowns from the manager.
     */
    public void clear() {
        cooldowns.values().forEach(map ->
                map.values().forEach(Cooldown::reset)
        );
        cooldowns.clear();
    }

    // ==================== Formatted Output ====================

    /**
     * Gets a formatted string of the remaining time for a cooldown.
     *
     * @param key the key
     * @return the formatted remaining time, or "Ready" if not on cooldown
     */
    public String formatRemaining(K key) {
        return formatRemaining(key, defaultCategory);
    }

    /**
     * Gets a formatted string of the remaining time for a cooldown in a category.
     *
     * @param key the key
     * @param category the cooldown category
     * @return the formatted remaining time, or "Ready" if not on cooldown
     */
    public String formatRemaining(K key, String category) {
        return getCooldown(key, category)
                .filter(Cooldown::isOnCooldown)
                .map(Cooldown::formatRemaining)
                .orElse("Ready");
    }

    @Override
    public String toString() {
        return String.format("CooldownManager[keys=%d, total=%d, active=%d]",
                cooldowns.size(), size(), activeCount());
    }
}
