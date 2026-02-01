/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Folia-aware plugin base class.
 *
 * <p>This class extends {@link UnifiedPlugin} to provide Folia-specific
 * functionality, particularly around region-aware task scheduling. Plugins
 * targeting Folia should extend this class instead of UnifiedPlugin directly.
 *
 * <h2>Threading Considerations</h2>
 * <p>Folia uses a multi-threaded model where different world regions are
 * processed by different threads. This means:
 * <ul>
 *   <li>Traditional main-thread scheduling doesn't work</li>
 *   <li>Entity/block operations must happen on the correct region thread</li>
 *   <li>Cross-region communication requires proper scheduling</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyFoliaPlugin extends FoliaUnifiedPlugin {
 *
 *     @Override
 *     public void onEnable() {
 *         getLogger().info("Plugin enabled on Folia!");
 *
 *         // Schedule a task at a specific location
 *         runAtLocation(someLocation, () -> {
 *             // This runs on the region that owns the location
 *             someLocation.world().getBlockAt(someLocation)
 *                     .setType(Material.STONE);
 *         });
 *
 *         // Schedule a task for a specific entity
 *         runAtEntity(player, () -> {
 *             // This runs on the region that owns the player
 *             player.sendMessage("Hello!");
 *         });
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         getLogger().info("Plugin disabled!");
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlugin
 * @see FoliaRegionScheduler
 */
public abstract class FoliaUnifiedPlugin extends UnifiedPlugin {

    /**
     * The platform-specific Bukkit plugin instance.
     */
    private Object bukkitPlugin;

    /**
     * The region scheduler for location-based tasks.
     */
    private FoliaRegionScheduler regionScheduler;

    /**
     * The entity scheduler for entity-bound tasks.
     */
    private FoliaEntityScheduler entityScheduler;

    /**
     * The global scheduler for server-wide tasks.
     */
    private FoliaGlobalScheduler globalScheduler;

    /**
     * Constructs a new FoliaUnifiedPlugin.
     *
     * @since 1.0.0
     */
    protected FoliaUnifiedPlugin() {
        // Subclasses use default constructor
    }

    /**
     * Initializes the Folia-specific components.
     *
     * <p>This method should be called by the platform adapter after
     * the base initialization is complete.
     *
     * @param bukkitPlugin the underlying Bukkit plugin
     * @since 1.0.0
     */
    public final void initializeFolia(@NotNull Object bukkitPlugin) {
        this.bukkitPlugin = bukkitPlugin;

        try {
            // Initialize schedulers
            this.regionScheduler = new FoliaRegionScheduler(bukkitPlugin);
            this.entityScheduler = new FoliaEntityScheduler(bukkitPlugin);
            this.globalScheduler = new FoliaGlobalScheduler(bukkitPlugin);

            getLogger().info("Folia schedulers initialized");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize Folia schedulers", e);
            throw new RuntimeException("Folia scheduler initialization failed", e);
        }
    }

    /**
     * Runs a task on the region that owns the specified location.
     *
     * <p>The task will be executed on the thread that owns the region
     * containing the specified location. This is the safe way to modify
     * blocks or spawn entities at a location in Folia.
     *
     * @param location the location determining which region thread to use
     * @param task the task to run
     * @return a future that completes when the task is scheduled
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAtLocation(@NotNull UnifiedLocation location, @NotNull Runnable task) {
        if (regionScheduler == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Folia not initialized")
            );
        }
        return regionScheduler.runAt(location, task);
    }

    /**
     * Runs a task on the region that owns the specified location with a delay.
     *
     * @param location the location determining which region thread to use
     * @param task the task to run
     * @param delayTicks the delay in ticks before executing
     * @return a future that completes when the task is scheduled
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAtLocationLater(@NotNull UnifiedLocation location,
                                                       @NotNull Runnable task,
                                                       long delayTicks) {
        if (regionScheduler == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Folia not initialized")
            );
        }
        return regionScheduler.runAtLater(location, task, delayTicks);
    }

    /**
     * Runs a task on the thread that owns the specified entity.
     *
     * <p>The task will be executed on the thread that owns the region
     * containing the entity. This is the safe way to modify entity
     * properties or perform entity operations in Folia.
     *
     * @param entity the entity (platform-specific object)
     * @param task the task to run
     * @param retired callback if entity is removed before task runs
     * @return true if the task was scheduled
     * @since 1.0.0
     */
    public boolean runAtEntity(@NotNull Object entity,
                               @NotNull Runnable task,
                               @Nullable Runnable retired) {
        if (entityScheduler == null) {
            return false;
        }
        return entityScheduler.run(entity, task, retired);
    }

    /**
     * Runs a task on the thread that owns the specified entity.
     *
     * @param entity the entity (platform-specific object)
     * @param task the task to run
     * @return true if the task was scheduled
     * @since 1.0.0
     */
    public boolean runAtEntity(@NotNull Object entity, @NotNull Runnable task) {
        return runAtEntity(entity, task, null);
    }

    /**
     * Runs a task on the global region thread.
     *
     * <p>The global region is used for tasks that don't belong to any
     * specific world location. Use this sparingly as it can become a
     * bottleneck if overused.
     *
     * @param task the task to run
     * @return a future that completes when the task is done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runGlobal(@NotNull Runnable task) {
        if (globalScheduler == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Folia not initialized")
            );
        }
        return globalScheduler.run(task);
    }

    /**
     * Runs a task on the global region thread with a delay.
     *
     * @param task the task to run
     * @param delayTicks the delay in ticks
     * @return a future that completes when the task is done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runGlobalLater(@NotNull Runnable task, long delayTicks) {
        if (globalScheduler == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Folia not initialized")
            );
        }
        return globalScheduler.runLater(task, delayTicks);
    }

    /**
     * Runs a task asynchronously.
     *
     * <p>Async tasks run on a thread pool and should not access
     * Bukkit API or modify game state directly.
     *
     * @param task the task to run
     * @return a future that completes when the task is done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAsync(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task);
    }

    /**
     * Checks if the current thread owns the region for a location.
     *
     * @param location the location to check
     * @return true if safe to modify this location on current thread
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread(@NotNull UnifiedLocation location) {
        return RegionContext.of(location).isOwnedByCurrentThread();
    }

    /**
     * Creates a region context for the specified location.
     *
     * @param location the location
     * @return a region context for scheduling
     * @since 1.0.0
     */
    @NotNull
    public RegionContext getRegionContext(@NotNull UnifiedLocation location) {
        return RegionContext.of(location);
    }

    /**
     * Returns the region scheduler.
     *
     * @return the region scheduler
     * @since 1.0.0
     */
    @NotNull
    public FoliaRegionScheduler getRegionScheduler() {
        if (regionScheduler == null) {
            throw new IllegalStateException("Folia not initialized");
        }
        return regionScheduler;
    }

    /**
     * Returns the entity scheduler.
     *
     * @return the entity scheduler
     * @since 1.0.0
     */
    @NotNull
    public FoliaEntityScheduler getEntityScheduler() {
        if (entityScheduler == null) {
            throw new IllegalStateException("Folia not initialized");
        }
        return entityScheduler;
    }

    /**
     * Returns the global scheduler.
     *
     * @return the global scheduler
     * @since 1.0.0
     */
    @NotNull
    public FoliaGlobalScheduler getGlobalScheduler() {
        if (globalScheduler == null) {
            throw new IllegalStateException("Folia not initialized");
        }
        return globalScheduler;
    }

    /**
     * Returns the underlying Bukkit plugin.
     *
     * @param <T> the plugin type
     * @return the Bukkit plugin
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getBukkitPlugin() {
        if (bukkitPlugin == null) {
            throw new IllegalStateException("Bukkit plugin not initialized");
        }
        return (T) bukkitPlugin;
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be empty");
        }

        Path dataFolder = getDataFolder();
        Path outFile = dataFolder.resolve(resourcePath);

        try {
            // Create parent directories if needed
            Files.createDirectories(outFile.getParent());

            // Check if file exists and we shouldn't replace
            if (Files.exists(outFile) && !replace) {
                return;
            }

            // Copy resource from JAR
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    throw new IllegalArgumentException(
                            "Resource not found: " + resourcePath
                    );
                }
                Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save resource: " + resourcePath, e);
        }
    }

    /**
     * Checks if this plugin is running on Folia.
     *
     * <p>This method always returns true for FoliaUnifiedPlugin instances.
     *
     * @return true
     * @since 1.0.0
     */
    public final boolean isFolia() {
        return true;
    }
}
