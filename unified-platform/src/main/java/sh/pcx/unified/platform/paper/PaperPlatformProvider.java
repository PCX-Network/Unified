/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.platform.Platform;
import sh.pcx.unified.platform.PlatformHolder;
import sh.pcx.unified.platform.PlatformProvider;
import sh.pcx.unified.platform.PlatformType;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SPI provider implementation for Paper/Spigot platforms.
 *
 * <p>This class serves as the bridge between the unified API and the Bukkit API,
 * providing factory methods for creating platform-specific wrapper objects and
 * managing their lifecycle.
 *
 * <h2>Service Loading</h2>
 * <p>This provider is loaded via Java's ServiceLoader mechanism. Register it in:
 * {@code META-INF/services/sh.pcx.unified.platform.PlatformProvider}
 *
 * <h2>Object Caching</h2>
 * <p>This provider caches wrapper objects using WeakHashMaps to prevent memory leaks
 * while allowing reuse of wrapper instances for the same underlying Bukkit objects.
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All caching operations use read-write locks to
 * ensure safe concurrent access.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlatformProvider
 * @see PaperPlatform
 */
public final class PaperPlatformProvider implements PlatformProvider {

    private final ReadWriteLock playerCacheLock = new ReentrantReadWriteLock();
    private final Map<Player, PaperUnifiedPlayer> playerCache = new WeakHashMap<>();

    private final ReadWriteLock offlinePlayerCacheLock = new ReentrantReadWriteLock();
    private final Map<OfflinePlayer, PaperOfflinePlayer> offlinePlayerCache = new WeakHashMap<>();

    private final ReadWriteLock worldCacheLock = new ReentrantReadWriteLock();
    private final Map<World, PaperUnifiedWorld> worldCache = new WeakHashMap<>();

    private volatile PaperPlatform platform;
    private volatile PaperUnifiedServer server;
    private volatile boolean initialized = false;

    /**
     * Creates a new PaperPlatformProvider.
     *
     * <p>The provider is not fully initialized until {@link #initialize()} is called.
     *
     * @since 1.0.0
     */
    public PaperPlatformProvider() {
        // Default constructor for ServiceLoader
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public PlatformType getPlatformType() {
        return PlatformType.BUKKIT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Paper provider has priority 100 to ensure it takes precedence over
     * any fallback providers.
     */
    @Override
    public int getPriority() {
        return 100;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This provider is compatible if Bukkit classes are available.
     */
    @Override
    public boolean isCompatible() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return Bukkit.getServer() != null;
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
        platform = new PaperPlatform();
        server = new PaperUnifiedServer(this);

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
        if (!(platformPlayer instanceof Player player)) {
            throw new IllegalArgumentException(
                    "Expected org.bukkit.entity.Player but got " +
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
        if (!(platformOfflinePlayer instanceof OfflinePlayer offlinePlayer)) {
            throw new IllegalArgumentException(
                    "Expected org.bukkit.OfflinePlayer but got " +
                    platformOfflinePlayer.getClass().getName()
            );
        }
        return getOrCreateOfflinePlayer(offlinePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld wrapWorld(@NotNull Object platformWorld) {
        if (!(platformWorld instanceof World world)) {
            throw new IllegalArgumentException(
                    "Expected org.bukkit.World but got " +
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
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(getOrCreatePlayer(player));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(getOrCreatePlayer(player));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(getOrCreateWorld(world));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        World world = Bukkit.getWorld(uuid);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(getOrCreateWorld(world));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnMainThread(@NotNull Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugins()[0],
                    task
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugins()[0],
                task
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    /**
     * Gets or creates a cached PaperUnifiedPlayer wrapper for the given Bukkit player.
     *
     * @param player the Bukkit player
     * @return the wrapped player
     */
    @NotNull
    PaperUnifiedPlayer getOrCreatePlayer(@NotNull Player player) {
        // Try read lock first for better concurrency
        playerCacheLock.readLock().lock();
        try {
            PaperUnifiedPlayer cached = playerCache.get(player);
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
            PaperUnifiedPlayer cached = playerCache.get(player);
            if (cached != null) {
                return cached;
            }

            PaperUnifiedPlayer wrapper = new PaperUnifiedPlayer(player, this);
            playerCache.put(player, wrapper);
            return wrapper;
        } finally {
            playerCacheLock.writeLock().unlock();
        }
    }

    /**
     * Gets or creates a cached PaperOfflinePlayer wrapper for the given Bukkit offline player.
     *
     * @param offlinePlayer the Bukkit offline player
     * @return the wrapped offline player
     */
    @NotNull
    PaperOfflinePlayer getOrCreateOfflinePlayer(@NotNull OfflinePlayer offlinePlayer) {
        // If the player is online, return the online wrapper
        if (offlinePlayer instanceof Player player) {
            return getOrCreatePlayer(player);
        }

        offlinePlayerCacheLock.readLock().lock();
        try {
            PaperOfflinePlayer cached = offlinePlayerCache.get(offlinePlayer);
            if (cached != null) {
                return cached;
            }
        } finally {
            offlinePlayerCacheLock.readLock().unlock();
        }

        offlinePlayerCacheLock.writeLock().lock();
        try {
            PaperOfflinePlayer cached = offlinePlayerCache.get(offlinePlayer);
            if (cached != null) {
                return cached;
            }

            PaperOfflinePlayer wrapper = new PaperOfflinePlayer(offlinePlayer, this);
            offlinePlayerCache.put(offlinePlayer, wrapper);
            return wrapper;
        } finally {
            offlinePlayerCacheLock.writeLock().unlock();
        }
    }

    /**
     * Gets or creates a cached PaperUnifiedWorld wrapper for the given Bukkit world.
     *
     * @param world the Bukkit world
     * @return the wrapped world
     */
    @NotNull
    PaperUnifiedWorld getOrCreateWorld(@NotNull World world) {
        worldCacheLock.readLock().lock();
        try {
            PaperUnifiedWorld cached = worldCache.get(world);
            if (cached != null) {
                return cached;
            }
        } finally {
            worldCacheLock.readLock().unlock();
        }

        worldCacheLock.writeLock().lock();
        try {
            PaperUnifiedWorld cached = worldCache.get(world);
            if (cached != null) {
                return cached;
            }

            PaperUnifiedWorld wrapper = new PaperUnifiedWorld(world, this);
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
    void invalidatePlayer(@NotNull Player player) {
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
    void invalidateWorld(@NotNull World world) {
        worldCacheLock.writeLock().lock();
        try {
            worldCache.remove(world);
        } finally {
            worldCacheLock.writeLock().unlock();
        }
    }
}
