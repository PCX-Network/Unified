/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Server abstraction interfaces for platform-agnostic server operations.
 *
 * <p>This package provides interfaces and classes for server information:
 * <ul>
 *   <li>{@link sh.pcx.unified.server.UnifiedServer} - Server operations interface</li>
 *   <li>{@link sh.pcx.unified.server.ServerType} - Server platform enumeration</li>
 *   <li>{@link sh.pcx.unified.server.MinecraftVersion} - Version comparison record</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the server
 * UnifiedServer server = UnifiedAPI.getServer();
 *
 * // Check server type
 * if (server.getServerType() == ServerType.FOLIA) {
 *     // Use region-aware scheduling
 * }
 *
 * // Version checks
 * MinecraftVersion version = server.getMinecraftVersion();
 * if (version.isAtLeast(MinecraftVersion.V1_21)) {
 *     // Use 1.21+ features
 * }
 *
 * // Get online players
 * Collection<UnifiedPlayer> players = server.getOnlinePlayers();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.server;
