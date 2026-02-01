/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Paper/Spigot platform implementation for the UnifiedPlugin API.
 *
 * <p>This package contains all the platform-specific implementations for Bukkit-based
 * servers including Paper, Spigot, and Folia. These classes wrap Bukkit API objects
 * and provide unified access through the common interfaces.
 *
 * <h2>Package Contents</h2>
 *
 * <h3>Core Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperPlatform} - Platform detection and information</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperPlatformProvider} - SPI provider implementation</li>
 *   <li>{@link sh.pcx.unified.platform.paper.BukkitUnifiedPlugin} - JavaPlugin base class for Bukkit platforms</li>
 * </ul>
 *
 * <h3>Player Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperUnifiedPlayer} - Online player wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperOfflinePlayer} - Offline player wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperPlayerSession} - Session data management</li>
 * </ul>
 *
 * <h3>World Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperUnifiedWorld} - World wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperUnifiedBlock} - Block wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperUnifiedChunk} - Chunk wrapper</li>
 * </ul>
 *
 * <h3>Server Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperUnifiedServer} - Server wrapper</li>
 * </ul>
 *
 * <h3>Item Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperUnifiedItemStack} - Item stack wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperItemBuilder} - Item builder implementation</li>
 * </ul>
 *
 * <h3>Bridge Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperEventBridge} - Bukkit to Unified event bridge</li>
 *   <li>{@link sh.pcx.unified.platform.paper.PaperConversions} - Type conversion utilities</li>
 * </ul>
 *
 * <h2>Platform Detection</h2>
 * <p>The implementation automatically detects the server type:
 * <ul>
 *   <li><b>Paper</b> - Full feature support including native Adventure</li>
 *   <li><b>Folia</b> - Region-aware scheduling support</li>
 *   <li><b>Spigot</b> - Basic Bukkit API with legacy string fallbacks</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The platform provider uses caching with read-write locks to ensure
 * thread-safe access to wrapper objects. Most read operations are safe
 * from any thread, while write operations should be performed on the
 * main server thread (or appropriate region thread for Folia).
 *
 * <h2>Service Loading</h2>
 * <p>This package is registered as a service provider through:
 * {@code META-INF/services/sh.pcx.unified.platform.PlatformProvider}
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.platform.paper;
