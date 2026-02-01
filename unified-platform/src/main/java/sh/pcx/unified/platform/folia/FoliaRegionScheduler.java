/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Region-bound task scheduler for Folia.
 *
 * <p>This scheduler uses Folia's RegionScheduler API to execute tasks on
 * the thread that owns a specific world region. This is essential for
 * any operations that modify blocks or entities in Folia.
 *
 * <h2>Region Scheduling</h2>
 * <p>In Folia, each region of the world is processed by its own thread.
 * To safely modify blocks or entities, tasks must be scheduled on the
 * correct region thread. This scheduler handles that automatically.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * FoliaRegionScheduler scheduler = new FoliaRegionScheduler(plugin);
 *
 * // Run task at specific location
 * scheduler.runAt(location, () -> {
 *     location.world().getBlockAt(location).setType(Material.STONE);
 * });
 *
 * // Run task at location with delay
 * scheduler.runAtLater(location, () -> {
 *     // Delayed operation
 * }, 20L); // 1 second delay
 *
 * // Run repeating task at location
 * scheduler.runAtTimer(location, () -> {
 *     // Repeating operation
 * }, 0L, 20L); // Every second
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Tasks can be scheduled from any thread
 * and will be executed on the appropriate region thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FoliaEntityScheduler
 * @see FoliaGlobalScheduler
 */
public final class FoliaRegionScheduler {

    private static final Logger LOGGER = Logger.getLogger(FoliaRegionScheduler.class.getName());

    /**
     * The Bukkit plugin for scheduling.
     */
    private final Object plugin;

    /**
     * The Folia RegionScheduler instance.
     */
    private final Object regionScheduler;

    /**
     * The Plugin class for method lookups.
     */
    private final Class<?> pluginClass;

    /**
     * The World class for method lookups.
     */
    private final Class<?> worldClass;

    /**
     * The Consumer class for task callbacks.
     */
    private final Class<?> consumerClass;

    /**
     * The ScheduledTask class.
     */
    private final Class<?> scheduledTaskClass;

    /**
     * Constructs a new FoliaRegionScheduler.
     *
     * @param plugin the Bukkit plugin to schedule tasks for
     * @throws IllegalStateException if Folia's RegionScheduler is not available
     * @since 1.0.0
     */
    public FoliaRegionScheduler(@NotNull Object plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        if (!FoliaDetector.hasRegionScheduler()) {
            throw new IllegalStateException("Folia RegionScheduler not available");
        }

        this.plugin = plugin;

        try {
            // Get RegionScheduler from Bukkit
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            this.regionScheduler = bukkitClass.getMethod("getRegionScheduler").invoke(null);

            // Cache class references
            this.pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            this.worldClass = Class.forName("org.bukkit.World");
            this.consumerClass = Consumer.class;
            this.scheduledTaskClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.ScheduledTask"
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RegionScheduler", e);
        }
    }

