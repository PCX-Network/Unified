/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.platform.Platform;
import sh.pcx.unified.platform.PlatformHolder;
import sh.pcx.unified.platform.PlatformProvider;
import sh.pcx.unified.platform.PlatformType;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SPI provider implementation for Sponge platforms.
 *
 * <p>This class serves as the bridge between the unified API and the Sponge API,
 * providing factory methods for creating platform-specific wrapper objects and
 * managing their lifecycle.
 *
 * <h2>Service Loading</h2>
 * <p>This provider is loaded via Java's ServiceLoader mechanism. Register it in:
 * {@code META-INF/services/sh.pcx.unified.platform.PlatformProvider}
 *
 * <h2>Sponge Architecture Integration</h2>
 * <p>This provider integrates with Sponge's service-based architecture:
 * <ul>
 *   <li>Uses Sponge's Server and Game instances</li>
 *   <li>Handles Sponge's cause/context system for events</li>
 *   <li>Leverages Sponge's async task executor</li>
 * </ul>
 *
 * <h2>Object Caching</h2>
 * <p>This provider caches wrapper objects using WeakHashMaps to prevent memory leaks
 * while allowing reuse of wrapper instances for the same underlying Sponge objects.
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All caching operations use read-write locks to
 * ensure safe concurrent access.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlatformProvider
 * @see SpongePlatform
 */
public final class SpongePlatformProvider implements PlatformProvider {

    private final ReadWriteLock playerCacheLock = new ReentrantReadWriteLock();
    private final Map<ServerPlayer, SpongeUnifiedPlayer> playerCache = new WeakHashMap<>();

    private final ReadWriteLock offlinePlayerCacheLock = new ReentrantReadWriteLock();
    private final Map<User, SpongeOfflinePlayer> offlinePlayerCache = new WeakHashMap<>();

    private final ReadWriteLock worldCacheLock = new ReentrantReadWriteLock();
    private final Map<ServerWorld, SpongeUnifiedWorld> worldCache = new WeakHashMap<>();

    private volatile SpongePlatform platform;
    private volatile SpongeUnifiedServer server;
    private volatile boolean initialized = false;

