/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.config.MinigameConfig;
import sh.pcx.example.minigame.data.PlayerStatsKeys;
import sh.pcx.unified.scheduler.TaskHandle;
import sh.pcx.unified.visual.hologram.Hologram;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages hologram leaderboards for the minigame.
 *
 * <p>This class demonstrates using the UnifiedPlugin HologramService for
 * creating and managing persistent hologram leaderboards. The leaderboards
 * display top players sorted by various statistics.
 *
 * <h2>Leaderboard Types</h2>
 * <ul>
 *   <li>Top Kills - Players with most total kills</li>
 *   <li>Top Wins - Players with most game wins</li>
 *   <li>Top K/D - Players with best kill/death ratio</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic periodic updates from player data</li>
 *   <li>Persistent storage of leaderboard locations</li>
 *   <li>Animated title with color cycling</li>
 *   <li>Configurable display count</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * LeaderboardManager manager = new LeaderboardManager(plugin, config);
 * manager.initialize();
 *
 * // Create a leaderboard
 * manager.createLeaderboard("kills", location, LeaderboardType.KILLS);
 *
 * // Get leaderboard locations
 * manager.getLeaderboard("kills").ifPresent(lb -> ...);
 *
 * // Shutdown
 * manager.shutdown();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class LeaderboardManager {

    private final MinigamePlugin plugin;
    private final MinigameConfig config;
    private final Map<String, LeaderboardDisplay> leaderboards;
    private TaskHandle updateTask;

    // In production, these would be injected:
    // @Inject private HologramService hologramService;
    // @Inject private PlayerDataService playerData;

    /**
     * Creates a new LeaderboardManager.
     *
     * @param plugin the plugin instance
     * @param config the plugin configuration
     */
    public LeaderboardManager(@NotNull MinigamePlugin plugin, @NotNull MinigameConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.leaderboards = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the leaderboard manager.
     *
     * <p>This loads saved leaderboard locations and starts the update task.
     */
    public void initialize() {
        // Load saved leaderboard locations from storage
        loadLeaderboards();

        // Start periodic update task
        startUpdateTask();

        plugin.getLogger().info("Leaderboard manager initialized with " + leaderboards.size() + " leaderboards.");
    }

    /**
     * Shuts down the leaderboard manager.
     *
     * <p>This saves leaderboard locations and removes all holograms.
     */
    public void shutdown() {
        // Stop update task
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Remove all holograms
        for (LeaderboardDisplay display : leaderboards.values()) {
            display.destroy();
        }
        leaderboards.clear();

        plugin.getLogger().info("Leaderboard manager shut down.");
    }

    /**
     * Reloads leaderboard configurations and updates all displays.
     */
    public void reload() {
        // Update all leaderboards
        for (LeaderboardDisplay display : leaderboards.values()) {
            display.update();
        }
        plugin.getLogger().info("Leaderboards reloaded.");
    }

    // ==================== Leaderboard CRUD ====================

    /**
     * Creates a new leaderboard at the specified location.
     *
     * @param name     the unique name for the leaderboard
     * @param location the location to create the hologram
     * @param type     the type of leaderboard
     * @return the created leaderboard display
     * @throws IllegalArgumentException if a leaderboard with that name exists
     */
    @NotNull
    public LeaderboardDisplay createLeaderboard(@NotNull String name, @NotNull UnifiedLocation location,
                                                 @NotNull LeaderboardType type) {
        if (leaderboards.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("Leaderboard '" + name + "' already exists");
        }

        LeaderboardDisplay display = new LeaderboardDisplay(name, location, type);
        display.create();
        leaderboards.put(name.toLowerCase(), display);

        plugin.getLogger().info("Created leaderboard '" + name + "' at " + location);
        return display;
    }

    /**
     * Removes a leaderboard.
     *
     * @param name the leaderboard name
     * @return true if the leaderboard was removed
     */
    public boolean removeLeaderboard(@NotNull String name) {
        LeaderboardDisplay display = leaderboards.remove(name.toLowerCase());
        if (display != null) {
            display.destroy();
            plugin.getLogger().info("Removed leaderboard '" + name + "'");
            return true;
        }
        return false;
    }

    /**
     * Gets a leaderboard by name.
     *
     * @param name the leaderboard name
     * @return an Optional containing the leaderboard display
     */
    @NotNull
    public Optional<LeaderboardDisplay> getLeaderboard(@NotNull String name) {
        return Optional.ofNullable(leaderboards.get(name.toLowerCase()));
    }

    /**
     * Returns all leaderboards.
     *
     * @return an unmodifiable collection of leaderboard displays
     */
    @NotNull
    public Collection<LeaderboardDisplay> getAllLeaderboards() {
        return Collections.unmodifiableCollection(leaderboards.values());
    }

    // ==================== Update Logic ====================

    /**
     * Starts the periodic update task.
     */
    private void startUpdateTask() {
        int intervalMinutes = config.getLeaderboardUpdateMinutes();
        long intervalTicks = intervalMinutes * 60 * 20L;

        // In production:
        //
        // updateTask = schedulerService.runTaskTimerAsync(() -> {
        //     updateAllLeaderboards();
        // }, 20L, intervalTicks);

        plugin.getLogger().info("Leaderboard update task started (every " + intervalMinutes + " minutes).");
    }

    /**
     * Updates all leaderboards.
     */
    public void updateAllLeaderboards() {
        for (LeaderboardDisplay display : leaderboards.values()) {
            display.update();
        }

        if (plugin.getPluginConfig().isDebug()) {
            plugin.getLogger().info("Updated " + leaderboards.size() + " leaderboards.");
        }
    }

    // ==================== Persistence ====================

    /**
     * Loads saved leaderboard locations from storage.
     */
    private void loadLeaderboards() {
        // In production, this would load from configuration:
        //
        // ConfigurationSection section = config.getConfigurationSection("leaderboards");
        // if (section != null) {
        //     for (String name : section.getKeys(false)) {
        //         UnifiedLocation location = section.getLocation(name + ".location");
        //         LeaderboardType type = LeaderboardType.valueOf(section.getString(name + ".type"));
        //         createLeaderboard(name, location, type);
        //     }
        // }
    }

    /**
     * Saves leaderboard locations to storage.
     */
    private void saveLeaderboards() {
        // In production, this would save to configuration:
        //
        // for (LeaderboardDisplay display : leaderboards.values()) {
        //     config.set("leaderboards." + display.getName() + ".location", display.getLocation());
        //     config.set("leaderboards." + display.getName() + ".type", display.getType().name());
        // }
        // plugin.saveConfig();
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a single leaderboard hologram display.
     */
    public class LeaderboardDisplay {

        private final String name;
        private final UnifiedLocation location;
        private final LeaderboardType type;
        private UUID hologramId;

        /**
         * Creates a new LeaderboardDisplay.
         *
         * @param name     the leaderboard name
         * @param location the hologram location
         * @param type     the leaderboard type
         */
        public LeaderboardDisplay(@NotNull String name, @NotNull UnifiedLocation location,
                                   @NotNull LeaderboardType type) {
            this.name = name;
            this.location = location;
            this.type = type;
        }

        /**
         * Creates the hologram.
         */
        public void create() {
            // In production:
            //
            // Hologram hologram = hologramService.create(location)
            //     .addLine(buildTitleLine())
            //     .addLine(Component.empty())
            //     .persistent(true)
            //     .build();
            //
            // hologramId = hologram.getId();
            //
            // // Initial population
            // update();

            if (plugin.getPluginConfig().isDebug()) {
                plugin.getLogger().info("Created hologram for leaderboard '" + name + "'");
            }
        }

        /**
         * Updates the hologram with current data.
         */
        public void update() {
            // In production:
            //
            // hologramService.getById(hologramId).ifPresent(hologram -> {
            //     // Clear existing lines (except title)
            //     hologram.clearLines();
            //
            //     // Add title
            //     hologram.addLine(buildTitleLine());
            //     hologram.addLine(Component.empty());
            //
            //     // Query top players
            //     List<LeaderboardEntry> entries = queryLeaderboardData();
            //
            //     // Add entries
            //     for (int i = 0; i < entries.size(); i++) {
            //         hologram.addLine(buildEntryLine(i + 1, entries.get(i)));
            //     }
            //
            //     // Add footer
            //     hologram.addLine(Component.empty());
            //     hologram.addLine(Component.text("Updated every " +
            //         config.getLeaderboardUpdateMinutes() + " min", NamedTextColor.GRAY));
            // });
        }

        /**
         * Destroys the hologram.
         */
        public void destroy() {
            // In production:
            // if (hologramId != null) {
            //     hologramService.remove(hologramId);
            //     hologramId = null;
            // }
        }

        /**
         * Builds the title line for the hologram.
         *
         * @return the title component
         */
        private Component buildTitleLine() {
            return Component.text("TOP " + type.getDisplayName().toUpperCase(), NamedTextColor.GOLD, TextDecoration.BOLD);
        }

        /**
         * Builds a leaderboard entry line.
         *
         * @param rank  the rank (1-indexed)
         * @param entry the leaderboard entry
         * @return the entry component
         */
        private Component buildEntryLine(int rank, @NotNull LeaderboardEntry entry) {
            NamedTextColor color = switch (rank) {
                case 1 -> NamedTextColor.GOLD;
                case 2 -> NamedTextColor.GRAY;
                case 3 -> NamedTextColor.DARK_RED;
                default -> NamedTextColor.WHITE;
            };

            String rankPrefix = switch (rank) {
                case 1 -> "1st";
                case 2 -> "2nd";
                case 3 -> "3rd";
                default -> rank + "th";
            };

            return Component.text(rankPrefix + " ", color, TextDecoration.BOLD)
                    .append(Component.text(entry.playerName(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + entry.formattedValue(), NamedTextColor.YELLOW));
        }

        /**
         * Queries leaderboard data from player data service.
         *
         * @return the top entries
         */
        private List<LeaderboardEntry> queryLeaderboardData() {
            // In production:
            //
            // return playerData.query()
            //     .orderBy(type.getDataKey(), Order.DESC)
            //     .limit(config.getLeaderboardTopCount())
            //     .execute()
            //     .thenApply(profiles -> profiles.stream()
            //         .map(p -> new LeaderboardEntry(
            //             p.getPlayerId(),
            //             p.getName(),
            //             p.getData(type.getDataKey()),
            //             type.formatValue(p.getData(type.getDataKey()))
            //         ))
            //         .toList())
            //     .join();

            return Collections.emptyList();
        }

        // Getters

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public UnifiedLocation getLocation() {
            return location;
        }

        @NotNull
        public LeaderboardType getType() {
            return type;
        }

        @Nullable
        public UUID getHologramId() {
            return hologramId;
        }
    }

    /**
     * A leaderboard entry.
     *
     * @param playerId       the player's UUID
     * @param playerName     the player's name
     * @param value          the raw value
     * @param formattedValue the formatted value for display
     */
    public record LeaderboardEntry(
            @NotNull UUID playerId,
            @NotNull String playerName,
            Object value,
            @NotNull String formattedValue
    ) {}

    /**
     * Types of leaderboards.
     */
    public enum LeaderboardType {
        KILLS("Kills") {
            @Override
            public String formatValue(Object value) {
                return value != null ? value.toString() : "0";
            }
        },
        WINS("Wins") {
            @Override
            public String formatValue(Object value) {
                return value != null ? value.toString() : "0";
            }
        },
        KD_RATIO("K/D Ratio") {
            @Override
            public String formatValue(Object value) {
                if (value instanceof Number num) {
                    return String.format("%.2f", num.doubleValue());
                }
                return "0.00";
            }
        },
        GAMES_PLAYED("Games") {
            @Override
            public String formatValue(Object value) {
                return value != null ? value.toString() : "0";
            }
        },
        COINS("Coins") {
            @Override
            public String formatValue(Object value) {
                if (value instanceof Number num) {
                    return String.format("%,d", num.longValue());
                }
                return "0";
            }
        };

        private final String displayName;

        LeaderboardType(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the display name.
         *
         * @return the display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Formats a value for display.
         *
         * @param value the raw value
         * @return the formatted string
         */
        public abstract String formatValue(Object value);
    }
}
