/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity-bound task scheduler for Folia.
 *
 * <p>This scheduler uses Folia's EntityScheduler API to execute tasks on
 * the thread that owns a specific entity. This ensures thread-safe access
 * to entity properties and methods.
 *
 * <h2>Entity Threading in Folia</h2>
 * <p>In Folia, entities are owned by the region thread that contains their
 * location. As entities move between regions, they may switch threads.
 * The EntityScheduler handles this complexity by ensuring tasks run on
 * the correct thread regardless of entity movement.
 *
 * <h2>Retired Entities</h2>
 * <p>Entities can be removed (retired) before a scheduled task runs.
 * The scheduler provides a callback mechanism to handle this case.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * FoliaEntityScheduler scheduler = new FoliaEntityScheduler(plugin);
 *
 * // Run task for entity
 * scheduler.run(player, () -> {
 *     player.sendMessage("Hello!");
 * }, () -> {
 *     // Player disconnected before task ran
 *     getLogger().info("Player left before task executed");
 * });
 *
 * // Run task with delay
 * scheduler.runLater(entity, () -> {
 *     entity.setVelocity(new Vector(0, 1, 0));
 * }, null, 20L); // 1 second delay
 *
 * // Run repeating task
 * Object task = scheduler.runAtFixedRate(entity, scheduledTask -> {
 *     // Update entity every second
 *     entity.setFireTicks(20);
 * }, null, 0L, 20L);
 *
 * // Cancel later
 * scheduler.cancel(task);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FoliaRegionScheduler
 * @see FoliaGlobalScheduler
 */
public final class FoliaEntityScheduler {

    private static final Logger LOGGER = Logger.getLogger(FoliaEntityScheduler.class.getName());

    /**
     * The Bukkit plugin for scheduling.
     */
    private final Object plugin;

    /**
     * The Plugin class for method lookups.
     */
    private final Class<?> pluginClass;

    /**
     * The Consumer class for task callbacks.
     */
    private final Class<?> consumerClass;

    /**
     * The Entity class for method lookups.
     */
    private final Class<?> entityClass;

    /**
     * Constructs a new FoliaEntityScheduler.
     *
     * @param plugin the Bukkit plugin to schedule tasks for
     * @throws IllegalStateException if Folia's EntityScheduler is not available
     * @since 1.0.0
     */
    public FoliaEntityScheduler(@NotNull Object plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        if (!FoliaDetector.hasEntityScheduler()) {
            throw new IllegalStateException("Folia EntityScheduler not available");
        }

        this.plugin = plugin;

        try {
            this.pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            this.consumerClass = Consumer.class;
            this.entityClass = Class.forName("org.bukkit.entity.Entity");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize EntityScheduler", e);
        }
    }

    /**
     * Runs a task on the thread that owns the specified entity.
     *
     * @param entity the entity (must be a Bukkit Entity)
     * @param task the task to run
     * @param retired callback if entity is removed before task runs (may be null)
     * @return true if the task was scheduled successfully
     * @since 1.0.0
     */
    public boolean run(@NotNull Object entity, @NotNull Runnable task, @Nullable Runnable retired) {
        Objects.requireNonNull(entity, "entity cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        try {
            // Get EntityScheduler from the entity
            Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getSchedulerMethod.invoke(entity);

            if (entityScheduler == null) {
                LOGGER.warning("Entity scheduler not available for: " + entity);
                return false;
            }

            // Create consumer wrapper for the task
            Consumer<Object> consumer = scheduledTask -> {
                try {
                    task.run();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Entity task failed", e);
                }
            };

            // Call EntityScheduler.run(Plugin, Consumer, Runnable)
            Method runMethod = entityScheduler.getClass().getMethod(
                    "run",
                    pluginClass,
                    consumerClass,
                    Runnable.class
            );

            Object result = runMethod.invoke(entityScheduler, plugin, consumer, retired);

            // Returns null if entity is already retired
            return result != null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule entity task", e);
            return false;
        }
    }

    /**
     * Runs a task on the thread that owns the specified entity.
     *
     * @param entity the entity
     * @param task the task to run
     * @return true if the task was scheduled
     * @since 1.0.0
     */
    public boolean run(@NotNull Object entity, @NotNull Runnable task) {
        return run(entity, task, null);
    }

