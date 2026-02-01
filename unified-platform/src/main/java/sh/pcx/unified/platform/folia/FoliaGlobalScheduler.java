/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global region scheduler for Folia.
 *
 * <p>This scheduler uses Folia's GlobalRegionScheduler API to execute tasks
 * on the global region thread. The global region handles server-wide operations
 * that don't belong to any specific world location.
 *
 * <h2>When to Use Global Scheduler</h2>
 * <p>Use the global scheduler for:
 * <ul>
 *   <li>Plugin initialization and shutdown</li>
 *   <li>Server-wide broadcasts and messages</li>
 *   <li>Cross-region coordination</li>
 *   <li>Operations that don't touch world state</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <p>The global region runs on a single thread, similar to the traditional
 * Bukkit main thread. Heavy use of the global scheduler can become a
 * bottleneck. Prefer region-specific scheduling when possible.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * FoliaGlobalScheduler scheduler = new FoliaGlobalScheduler(plugin);
 *
 * // Run task on global region
 * scheduler.run(() -> {
 *     server.broadcast(Component.text("Server message!"));
 * });
 *
 * // Run with delay
 * scheduler.runLater(() -> {
 *     // Delayed global task
 * }, 100L); // 5 second delay
 *
 * // Run repeating task
 * Object task = scheduler.runAtFixedRate(scheduledTask -> {
 *     // Update scoreboard, etc.
 * }, 0L, 20L);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FoliaRegionScheduler
 * @see FoliaEntityScheduler
 */
public final class FoliaGlobalScheduler {

    private static final Logger LOGGER = Logger.getLogger(FoliaGlobalScheduler.class.getName());

    /**
     * The Bukkit plugin for scheduling.
     */
    private final Object plugin;

    /**
     * The Folia GlobalRegionScheduler instance.
     */
    private final Object globalScheduler;

    /**
     * The Plugin class for method lookups.
     */
    private final Class<?> pluginClass;

    /**
     * The Consumer class for task callbacks.
     */
    private final Class<?> consumerClass;

    /**
     * Constructs a new FoliaGlobalScheduler.
     *
     * @param plugin the Bukkit plugin to schedule tasks for
     * @throws IllegalStateException if Folia's GlobalRegionScheduler is not available
     * @since 1.0.0
     */
    public FoliaGlobalScheduler(@NotNull Object plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        if (!FoliaDetector.hasGlobalScheduler()) {
            throw new IllegalStateException("Folia GlobalRegionScheduler not available");
        }

        this.plugin = plugin;

        try {
            // Get GlobalRegionScheduler from Bukkit
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            this.globalScheduler = bukkitClass.getMethod("getGlobalRegionScheduler").invoke(null);

            // Cache class references
            this.pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            this.consumerClass = Consumer.class;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GlobalRegionScheduler", e);
        }
    }

    /**
     * Runs a task on the global region thread.
     *
     * @param task the task to run
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> run(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");

        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            // Try execute method first (runs immediately on global thread)
            Method executeMethod = globalScheduler.getClass().getMethod(
                    "execute",
                    pluginClass,
                    Runnable.class
            );

            Runnable wrappedTask = () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    LOGGER.log(Level.WARNING, "Global task failed", e);
                }
            };

            executeMethod.invoke(globalScheduler, plugin, wrappedTask);
        } catch (NoSuchMethodException e) {
            // Fall back to run method with consumer
            try {
                Consumer<Object> consumer = scheduledTask -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception ex) {
                        future.completeExceptionally(ex);
                    }
                };

                Method runMethod = globalScheduler.getClass().getMethod(
                        "run",
                        pluginClass,
                        consumerClass
                );

                runMethod.invoke(globalScheduler, plugin, consumer);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
            LOGGER.log(Level.WARNING, "Failed to schedule global task", e);
        }

        return future;
    }

    /**
     * Runs a task on the global region thread after a delay.
     *
     * @param task the task to run
     * @param delayTicks the delay in ticks (20 ticks = 1 second)
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> runLater(@NotNull Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task cannot be null");

        if (delayTicks <= 0) {
            return run(task);
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

            Method runDelayedMethod = globalScheduler.getClass().getMethod(
                    "runDelayed",
                    pluginClass,
                    consumerClass,
                    long.class
            );

            runDelayedMethod.invoke(globalScheduler, plugin, consumer, delayTicks);
        } catch (Exception e) {
            future.completeExceptionally(e);
            LOGGER.log(Level.WARNING, "Failed to schedule delayed global task", e);
        }

        return future;
    }

    /**
     * Runs a repeating task on the global region thread.
     *
     * @param task the task to run (receives ScheduledTask for cancellation)
     * @param initialDelayTicks the initial delay before first execution
     * @param periodTicks the period between executions
     * @return the scheduled task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runAtFixedRate(@NotNull Consumer<Object> task,
                                  long initialDelayTicks,
                                  long periodTicks) {
        Objects.requireNonNull(task, "task cannot be null");

        try {
            Method runAtFixedRateMethod = globalScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    pluginClass,
                    consumerClass,
                    long.class,
                    long.class
            );

            return runAtFixedRateMethod.invoke(globalScheduler, plugin, task,
                    initialDelayTicks, periodTicks);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to schedule repeating global task", e);
            return null;
        }
    }

    /**
     * Runs a repeating task on the global region thread with a simple Runnable.
     *
     * @param task the task to run
     * @param initialDelayTicks the initial delay
     * @param periodTicks the period between executions
     * @return the scheduled task handle, or null on failure
     * @since 1.0.0
     */
    @Nullable
    public Object runAtFixedRate(@NotNull Runnable task,
                                  long initialDelayTicks,
                                  long periodTicks) {
        return runAtFixedRate((Consumer<Object>) ignored -> task.run(),
                initialDelayTicks, periodTicks);
    }

    /**
     * Cancels a scheduled global task.
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
            LOGGER.log(Level.FINE, "Failed to cancel global task", e);
            return false;
        }
    }

    /**
     * Checks if a global task is still scheduled and not cancelled.
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
     * Cancels all tasks scheduled by the plugin through this scheduler.
     *
     * <p>This is useful during plugin shutdown to ensure all tasks are stopped.
     *
     * @since 1.0.0
     */
    public void cancelAll() {
        try {
            Method cancelTasksMethod = globalScheduler.getClass().getMethod(
                    "cancelTasks",
                    pluginClass
            );
            cancelTasksMethod.invoke(globalScheduler, plugin);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to cancel all global tasks", e);
        }
    }

    /**
     * Checks if the current thread is the global region thread.
     *
     * @return true if on the global region thread
     * @since 1.0.0
     */
    public static boolean isGlobalThread() {
        return FoliaDetector.isGlobalTickThread();
    }

    /**
     * Executes a task immediately if on the global thread,
     * otherwise schedules it.
     *
     * @param task the task to run
     * @return a future that completes when the task finishes
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> executeOrSchedule(@NotNull Runnable task) {
        if (isGlobalThread()) {
            try {
                task.run();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return run(task);
    }
}
