/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.arena;

import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.config.MinigameConfig;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.region.Region;
import sh.pcx.unified.region.RegionService;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all arenas in the minigame plugin.
 *
 * <p>The ArenaManager handles arena creation, deletion, lookup, and persistence.
 * It also provides methods to find arenas containing specific locations or players.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a new arena
 * Arena arena = arenaManager.createArena("battlefield", world);
 *
 * // Get arena by name
 * Optional<Arena> arena = arenaManager.getArena("battlefield");
 *
 * // Find arena at location
 * Optional<Arena> arena = arenaManager.getArenaAt(location);
 *
 * // Get all available arenas
 * List<Arena> available = arenaManager.getAvailableArenas();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Arena
 * @see ArenaModule
 */
public class ArenaManager {

    private final MinigamePlugin plugin;
    private final MinigameConfig config;
    private final Map<String, Arena> arenasByName;
    private final Map<UUID, Arena> arenasById;

    // In production, this would be injected
    // @Inject private RegionService regionService;

    /**
     * Creates a new ArenaManager.
     *
     * @param plugin the plugin instance
     * @param config the plugin configuration
     */
    public ArenaManager(@NotNull MinigamePlugin plugin, @NotNull MinigameConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.arenasByName = new ConcurrentHashMap<>();
        this.arenasById = new ConcurrentHashMap<>();
    }

    // ==================== Arena CRUD Operations ====================

    /**
     * Creates a new arena with the specified name.
     *
     * @param name  the unique name for the arena
     * @param world the world the arena is in
     * @return the created arena
     * @throws IllegalArgumentException if an arena with that name already exists
     */
    @NotNull
    public Arena createArena(@NotNull String name, @NotNull UnifiedWorld world) {
        String lowerName = name.toLowerCase();
        if (arenasByName.containsKey(lowerName)) {
            throw new IllegalArgumentException("Arena with name '" + name + "' already exists");
        }

        Arena arena = new Arena(name, world);
        arena.setMinPlayers(config.getMinPlayers());
        arena.setMaxPlayers(config.getMaxPlayers());

        arenasByName.put(lowerName, arena);
        arenasById.put(arena.getId(), arena);

        plugin.getLogger().info("Created arena: " + name);
        return arena;
    }

    /**
     * Deletes an arena by name.
     *
     * @param name the name of the arena to delete
     * @return true if the arena was deleted
     */
    public boolean deleteArena(@NotNull String name) {
        String lowerName = name.toLowerCase();
        Arena arena = arenasByName.remove(lowerName);
        if (arena != null) {
            arenasById.remove(arena.getId());
            plugin.getLogger().info("Deleted arena: " + name);
            return true;
        }
        return false;
    }

    /**
     * Deletes an arena by UUID.
     *
     * @param id the UUID of the arena to delete
     * @return true if the arena was deleted
     */
    public boolean deleteArena(@NotNull UUID id) {
        Arena arena = arenasById.remove(id);
        if (arena != null) {
            arenasByName.remove(arena.getName().toLowerCase());
            plugin.getLogger().info("Deleted arena: " + arena.getName());
            return true;
        }
        return false;
    }

    // ==================== Arena Lookup Methods ====================

    /**
     * Gets an arena by name.
     *
     * @param name the arena name
     * @return an Optional containing the arena if found
     */
    @NotNull
    public Optional<Arena> getArena(@NotNull String name) {
        return Optional.ofNullable(arenasByName.get(name.toLowerCase()));
    }

    /**
     * Gets an arena by UUID.
     *
     * @param id the arena UUID
     * @return an Optional containing the arena if found
     */
    @NotNull
    public Optional<Arena> getArena(@NotNull UUID id) {
        return Optional.ofNullable(arenasById.get(id));
    }

    /**
     * Gets the arena at a specific location.
     *
     * @param location the location to check
     * @return an Optional containing the arena if found
     */
    @NotNull
    public Optional<Arena> getArenaAt(@NotNull UnifiedLocation location) {
        return arenasByName.values().stream()
                .filter(arena -> arena.contains(location))
                .findFirst();
    }

    /**
     * Gets the arena that a player is currently in.
     *
     * @param player the player to check
     * @return an Optional containing the arena if the player is in one
     */
    @NotNull
    public Optional<Arena> getArenaContaining(@NotNull UnifiedPlayer player) {
        return getArenaAt(player.getLocation());
    }

    /**
     * Checks if a location is inside any arena.
     *
     * @param location the location to check
     * @return true if the location is in an arena
     */
    public boolean isInArena(@NotNull UnifiedLocation location) {
        return getArenaAt(location).isPresent();
    }

