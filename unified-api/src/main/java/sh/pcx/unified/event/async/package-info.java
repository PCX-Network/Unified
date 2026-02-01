/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Asynchronous event handling components.
 *
 * <p>This package provides classes and interfaces for handling events
 * asynchronously. Async events allow long-running operations (database
 * queries, network requests) without blocking the main server thread.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.event.async.AsyncEvent} - Base class for async events</li>
 *   <li>{@link sh.pcx.unified.event.async.AsyncEventHandler} - Handler for async event processing</li>
 *   <li>{@link sh.pcx.unified.event.async.EventFuture} - Future for tracking async event completion</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create an async event
 * AsyncDataLoadEvent event = new AsyncDataLoadEvent(player);
 *
 * // Fire async and handle result
 * eventBus.fireAsync(event).thenAccept(e -> {
 *     if (!e.isCancelled()) {
 *         processData(e.getData());
 *     }
 * });
 *
 * // Or use EventFuture for more control
 * EventFuture<AsyncDataLoadEvent> future = EventFuture.of(event);
 * future.whenComplete((e, error) -> {
 *     if (error != null) {
 *         logger.error("Event failed", error);
 *     } else {
 *         processData(e.getData());
 *     }
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.event.async;
