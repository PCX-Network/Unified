/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Fallback implementations for non-Folia servers.
 *
 * <p>This class provides fallback implementations of Folia scheduling
 * operations that work on standard Paper/Spigot servers. When not running
 * on Folia, these methods delegate to the traditional Bukkit scheduler.
 *
 * <h2>Usage</h2>
 * <p>This class is used internally by the Folia adapters when Folia is
 * not detected. Plugin developers should use the unified scheduling API
 * instead of using this class directly.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * if (FoliaDetector.isFolia()) {
 *     foliaScheduler.runAt(location, task);
 * } else {
 *     FoliaFallback.runOnMainThread(plugin, task);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FoliaDetector
 * @see FoliaRegionScheduler
 */
public final class FoliaFallback {

    private static final Logger LOGGER = Logger.getLogger(FoliaFallback.class.getName());

    private FoliaFallback() {
        // Utility class - no instantiation
    }

    /**
     * Runs a task on the main thread using Bukkit scheduler.
     *
     * @param plugin the plugin to schedule for
     * @param task the task to run
     * @return true if the task was scheduled
     * @since 1.0.0
     */
    public static boolean runOnMainThread(@NotNull Object plugin, @NotNull Runnable task) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object scheduler = bukkitClass.getMethod("getScheduler").invoke(null);
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");

            scheduler.getClass().getMethod("runTask", pluginClass, Runnable.class)
                    .invoke(scheduler, plugin, task);
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to schedule main thread task: " + e.getMessage());
            return false;
        }
    }

    /**
     * Runs a task on the main thread with a delay.
     *
     * @param plugin the plugin to schedule for
     * @param task the task to run
     * @param delayTicks the delay in ticks
     * @return the task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public static Object runLater(@NotNull Object plugin, @NotNull Runnable task, long delayTicks) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object scheduler = bukkitClass.getMethod("getScheduler").invoke(null);
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");

            return scheduler.getClass().getMethod("runTaskLater", pluginClass, Runnable.class, long.class)
                    .invoke(scheduler, plugin, task, delayTicks);
        } catch (Exception e) {
            LOGGER.warning("Failed to schedule delayed task: " + e.getMessage());
            return null;
        }
    }

    /**
     * Runs a repeating task on the main thread.
     *
     * @param plugin the plugin to schedule for
     * @param task the task to run
     * @param initialDelayTicks the initial delay
     * @param periodTicks the period between executions
     * @return the task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public static Object runTimer(@NotNull Object plugin, @NotNull Runnable task,
                                   long initialDelayTicks, long periodTicks) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object scheduler = bukkitClass.getMethod("getScheduler").invoke(null);
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");

            return scheduler.getClass().getMethod("runTaskTimer", pluginClass,
                    Runnable.class, long.class, long.class)
                    .invoke(scheduler, plugin, task, initialDelayTicks, periodTicks);
        } catch (Exception e) {
            LOGGER.warning("Failed to schedule timer task: " + e.getMessage());
            return null;
        }
    }

    /**
     * Runs a task asynchronously.
     *
     * @param plugin the plugin to schedule for
     * @param task the task to run
     * @return true if the task was scheduled
     * @since 1.0.0
     */
    public static boolean runAsync(@NotNull Object plugin, @NotNull Runnable task) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object scheduler = bukkitClass.getMethod("getScheduler").invoke(null);
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");

            scheduler.getClass().getMethod("runTaskAsynchronously", pluginClass, Runnable.class)
                    .invoke(scheduler, plugin, task);
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to schedule async task: " + e.getMessage());
            // Fallback to thread
            new Thread(task, "UnifiedAPI-Async-Fallback").start();
            return true;
        }
    }

    /**
     * Runs an async task with a delay.
     *
     * @param plugin the plugin to schedule for
     * @param task the task to run
     * @param delayTicks the delay in ticks
     * @return the task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public static Object runAsyncLater(@NotNull Object plugin, @NotNull Runnable task, long delayTicks) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object scheduler = bukkitClass.getMethod("getScheduler").invoke(null);
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");

            return scheduler.getClass().getMethod("runTaskLaterAsynchronously",
                    pluginClass, Runnable.class, long.class)
                    .invoke(scheduler, plugin, task, delayTicks);
        } catch (Exception e) {
            LOGGER.warning("Failed to schedule delayed async task: " + e.getMessage());
            return null;
        }
    }

    /**
     * Runs a task at a location - falls back to main thread on non-Folia.
     *
     * @param plugin the plugin to schedule for
     * @param location the location (ignored on non-Folia)
     * @param task the task to run
     * @return a future that completes when the task is done
     * @since 1.0.0
     */
    @NotNull
    public static CompletableFuture<Void> runAtLocation(@NotNull Object plugin,
                                                         @NotNull UnifiedLocation location,
                                                         @NotNull Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable wrappedTask = () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };

        if (runOnMainThread(plugin, wrappedTask)) {
            return future;
        }

        return CompletableFuture.failedFuture(
                new RuntimeException("Failed to schedule location task")
        );
    }

    /**
     * Runs a task for an entity - falls back to main thread on non-Folia.
     *
     * @param plugin the plugin to schedule for
     * @param entity the entity (ignored on non-Folia)
     * @param task the task to run
     * @param retired callback if entity is removed (may be null)
     * @return true if the task was scheduled
     * @since 1.0.0
     */
    public static boolean runAtEntity(@NotNull Object plugin,
                                       @NotNull Object entity,
                                       @NotNull Runnable task,
                                       @Nullable Runnable retired) {
        // On non-Folia, just run on main thread
        // We can't really handle the "retired" case without Folia's EntityScheduler
        return runOnMainThread(plugin, task);
    }

    /**
     * Cancels a scheduled task.
     *
     * @param task the task handle from scheduling methods
     * @return true if the task was cancelled
     * @since 1.0.0
     */
    public static boolean cancel(@Nullable Object task) {
        if (task == null) {
            return false;
        }

        try {
            task.getClass().getMethod("cancel").invoke(task);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if running on the main thread.
     *
     * @return true if on the main thread
     * @since 1.0.0
     */
    public static boolean isMainThread() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            return (boolean) bukkitClass.getMethod("isPrimaryThread").invoke(null);
        } catch (Exception e) {
            return Thread.currentThread().getName().equals("Server thread");
        }
    }

    /**
     * Checks if a location is "owned" by the current thread.
     *
     * <p>On non-Folia servers, this returns true if on the main thread.
     *
     * @param location the location to check
     * @return true if safe to modify
     * @since 1.0.0
     */
    public static boolean isOwnedByCurrentThread(@NotNull UnifiedLocation location) {
        return isMainThread();
    }

    /**
     * Executes a task immediately if on main thread, otherwise schedules it.
     *
     * @param plugin the plugin to schedule for
     * @param task the task to run
     * @return a future that completes when done
     * @since 1.0.0
     */
    @NotNull
    public static CompletableFuture<Void> executeOrSchedule(@NotNull Object plugin,
                                                             @NotNull Runnable task) {
        if (isMainThread()) {
            try {
                task.run();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        runOnMainThread(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