    /**
     * Checks if a player is inside any arena.
     *
     * @param player the player to check
     * @return true if the player is in an arena
     */
    public boolean isInArena(@NotNull UnifiedPlayer player) {
        return isInArena(player.getLocation());
    }

    // ==================== Collection Methods ====================

    /**
     * Returns all arenas.
     *
     * @return an unmodifiable collection of all arenas
     */
    @NotNull
    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenasByName.values());
    }

    /**
     * Returns all enabled arenas.
     *
     * @return a list of enabled arenas
     */
    @NotNull
    public List<Arena> getEnabledArenas() {
        return arenasByName.values().stream()
                .filter(Arena::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Returns all arenas that are available for a new game.
     *
     * @return a list of available arenas
     */
    @NotNull
    public List<Arena> getAvailableArenas() {
        return arenasByName.values().stream()
                .filter(Arena::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Returns all arenas in a specific state.
     *
     * @param state the state to filter by
     * @return a list of arenas in the specified state
     */
    @NotNull
    public List<Arena> getArenasByState(@NotNull ArenaState state) {
        return arenasByName.values().stream()
                .filter(arena -> arena.getState() == state)
                .collect(Collectors.toList());
    }

    /**
     * Returns the total number of arenas.
     *
     * @return the arena count
     */
    public int getArenaCount() {
        return arenasByName.size();
    }

    /**
     * Returns the number of available arenas.
     *
     * @return the available arena count
     */
    public int getAvailableArenaCount() {
        return (int) arenasByName.values().stream()
                .filter(Arena::isAvailable)
                .count();
    }

    /**
     * Returns all arena names.
     *
     * @return a set of all arena names
     */
    @NotNull
    public Set<String> getArenaNames() {
        return arenasByName.values().stream()
                .map(Arena::getName)
                .collect(Collectors.toSet());
    }

    // ==================== Arena Selection ====================

    /**
     * Returns a random available arena.
     *
     * @return an Optional containing a random available arena
     */
    @NotNull
    public Optional<Arena> getRandomAvailableArena() {
        List<Arena> available = getAvailableArenas();
        if (available.isEmpty()) {
            return Optional.empty();
        }
        int index = (int) (Math.random() * available.size());
        return Optional.of(available.get(index));
    }

    /**
     * Returns the arena with the most players waiting.
     *
     * @param gameManager a function to get player count for an arena
     * @return an Optional containing the most populated waiting arena
     */
    @NotNull
    public Optional<Arena> getMostPopulatedArena(java.util.function.ToIntFunction<Arena> getPlayerCount) {
        return getArenasByState(ArenaState.WAITING).stream()
                .max(Comparator.comparingInt(getPlayerCount));
    }

    // ==================== Persistence ====================

    /**
     * Saves all arena data to storage.
     */
    public void saveAll() {
        plugin.getLogger().info("Saving " + arenasByName.size() + " arenas...");
        // In production, this would use the UnifiedPlugin data service:
        // dataService.saveAll(arenasByName.values());
    }

    /**
     * Loads all arena data from storage.
     */
    public void loadAll() {
        plugin.getLogger().info("Loading arenas...");
        // In production, this would use the UnifiedPlugin data service:
        // List<Arena> loaded = dataService.loadAll(Arena.class);
        // loaded.forEach(arena -> {
        //     arenasByName.put(arena.getName().toLowerCase(), arena);
        //     arenasById.put(arena.getId(), arena);
        // });
    }

    /**
     * Reloads all arena data from storage.
     */
    public void reload() {
        arenasByName.clear();
        arenasById.clear();
        loadAll();
        plugin.getLogger().info("Reloaded " + arenasByName.size() + " arenas.");
    }

    // ==================== Region Integration ====================

    /**
     * Creates a region for an arena using the RegionService.
     *
     * <p>This demonstrates integration with the UnifiedPlugin RegionService.
     *
     * @param arena the arena to create a region for
     * @param min   the minimum corner of the region
     * @param max   the maximum corner of the region
     */
    public void setupArenaRegion(@NotNull Arena arena, @NotNull UnifiedLocation min, @NotNull UnifiedLocation max) {
        // In production, this would use the RegionService:
        //
        // Region region = regionService.cuboid("arena-" + arena.getName())
        //     .world(arena.getWorld())
        //     .min(min)
        //     .max(max)
        //     .flag(RegionFlag.PVP, true)
        //     .flag(RegionFlag.BUILD, false)
        //     .flag(RegionFlag.ENTRY_MESSAGE, "Welcome to " + arena.getDisplayName())
        //     .priority(10)
        //     .create();
        //
        // arena.setRegion(region);

        plugin.getLogger().info("Set up region for arena: " + arena.getName());
    }
}
