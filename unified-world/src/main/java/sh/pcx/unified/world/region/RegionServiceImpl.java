/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.region;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.region.*;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link RegionService}.
 *
 * <p>This implementation provides:
 * <ul>
 *   <li>Thread-safe region management</li>
 *   <li>Efficient spatial queries using world-based indexing</li>
 *   <li>Priority-based flag resolution</li>
 *   <li>Player tracking for entry/exit events</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class RegionServiceImpl implements RegionService {

    // Regions indexed by world and name
    private final Map<String, Map<String, Region>> regionsByWorld = new ConcurrentHashMap<>();

    // Regions indexed by UUID for fast lookup
    private final Map<UUID, Region> regionsById = new ConcurrentHashMap<>();

    // Global regions per world
    private final Map<String, GlobalRegion> globalRegions = new ConcurrentHashMap<>();

    // Player region tracking
    private final Map<UUID, Set<Region>> playerRegions = new ConcurrentHashMap<>();

    // Bypass permissions per flag
    private final Map<RegionFlag<?>, String> bypassPermissions = new ConcurrentHashMap<>();

    // Storage provider
    private volatile RegionStorage storage;

    // WorldGuard integration
    private volatile boolean worldGuardIntegration = false;

    /**
     * Creates a new region service.
     */
    public RegionServiceImpl() {
        // Use file-based storage by default
        this.storage = new FileRegionStorage();
    }

    /**
     * Creates a new region service with specified storage.
     *
     * @param storage the storage provider
     */
    public RegionServiceImpl(@NotNull RegionStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
    }

    // =========================================================================
    // Region Builders
    // =========================================================================

    @Override
    @NotNull
    public CuboidRegionBuilder cuboid(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new CuboidRegionBuilderImpl(name, this);
    }

    @Override
    @NotNull
    public SphereRegionBuilder sphere(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new SphereRegionBuilderImpl(name, this);
    }

    @Override
    @NotNull
    public CylinderRegionBuilder cylinder(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new CylinderRegionBuilderImpl(name, this);
    }

    @Override
    @NotNull
    public PolygonRegionBuilder polygon(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new PolygonRegionBuilderImpl(name, this);
    }

    @Override
    @NotNull
    public GlobalRegion global(@NotNull UnifiedWorld world) {
        Objects.requireNonNull(world, "world cannot be null");
        return globalRegions.computeIfAbsent(world.getName(), k -> {
            GlobalRegionImpl globalRegion = new GlobalRegionImpl(world);
            registerRegionInternal(globalRegion);
            return globalRegion;
        });
    }

    // =========================================================================
    // Region Lookup
    // =========================================================================

    @Override
    @NotNull
    public Optional<Region> getRegion(@NotNull UnifiedWorld world, @NotNull String name) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        Map<String, Region> worldRegions = regionsByWorld.get(world.getName());
        if (worldRegions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(worldRegions.get(name.toLowerCase()));
    }

    @Override
    @NotNull
    public Optional<Region> getRegion(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        return Optional.ofNullable(regionsById.get(id));
    }

    @Override
    @NotNull
    public Collection<Region> getRegions(@NotNull UnifiedWorld world) {
        Objects.requireNonNull(world, "world cannot be null");

        Map<String, Region> worldRegions = regionsByWorld.get(world.getName());
        if (worldRegions == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(new ArrayList<>(worldRegions.values()));
    }

    @Override
    @NotNull
    public Set<Region> getRegions(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");

        UnifiedWorld world = location.world();
        if (world == null) {
            return Collections.emptySet();
        }

        Map<String, Region> worldRegions = regionsByWorld.get(world.getName());
        if (worldRegions == null) {
            return Collections.emptySet();
        }

        return worldRegions.values().stream()
            .filter(region -> region.contains(location))
            .sorted() // Sort by priority (descending)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @NotNull
    public Optional<Region> getPrimaryRegion(@NotNull UnifiedLocation location) {
        Set<Region> regions = getRegions(location);
        return regions.stream().findFirst();
    }

    @Override
    @NotNull
    public Collection<Region> getAllRegions() {
        return Collections.unmodifiableCollection(new ArrayList<>(regionsById.values()));
    }

    @Override
    public int getRegionCount(@NotNull UnifiedWorld world) {
        Objects.requireNonNull(world, "world cannot be null");

        Map<String, Region> worldRegions = regionsByWorld.get(world.getName());
        return worldRegions == null ? 0 : worldRegions.size();
    }

    @Override
    public int getTotalRegionCount() {
        return regionsById.size();
    }

    // =========================================================================
    // Region Checks
    // =========================================================================

    @Override
    public boolean isInRegion(@NotNull UnifiedLocation location, @NotNull String name) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        UnifiedWorld world = location.world();
        if (world == null) {
            return false;
        }

        return getRegion(world, name)
            .map(region -> region.contains(location))
            .orElse(false);
    }

    @Override
    public boolean isInAnyRegion(@NotNull UnifiedLocation location) {
        return !getRegions(location).isEmpty();
    }

    // =========================================================================
    // Flag Queries
    // =========================================================================

    @Override
    public <T> T queryFlag(
            @NotNull UnifiedLocation location,
            @NotNull RegionFlag<T> flag,
            @NotNull T defaultValue
    ) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(flag, "flag cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");

        Set<Region> regions = getRegions(location);

        // Check regions in priority order
        for (Region region : regions) {
            Optional<T> value = region.getFlag(flag);
            if (value.isPresent()) {
                return value.get();
            }
        }

        return defaultValue;
    }

    @Override
    public <T> T queryFlag(
            @NotNull UnifiedPlayer player,
            @NotNull RegionFlag<T> flag,
            @NotNull T defaultValue
    ) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(flag, "flag cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");

        // Check bypass permission
        String bypassPerm = bypassPermissions.get(flag);
        if (bypassPerm != null && player.hasPermission(bypassPerm)) {
            // Return the "allowed" default for bypassed flags
            return defaultValue;
        }

        return queryFlag(player.getLocation(), flag, defaultValue);
    }

    // =========================================================================
    // Region Management
    // =========================================================================

    @Override
    public void registerRegion(@NotNull Region region) {
        Objects.requireNonNull(region, "region cannot be null");

        String worldName = region.getWorld().getName();
        String regionName = region.getName().toLowerCase();

        // Check for duplicate names
        Map<String, Region> worldRegions = regionsByWorld.computeIfAbsent(
            worldName, k -> new ConcurrentHashMap<>()
        );

        Region existing = worldRegions.putIfAbsent(regionName, region);
        if (existing != null && !existing.getId().equals(region.getId())) {
            throw new IllegalArgumentException(
                "Region with name '" + region.getName() + "' already exists in world " + worldName
            );
        }

        regionsById.put(region.getId(), region);
    }

    private void registerRegionInternal(@NotNull Region region) {
        String worldName = region.getWorld().getName();
        String regionName = region.getName().toLowerCase();

        Map<String, Region> worldRegions = regionsByWorld.computeIfAbsent(
            worldName, k -> new ConcurrentHashMap<>()
        );
        worldRegions.put(regionName, region);
        regionsById.put(region.getId(), region);
    }

    @Override
    public boolean unregisterRegion(@NotNull Region region) {
        Objects.requireNonNull(region, "region cannot be null");

        String worldName = region.getWorld().getName();
        String regionName = region.getName().toLowerCase();

        Map<String, Region> worldRegions = regionsByWorld.get(worldName);
        if (worldRegions != null) {
            worldRegions.remove(regionName);
        }

        return regionsById.remove(region.getId()) != null;
    }

    @Override
    public boolean removeRegion(@NotNull UnifiedWorld world, @NotNull String name) {
        Optional<Region> region = getRegion(world, name);
        return region.map(this::unregisterRegion).orElse(false);
    }

    @Override
    public boolean removeRegion(@NotNull UUID id) {
        Optional<Region> region = getRegion(id);
        return region.map(this::unregisterRegion).orElse(false);
    }

    @Override
    public int clearRegions(@NotNull UnifiedWorld world) {
        Objects.requireNonNull(world, "world cannot be null");

        Map<String, Region> worldRegions = regionsByWorld.remove(world.getName());
        if (worldRegions == null) {
            return 0;
        }

        int count = worldRegions.size();
        for (Region region : worldRegions.values()) {
            regionsById.remove(region.getId());
        }

        // Also remove global region
        GlobalRegion global = globalRegions.remove(world.getName());
        if (global != null) {
            regionsById.remove(global.getId());
            count--;
        }

        return count;
    }

    // =========================================================================
    // Persistence
    // =========================================================================

    @Override
    @NotNull
    public CompletableFuture<Void> saveAll() {
        Collection<Region> nonTransient = regionsById.values().stream()
            .filter(r -> !r.isTransient())
            .toList();
        return storage.saveAll(nonTransient);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> save(@NotNull Region region) {
        Objects.requireNonNull(region, "region cannot be null");
        if (region.isTransient()) {
            return CompletableFuture.completedFuture(null);
        }
        return storage.save(region);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> reload() {
        // Clear all regions
        regionsByWorld.clear();
        regionsById.clear();
        globalRegions.clear();

        // Load from storage
        return storage.loadAll().thenAccept(regions -> {
            for (Region region : regions) {
                registerRegionInternal(region);
                if (region instanceof GlobalRegion global) {
                    globalRegions.put(region.getWorld().getName(), global);
                }
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> loadWorld(@NotNull UnifiedWorld world) {
        Objects.requireNonNull(world, "world cannot be null");

        return storage.loadAll(world).thenAccept(regions -> {
            for (Region region : regions) {
                registerRegionInternal(region);
                if (region instanceof GlobalRegion global) {
                    globalRegions.put(world.getName(), global);
                }
            }
        });
    }

    // =========================================================================
    // WorldGuard Integration
    // =========================================================================

    @Override
    public boolean isWorldGuardAvailable() {
        // TODO: Implement actual WorldGuard detection
        return false;
    }

    @Override
    @NotNull
    public Optional<Region> getWorldGuardRegion(@NotNull UnifiedWorld world, @NotNull String name) {
        // TODO: Implement WorldGuard region wrapping
        return Optional.empty();
    }

    @Override
    @NotNull
    public Collection<Region> getWorldGuardRegions(@NotNull UnifiedWorld world) {
        // TODO: Implement WorldGuard region listing
        return Collections.emptyList();
    }

    @Override
    public void setWorldGuardIntegration(boolean enabled) {
        this.worldGuardIntegration = enabled;
    }

    @Override
    public boolean isWorldGuardIntegrationEnabled() {
        return worldGuardIntegration && isWorldGuardAvailable();
    }

    // =========================================================================
    // Player Tracking
    // =========================================================================

    @Override
    @NotNull
    public Set<Region> getPlayerRegions(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");

        Set<Region> cached = playerRegions.get(player.getUniqueId());
        if (cached != null) {
            return Collections.unmodifiableSet(new HashSet<>(cached));
        }

        // Calculate current regions
        Set<Region> current = getRegions(player.getLocation());
        playerRegions.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());
        playerRegions.get(player.getUniqueId()).addAll(current);
        return Collections.unmodifiableSet(current);
    }

    @Override
    @NotNull
    public Collection<UnifiedPlayer> getPlayersInRegion(@NotNull Region region) {
        Objects.requireNonNull(region, "region cannot be null");

        return region.getWorld().getPlayers().stream()
            .filter(player -> region.contains(player.getLocation()))
            .toList();
    }

    @Override
    public int getPlayerCount(@NotNull Region region) {
        return (int) region.getWorld().getPlayers().stream()
            .filter(player -> region.contains(player.getLocation()))
            .count();
    }

    /**
     * Updates player region tracking.
     *
     * <p>Should be called on player movement to detect entry/exit events.
     *
     * @param player the player
     * @return the regions the player is now in
     */
    @NotNull
    public Set<Region> updatePlayerRegions(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");

        Set<Region> current = getRegions(player.getLocation());
        Set<Region> previous = playerRegions.put(player.getUniqueId(),
            ConcurrentHashMap.newKeySet());

        if (previous == null) {
            previous = Collections.emptySet();
        }

        playerRegions.get(player.getUniqueId()).addAll(current);
        return current;
    }

    /**
     * Clears player region tracking.
     *
     * <p>Should be called when a player disconnects.
     *
     * @param player the player UUID
     */
    public void clearPlayerRegions(@NotNull UUID player) {
        Objects.requireNonNull(player, "player cannot be null");
        playerRegions.remove(player);
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    @Override
    @NotNull
    public RegionStorage getStorage() {
        return storage;
    }

    @Override
    public void setStorage(@NotNull RegionStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
    }

    @Override
    @Nullable
    public String getBypassPermission(@NotNull RegionFlag<?> flag) {
        Objects.requireNonNull(flag, "flag cannot be null");
        return bypassPermissions.get(flag);
    }

    @Override
    public void setBypassPermission(@NotNull RegionFlag<?> flag, @Nullable String permission) {
        Objects.requireNonNull(flag, "flag cannot be null");
        if (permission == null) {
            bypassPermissions.remove(flag);
        } else {
            bypassPermissions.put(flag, permission);
        }
    }

    @Override
    public String getServiceName() {
        return "RegionService";
    }
}
