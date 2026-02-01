/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.platform.Platform;
import sh.pcx.unified.platform.PlatformProvider;
import sh.pcx.unified.platform.PlatformType;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * SPI provider for the Folia platform.
 *
 * <p>This class implements the {@link PlatformProvider} interface for Folia servers,
 * providing factory methods for creating platform-specific wrapper objects and
 * handling Folia's unique threading requirements.
 *
 * <h2>Registration</h2>
 * <p>This provider is registered via the Java ServiceLoader mechanism. Add the
 * following to {@code META-INF/services/sh.pcx.unified.platform.PlatformProvider}:
 * <pre>
 * sh.pcx.unified.platform.folia.FoliaPlatformProvider
 * </pre>
 *
 * <h2>Priority</h2>
 * <p>This provider has priority 100 to ensure it takes precedence over the
 * generic Paper provider when running on Folia.
 *
 * <h2>Thread Safety</h2>
 * <p>This provider is thread-safe. Wrapper caches use concurrent data structures
 * to allow safe access from multiple region threads.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlatformProvider
 * @see FoliaPlatform
 */
public final class FoliaPlatformProvider implements PlatformProvider {

    private static final Logger LOGGER = Logger.getLogger(FoliaPlatformProvider.class.getName());

    /**
     * The platform instance.
     */
    private FoliaPlatform platform;

    /**
     * The server instance.
     */
    private FoliaUnifiedServer server;

    /**
     * Cache for player wrappers.
     */
    private final Map<UUID, FoliaUnifiedPlayer> playerCache = new ConcurrentHashMap<>();

    /**
     * Cache for world wrappers.
     */
    private final Map<UUID, FoliaUnifiedWorld> worldCache = new ConcurrentHashMap<>();

    /**
     * Reference to the Bukkit server for delegating operations.
     */
    private Object bukkitServer;

    /**
     * Constructs a new FoliaPlatformProvider.
     *
     * @since 1.0.0
     */
    public FoliaPlatformProvider() {
        // Default constructor for ServiceLoader
    }

    @Override
    @NotNull
    public PlatformType getPlatformType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public int getPriority() {
        // Higher priority than Paper provider (100) since Folia is more specific
        return 150;
    }

    @Override
    public boolean isCompatible() {
        return FoliaDetector.isFolia();
    }

    @Override
    public void initialize() throws Exception {
        if (!FoliaDetector.isFolia()) {
            throw new IllegalStateException("FoliaPlatformProvider can only be used on Folia servers");
        }

        LOGGER.info("Initializing Folia platform provider...");

        // Get Bukkit server reference
        Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
        this.bukkitServer = bukkitClass.getMethod("getServer").invoke(null);

        // Create platform and server instances
        this.platform = new FoliaPlatform();
        this.server = new FoliaUnifiedServer(bukkitServer, this);

        LOGGER.info("Folia platform provider initialized successfully");
    }

    @Override
    public void shutdown() {
        LOGGER.info("Shutting down Folia platform provider...");

        // Clear caches
        playerCache.clear();
        worldCache.clear();

        // Cleanup references
        this.bukkitServer = null;

        LOGGER.info("Folia platform provider shutdown complete");
    }

    @Override
    @NotNull
    public Platform createPlatform() {
        if (platform == null) {
            throw new IllegalStateException("Platform not initialized. Call initialize() first.");
        }
        return platform;
    }

    @Override
    @NotNull
    public UnifiedServer createServer() {
        if (server == null) {
            throw new IllegalStateException("Server not initialized. Call initialize() first.");
        }
        return server;
    }

    @Override
    @NotNull
    public UnifiedPlayer wrapPlayer(@NotNull Object platformPlayer) {
        try {
            // Get UUID from Bukkit Player
            UUID uuid = (UUID) platformPlayer.getClass().getMethod("getUniqueId").invoke(platformPlayer);

            // Check cache first
            FoliaUnifiedPlayer cached = playerCache.get(uuid);
            if (cached != null && cached.isValid()) {
                return cached;
            }

            // Create new wrapper
            FoliaUnifiedPlayer player = new FoliaUnifiedPlayer(platformPlayer, this);
            playerCache.put(uuid, player);
            return player;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to wrap player: " + e.getMessage(), e);
        }
    }

    @Override
    @NotNull
    public OfflineUnifiedPlayer wrapOfflinePlayer(@NotNull Object platformOfflinePlayer) {
        try {
            return new FoliaOfflineUnifiedPlayer(platformOfflinePlayer, this);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to wrap offline player: " + e.getMessage(), e);
        }
    }

