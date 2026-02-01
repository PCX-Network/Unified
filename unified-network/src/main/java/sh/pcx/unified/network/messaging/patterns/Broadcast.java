/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.patterns;

import sh.pcx.unified.network.messaging.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Helper interface for broadcasting messages to all servers.
 *
 * <p>This pattern simplifies sending messages to the entire network.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * messaging.broadcastAll()
 *     .send(new ServerAnnouncement("Maintenance in 5 minutes"));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.network.messaging.core.MessagingService#broadcastAll()
 */
public interface Broadcast {

    /**
     * Sends a message to all servers in the network.
     *
     * @param message the message to broadcast
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> send(@NotNull Message message);

    /**
     * Sends a message to all servers except the source server.
     *
     * @param message the message to broadcast
     * @return a future that completes when the message is sent
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> sendExcludingSelf(@NotNull Message message);
}
