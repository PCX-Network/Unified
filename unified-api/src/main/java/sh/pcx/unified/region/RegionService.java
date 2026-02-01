/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for region management.
 *
 * <p>The RegionService provides centralized access to all region-related functionality
 * including creation, lookup, flag queries, and persistence. It manages regions
 * across all worlds and handles priority-based flag resolution when regions overlap.
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Create regions of various shapes (cuboid, sphere, cylinder, polygon)</li>
 *   <li>Set and query flags on regions</li>
 *   <li>Priority-based flag resolution for overlapping regions</li>
 *   <li>Entry/exit event handling</li>
 *   <li>Persistent storage of regions</li>
 *   <li>Optional WorldGuard integration</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @Inject
 * private RegionService regions;
 *
 * // Create a cuboid region
 * Region spawn = regions.cuboid("spawn")
 *     .world(world)
 *     .min(new UnifiedLocation(world, -50, 60, -50))
 *     .max(new UnifiedLocation(world, 50, 120, 50))
 *     .flag(RegionFlag.PVP, false)
 *     .flag(RegionFlag.BUILD, false)
 *     .priority(10)
 *     .create();
 *
 * // Create a spherical region
 * Region arena = regions.sphere("arena-1")
 *     .center(arenaCenter)
 *     .radius(30)
 *     .flag(RegionFlag.PVP, true)
 *     .flag(RegionFlag.HUNGER, false)
 *     .create();
 *
 * // Check if location is in region
 * if (regions.isInRegion(location, "spawn")) {
 *     // In spawn
 * }
 *
 * // Get regions at location
 * Set<Region> atLocation = regions.getRegions(location);
 *
 * // Get highest priority region
 * Optional<Region> primary = regions.getPrimaryRegion(location);
 *
 * // Query flag value at location (considering all overlapping regions)
 * boolean canPvP = regions.queryFlag(location, RegionFlag.PVP, true);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Region
 * @see RegionFlag
 */
public interface RegionService extends Service {

    // =========================================================================
    // Region Builders
    // =========================================================================

    /**
     * Creates a builder for a new cuboid region.
     *
     * @param name the region name (unique within world)
     * @return a new cuboid region builder
     * @since 1.0.0
     */
    @NotNull
    CuboidRegionBuilder cuboid(@NotNull String name);

    /**
     * Creates a builder for a new spherical region.
     *
     * @param name the region name (unique within world)
     * @return a new sphere region builder
     * @since 1.0.0
     */
    @NotNull
    SphereRegionBuilder sphere(@NotNull String name);

    /**
     * Creates a builder for a new cylindrical region.
     *
     * @param name the region name (unique within world)
     * @return a new cylinder region builder
     * @since 1.0.0
     */
    @NotNull
    CylinderRegionBuilder cylinder(@NotNull String name);

    /**
     * Creates a builder for a new polygonal region.
     *
     * @param name the region name (unique within world)
     * @return a new polygon region builder
     * @since 1.0.0
     */
    @NotNull
    PolygonRegionBuilder polygon(@NotNull String name);

    /**
     * Gets or creates the global region for a world.
     *
     * <p>Each world has one global region that applies to the entire world.
     * Global regions have the lowest priority ({@code Integer.MIN_VALUE}).
     *
     * @param world the world
     * @return the global region for the world
     * @since 1.0.0
     */
    @NotNull
    GlobalRegion global(@NotNull UnifiedWorld world);

    // =========================================================================
    // Region Lookup
    // =========================================================================

    /**
     * Gets a region by name in a specific world.
     *
     * @param world the world
     * @param name  the region name
     * @return an Optional containing the region if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Region> getRegion(@NotNull UnifiedWorld world, @NotNull String name);

    /**
     * Gets a region by its unique ID.
     *
     * @param id the region UUID
     * @return an Optional containing the region if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Region> getRegion(@NotNull UUID id);

    /**
     * Gets all regions in a world.
     *
     * @param world the world
     * @return an unmodifiable collection of regions
     * @since 1.0.0
     */
    @NotNull
    Collection<Region> getRegions(@NotNull UnifiedWorld world);

    /**
     * Gets all regions that contain a location.
     *
     * <p>The returned set is ordered by priority (highest first).
     *
     * @param location the location to check
     * @return a set of regions containing the location
     * @since 1.0.0
     */
    @NotNull
    Set<Region> getRegions(@NotNull UnifiedLocation location);

