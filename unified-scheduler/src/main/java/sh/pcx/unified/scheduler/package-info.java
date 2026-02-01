/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Unified scheduler system with Folia region-aware support.
 *
 * <p>This package provides a platform-agnostic scheduling API that works
 * across Paper, Spigot, Folia, and Sponge servers. It automatically detects
 * the platform and uses the appropriate scheduler implementation.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.SchedulerService} - Main entry point</li>
 *   <li>{@link sh.pcx.unified.scheduler.Task} - Task representation</li>
 *   <li>{@link sh.pcx.unified.scheduler.TaskBuilder} - Fluent task builder</li>
 *   <li>{@link sh.pcx.unified.scheduler.TaskHandle} - Task lifecycle control</li>
 *   <li>{@link sh.pcx.unified.scheduler.TaskChain} - Sequential task execution</li>
 * </ul>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.types} - Task type implementations</li>
 *   <li>{@link sh.pcx.unified.scheduler.folia} - Folia-specific task types</li>
 *   <li>{@link sh.pcx.unified.scheduler.execution} - Task execution infrastructure</li>
 *   <li>{@link sh.pcx.unified.scheduler.util} - Utility classes</li>
 *   <li>{@link sh.pcx.unified.scheduler.detection} - Platform detection</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Get the scheduler service
 * SchedulerService scheduler = api.getService(SchedulerService.class);
 *
 * // Run a simple sync task
 * scheduler.runTask(() -> player.sendMessage("Hello!"));
 *
 * // Run async with delay
 * scheduler.runTaskLaterAsync(() -> saveData(), 20L);
 *
 * // Entity-bound task (Folia-safe)
 * scheduler.runAtEntity(player, () -> player.setHealth(20.0));
 *
 * // Fluent builder
 * scheduler.builder()
 *     .async()
 *     .delay(5, TimeUnit.SECONDS)
 *     .repeat(1, TimeUnit.MINUTES)
 *     .execute(() -> cleanupCache());
 *
 * // Task chain
 * scheduler.chain()
 *     .async(() -> loadFromDatabase())
 *     .sync(data -> applyToPlayer(data))
 *     .execute();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.scheduler;
