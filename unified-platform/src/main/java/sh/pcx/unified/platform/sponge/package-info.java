/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Sponge platform implementation for the UnifiedPlugin API.
 *
 * <p>This package contains the Sponge-specific implementations of the unified
 * platform interfaces. It provides seamless integration with Sponge servers
 * running on Forge or Fabric mod loaders.
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongePlatform} -
 *       Platform detection and information</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongePlatformProvider} -
 *       SPI provider implementation</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeUnifiedPlugin} -
 *       Plugin container wrapper</li>
 * </ul>
 *
 * <h2>Player Implementations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeUnifiedPlayer} -
 *       Online player wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeOfflinePlayer} -
 *       Offline player (User) wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongePlayerSession} -
 *       Session data management</li>
 * </ul>
 *
 * <h2>World Implementations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeUnifiedWorld} -
 *       ServerWorld wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeUnifiedBlock} -
 *       Block state wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeUnifiedChunk} -
 *       Chunk wrapper</li>
 * </ul>
 *
 * <h2>Item Implementations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeUnifiedItemStack} -
 *       ItemStack wrapper</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeItemBuilder} -
 *       Fluent item builder</li>
 * </ul>
 *
 * <h2>Bridge & Utilities</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeEventBridge} -
 *       Event system bridge</li>
 *   <li>{@link sh.pcx.unified.platform.sponge.SpongeConversions} -
 *       Type conversion utilities</li>
 * </ul>
 *
 * <h2>Sponge API Compatibility</h2>
 * <p>This implementation targets Sponge API 12.0.0 and is compatible with:
 * <ul>
 *   <li>SpongeForge (Minecraft Forge)</li>
 *   <li>SpongeVanilla</li>
 *   <li>SpongeFabric (if available)</li>
 * </ul>
 *
 * <h2>Service Loading</h2>
 * <p>The {@link sh.pcx.unified.platform.sponge.SpongePlatformProvider}
 * is registered via Java's ServiceLoader mechanism in:
 * {@code META-INF/services/sh.pcx.unified.platform.PlatformProvider}
 *
 * <h2>Cause/Context System</h2>
 * <p>This implementation properly handles Sponge's cause tracking system,
 * which provides detailed information about event origins. The
 * {@link sh.pcx.unified.platform.sponge.SpongeEventBridge} includes
 * utilities for working with causes.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.platform.Platform
 * @see sh.pcx.unified.platform.PlatformProvider
 */
package sh.pcx.unified.platform.sponge;