    /**
     * Gets the highest-priority region at a location.
     *
     * @param location the location to check
     * @return an Optional containing the primary region
     * @since 1.0.0
     */
    @NotNull
    Optional<Region> getPrimaryRegion(@NotNull UnifiedLocation location);

    /**
     * Gets all regions across all worlds.
     *
     * @return an unmodifiable collection of all regions
     * @since 1.0.0
     */
    @NotNull
    Collection<Region> getAllRegions();

    /**
     * Gets the count of regions in a world.
     *
     * @param world the world
     * @return the number of regions
     * @since 1.0.0
     */
    int getRegionCount(@NotNull UnifiedWorld world);

    /**
     * Gets the total count of regions across all worlds.
     *
     * @return the total number of regions
     * @since 1.0.0
     */
    int getTotalRegionCount();

    // =========================================================================
    // Region Checks
    // =========================================================================

    /**
     * Checks if a location is inside a named region.
     *
     * @param location the location to check
     * @param name     the region name
     * @return true if the location is in the region
     * @since 1.0.0
     */
    boolean isInRegion(@NotNull UnifiedLocation location, @NotNull String name);

    /**
     * Checks if a location is inside any region.
     *
     * @param location the location to check
     * @return true if the location is in any region
     * @since 1.0.0
     */
    boolean isInAnyRegion(@NotNull UnifiedLocation location);

    /**
     * Checks if a player is inside a named region.
     *
     * @param player the player
     * @param name   the region name
     * @return true if the player is in the region
     * @since 1.0.0
     */
    default boolean isInRegion(@NotNull UnifiedPlayer player, @NotNull String name) {
        return isInRegion(player.getLocation(), name);
    }

    // =========================================================================
    // Flag Queries
    // =========================================================================

    /**
     * Queries a flag value at a location, considering all overlapping regions.
     *
     * <p>The value is determined by the highest-priority region that has the
     * flag set. If no region has the flag set, the default value is returned.
     *
     * @param location     the location to query
     * @param flag         the flag to query
     * @param defaultValue the default value if no region has the flag set
     * @param <T>          the flag value type
     * @return the effective flag value at the location
     * @since 1.0.0
     */
    <T> T queryFlag(@NotNull UnifiedLocation location, @NotNull RegionFlag<T> flag, @NotNull T defaultValue);

    /**
     * Queries a flag value at a location using the flag's default.
     *
     * @param location the location to query
     * @param flag     the flag to query
     * @param <T>      the flag value type
     * @return the effective flag value at the location
     * @since 1.0.0
     */
    default <T> T queryFlag(@NotNull UnifiedLocation location, @NotNull RegionFlag<T> flag) {
        return queryFlag(location, flag, flag.getDefaultValue());
    }

    /**
     * Queries a flag value for a player, considering player permissions.
     *
     * <p>Some flags may be bypassed by players with specific permissions.
     * This method handles permission-based overrides.
     *
     * @param player       the player
     * @param flag         the flag to query
     * @param defaultValue the default value if no region has the flag set
     * @param <T>          the flag value type
     * @return the effective flag value for the player
     * @since 1.0.0
     */
    <T> T queryFlag(@NotNull UnifiedPlayer player, @NotNull RegionFlag<T> flag, @NotNull T defaultValue);

    /**
     * Queries a boolean flag, checking if the action is allowed.
     *
     * <p>This is a convenience method for boolean flags that returns true
     * if the flag allows the action.
     *
     * @param location the location to check
     * @param flag     the boolean flag
     * @return true if the action is allowed
     * @since 1.0.0
     */
    default boolean allows(@NotNull UnifiedLocation location, @NotNull RegionFlag<Boolean> flag) {
        return Boolean.TRUE.equals(queryFlag(location, flag, true));
    }

    /**
     * Queries a boolean flag for a player.
     *
     * @param player the player
     * @param flag   the boolean flag
     * @return true if the action is allowed
     * @since 1.0.0
     */
    default boolean allows(@NotNull UnifiedPlayer player, @NotNull RegionFlag<Boolean> flag) {
        return Boolean.TRUE.equals(queryFlag(player, flag, true));
    }

    // =========================================================================
    // Region Management
    // =========================================================================

    /**
     * Registers a region with the service.
     *
     * <p>This is called automatically by region builders.
     *
     * @param region the region to register
     * @throws IllegalArgumentException if a region with the same name exists
     * @since 1.0.0
     */
    void registerRegion(@NotNull Region region);

