/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.channels;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Abstract base implementation for channel adapters.
 *
 * <p>Provides common functionality for managing channels, subscriptions,
 * and message handling.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractChannelAdapter implements ChannelAdapter {

    protected final Logger logger;
    protected final String serverId;
    protected final AtomicBoolean connected;
    protected final Set<String> registeredChannels;
    protected final Map<String, Consumer<ReceivedMessage>> handlers;
    protected final Executor executor;

    /**
     * Creates a new adapter.
     *
     * @param serverId the current server ID
     */
    protected AbstractChannelAdapter(@NotNull String serverId) {
        this.serverId = serverId;
        this.logger = LoggerFactory.getLogger(getClass());
        this.connected = new AtomicBoolean(false);
        this.registeredChannels = ConcurrentHashMap.newKeySet();
        this.handlers = new ConcurrentHashMap<>();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    @NotNull
    public String serverId() {
        return serverId;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> registerChannel(@NotNull String channelName) {
        if (registeredChannels.add(channelName)) {
            logger.debug("Registered channel: {}", channelName);
            return doRegisterChannel(channelName);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> unregisterChannel(@NotNull String channelName) {
        if (registeredChannels.remove(channelName)) {
            handlers.remove(channelName);
            logger.debug("Unregistered channel: {}", channelName);
            return doUnregisterChannel(channelName);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribe(@NotNull String channelName, @NotNull Consumer<ReceivedMessage> handler) {
        handlers.put(channelName, handler);
        if (!registeredChannels.contains(channelName)) {
            registerChannel(channelName);
        }
    }

    @Override
    public void unsubscribe(@NotNull String channelName) {
        handlers.remove(channelName);
    }

    /**
     * Dispatches a received message to handlers.
     *
     * @param message the received message
     */
    protected void dispatchMessage(@NotNull ReceivedMessage message) {
        Consumer<ReceivedMessage> handler = handlers.get(message.channel());
        if (handler != null) {
            executor.execute(() -> {
                try {
                    handler.accept(message);
                } catch (Exception e) {
                    logger.error("Error handling message on channel {}", message.channel(), e);
                }
            });
        }
    }

    /**
     * Performs the actual channel registration.
     *
     * @param channelName the channel name
     * @return future completing when registered
     */
    @NotNull
    protected abstract CompletableFuture<Void> doRegisterChannel(@NotNull String channelName);

    /**
     * Performs the actual channel unregistration.
     *
     * @param channelName the channel name
     * @return future completing when unregistered
     */
    @NotNull
    protected abstract CompletableFuture<Void> doUnregisterChannel(@NotNull String channelName);

    /**
     * Ensures the adapter is connected.
     *
     * @throws IllegalStateException if not connected
     */
    protected void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Channel adapter is not connected");
        }
    }

    /**
     * Simple implementation of ReceivedMessage.
     */
    protected record SimpleReceivedMessage(
            @NotNull String channel,
            @NotNull String sourceServer,
            byte @NotNull [] data
    ) implements ReceivedMessage {}
}
