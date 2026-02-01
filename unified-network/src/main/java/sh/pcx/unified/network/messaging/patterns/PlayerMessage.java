/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.patterns;

import sh.pcx.unified.network.messaging.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Helper interface for sending messages that follow a player.
 *
 * <p>This pattern automatically routes messages to the server where
 * the target player is currently located.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * messaging.followPlayer(playerId)
 *     .send(new PlayerNotification("You have new mail!"));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.network.messaging.core.MessagingService#followPlayer(java.util.UUID)
 */
public interface PlayerMessage {

    /**
     * Sends a message to the server where the player is located.
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
