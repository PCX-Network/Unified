/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.messaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Utilities for transferring players between servers.
 *
 * <p>The PlayerTransfer service provides methods for moving players across
 * servers in the network. It works with BungeeCord, Velocity, and custom
 * proxy implementations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlayerTransfer transfers = messaging.transfers();
 *
 * // Simple transfer
 * transfers.send(player, "lobby-1");
 *
 * // Transfer with callback
 * transfers.send(player, "minigames")
 *     .thenRun(() -> log.info(player.getName() + " transferred"))
 *     .exceptionally(ex -> {
 *         player.sendMessage("Transfer failed!");
 *         return null;
 *     });
 *
 * // Transfer to best available server in group
 * transfers.sendToGroup(player, "lobby")
 *     .exceptionally(ex -> {
 *         player.sendMessage("No lobbies available!");
 *         return null;
 *     });
 *
 * // Transfer with context data
 * transfers.send(player, "arena-1")
 *     .withData("game", "bedwars")
 *     .withData("team", "red")
 *     .execute();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessagingService#transfers()
 */
public interface PlayerTransfer {

    /**
     * Sends a player to a specific server.
     *
     * @param playerId the player's UUID
     * @param server   the target server ID
     * @return future completing when transfer is initiated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<TransferResult> send(@NotNull UUID playerId, @NotNull String server);

    /**
     * Sends a player to a server in a group.
     *
     * <p>Selects the best available server based on player count and availability.
     *
     * @param playerId the player's UUID
     * @param group    the server group
     * @return future completing when transfer is initiated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<TransferResult> sendToGroup(@NotNull UUID playerId, @NotNull String group);

    /**
     * Sends a player to a server matching criteria.
     *
     * @param playerId the player's UUID
     * @param filter   the server filter
     * @return future completing when transfer is initiated
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<TransferResult> sendToMatching(@NotNull UUID playerId, @NotNull Predicate<ServerInfo> filter);

    /**
     * Sends multiple players to a server.
     *
     * @param playerIds the player UUIDs
     * @param server    the target server
     * @return future with results for each player
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<TransferResult>> sendAll(
            @NotNull Collection<UUID> playerIds,
            @NotNull String server
    );

    /**
     * Creates a transfer request builder for complex transfers.
     *
     * @param playerId the player's UUID
     * @return a transfer builder
     * @since 1.0.0
     */
    @NotNull
    TransferBuilder transfer(@NotNull UUID playerId);

    /**
     * Finds which server a player is on.
     *
     * @param playerId the player's UUID
     * @return the server ID, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<String>> findPlayer(@NotNull UUID playerId);

    /**
     * Gets the server info for where a player is located.
     *
     * @param playerId the player's UUID
     * @return server info, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<ServerInfo>> findPlayerServer(@NotNull UUID playerId);

    /**
     * Kicks a player from the network.
     *
     * @param playerId the player's UUID
     * @param reason   the kick reason (can be null)
     * @return future completing when kicked
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> kick(@NotNull UUID playerId, @Nullable String reason);

    /**
     * Gets the total player count across the network.
     *
     * @return the total player count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> networkPlayerCount();

    /**
     * Gets players on a specific server.
     *
     * @param server the server ID
     * @return player UUIDs on that server
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<UUID>> playersOn(@NotNull String server);

    /**
     * Builder for complex transfer requests.
     *
     * @since 1.0.0
     */
    interface TransferBuilder {

        /**
         * Sets the target server.
         *
         * @param server the server ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder to(@NotNull String server);

        /**
         * Sets the target server group.
         *
         * <p>A server from the group will be selected.
         *
         * @param group the server group
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder toGroup(@NotNull String group);

        /**
         * Sets a filter for server selection.
         *
         * @param filter the server filter
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder matching(@NotNull Predicate<ServerInfo> filter);

        /**
         * Attaches custom data to the transfer.
         *
         * <p>This data is available on the receiving server.
         *
         * @param key   the data key
         * @param value the data value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder withData(@NotNull String key, @NotNull Object value);

        /**
         * Sets a timeout for the transfer.
         *
         * @param timeout the timeout duration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder timeout(@NotNull Duration timeout);

        /**
         * Sets whether to bypass server capacity limits.
         *
         * @param bypass true to bypass limits
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder bypassLimit(boolean bypass);

        /**
         * Sets a callback for when the player arrives.
         *
         * @param callback the arrival callback
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder onArrival(@NotNull Runnable callback);

        /**
         * Sets a callback for transfer failure.
         *
         * @param callback the failure callback
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        TransferBuilder onFailure(@NotNull java.util.function.Consumer<TransferResult> callback);

        /**
         * Executes the transfer.
         *
         * @return future with the transfer result
         * @since 1.0.0
         */
        @NotNull
        CompletableFuture<TransferResult> execute();
    }

    /**
     * Result of a player transfer attempt.
     *
     * @since 1.0.0
     */
    interface TransferResult {

        /**
         * Whether the transfer was successful.
         *
         * @return true if successful
         * @since 1.0.0
         */
        boolean success();

        /**
         * The player who was transferred.
         *
         * @return the player UUID
         * @since 1.0.0
         */
        @NotNull
        UUID playerId();

        /**
         * The source server.
         *
         * @return the source server ID
         * @since 1.0.0
         */
        @NotNull
        String sourceServer();

        /**
         * The target server.
         *
         * @return the target server ID
         * @since 1.0.0
         */
        @NotNull
        String targetServer();

        /**
         * The failure reason, if any.
         *
         * @return the failure reason
         * @since 1.0.0
         */
        @NotNull
        Optional<FailureReason> failureReason();

        /**
         * A human-readable error message.
         *
         * @return error message, or empty if successful
         * @since 1.0.0
         */
        @NotNull
        Optional<String> errorMessage();

        /**
         * Custom data attached to the transfer.
         *
         * @param key the data key
         * @param <T> expected type
         * @return the data value
         * @since 1.0.0
         */
        @Nullable
        <T> T data(@NotNull String key);

        /**
         * Duration of the transfer in milliseconds.
         *
         * @return transfer duration
         * @since 1.0.0
         */
        long durationMs();

        /**
         * Reasons why a transfer might fail.
         *
         * @since 1.0.0
         */
        enum FailureReason {
            /**
             * Target server is offline.
             */
            SERVER_OFFLINE,

            /**
             * Target server is full.
             */
            SERVER_FULL,

            /**
             * Player not found on source server.
             */
            PLAYER_NOT_FOUND,

            /**
             * Player disconnected during transfer.
             */
            PLAYER_DISCONNECTED,

            /**
             * Player is already on the target server.
             */
            ALREADY_ON_SERVER,

            /**
             * Transfer was denied by a plugin.
             */
            DENIED,

            /**
             * Transfer request timed out.
             */
            TIMEOUT,

            /**
             * Network or connection error.
             */
            NETWORK_ERROR,

            /**
             * Unknown or unspecified error.
             */
            UNKNOWN
        }
    }
}
