/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.impl;

import sh.pcx.unified.messaging.Message;
import sh.pcx.unified.messaging.MessageChannel;
import sh.pcx.unified.messaging.MessagingService;
import sh.pcx.unified.messaging.PlayerTransfer;
import sh.pcx.unified.messaging.ServerInfo;
import sh.pcx.unified.network.messaging.MessageWrapper;
import sh.pcx.unified.network.messaging.channels.ChannelAdapter;
import sh.pcx.unified.network.messaging.serialization.MessageCodec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Default implementation of {@link MessagingService}.
 *
 * <p>This implementation provides cross-server messaging using pluggable
 * channel adapters for different transports (BungeeCord, Velocity, Redis).
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultMessagingService implements MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessagingService.class);

    private final ChannelAdapter adapter;
    private final MessageCodec codec;
    private final Map<String, DefaultMessageChannel<?>> channels;
    private final Map<UUID, PendingRequest<?>> pendingRequests;
    private final DefaultPlayerTransfer playerTransfer;
    private final DefaultServerInfo localServerInfo;

    private Duration defaultTimeout = Duration.ofSeconds(10);

    /**
     * Creates a new messaging service.
     *
     * @param adapter the channel adapter
     * @param codec   the message codec
     */
    public DefaultMessagingService(@NotNull ChannelAdapter adapter, @NotNull MessageCodec codec) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.channels = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.playerTransfer = new DefaultPlayerTransfer(adapter, this);
        this.localServerInfo = new DefaultServerInfo(adapter.serverId(), true);
    }

    @Override
    @NotNull
    public String serverId() {
        return adapter.serverId();
    }

    @Override
    @NotNull
    public ServerInfo serverInfo() {
        return localServerInfo;
    }

    @Override
    @NotNull
    public Collection<ServerInfo> servers() {
        return adapter.serverIds()
                .join()
                .stream()
                .map(id -> new DefaultServerInfo(id, id.equals(serverId())))
                .map(info -> (ServerInfo) info)
                .toList();
    }

    @Override
    @NotNull
    public Optional<ServerInfo> server(@NotNull String serverId) {
        return servers().stream()
                .filter(s -> s.id().equals(serverId))
                .findFirst();
    }

    @Override
    @NotNull
    public <T> MessageChannel<T> channel(@NotNull Class<T> messageClass) {
        Message annotation = messageClass.getAnnotation(Message.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Message class must have @Message annotation: " + messageClass.getName()
            );
        }
        return channel(annotation.channel(), messageClass);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> MessageChannel<T> channel(@NotNull String channelName, @NotNull Class<T> messageClass) {
        return (MessageChannel<T>) channels.computeIfAbsent(channelName, name -> {
            var channel = new DefaultMessageChannel<>(name, messageClass, adapter, codec, serverId());
            adapter.registerChannel(name);
            adapter.subscribe(name, msg -> channel.handleIncoming(msg.data()));
            return channel;
        });
    }

    @Override
    public boolean closeChannel(@NotNull String channelName) {
        DefaultMessageChannel<?> channel = channels.remove(channelName);
        if (channel != null) {
            channel.close();
            adapter.unregisterChannel(channelName);
            return true;
        }
        return false;
    }

    @Override
    @NotNull
    public Collection<String> channelNames() {
        return Set.copyOf(channels.keySet());
    }

    @Override
    @NotNull
    public CompletableFuture<Void> send(@NotNull Object message) {
        MessageWrapper<?> wrapper = MessageWrapper.wrap(message, serverId());
        return broadcast(message);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendTo(@NotNull String targetServer, @NotNull Object message) {
        Message annotation = getMessageAnnotation(message);
        MessageWrapper<?> wrapper = MessageWrapper.wrap(message, serverId(), targetServer);
        byte[] data = codec.encode(wrapper);
        return adapter.sendTo(annotation.channel(), targetServer, data);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> broadcast(@NotNull Object message) {
        Message annotation = getMessageAnnotation(message);
        MessageWrapper<?> wrapper = MessageWrapper.wrap(message, serverId());
        byte[] data = codec.encode(wrapper);
        return adapter.broadcast(annotation.channel(), data);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull Object message) {
        Message annotation = getMessageAnnotation(message);
        MessageWrapper<?> wrapper = MessageWrapper.wrap(message, serverId());
        byte[] data = codec.encode(wrapper);
        return adapter.sendToPlayer(annotation.channel(), playerId, data);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendToMany(@NotNull Collection<String> servers, @NotNull Object message) {
        Message annotation = getMessageAnnotation(message);
        MessageWrapper<?> wrapper = MessageWrapper.wrap(message, serverId());
        byte[] data = codec.encode(wrapper);

        return CompletableFuture.allOf(
                servers.stream()
                        .map(server -> adapter.sendTo(annotation.channel(), server, data))
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> request(@NotNull Object request) {
        Message annotation = getMessageAnnotation(request);
        if (annotation.responseType() == Void.class) {
            throw new IllegalArgumentException("Request message must specify responseType");
        }
        // For broadcasts, we take the first response
        return request(null, request, (Class<R>) annotation.responseType(), defaultTimeout);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> request(@NotNull String targetServer, @NotNull Object request) {
        Message annotation = getMessageAnnotation(request);
        if (annotation.responseType() == Void.class) {
            throw new IllegalArgumentException("Request message must specify responseType");
        }
        return request(targetServer, request, (Class<R>) annotation.responseType(), defaultTimeout);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull Object request,
            @NotNull Duration timeout
    ) {
        Message annotation = getMessageAnnotation(request);
        if (annotation.responseType() == Void.class) {
            throw new IllegalArgumentException("Request message must specify responseType");
        }
        return request(targetServer, request, (Class<R>) annotation.responseType(), timeout);
    }

    @Override
    @NotNull
    public <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull Object request,
            @NotNull Class<R> responseClass,
            @NotNull Duration timeout
    ) {
        Message annotation = getMessageAnnotation(request);
        MessageWrapper<?> wrapper = MessageWrapper.wrap(request, serverId(), targetServer);
        byte[] data = codec.encode(wrapper);

        CompletableFuture<R> future = new CompletableFuture<>();
        PendingRequest<R> pending = new PendingRequest<>(wrapper.messageId(), responseClass, future);
        pendingRequests.put(wrapper.messageId(), pending);

        // Send the request
        adapter.sendTo(annotation.channel(), targetServer, data)
                .exceptionally(ex -> {
                    pendingRequests.remove(wrapper.messageId());
                    future.completeExceptionally(ex);
                    return null;
                });

        // Apply timeout
        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((result, ex) -> {
                    pendingRequests.remove(wrapper.messageId());
                    if (ex instanceof TimeoutException) {
                        logger.warn("Request {} timed out after {}", wrapper.messageId(), timeout);
                    }
                });
    }

    @Override
    @NotNull
    public <T> MessageChannel.Subscription subscribe(
            @NotNull Class<T> messageClass,
            @NotNull Consumer<T> handler
    ) {
        MessageChannel<T> ch = channel(messageClass);
        return ch.subscribe(handler);
    }

    @Override
    @NotNull
    public <T, R> MessageChannel.Subscription handleRequests(
            @NotNull Class<T> requestClass,
            @NotNull RequestHandler<T, R> handler
    ) {
        Message annotation = requestClass.getAnnotation(Message.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Request class must have @Message annotation: " + requestClass.getName()
            );
        }

        return subscribe(requestClass, request -> {
            try {
                Object result = handler.handle(request);
                if (result instanceof CompletableFuture<?> futureResult) {
                    futureResult.thenAccept(response -> sendResponse(request, response));
                } else {
                    sendResponse(request, result);
                }
            } catch (Exception e) {
                logger.error("Error handling request", e);
            }
        });
    }

    private void sendResponse(Object request, Object response) {
        if (request instanceof MessageWrapper<?> wrapper) {
            MessageWrapper<?> responseWrapper = MessageWrapper.wrap(response, serverId())
                    .withTarget(wrapper.sourceServer())
                    .withCorrelation(wrapper.messageId());

            Message annotation = getMessageAnnotation(response);
            byte[] data = codec.encode(responseWrapper);
            adapter.sendTo(annotation.channel(), wrapper.sourceServer(), data);
        }
    }

    /**
     * Handles an incoming response message.
     *
     * @param wrapper the response wrapper
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handleResponse(MessageWrapper<?> wrapper) {
        wrapper.correlationId().ifPresent(correlationId -> {
            PendingRequest pending = pendingRequests.remove(correlationId);
            if (pending != null) {
                pending.future.complete(wrapper.payload());
            }
        });
    }

    @Override
    @NotNull
    public PlayerTransfer transfers() {
        return playerTransfer;
    }

    @Override
    @NotNull
    public Duration defaultTimeout() {
        return defaultTimeout;
    }

    @Override
    public void setDefaultTimeout(@NotNull Duration timeout) {
        this.defaultTimeout = Objects.requireNonNull(timeout);
    }

    @Override
    @NotNull
    public TransportType transportType() {
        return adapter.transportType();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> connect() {
        return adapter.connect();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> disconnect() {
        channels.values().forEach(DefaultMessageChannel::close);
        channels.clear();
        pendingRequests.clear();
        return adapter.disconnect();
    }

    @Override
    public boolean isConnected() {
        return adapter.isConnected();
    }

    @Override
    public boolean isAvailable() {
        return isConnected();
    }

    private Message getMessageAnnotation(Object message) {
        Message annotation = message.getClass().getAnnotation(Message.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Message class must have @Message annotation: " + message.getClass().getName()
            );
        }
        return annotation;
    }

    private record PendingRequest<R>(
            UUID requestId,
            Class<R> responseClass,
            CompletableFuture<R> future
    ) {}
}
