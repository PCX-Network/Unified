/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Platform detection and SPI interfaces for platform implementations.
 *
 * <p>This package provides interfaces for platform abstraction:
 * <ul>
 *   <li>{@link sh.pcx.unified.platform.Platform} - Platform detection interface</li>
 *   <li>{@link sh.pcx.unified.platform.PlatformType} - Platform type enumeration</li>
 *   <li>{@link sh.pcx.unified.platform.PlatformProvider} - SPI for implementations</li>
 * </ul>
 *
 * <h2>Platform Detection</h2>
 * <pre>{@code
 * Platform platform = Platform.current();
 *
 * // Check platform type
 * if (platform.getType() == PlatformType.BUKKIT) {
 *     // Bukkit-specific code
 * }
 *
 * // Check for Paper features
 * if (platform.isPaper()) {
 *     // Use Paper-specific optimizations
 * }
 *
 * // Check for Folia region threading
 * if (platform.isFolia()) {
 *     // Use region-aware scheduling
 * }
 * }</pre>
 *
 * <h2>Implementing a Platform Provider</h2>
 * <p>Platform providers implement the {@link sh.pcx.unified.platform.PlatformProvider}
 * interface and register via ServiceLoader.
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.platform;
