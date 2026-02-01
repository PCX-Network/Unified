/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an event handler for asynchronous execution.
 *
 * <p>Methods annotated with {@code @AsyncHandler} will be executed on an
 * asynchronous thread pool, allowing for long-running operations without
 * blocking the main server thread. This is particularly useful for:
 * <ul>
 *   <li>Database operations</li>
 *   <li>Network requests (REST APIs, webhooks)</li>
 *   <li>File I/O operations</li>
 *   <li>Complex calculations</li>
 * </ul>
 *
 * <h2>Important Considerations</h2>
 * <ul>
 *   <li>Async handlers CANNOT safely modify game state directly</li>
 *   <li>Use the scheduler to synchronize back to the main thread</li>
 *   <li>Async handlers may complete after the event has been processed</li>
 *   <li>Cancelling the event in an async handler may be too late</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     // Simple async handler
 *     @AsyncHandler
 *     public void onPlayerJoinAsync(PlayerJoinEvent event) {
 *         // Runs asynchronously
 *         fetchPlayerDataFromDatabase(event.getPlayer().getUniqueId());
 *     }
 *
 *     // Async handler with priority
 *     @AsyncHandler(priority = EventPriority.HIGH)
 *     public void onBlockBreakAsync(BlockBreakEvent event) {
 *         // Log to external system asynchronously
 *         externalLogger.logBlockBreak(event);
 *     }
 *
 *     // Async handler with synchronization back to main thread
 *     @AsyncHandler
 *     public void onPlayerJoinWithSync(PlayerJoinEvent event) {
 *         // Fetch data async
 *         PlayerData data = fetchPlayerData(event.getPlayer());
 *
 *         // Sync back to main thread to modify game state
 *         scheduler.runSync(() -> {
 *             event.getPlayer().sendMessage(Component.text("Loaded: " + data));
 *         });
 *     }
 *
 *     // Async handler for explicitly async events
 *     @AsyncHandler
 *     public void onAsyncChat(PlayerChatEvent event) {
 *         // Chat events on modern servers are already async
 *         processChat(event);
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Pool</h2>
 * <p>Async handlers are executed on a dedicated thread pool managed by the
 * event system. The pool size is automatically configured based on available
 * system resources, but can be customized through configuration.
 *
 * <h2>Ordering</h2>
 * <p>Async handlers respect the priority ordering, but because they run
 * asynchronously, there's no guarantee about when they complete relative
 * to sync handlers or each other. If you need guaranteed ordering, use
 * sync handlers or explicit synchronization.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EventHandler
 * @see AsyncEvent
 * @see AsyncEventHandler
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncHandler {

    /**
     * The priority of this async event handler.
     *
     * <p>Priority determines the relative order of async handler execution,
     * though completion order is not guaranteed due to async execution.
     *
     * @return the handler priority
     * @since 1.0.0
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Whether to skip this handler if the event is cancelled.
     *
     * <p>When set to {@code true}, this handler will not be called if the
     * event has been cancelled. Note that due to async execution, the
     * cancellation state checked is the state at dispatch time, not at
     * execution time.
     *
     * @return true to skip cancelled events
     * @since 1.0.0
     */
    boolean ignoreCancelled() default false;

    /**
     * The executor name for this handler.
     *
     * <p>Allows specifying a custom executor for this handler instead of
     * the default async pool. This is useful for:
     * <ul>
     *   <li>Isolating slow operations</li>
     *   <li>Using a single-threaded executor for ordering</li>
     *   <li>Limiting concurrency for rate-limited operations</li>
     * </ul>
     *
     * <p>Leave empty to use the default async executor.
     *
     * @return the executor name
     * @since 1.0.0
     */
    String executor() default "";

    /**
     * Timeout in milliseconds for this handler.
     *
     * <p>If the handler takes longer than this timeout, it will be
     * interrupted and a warning will be logged. Set to 0 (the default)
     * for no timeout.
     *
     * <p>Note: Not all operations can be interrupted cleanly.
     *
     * @return the timeout in milliseconds
     * @since 1.0.0
     */
    long timeout() default 0;

    /**
     * Whether to await completion before proceeding with event dispatch.
     *
     * <p>When set to {@code true}, the event system will wait for this
     * async handler to complete before calling subsequent handlers.
     * This provides ordering guarantees at the cost of performance.
     *
     * <p>When set to {@code false} (the default), the handler runs
     * independently and may complete at any time.
     *
     * @return true to await completion
     * @since 1.0.0
     */
    boolean await() default false;
}
