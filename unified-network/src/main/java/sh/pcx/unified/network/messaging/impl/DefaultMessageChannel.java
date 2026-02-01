/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.impl;

import sh.pcx.unified.messaging.MessageChannel;
import sh.pcx.unified.network.messaging.MessageWrapper;
import sh.pcx.unified.network.messaging.channels.ChannelAdapter;
import sh.pcx.unified.network.messaging.serialization.MessageCodec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Default implementation of {@link MessageChannel}.
 *
 * @param <T> the message type
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultMessageChannel<T> implements MessageChannel<T> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageChannel.class);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String name;
    private final Class<T> messageType;
    private final ChannelAdapter adapter;
    private final MessageCodec codec;
    private final String serverId;
    private final CopyOnWriteArrayList<SubscriptionImpl> subscriptions;
    private final AtomicBoolean open;
    private final DefaultChannelStats stats;

    /**
     * Creates a new message channel.
     *
     * @param name        the channel name
     * @param messageType the message class
     * @param adapter     the channel adapter
     * @param codec       the message codec
     * @param serverId    the local server ID
     */
    public DefaultMessageChannel(
            @NotNull String name,
            @NotNull Class<T> messageType,
            @NotNull ChannelAdapter adapter,
            @NotNull MessageCodec codec,
            @NotNull String serverId
    ) {
        this.name = Objects.requireNonNull(name);
        this.messageType = Objects.requireNonNull(messageType);
        this.adapter = Objects.requireNonNull(adapter);
        this.codec = Objects.requireNonNull(codec);
        this.serverId = Objects.requireNonNull(serverId);
        this.subscriptions = new CopyOnWriteArrayList<>();
        this.open = new AtomicBoolean(true);
        this.stats = new DefaultChannelStats();
    }

    @Override
    @NotNull
    public String name() {
        return name;
    }

    @Override
    @NotNull
    public Class<T> messageType() {
        return messageType;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> send(@NotNull T message) {
        return broadcast(message);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendTo(@NotNull String server, @NotNull T message) {
        ensureOpen();
        MessageWrapper<T> wrapper = MessageWrapper.wrap(message, serverId, server);
        byte[] data = codec.encode(wrapper);
        stats.recordSend(data.length);
        return adapter.sendTo(name, server, data);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> broadcast(@NotNull T message) {
        ensureOpen();
        MessageWrapper<T> wrapper = MessageWrapper.wrap(message, serverId);
        byte[] data = codec.encode(wrapper);
        stats.recordSend(data.length);
        return adapter.broadcast(name, data);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull T message) {
        ensureOpen();
        MessageWrapper<T> wrapper = MessageWrapper.wrap(message, serverId);
        byte[] data = codec.encode(wrapper);
        stats.recordSend(data.length);
        return adapter.sendToPlayer(name, playerId, data);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendToMany(@NotNull Collection<String> servers, @NotNull T message) {
        ensureOpen();
        MessageWrapper<T> wrapper = MessageWrapper.wrap(message, serverId);
        byte[] data = codec.encode(wrapper);
        stats.recordSend(data.length);

        return CompletableFuture.allOf(
                servers.stream()
                        .map(server -> adapter.sendTo(name, server, data))
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    @NotNull
    public CompletableFuture<Void> sendExcluding(@NotNull Collection<String> excludedServers, @NotNull T message) {
        return adapter.serverIds().thenCompose(allServers -> {
            Collection<String> targets = allServers.stream()
                    .filter(s -> !excludedServers.contains(s))
                    .filter(s -> !s.equals(serverId))
                    .toList();
            return sendToMany(targets, message);
        });
    }

    @Override
    @NotNull
    public <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull T request,
            @NotNull Class<R> responseType
    ) {
        return request(targetServer, request, responseType, DEFAULT_REQUEST_TIMEOUT);
    }

    @Override
    @NotNull
    public <R> CompletableFuture<R> request(
            @NotNull String targetServer,
            @NotNull T request,
            @NotNull Class<R> responseType,
            @NotNull Duration timeout
    ) {
        ensureOpen();
        MessageWrapper<T> wrapper = MessageWrapper.wrap(request, serverId, targetServer);
        byte[] data = codec.encode(wrapper);
        stats.recordSend(data.length);

        CompletableFuture<R> future = new CompletableFuture<>();

        // Register for response
        UUID requestId = wrapper.messageId();
        Subscription responseSub = subscribe(msg -> {
            // Check if this is a response to our request
            // This is a simplified version - real implementation would use correlation IDs
        });

        adapter.sendTo(name, targetServer, data)
                .exceptionally(ex -> {
                    responseSub.cancel();
                    future.completeExceptionally(ex);
                    return null;
                });

        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((r, ex) -> responseSub.cancel());
    }

    @Override
    @NotNull
    public Subscription subscribe(@NotNull Consumer<T> handler) {
        return subscribe(msg -> true, handler);
    }

    @Override
    @NotNull
    public Subscription subscribe(@NotNull Predicate<T> filter, @NotNull Consumer<T> handler) {
        ensureOpen();
        SubscriptionImpl sub = new SubscriptionImpl(filter, handler);
        subscriptions.add(sub);
        return sub;
    }

    @Override
    @NotNull
    public MessageChannel<T> filter(@NotNull Predicate<T> filter) {
        return new FilteredMessageChannel<>(this, filter);
    }

    @Override
    @NotNull
    public MessageChannel<T> fromServer(@NotNull String sourceServer) {
        return filter(msg -> {
            if (msg instanceof MessageWrapper<?> wrapper) {
                return sourceServer.equals(wrapper.sourceServer());
            }
            return true;
        });
    }

    @Override
    @NotNull
    public MessageChannel<T> broadcastsOnly() {
        return filter(msg -> {
            if (msg instanceof MessageWrapper<?> wrapper) {
                return wrapper.isBroadcast();
            }
            return true;
        });
    }

    @Override
    public void close() {
        if (open.compareAndSet(true, false)) {
            subscriptions.forEach(SubscriptionImpl::cancel);
            subscriptions.clear();
        }
    }

    @Override
    @NotNull
    public ChannelStats stats() {
        return stats;
    }

    /**
     * Handles incoming message data.
     *
     * @param data the serialized message data
     */
    @SuppressWarnings("unchecked")
    public void handleIncoming(byte[] data) {
        if (!isOpen()) return;

        try {
            stats.recordReceive(data.length);
            MessageWrapper<?> wrapper = codec.decode(data, messageType);
            T message = (T) wrapper.payload();

            for (SubscriptionImpl sub : subscriptions) {
                if (sub.isActive() && sub.filter.test(message)) {
                    try {
                        sub.handler.accept(message);
                    } catch (Exception e) {
                        logger.error("Error in message handler", e);
                        stats.recordError();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error decoding message", e);
            stats.recordError();
        }
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("Channel is closed: " + name);
        }
    }

    /**
     * Implementation of Subscription.
     */
    private class SubscriptionImpl implements Subscription {
        private final Predicate<T> filter;
        private final Consumer<T> handler;
        private final AtomicBoolean active = new AtomicBoolean(true);

        SubscriptionImpl(Predicate<T> filter, Consumer<T> handler) {
            this.filter = filter;
            this.handler = handler;
        }

        @Override
        public boolean isActive() {
            return active.get() && isOpen();
        }

        @Override
        public void cancel() {
            if (active.compareAndSet(true, false)) {
                subscriptions.remove(this);
            }
        }
    }

    /**
     * Implementation of ChannelStats.
     */
    private static class DefaultChannelStats implements ChannelStats {
        private final AtomicLong messagesSent = new AtomicLong();
        private final AtomicLong messagesReceived = new AtomicLong();
        private final AtomicLong bytesSent = new AtomicLong();
        private final AtomicLong bytesReceived = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
        private int subscriptionCount = 0;

        void recordSend(int bytes) {
            messagesSent.incrementAndGet();
            bytesSent.addAndGet(bytes);
        }

        void recordReceive(int bytes) {
            messagesReceived.incrementAndGet();
            bytesReceived.addAndGet(bytes);
        }

        void recordError() {
            errors.incrementAndGet();
        }

        @Override
        public long messagesSent() {
            return messagesSent.get();
        }

        @Override
        public long messagesReceived() {
            return messagesReceived.get();
        }

        @Override
        public long bytesSent() {
            return bytesSent.get();
        }

        @Override
        public long bytesReceived() {
            return bytesReceived.get();
        }

        @Override
        public int subscriptionCount() {
            return subscriptionCount;
        }

        @Override
        public long errorCount() {
            return errors.get();
        }

        @Override
        public void reset() {
            messagesSent.set(0);
            messagesReceived.set(0);
            bytesSent.set(0);
            bytesReceived.set(0);
            errors.set(0);
        }
    }

    /**
     * A filtered view of a channel.
     */
    private static class FilteredMessageChannel<T> implements MessageChannel<T> {
        private final DefaultMessageChannel<T> delegate;
        private final Predicate<T> filter;

        FilteredMessageChannel(DefaultMessageChannel<T> delegate, Predicate<T> filter) {
            this.delegate = delegate;
            this.filter = filter;
        }

        @Override
        @NotNull
        public String name() {
            return delegate.name();
        }

        @Override
        @NotNull
        public Class<T> messageType() {
            return delegate.messageType();
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        @NotNull
        public CompletableFuture<Void> send(@NotNull T message) {
            return delegate.send(message);
        }

        @Override
        @NotNull
        public CompletableFuture<Void> sendTo(@NotNull String server, @NotNull T message) {
            return delegate.sendTo(server, message);
        }

        @Override
        @NotNull
        public CompletableFuture<Void> broadcast(@NotNull T message) {
            return delegate.broadcast(message);
        }

        @Override
        @NotNull
        public CompletableFuture<Void> sendToPlayer(@NotNull UUID playerId, @NotNull T message) {
            return delegate.sendToPlayer(playerId, message);
        }

        @Override
        @NotNull
        public CompletableFuture<Void> sendToMany(@NotNull Collection<String> servers, @NotNull T message) {
            return delegate.sendToMany(servers, message);
        }

        @Override
        @NotNull
        public CompletableFuture<Void> sendExcluding(@NotNull Collection<String> excludedServers, @NotNull T message) {
            return delegate.sendExcluding(excludedServers, message);
        }

        @Override
        @NotNull
        public <R> CompletableFuture<R> request(@NotNull String targetServer, @NotNull T request, @NotNull Class<R> responseType) {
            return delegate.request(targetServer, request, responseType);
        }

        @Override
        @NotNull
        public <R> CompletableFuture<R> request(@NotNull String targetServer, @NotNull T request, @NotNull Class<R> responseType, @NotNull Duration timeout) {
            return delegate.request(targetServer, request, responseType, timeout);
        }

        @Override
        @NotNull
        public Subscription subscribe(@NotNull Consumer<T> handler) {
            return delegate.subscribe(filter, handler);
        }

        @Override
        @NotNull
        public Subscription subscribe(@NotNull Predicate<T> additionalFilter, @NotNull Consumer<T> handler) {
            return delegate.subscribe(msg -> filter.test(msg) && additionalFilter.test(msg), handler);
        }

        @Override
        @NotNull
        public MessageChannel<T> filter(@NotNull Predicate<T> additionalFilter) {
            return new FilteredMessageChannel<>(delegate, msg -> filter.test(msg) && additionalFilter.test(msg));
        }

        @Override
        @NotNull
        public MessageChannel<T> fromServer(@NotNull String sourceServer) {
            return delegate.fromServer(sourceServer);
        }

        @Override
        @NotNull
        public MessageChannel<T> broadcastsOnly() {
            return delegate.broadcastsOnly();
        }

        @Override
        public void close() {
            // Don't close the delegate, just this view
        }

        @Override
        @NotNull
        public ChannelStats stats() {
            return delegate.stats();
        }
    }
}