    @Override
    @NotNull
    public UnifiedWorld wrapWorld(@NotNull Object platformWorld) {
        try {
            // Get UUID from Bukkit World
            UUID uuid = (UUID) platformWorld.getClass().getMethod("getUID").invoke(platformWorld);

            // Check cache first
            FoliaUnifiedWorld cached = worldCache.get(uuid);
            if (cached != null) {
                return cached;
            }

            // Create new wrapper
            FoliaUnifiedWorld world = new FoliaUnifiedWorld(platformWorld, this);
            worldCache.put(uuid, world);
            return world;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to wrap world: " + e.getMessage(), e);
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        try {
            Method getPlayerMethod = bukkitServer.getClass().getMethod("getPlayer", UUID.class);
            Object player = getPlayerMethod.invoke(bukkitServer, uuid);
            if (player == null) {
                return Optional.empty();
            }
            return Optional.of(wrapPlayer(player));
        } catch (Exception e) {
            LOGGER.warning("Failed to get player by UUID: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        try {
            Method getPlayerMethod = bukkitServer.getClass().getMethod("getPlayer", String.class);
            Object player = getPlayerMethod.invoke(bukkitServer, name);
            if (player == null) {
                return Optional.empty();
            }
            return Optional.of(wrapPlayer(player));
        } catch (Exception e) {
            LOGGER.warning("Failed to get player by name: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        try {
            Method getWorldMethod = bukkitServer.getClass().getMethod("getWorld", String.class);
            Object world = getWorldMethod.invoke(bukkitServer, name);
            if (world == null) {
                return Optional.empty();
            }
            return Optional.of(wrapWorld(world));
        } catch (Exception e) {
            LOGGER.warning("Failed to get world by name: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        try {
            Method getWorldMethod = bukkitServer.getClass().getMethod("getWorld", UUID.class);
            Object world = getWorldMethod.invoke(bukkitServer, uuid);
            if (world == null) {
                return Optional.empty();
            }
            return Optional.of(wrapWorld(world));
        } catch (Exception e) {
            LOGGER.warning("Failed to get world by UUID: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void runOnMainThread(@NotNull Runnable task) {
        // On Folia, there is no single main thread
        // Run on the global region thread instead
        try {
            FoliaGlobalScheduler globalScheduler = new FoliaGlobalScheduler(bukkitServer);
            globalScheduler.run(task);
        } catch (Exception e) {
            LOGGER.warning("Failed to run on global thread: " + e.getMessage());
            // Fallback: just run it (may cause issues)
            task.run();
        }
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        try {
            // Use Folia's async scheduler
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object asyncScheduler = bukkitClass.getMethod("getAsyncScheduler").invoke(null);

            // Get plugin for scheduling
            Object plugin = getPlugin();
            if (plugin == null) {
                // Fallback to thread pool
                new Thread(task, "UnifiedAPI-Async").start();
                return;
            }

            // Schedule async task
            Method runNowMethod = asyncScheduler.getClass().getMethod("runNow",
                    Class.forName("org.bukkit.plugin.Plugin"),
                    Class.forName("java.util.function.Consumer"));

            runNowMethod.invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) t -> task.run());
        } catch (Exception e) {
            LOGGER.warning("Failed to run async: " + e.getMessage());
            new Thread(task, "UnifiedAPI-Async-Fallback").start();
        }
    }

    @Override
    public boolean isMainThread() {
        // On Folia, check if we're on the global tick thread
        return FoliaDetector.isGlobalTickThread();
    }

    /**
     * Returns the Bukkit server instance.
     *
     * @return the Bukkit server
     * @since 1.0.0
     */
    @NotNull
    public Object getBukkitServer() {
        if (bukkitServer == null) {
            throw new IllegalStateException("Server not initialized");
        }
        return bukkitServer;
    }

    /**
     * Invalidates a player from the cache.
     *
     * @param uuid the player's UUID
     * @since 1.0.0
     */
    public void invalidatePlayer(@NotNull UUID uuid) {
        playerCache.remove(uuid);
    }

    /**
     * Invalidates a world from the cache.
     *
     * @param uuid the world's UUID
     * @since 1.0.0
     */
    public void invalidateWorld(@NotNull UUID uuid) {
        worldCache.remove(uuid);
    }

    /**
     * Gets a plugin instance for scheduler operations.
     */
    private Object getPlugin() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object pluginManager = bukkitClass.getMethod("getPluginManager").invoke(null);
            Object[] plugins = (Object[]) pluginManager.getClass().getMethod("getPlugins").invoke(pluginManager);

            // Try to find UnifiedPluginAPI, or return first plugin
            for (Object plugin : plugins) {
                String name = (String) plugin.getClass().getMethod("getName").invoke(plugin);
                if ("UnifiedPluginAPI".equals(name)) {
                    return plugin;
                }
            }

            return plugins.length > 0 ? plugins[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
}
