/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.impl;

import sh.pcx.unified.messaging.MessagingService;
import sh.pcx.unified.messaging.PlayerTransfer;
import sh.pcx.unified.messaging.ServerInfo;
import sh.pcx.unified.network.messaging.channels.ChannelAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Default implementation of {@link PlayerTransfer}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultPlayerTransfer implements PlayerTransfer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPlayerTransfer.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final ChannelAdapter adapter;
    private final MessagingService messaging;

    /**
     * Creates a new player transfer service.
     *
     * @param adapter   the channel adapter
     * @param messaging the messaging service
     */
    public DefaultPlayerTransfer(
            @NotNull ChannelAdapter adapter,
            @NotNull MessagingService messaging
    ) {
        this.adapter = Objects.requireNonNull(adapter);
        this.messaging = Objects.requireNonNull(messaging);
    }

    @Override
    @NotNull
    public CompletableFuture<TransferResult> send(@NotNull UUID playerId, @NotNull String server) {
        long startTime = System.currentTimeMillis();
        String sourceServer = adapter.serverId();

        return adapter.transferPlayer(playerId, server)
                .<TransferResult>thenApply(success -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (success) {
                        return new DefaultTransferResult(
                                true, playerId, sourceServer, server,
                                null, null, Map.of(), duration
                        );
                    } else {
                        return new DefaultTransferResult(
                                false, playerId, sourceServer, server,
                                TransferResult.FailureReason.UNKNOWN,
                                "Transfer failed",
                                Map.of(), duration
                        );
                    }
                })
                .exceptionally(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.error("Failed to transfer player {} to {}", playerId, server, ex);
                    return new DefaultTransferResult(
                            false, playerId, sourceServer, server,
                            TransferResult.FailureReason.NETWORK_ERROR,
                            ex.getMessage(),
                            Map.of(), duration
                    );
                });
    }

    @Override
    @NotNull
    public CompletableFuture<TransferResult> sendToGroup(@NotNull UUID playerId, @NotNull String group) {
        return findBestServerInGroup(group)
                .thenCompose(server -> {
                    if (server.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new DefaultTransferResult(
                                        false, playerId, adapter.serverId(), group,
                                        TransferResult.FailureReason.SERVER_OFFLINE,
                                        "No servers available in group: " + group,
                                        Map.of(), 0
                                )
                        );
                    }
                    return send(playerId, server.get());
                });
    }

    @Override
    @NotNull
    public CompletableFuture<TransferResult> sendToMatching(@NotNull UUID playerId, @NotNull Predicate<ServerInfo> filter) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<ServerInfo> target = messaging.servers().stream()
                    .filter(s -> !s.id().equals(adapter.serverId()))
                    .filter(ServerInfo::isOnline)
                    .filter(filter)
                    .min(Comparator.comparingInt(ServerInfo::playerCount));

            return target.map(ServerInfo::id);
        }).thenCompose(server -> {
            if (server.isEmpty()) {
                return CompletableFuture.completedFuture(
                        new DefaultTransferResult(
                                false, playerId, adapter.serverId(), "unknown",
                                TransferResult.FailureReason.SERVER_OFFLINE,
                                "No matching servers available",
                                Map.of(), 0
                        )
                );
            }
            return send(playerId, server.get());
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<TransferResult>> sendAll(
            @NotNull Collection<UUID> playerIds,
            @NotNull String server
    ) {
        return CompletableFuture.supplyAsync(() ->
                playerIds.stream()
                        .map(id -> send(id, server))
                        .map(CompletableFuture::join)
                        .toList()
        );
    }

    @Override
    @NotNull
    public TransferBuilder transfer(@NotNull UUID playerId) {
        return new DefaultTransferBuilder(playerId);
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> findPlayer(@NotNull UUID playerId) {
        return adapter.findPlayerServer(playerId)
                .thenApply(Optional::ofNullable)
                .exceptionally(ex -> Optional.empty());
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<ServerInfo>> findPlayerServer(@NotNull UUID playerId) {
        return findPlayer(playerId)
                .thenApply(serverId -> serverId.flatMap(messaging::server));
    }

    @Override
    @NotNull
    public CompletableFuture<Void> kick(@NotNull UUID playerId, @Nullable String reason) {
        return adapter.kickPlayer(playerId, reason);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> networkPlayerCount() {
        return adapter.networkPlayerCount();
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<UUID>> playersOn(@NotNull String server) {
        return adapter.playersOn(server);
    }

    private CompletableFuture<Optional<String>> findBestServerInGroup(String group) {
        return CompletableFuture.supplyAsync(() ->
                messaging.servers().stream()
                        .filter(s -> !s.id().equals(adapter.serverId()))
                        .filter(ServerInfo::isOnline)
                        .filter(s -> s.group().map(g -> g.equals(group)).orElse(false))
                        .filter(ServerInfo::isAcceptingPlayers)
                        .min(Comparator.comparingInt(ServerInfo::playerCount))
                        .map(ServerInfo::id)
        );
    }

    /**
     * Default implementation of TransferResult.
     */
    private record DefaultTransferResult(
            boolean success,
            @NotNull UUID playerId,
            @NotNull String sourceServer,
            @NotNull String targetServer,
            @Nullable FailureReason reason,
            @Nullable String error,
            @NotNull Map<String, Object> transferData,
            long durationMs
    ) implements TransferResult {

        @Override
        @NotNull
        public Optional<FailureReason> failureReason() {
            return Optional.ofNullable(reason);
        }

        @Override
        @NotNull
        public Optional<String> errorMessage() {
            return Optional.ofNullable(error);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T data(@NotNull String key) {
            return (T) transferData.get(key);
        }
    }

    /**
     * Default implementation of TransferBuilder.
     */
    private class DefaultTransferBuilder implements TransferBuilder {
        private final UUID playerId;
        private String targetServer;
        private String targetGroup;
        private Predicate<ServerInfo> filter;
        private final Map<String, Object> data = new ConcurrentHashMap<>();
        private Duration timeout = DEFAULT_TIMEOUT;
        private boolean bypassLimit;
        private Runnable onArrival;
        private Consumer<TransferResult> onFailure;

        DefaultTransferBuilder(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        @NotNull
        public TransferBuilder to(@NotNull String server) {
            this.targetServer = server;
            this.targetGroup = null;
            this.filter = null;
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder toGroup(@NotNull String group) {
            this.targetServer = null;
            this.targetGroup = group;
            this.filter = null;
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder matching(@NotNull Predicate<ServerInfo> filter) {
            this.targetServer = null;
            this.targetGroup = null;
            this.filter = filter;
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder withData(@NotNull String key, @NotNull Object value) {
            this.data.put(key, value);
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder timeout(@NotNull Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder bypassLimit(boolean bypass) {
            this.bypassLimit = bypass;
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder onArrival(@NotNull Runnable callback) {
            this.onArrival = callback;
            return this;
        }

        @Override
        @NotNull
        public TransferBuilder onFailure(@NotNull Consumer<TransferResult> callback) {
            this.onFailure = callback;
            return this;
        }

        @Override
        @NotNull
        public CompletableFuture<TransferResult> execute() {
            CompletableFuture<TransferResult> result;

            if (targetServer != null) {
                result = send(playerId, targetServer);
            } else if (targetGroup != null) {
                result = sendToGroup(playerId, targetGroup);
            } else if (filter != null) {
                result = sendToMatching(playerId, filter);
            } else {
                return CompletableFuture.completedFuture(
                        new DefaultTransferResult(
                                false, playerId, adapter.serverId(), "unknown",
                                TransferResult.FailureReason.UNKNOWN,
                                "No target specified",
                                data, 0
                        )
                );
            }

            return result.whenComplete((r, ex) -> {
                if (ex != null || !r.success()) {
                    if (onFailure != null) {
                        TransferResult failResult = ex != null ?
                                new DefaultTransferResult(
                                        false, playerId, adapter.serverId(), "unknown",
                                        TransferResult.FailureReason.NETWORK_ERROR,
                                        ex.getMessage(), data, 0
                                ) : r;
                        onFailure.accept(failResult);
                    }
                } else if (onArrival != null) {
                    // Note: In a real implementation, we'd need to listen for
                    // a confirmation message from the target server
                    onArrival.run();
                }
            });
        }
    }
}