    /**
     * Unregisters a region from the service.
     *
     * @param region the region to unregister
     * @return true if the region was unregistered
     * @since 1.0.0
     */
    boolean unregisterRegion(@NotNull Region region);

    /**
     * Removes a region by name from a world.
     *
     * @param world the world
     * @param name  the region name
     * @return true if the region was removed
     * @since 1.0.0
     */
    boolean removeRegion(@NotNull UnifiedWorld world, @NotNull String name);

    /**
     * Removes a region by its unique ID.
     *
     * @param id the region UUID
     * @return true if the region was removed
     * @since 1.0.0
     */
    boolean removeRegion(@NotNull UUID id);

    /**
     * Removes all regions in a world.
     *
     * @param world the world
     * @return the number of regions removed
     * @since 1.0.0
     */
    int clearRegions(@NotNull UnifiedWorld world);

    // =========================================================================
    // Persistence
    // =========================================================================

    /**
     * Saves all regions to persistent storage.
     *
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAll();

    /**
     * Saves a specific region.
     *
     * @param region the region to save
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull Region region);

    /**
     * Reloads all regions from persistent storage.
     *
     * @return a future that completes when loading is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> reload();

    /**
     * Loads regions for a specific world.
     *
     * @param world the world
     * @return a future that completes when loading is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> loadWorld(@NotNull UnifiedWorld world);

    // =========================================================================
    // WorldGuard Integration
    // =========================================================================

    /**
     * Checks if WorldGuard integration is available.
     *
     * @return true if WorldGuard is available
     * @since 1.0.0
     */
    boolean isWorldGuardAvailable();

    /**
     * Gets a WorldGuard region wrapper if available.
     *
     * @param world the world
     * @param name  the WorldGuard region name
     * @return an Optional containing the wrapped region
     * @since 1.0.0
     */
    @NotNull
    Optional<Region> getWorldGuardRegion(@NotNull UnifiedWorld world, @NotNull String name);

    /**
     * Gets all WorldGuard regions in a world.
     *
     * @param world the world
     * @return a collection of wrapped WorldGuard regions
     * @since 1.0.0
     */
    @NotNull
    Collection<Region> getWorldGuardRegions(@NotNull UnifiedWorld world);

    /**
     * Enables or disables WorldGuard integration.
     *
     * <p>When enabled, WorldGuard regions are included in flag queries.
     *
     * @param enabled true to enable integration
     * @since 1.0.0
     */
    void setWorldGuardIntegration(boolean enabled);

    /**
     * Checks if WorldGuard integration is enabled.
     *
     * @return true if integration is enabled
     * @since 1.0.0
     */
    boolean isWorldGuardIntegrationEnabled();

    // =========================================================================
    // Player Tracking
    // =========================================================================

    /**
     * Gets the regions a player is currently in.
     *
     * @param player the player
     * @return a set of regions the player is in
     * @since 1.0.0
     */
    @NotNull
    Set<Region> getPlayerRegions(@NotNull UnifiedPlayer player);

    /**
     * Gets players currently in a region.
     *
     * @param region the region
     * @return a collection of players in the region
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedPlayer> getPlayersInRegion(@NotNull Region region);

    /**
     * Gets the count of players in a region.
     *
     * @param region the region
     * @return the number of players
     * @since 1.0.0
     */
    int getPlayerCount(@NotNull Region region);

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Gets the region storage provider.
     *
     * @return the storage provider
     * @since 1.0.0
     */
    @NotNull
    RegionStorage getStorage();

    /**
     * Sets the region storage provider.
     *
     * @param storage the storage provider
     * @since 1.0.0
     */
    void setStorage(@NotNull RegionStorage storage);

    /**
     * Gets the bypass permission for a flag.
     *
     * <p>Players with this permission will bypass the flag restriction.
     *
     * @param flag the flag
     * @return the bypass permission, or null if none
     * @since 1.0.0
     */
    @Nullable
    String getBypassPermission(@NotNull RegionFlag<?> flag);

    /**
     * Sets the bypass permission for a flag.
     *
     * @param flag       the flag
     * @param permission the bypass permission (null to remove)
     * @since 1.0.0
     */
    void setBypassPermission(@NotNull RegionFlag<?> flag, @Nullable String permission);
}
