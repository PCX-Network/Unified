/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Folia platform adapter for the UnifiedPlugin API.
 *
 * <p>This package provides Folia-specific implementations of the unified
 * platform abstractions. Folia is a Paper fork that implements region-based
 * multithreading, fundamentally changing the server's threading model.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaPlatform} -
 *       Platform detection and information</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaPlatformProvider} -
 *       SPI provider for platform initialization</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaUnifiedPlugin} -
 *       Folia-aware plugin base class</li>
 * </ul>
 *
 * <h2>Region-Aware Scheduling</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaRegionScheduler} -
 *       Schedule tasks at specific world locations</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaEntityScheduler} -
 *       Schedule tasks bound to specific entities</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaGlobalScheduler} -
 *       Schedule tasks on the global region</li>
 *   <li>{@link sh.pcx.unified.platform.folia.RegionContext} -
 *       Represents a region for scheduling</li>
 * </ul>
 *
 * <h2>Player & World Wrappers</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaUnifiedPlayer} -
 *       Thread-safe player wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaPlayerSession} -
 *       Region-aware session management</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaUnifiedWorld} -
 *       Thread-safe world wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaUnifiedChunk} -
 *       Region-aware chunk wrapper</li>
 * </ul>
 *
 * <h2>Detection & Fallback</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaDetector} -
 *       Runtime Folia detection</li>
 *   <li>{@link sh.pcx.unified.platform.folia.FoliaFallback} -
 *       Fallback for non-Folia servers</li>
 * </ul>
 *
 * <h2>Folia Threading Model</h2>
 * <p>Unlike traditional Bukkit servers that use a single main thread,
 * Folia divides the world into regions, each processed by its own thread.
 * This provides significant performance improvements for large servers but
 * requires careful attention to thread safety:
 *
 * <ul>
 *   <li>No single "main thread" - each region has its own tick thread</li>
 *   <li>Entities can only be modified by their region's thread</li>
 *   <li>Cross-region operations require proper scheduling</li>
 *   <li>Traditional Bukkit schedulers are not safe to use</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyPlugin extends FoliaUnifiedPlugin {
 *
 *     @Override
 *     public void onEnable() {
 *         // Schedule a task at a location
 *         runAtLocation(someLocation, () -> {
 *             // This runs on the correct region thread
 *             world.getBlockAt(someLocation).setType(Material.STONE);
 *         });
 *
 *         // Schedule a task for an entity
 *         runAtEntity(player, () -> {
 *             // This runs on the player's region thread
 *             player.sendMessage("Hello!");
 *         });
 *
 *         // Global region task
 *         runGlobal(() -> {
 *             // Server-wide operations
 *             getServer().broadcast(Component.text("Announcement!"));
 *         });
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.platform.Platform
 * @see sh.pcx.unified.platform.PlatformProvider
 */
package sh.pcx.unified.platform.folia;
