/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.patterns;

import sh.pcx.unified.network.messaging.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Helper interface for request/response messaging patterns.
 *
 * <p>This pattern provides a type-safe way to send requests and receive
 * responses with configurable timeouts.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * messaging.<PlayerCountResponse>prepareRequest(PlayerCountResponse.class)
 *     .withTimeout(Duration.ofSeconds(5))
 *     .toServer("hub")
 *     .send(new PlayerCountRequest())
 *     .thenAccept(response -> {
 *         log.info("Hub has " + response.getCount() + " players");
 *     });
 * }</pre>
 *
 * @param <R> the expected response type
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.network.messaging.core.MessagingService#prepareRequest(Class)
 */
public interface RequestResponse<R extends Message> {

    /**
     * Sets the timeout for the request.
     *
     * @param timeout the timeout duration
     * @return this helper for chaining
     * @since 1.0.0
     */
    @NotNull
    RequestResponse<R> withTimeout(@NotNull Duration timeout);

    /**
     * Sets the target server for the request.
     *
     * @param serverId the target server ID
     * @return this helper for chaining
     * @since 1.0.0
     */
    @NotNull
    RequestResponse<R> toServer(@NotNull String serverId);

    /**
     * Sends the request and awaits a response.
     *
     * @param request the request message
     * @return a future containing the response
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<R> send(@NotNull Message request);

    /**
     * Sends the request to all servers and collects responses.
     *
     * @param request the request message
     * @return a future containing all responses
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<java.util.List<R>> sendToAll(@NotNull Message request);
}
