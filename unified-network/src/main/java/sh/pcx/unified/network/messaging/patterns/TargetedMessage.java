/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.patterns;

import sh.pcx.unified.network.messaging.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Helper interface for sending messages to a specific server.
 *
 * <p>This pattern simplifies sending messages to a known target server.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * messaging.targetServer("lobby-1")
 *     .send(new PlayerTransfer(player.getUniqueId()));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.network.messaging.core.MessagingService#targetServer(String)
 */
public interface TargetedMessage {

    /**
     * Returns the target server ID.
     *
     * @return the target server
     * @since 1.0.0
     */
    @NotNull
    String getTargetServer();

    /**
     * Sends a message to the target server.
     *
     * @param message the message to send
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> send(@NotNull Message message);

    /**
     * Sends a message and waits for acknowledgement.
     *
     * @param message the message to send
     * @return a future that completes when acknowledged
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> sendAndConfirm(@NotNull Message message);
}