    /**
     * Runs a task on the entity's thread after a delay.
     *
     * @param entity the entity
     * @param task the task to run
     * @param retired callback if entity is removed (may be null)
     * @param delayTicks the delay in ticks
     * @return the scheduled task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runLater(@NotNull Object entity, @NotNull Runnable task,
                           @Nullable Runnable retired, long delayTicks) {
        Objects.requireNonNull(entity, "entity cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        if (delayTicks <= 0) {
            return run(entity, task, retired) ? entity : null;
        }

        try {
            Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getSchedulerMethod.invoke(entity);

            if (entityScheduler == null) {
                return null;
            }

            Consumer<Object> consumer = scheduledTask -> {
                try {
                    task.run();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Delayed entity task failed", e);
                }
            };

            Method runDelayedMethod = entityScheduler.getClass().getMethod(
                    "runDelayed",
                    pluginClass,
                    consumerClass,
                    Runnable.class,
                    long.class
            );

            return runDelayedMethod.invoke(entityScheduler, plugin, consumer, retired, delayTicks);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule delayed entity task", e);
            return null;
        }
    }

    /**
     * Runs a task on the entity's thread after a delay.
     *
     * @param entity the entity
     * @param task the task to run
     * @param delayTicks the delay in ticks
     * @return the scheduled task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runLater(@NotNull Object entity, @NotNull Runnable task, long delayTicks) {
        return runLater(entity, task, null, delayTicks);
    }

    /**
     * Runs a repeating task on the entity's thread.
     *
     * @param entity the entity
     * @param task the task to run (receives ScheduledTask for cancellation)
     * @param retired callback if entity is removed (may be null)
     * @param initialDelayTicks the initial delay before first execution
     * @param periodTicks the period between executions
     * @return the scheduled task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runAtFixedRate(@NotNull Object entity,
                                  @NotNull Consumer<Object> task,
                                  @Nullable Runnable retired,
                                  long initialDelayTicks,
                                  long periodTicks) {
        Objects.requireNonNull(entity, "entity cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        try {
            Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getSchedulerMethod.invoke(entity);

            if (entityScheduler == null) {
                return null;
            }

            Method runAtFixedRateMethod = entityScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    pluginClass,
                    consumerClass,
                    Runnable.class,
                    long.class,
                    long.class
            );

            return runAtFixedRateMethod.invoke(entityScheduler, plugin, task, retired,
                    initialDelayTicks, periodTicks);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule repeating entity task", e);
            return null;
        }
    }

    /**
     * Runs a repeating task on the entity's thread with a simple Runnable.
     *
     * @param entity the entity
     * @param task the task to run
     * @param retired callback if entity is removed
     * @param initialDelayTicks the initial delay
     * @param periodTicks the period between executions
     * @return the scheduled task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runAtFixedRate(@NotNull Object entity,
                                  @NotNull Runnable task,
                                  @Nullable Runnable retired,
                                  long initialDelayTicks,
                                  long periodTicks) {
        return runAtFixedRate(entity, (Consumer<Object>) ignored -> task.run(),
                retired, initialDelayTicks, periodTicks);
    }

    /**
     * Cancels a scheduled entity task.
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
            LOGGER.log(Level.FINE, "Failed to cancel entity task", e);
            return false;
        }
    }

    /**
     * Checks if an entity task is still scheduled and not cancelled.
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
     * Checks if the current thread owns the specified entity.
     *
     * <p>This can be used to determine if it's safe to directly modify
     * the entity or if scheduling is required.
     *
     * @param entity the entity to check
     * @return true if the current thread owns the entity
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread(@NotNull Object entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        if (!FoliaDetector.isFolia()) {
            // On non-Folia servers, main thread owns everything
            return Thread.currentThread().getName().equals("Server thread");
        }

        try {
            // Get entity's location
            Method getLocationMethod = entity.getClass().getMethod("getLocation");
            Object location = getLocationMethod.invoke(entity);

            // Get world from location
            Method getWorldMethod = location.getClass().getMethod("getWorld");
            Object world = getWorldMethod.invoke(location);

            // Get chunk coordinates
            Method getBlockXMethod = location.getClass().getMethod("getBlockX");
            Method getBlockZMethod = location.getClass().getMethod("getBlockZ");

            int blockX = (int) getBlockXMethod.invoke(location);
            int blockZ = (int) getBlockZMethod.invoke(location);

            return FoliaDetector.isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to check entity ownership", e);
            return false;
        }
    }

    /**
     * Executes a task immediately if the current thread owns the entity,
     * otherwise schedules it for the correct thread.
     *
     * @param entity the entity
     * @param task the task to run
     * @return true if the task was executed or scheduled
     * @since 1.0.0
     */
    public boolean executeOrSchedule(@NotNull Object entity, @NotNull Runnable task) {
        if (isOwnedByCurrentThread(entity)) {
            try {
                task.run();
                return true;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Direct entity task failed", e);
                return false;
            }
        }
        return run(entity, task);
    }
}