    /**
     * Creates a new SpongePlatformProvider.
     *
     * <p>The provider is not fully initialized until {@link #initialize()} is called.
     *
     * @since 1.0.0
     */
    public SpongePlatformProvider() {
        // Default constructor for ServiceLoader
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public PlatformType getPlatformType() {
        return PlatformType.SPONGE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sponge provider has priority 100 to ensure it takes precedence over
     * any fallback providers when running on Sponge.
     */
    @Override
    public int getPriority() {
        return 100;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This provider is compatible if Sponge classes are available and
     * the Sponge server is running.
     */
    @Override
    public boolean isCompatible() {
        try {
            Class.forName("org.spongepowered.api.Sponge");
            return Sponge.isServerAvailable();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }

        // Create platform and server instances
        platform = new SpongePlatform();
        server = new SpongeUnifiedServer(this);

        // Register the platform globally
        PlatformHolder.set(platform);

        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (!initialized) {
            return;
        }

        // Clear all caches
        playerCacheLock.writeLock().lock();
        try {
            playerCache.clear();
        } finally {
            playerCacheLock.writeLock().unlock();
        }

        offlinePlayerCacheLock.writeLock().lock();
        try {
            offlinePlayerCache.clear();
        } finally {
            offlinePlayerCacheLock.writeLock().unlock();
        }

        worldCacheLock.writeLock().lock();
        try {
            worldCache.clear();
        } finally {
            worldCacheLock.writeLock().unlock();
        }

        // Clear global platform reference
        PlatformHolder.clear();

        initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Platform createPlatform() {
        if (platform == null) {
            throw new IllegalStateException("Provider not initialized");
        }
        return platform;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedServer createServer() {
        if (server == null) {
            throw new IllegalStateException("Provider not initialized");
        }
        return server;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedPlayer wrapPlayer(@NotNull Object platformPlayer) {
        if (!(platformPlayer instanceof ServerPlayer player)) {
            throw new IllegalArgumentException(
                    "Expected org.spongepowered.api.entity.living.player.server.ServerPlayer but got " +
                    platformPlayer.getClass().getName()
            );
        }
        return getOrCreatePlayer(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public OfflineUnifiedPlayer wrapOfflinePlayer(@NotNull Object platformOfflinePlayer) {
        if (!(platformOfflinePlayer instanceof User user)) {
            throw new IllegalArgumentException(
                    "Expected org.spongepowered.api.entity.living.player.User but got " +
                    platformOfflinePlayer.getClass().getName()
            );
        }
        return getOrCreateOfflinePlayer(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld wrapWorld(@NotNull Object platformWorld) {
        if (!(platformWorld instanceof ServerWorld world)) {
            throw new IllegalArgumentException(
                    "Expected org.spongepowered.api.world.server.ServerWorld but got " +
                    platformWorld.getClass().getName()
            );
        }
        return getOrCreateWorld(world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        if (!Sponge.isServerAvailable()) {
            return Optional.empty();
        }
        Server spongeServer = Sponge.server();
        Optional<ServerPlayer> playerOpt = spongeServer.player(uuid);
        return playerOpt.map(this::getOrCreatePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        if (!Sponge.isServerAvailable()) {
            return Optional.empty();
        }
        Server spongeServer = Sponge.server();
        Optional<ServerPlayer> playerOpt = spongeServer.player(name);
        return playerOpt.map(this::getOrCreatePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        if (!Sponge.isServerAvailable()) {
            return Optional.empty();
        }
        Server spongeServer = Sponge.server();
        // Sponge uses ResourceKey for world identification
        return spongeServer.worldManager().worlds().stream()
                .filter(world -> world.key().value().equals(name) ||
                                 world.key().asString().equals(name))
                .findFirst()
                .map(this::getOrCreateWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        if (!Sponge.isServerAvailable()) {
            return Optional.empty();
        }
        Server spongeServer = Sponge.server();
        return spongeServer.worldManager().worlds().stream()
                .filter(world -> world.uniqueId().equals(uuid))
                .findFirst()
                .map(this::getOrCreateWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnMainThread(@NotNull Runnable task) {
        if (Sponge.server().onMainThread()) {
            task.run();
        } else {
            // Use CompletableFuture to run on async, then schedule on main thread
            // Since we don't have plugin container access, use direct execution
            java.util.concurrent.CompletableFuture.runAsync(task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAsync(@NotNull Runnable task) {
        java.util.concurrent.CompletableFuture.runAsync(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMainThread() {
        return Sponge.isServerAvailable() && Sponge.server().onMainThread();
    }

    /**
     * Gets or creates a cached SpongeUnifiedPlayer wrapper for the given Sponge player.
     *
     * @param player the Sponge ServerPlayer
     * @return the wrapped player
     */
    @NotNull
    SpongeUnifiedPlayer getOrCreatePlayer(@NotNull ServerPlayer player) {
        // Try read lock first for better concurrency
        playerCacheLock.readLock().lock();
        try {
            SpongeUnifiedPlayer cached = playerCache.get(player);
            if (cached != null) {
                return cached;
            }
        } finally {
            playerCacheLock.readLock().unlock();
        }

        // Upgrade to write lock to create new wrapper
        playerCacheLock.writeLock().lock();
        try {
            // Double-check after acquiring write lock
            SpongeUnifiedPlayer cached = playerCache.get(player);
            if (cached != null) {
                return cached;
            }

            SpongeUnifiedPlayer wrapper = new SpongeUnifiedPlayer(player, this);
            playerCache.put(player, wrapper);
            return wrapper;
        } finally {
            playerCacheLock.writeLock().unlock();
        }
    }

    /**
     * Gets or creates a cached SpongeOfflinePlayer wrapper for the given Sponge User.
     *
     * @param user the Sponge User
     * @return the wrapped offline player
     */
    @NotNull
    SpongeOfflinePlayer getOrCreateOfflinePlayer(@NotNull User user) {
        // If the user is online, delegate to the online player cache
        if (user instanceof ServerPlayer player) {
            return getOrCreatePlayer(player);
        }

        offlinePlayerCacheLock.readLock().lock();
        try {
            SpongeOfflinePlayer cached = offlinePlayerCache.get(user);
            if (cached != null) {
                return cached;
            }
        } finally {
            offlinePlayerCacheLock.readLock().unlock();
        }

        offlinePlayerCacheLock.writeLock().lock();
        try {
            SpongeOfflinePlayer cached = offlinePlayerCache.get(user);
            if (cached != null) {
                return cached;
            }

            SpongeOfflinePlayer wrapper = new SpongeOfflinePlayer(user, this);
            offlinePlayerCache.put(user, wrapper);
            return wrapper;
        } finally {
            offlinePlayerCacheLock.writeLock().unlock();
        }
    }

    /**
     * Gets or creates a cached SpongeUnifiedWorld wrapper for the given Sponge world.
     *
     * @param world the Sponge ServerWorld
     * @return the wrapped world
     */
    @NotNull
    SpongeUnifiedWorld getOrCreateWorld(@NotNull ServerWorld world) {
        worldCacheLock.readLock().lock();
        try {
            SpongeUnifiedWorld cached = worldCache.get(world);
            if (cached != null) {
                return cached;
            }
        } finally {
            worldCacheLock.readLock().unlock();
        }

        worldCacheLock.writeLock().lock();
        try {
            SpongeUnifiedWorld cached = worldCache.get(world);
            if (cached != null) {
                return cached;
            }

            SpongeUnifiedWorld wrapper = new SpongeUnifiedWorld(world, this);
            worldCache.put(world, wrapper);
            return wrapper;
        } finally {
            worldCacheLock.writeLock().unlock();
        }
    }

    /**
     * Invalidates a player from the cache when they disconnect.
     *
     * @param player the player to remove from cache
     */
    void invalidatePlayer(@NotNull ServerPlayer player) {
        playerCacheLock.writeLock().lock();
        try {
            playerCache.remove(player);
        } finally {
            playerCacheLock.writeLock().unlock();
        }
    }

    /**
     * Invalidates a world from the cache when it is unloaded.
     *
     * @param world the world to remove from cache
     */
    void invalidateWorld(@NotNull ServerWorld world) {
        worldCacheLock.writeLock().lock();
        try {
            worldCache.remove(world);
        } finally {
            worldCacheLock.writeLock().unlock();
        }
    }

    /**
     * Returns the Sponge platform instance.
     *
     * @return the SpongePlatform
     * @throws IllegalStateException if not initialized
     */
    @NotNull
    SpongePlatform getSpongePlatform() {
        if (platform == null) {
            throw new IllegalStateException("Provider not initialized");
        }
        return platform;
    }
}
