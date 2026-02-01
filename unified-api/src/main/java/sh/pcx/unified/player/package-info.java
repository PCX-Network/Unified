/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Player abstraction interfaces for platform-agnostic player handling.
 *
 * <p>This package provides interfaces for interacting with players:
 * <ul>
 *   <li>{@link sh.pcx.unified.player.UnifiedPlayer} - Online player interface</li>
 *   <li>{@link sh.pcx.unified.player.OfflineUnifiedPlayer} - Offline player interface</li>
 *   <li>{@link sh.pcx.unified.player.PlayerSession} - Session data management</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get an online player
 * Optional<UnifiedPlayer> player = UnifiedAPI.getPlayer(uuid);
 *
 * player.ifPresent(p -> {
 *     // Send a message
 *     p.sendMessage(Component.text("Hello!"));
 *
 *     // Teleport
 *     p.teleport(location);
 *
 *     // Access session data
 *     PlayerSession session = p.getSession();
 *     session.set("last_action", Instant.now());
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.player;
