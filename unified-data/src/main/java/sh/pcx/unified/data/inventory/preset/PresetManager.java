/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.preset;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages inventory presets (kits) including registration, storage, and cooldowns.
 *
 * <p>PresetManager provides a centralized system for managing reusable inventory
 * configurations. It supports loading presets from configuration files or databases,
 * permission-based access, cooldown tracking, and usage limits.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Registration</b>: Register presets programmatically or from config</li>
 *   <li><b>Categories</b>: Organize presets into categories</li>
 *   <li><b>Cooldowns</b>: Track per-player cooldowns for each preset</li>
 *   <li><b>Usage Limits</b>: Limit how many times a preset can be used</li>
 *   <li><b>Permissions</b>: Check permissions before applying</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private PresetManager presets;
 *
 * // Register a preset
 * presets.register(pvpKit);
 *
 * // Register with category
 * presets.register(pvpKit, "combat");
 *
 * // Get a preset
 * Optional<InventoryPreset> kit = presets.get("kit_pvp");
 *
 * // List by category
 * List<InventoryPreset> combatKits = presets.getByCategory("combat");
 *
 * // Apply with cooldown check
 * PresetResult result = presets.apply(player, "kit_pvp");
 * if (result.isSuccess()) {
 *     player.sendMessage("Kit applied!");
 * } else {
 *     player.sendMessage(result.getMessage());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventoryPreset
 * @see PresetLoader
 */
public class PresetManager {

    private final Map<String, InventoryPreset> presets;
    private final Map<String, String> presetCategories;
    private final Map<UUID, Map<String, Instant>> cooldowns;
    private final Map<UUID, Map<String, Integer>> usageCounts;
    private final List<PresetLoader> loaders;

    /**
     * Creates a new PresetManager.
     *
     * @since 1.0.0
     */
    public PresetManager() {
        this.presets = new ConcurrentHashMap<>();
        this.presetCategories = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.usageCounts = new ConcurrentHashMap<>();
        this.loaders = new ArrayList<>();
    }

    // ========== Registration ==========

    /**
     * Registers a preset.
     *
     * @param preset the preset to register
     * @since 1.0.0
     */
    public void register(@NotNull InventoryPreset preset) {
        register(preset, null);
    }

    /**
     * Registers a preset with a category.
     *
     * @param preset   the preset to register
     * @param category the category, or null for uncategorized
     * @since 1.0.0
     */
    public void register(@NotNull InventoryPreset preset, @Nullable String category) {
        Objects.requireNonNull(preset, "Preset cannot be null");
        presets.put(preset.getName().toLowerCase(), preset);
        if (category != null && !category.isBlank()) {
            presetCategories.put(preset.getName().toLowerCase(), category.toLowerCase());
        }
    }

    /**
     * Unregisters a preset.
     *
     * @param name the preset name
     * @return true if the preset was unregistered
     * @since 1.0.0
     */
    public boolean unregister(@NotNull String name) {
        String key = name.toLowerCase();
        presetCategories.remove(key);
        return presets.remove(key) != null;
    }

    /**
     * Registers a preset loader.
     *
     * @param loader the loader to register
     * @since 1.0.0
     */
    public void registerLoader(@NotNull PresetLoader loader) {
        Objects.requireNonNull(loader, "Loader cannot be null");
        loaders.add(loader);
    }

    // ========== Retrieval ==========

    /**
     * Gets a preset by name.
     *
     * @param name the preset name
     * @return the preset, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public Optional<InventoryPreset> get(@NotNull String name) {
        return Optional.ofNullable(presets.get(name.toLowerCase()));
    }

    /**
     * Gets all registered presets.
     *
     * @return unmodifiable collection of presets
     * @since 1.0.0
     */
    @NotNull
    public Collection<InventoryPreset> getAll() {
        return Collections.unmodifiableCollection(presets.values());
    }

    /**
     * Gets all preset names.
     *
     * @return unmodifiable set of preset names
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getNames() {
        return Collections.unmodifiableSet(presets.keySet());
    }

    /**
     * Gets presets by category.
     *
     * @param category the category
     * @return list of presets in the category
     * @since 1.0.0
     */
    @NotNull
    public List<InventoryPreset> getByCategory(@NotNull String category) {
        String categoryLower = category.toLowerCase();
        return presets.entrySet().stream()
            .filter(e -> categoryLower.equals(presetCategories.get(e.getKey())))
            .map(Map.Entry::getValue)
            .toList();
    }

    /**
     * Gets all categories.
     *
     * @return set of unique categories
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getCategories() {
        return new HashSet<>(presetCategories.values());
    }

    /**
     * Gets the category of a preset.
     *
     * @param presetName the preset name
     * @return the category, or null if uncategorized
     * @since 1.0.0
     */
    @Nullable
    public String getCategory(@NotNull String presetName) {
        return presetCategories.get(presetName.toLowerCase());
    }

    /**
     * Gets presets available to a player.
     *
     * @param player the player
     * @return list of presets the player has permission to use
     * @since 1.0.0
     */
    @NotNull
    public List<InventoryPreset> getAvailable(@NotNull UnifiedPlayer player) {
        return presets.values().stream()
            .filter(preset -> preset.canUse(player))
            .toList();
    }

    /**
     * Checks if a preset exists.
     *
     * @param name the preset name
     * @return true if exists
     * @since 1.0.0
     */
    public boolean exists(@NotNull String name) {
        return presets.containsKey(name.toLowerCase());
    }

    // ========== Application ==========

    /**
     * Applies a preset to a player.
     *
     * <p>This method checks permissions and cooldowns before applying.
     *
     * @param player     the player
     * @param presetName the preset name
     * @return the result of the application
     * @since 1.0.0
     */
    @NotNull
    public PresetResult apply(@NotNull UnifiedPlayer player, @NotNull String presetName) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(presetName, "Preset name cannot be null");

        Optional<InventoryPreset> optPreset = get(presetName);
        if (optPreset.isEmpty()) {
            return PresetResult.notFound(presetName);
        }

        InventoryPreset preset = optPreset.get();
        return apply(player, preset);
    }

    /**
     * Applies a preset to a player.
     *
     * @param player the player
     * @param preset the preset
     * @return the result of the application
     * @since 1.0.0
     */
    @NotNull
    public PresetResult apply(@NotNull UnifiedPlayer player, @NotNull InventoryPreset preset) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(preset, "Preset cannot be null");

        // Check permission
        if (!preset.canUse(player)) {
            return PresetResult.noPermission(preset.getName(), preset.getPermission());
        }

        // Check cooldown
        Duration remaining = getRemainingCooldown(player.getUniqueId(), preset.getName());
        if (!remaining.isZero() && !remaining.isNegative()) {
            return PresetResult.onCooldown(preset.getName(), remaining);
        }

        // Check one-time use
        if (preset.isOneTimeUse()) {
            int uses = getUsageCount(player.getUniqueId(), preset.getName());
            if (uses > 0) {
                return PresetResult.alreadyUsed(preset.getName());
            }
        }

        // Apply the preset
        preset.applyTo(player);

        // Record cooldown
        if (preset.getCooldown() != null) {
            setCooldown(player.getUniqueId(), preset.getName(), preset.getCooldown());
        }

        // Record usage
        incrementUsage(player.getUniqueId(), preset.getName());

        return PresetResult.success(preset.getName());
    }

    // ========== Cooldowns ==========

    /**
     * Gets the remaining cooldown for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @return the remaining duration, or zero if no cooldown
     * @since 1.0.0
     */
    @NotNull
    public Duration getRemainingCooldown(@NotNull UUID playerId, @NotNull String presetName) {
        Map<String, Instant> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return Duration.ZERO;
        }

        Instant expiry = playerCooldowns.get(presetName.toLowerCase());
        if (expiry == null) {
            return Duration.ZERO;
        }

        Duration remaining = Duration.between(Instant.now(), expiry);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Sets a cooldown for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @param duration   the cooldown duration
     * @since 1.0.0
     */
    public void setCooldown(@NotNull UUID playerId, @NotNull String presetName, @NotNull Duration duration) {
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(presetName.toLowerCase(), Instant.now().plus(duration));
    }

    /**
     * Clears a cooldown for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @since 1.0.0
     */
    public void clearCooldown(@NotNull UUID playerId, @NotNull String presetName) {
        Map<String, Instant> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(presetName.toLowerCase());
        }
    }

    /**
     * Clears all cooldowns for a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    public void clearAllCooldowns(@NotNull UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Checks if a player is on cooldown for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @return true if on cooldown
     * @since 1.0.0
     */
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String presetName) {
        return !getRemainingCooldown(playerId, presetName).isZero();
    }

    // ========== Usage Tracking ==========

    /**
     * Gets the usage count for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @return the usage count
     * @since 1.0.0
     */
    public int getUsageCount(@NotNull UUID playerId, @NotNull String presetName) {
        Map<String, Integer> playerUsage = usageCounts.get(playerId);
        if (playerUsage == null) {
            return 0;
        }
        return playerUsage.getOrDefault(presetName.toLowerCase(), 0);
    }

    /**
     * Increments the usage count for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @since 1.0.0
     */
    public void incrementUsage(@NotNull UUID playerId, @NotNull String presetName) {
        usageCounts.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .merge(presetName.toLowerCase(), 1, Integer::sum);
    }

    /**
     * Resets usage count for a preset.
     *
     * @param playerId   the player's UUID
     * @param presetName the preset name
     * @since 1.0.0
     */
    public void resetUsage(@NotNull UUID playerId, @NotNull String presetName) {
        Map<String, Integer> playerUsage = usageCounts.get(playerId);
        if (playerUsage != null) {
            playerUsage.remove(presetName.toLowerCase());
        }
    }

    /**
     * Resets all usage for a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    public void resetAllUsage(@NotNull UUID playerId) {
        usageCounts.remove(playerId);
    }

    // ========== Loading ==========

    /**
     * Loads presets from all registered loaders.
     *
     * @return future containing the count of loaded presets
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> loadAll() {
        List<CompletableFuture<List<InventoryPreset>>> loadFutures = loaders.stream()
            .map(PresetLoader::loadPresets)
            .toList();

        return CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                int count = 0;
                for (CompletableFuture<List<InventoryPreset>> future : loadFutures) {
                    List<InventoryPreset> loaded = future.join();
                    for (InventoryPreset preset : loaded) {
                        register(preset);
                        count++;
                    }
                }
                return count;
            });
    }

    /**
     * Reloads all presets.
     *
     * @return future containing the count of loaded presets
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Integer> reload() {
        presets.clear();
        presetCategories.clear();
        return loadAll();
    }

    // ========== Cleanup ==========

    /**
     * Cleans up expired cooldowns.
     *
     * @since 1.0.0
     */
    public void cleanupExpiredCooldowns() {
        Instant now = Instant.now();
        cooldowns.forEach((playerId, playerCooldowns) -> {
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        });
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Clears all data for a player.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    public void clearPlayerData(@NotNull UUID playerId) {
        cooldowns.remove(playerId);
        usageCounts.remove(playerId);
    }

    /**
     * Gets the total number of registered presets.
     *
     * @return the preset count
     * @since 1.0.0
     */
    public int getPresetCount() {
        return presets.size();
    }

    // ========== Result Class ==========

    /**
     * Result of a preset application attempt.
     *
     * @since 1.0.0
     */
    public record PresetResult(
        boolean success,
        @NotNull String presetName,
        @NotNull ResultType type,
        @Nullable String message,
        @Nullable Duration cooldownRemaining
    ) {
        /**
         * Creates a success result.
         */
        @NotNull
        public static PresetResult success(@NotNull String presetName) {
            return new PresetResult(true, presetName, ResultType.SUCCESS, null, null);
        }

        /**
         * Creates a not found result.
         */
        @NotNull
        public static PresetResult notFound(@NotNull String presetName) {
            return new PresetResult(false, presetName, ResultType.NOT_FOUND,
                "Preset not found: " + presetName, null);
        }

        /**
         * Creates a no permission result.
         */
        @NotNull
        public static PresetResult noPermission(@NotNull String presetName, @Nullable String permission) {
            return new PresetResult(false, presetName, ResultType.NO_PERMISSION,
                "Missing permission: " + permission, null);
        }

        /**
         * Creates an on cooldown result.
         */
        @NotNull
        public static PresetResult onCooldown(@NotNull String presetName, @NotNull Duration remaining) {
            return new PresetResult(false, presetName, ResultType.ON_COOLDOWN,
                "On cooldown for " + formatDuration(remaining), remaining);
        }

        /**
         * Creates an already used result.
         */
        @NotNull
        public static PresetResult alreadyUsed(@NotNull String presetName) {
            return new PresetResult(false, presetName, ResultType.ALREADY_USED,
                "Already used one-time preset", null);
        }

        /**
         * Checks if the application was successful.
         */
        public boolean isSuccess() {
            return success;
        }

        private static String formatDuration(Duration duration) {
            long seconds = duration.getSeconds();
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }

        /**
         * Result types.
         */
        public enum ResultType {
            SUCCESS,
            NOT_FOUND,
            NO_PERMISSION,
            ON_COOLDOWN,
            ALREADY_USED
        }
    }
}
