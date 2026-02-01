/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Cross-server messaging API for the UnifiedPlugin framework.
 *
 * <p>This package provides a unified interface for sending messages between
 * servers in a Minecraft network. It supports multiple transport mechanisms:
 * <ul>
 *   <li>BungeeCord plugin messaging channels</li>
 *   <li>Velocity plugin messaging channels</li>
 *   <li>Redis pub/sub for custom networks</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.network.messaging.core.MessagingService} - Main entry point for messaging</li>
 *   <li>{@link sh.pcx.unified.network.messaging.core.MessageChannel} - Represents a communication channel</li>
 *   <li>{@link sh.pcx.unified.network.messaging.messages.Message} - Base interface for messages</li>
 *   <li>{@link sh.pcx.unified.network.messaging.patterns.RequestResponse} - Request/response pattern</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the messaging service
 * MessagingService messaging = services.get(MessagingService.class).orElseThrow();
 *
 * // Register a custom channel
 * MessageChannel<MyMessage> channel = messaging.registerChannel(
 *     "myplugin:updates",
 *     MyMessage.class
 * );
 *
 * // Send a broadcast message
 * channel.broadcast(new MyMessage("Hello, network!"));
 *
 * // Send to a specific server
 * channel.sendTo("lobby-1", new MyMessage("Hello, lobby!"));
 *
 * // Request/response pattern
 * CompletableFuture<PlayerCountResponse> response = messaging.request(
 *     "hub",
 *     new PlayerCountRequest(),
 *     PlayerCountResponse.class
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.network.messaging;