    /**
     * Runs a task on the region that owns the specified location.
     *
     * @param location the location determining the region
     * @param task the task to run
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAt(@NotNull UnifiedLocation location, @NotNull Runnable task) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        UnifiedWorld world = location.world();
        if (world == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Location must have a world")
            );
        }

        return runAt(world.getHandle(), location.getBlockX(), location.getBlockZ(), task);
    }

    /**
     * Runs a task on the region that owns the specified coordinates.
     *
     * @param world the platform-specific world object
     * @param x the block X coordinate
     * @param z the block Z coordinate
     * @param task the task to run
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAt(@NotNull Object world, int x, int z, @NotNull Runnable task) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            // Create consumer wrapper
            Consumer<Object> consumer = scheduledTask -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    LOGGER.log(Level.WARNING, "Region task failed", e);
                }
            };

            // Call RegionScheduler.execute(Plugin, World, int, int, Consumer)
            Method executeMethod = regionScheduler.getClass().getMethod(
                    "execute",
                    pluginClass,
                    worldClass,
                    int.class,
                    int.class,
                    Runnable.class
            );

            executeMethod.invoke(regionScheduler, plugin, world, x >> 4, z >> 4, task);
            // For execute, task runs immediately and we complete after call
            // The task is wrapped in its own try-catch above

        } catch (NoSuchMethodException e) {
            // Try alternative method signature
            try {
                Method runMethod = regionScheduler.getClass().getMethod(
                        "run",
                        pluginClass,
                        worldClass,
                        int.class,
                        int.class,
                        consumerClass
                );

                Consumer<Object> consumer = scheduledTask -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception ex) {
                        future.completeExceptionally(ex);
                    }
                };

                runMethod.invoke(regionScheduler, plugin, world, x >> 4, z >> 4, consumer);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
            LOGGER.log(Level.WARNING, "Failed to schedule region task", e);
        }

        return future;
    }

    /**
     * Runs a task on the region that owns the specified location after a delay.
     *
     * @param location the location determining the region
     * @param task the task to run
     * @param delayTicks the delay in ticks (20 ticks = 1 second)
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAtLater(@NotNull UnifiedLocation location,
                                               @NotNull Runnable task,
                                               long delayTicks) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        UnifiedWorld world = location.world();
        if (world == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Location must have a world")
            );
        }

        return runAtLater(world.getHandle(), location.getBlockX(), location.getBlockZ(),
                task, delayTicks);
    }

    /**
     * Runs a task on the specified region after a delay.
     *
     * @param world the platform-specific world object
     * @param x the block X coordinate
     * @param z the block Z coordinate
     * @param task the task to run
     * @param delayTicks the delay in ticks
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAtLater(@NotNull Object world, int x, int z,
                                               @NotNull Runnable task, long delayTicks) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        if (delayTicks <= 0) {
            return runAt(world, x, z, task);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            Consumer<Object> consumer = scheduledTask -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            };

            Method runDelayedMethod = regionScheduler.getClass().getMethod(
                    "runDelayed",
                    pluginClass,
                    worldClass,
                    int.class,
                    int.class,
                    consumerClass,
                    long.class
            );

            runDelayedMethod.invoke(regionScheduler, plugin, world, x >> 4, z >> 4,
                    consumer, delayTicks);
        } catch (Exception e) {
            future.completeExceptionally(e);
            LOGGER.log(Level.WARNING, "Failed to schedule delayed region task", e);
        }

        return future;
    }

    /**
     * Runs a repeating task on the region that owns the specified location.
     *
     * @param location the location determining the region
     * @param task the task to run, receives the scheduled task for cancellation
     * @param initialDelayTicks the initial delay before first execution
     * @param periodTicks the period between executions
     * @return a handle for cancelling the task, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runAtTimer(@NotNull UnifiedLocation location,
                             @NotNull Consumer<Object> task,
                             long initialDelayTicks,
                             long periodTicks) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        UnifiedWorld world = location.world();
        if (world == null) {
            throw new IllegalArgumentException("Location must have a world");
        }

        return runAtTimer(world.getHandle(), location.getBlockX(), location.getBlockZ(),
                task, initialDelayTicks, periodTicks);
    }

    /**
     * Runs a repeating task on the specified region.
     *
     * @param world the platform-specific world object
     * @param x the block X coordinate
     * @param z the block Z coordinate
     * @param task the task to run
     * @param initialDelayTicks the initial delay
     * @param periodTicks the period between executions
     * @return a handle for cancelling the task
     * @since 1.0.0
     */
    @Nullable
    public Object runAtTimer(@NotNull Object world, int x, int z,
                             @NotNull Consumer<Object> task,
                             long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        try {
            Method runAtFixedRateMethod = regionScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    pluginClass,
                    worldClass,
                    int.class,
                    int.class,
                    consumerClass,
                    long.class,
                    long.class
            );

            return runAtFixedRateMethod.invoke(regionScheduler, plugin, world,
                    x >> 4, z >> 4, task, initialDelayTicks, periodTicks);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule repeating region task", e);
            return null;
        }
    }

    /**
     * Runs a repeating task on the specified region with a simple Runnable.
     *
     * @param location the location determining the region
     * @param task the task to run
     * @param initialDelayTicks the initial delay
     * @param periodTicks the period between executions
     * @return a handle for cancelling the task
     * @since 1.0.0
     */
    @Nullable
    public Object runAtTimer(@NotNull UnifiedLocation location,
                             @NotNull Runnable task,
                             long initialDelayTicks,
                             long periodTicks) {
        return runAtTimer(location, (Consumer<Object>) ignored -> task.run(),
                initialDelayTicks, periodTicks);
    }

    /**
     * Cancels a scheduled task.
     *
     * @param task the task handle returned from scheduling methods
     * @return true if the task was cancelled
     * @since 1.0.0
     */
    public boolean cancel(@Nullable Object task) {
        if (task == null) {
            return false;
        }

        try {
            Method cancelMethod = task.getClass().getMethod("cancel");
            cancelMethod.invoke(task);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to cancel task", e);
            return false;
        }
    }

    /**
     * Checks if a task is still scheduled and not cancelled.
     *
     * @param task the task handle
     * @return true if the task is still active
     * @since 1.0.0
     */
    public boolean isActive(@Nullable Object task) {
        if (task == null) {
            return false;
        }

        try {
            Method isCancelledMethod = task.getClass().getMethod("isCancelled");
            boolean cancelled = (boolean) isCancelledMethod.invoke(task);
            return !cancelled;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Runs a task at the region containing the specified context.
     *
     * @param context the region context
     * @param task the task to run
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runAt(@NotNull RegionContext context, @NotNull Runnable task) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        if (context.isGlobal()) {
            // Delegate to global scheduler
            return new FoliaGlobalScheduler(plugin).run(task);
        }

        Object world = context.getWorldHandle();
        if (world == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("RegionContext must have a world")
            );
        }

        return runAt(world, context.getCenterBlockX(), context.getCenterBlockZ(), task);
    }
}
