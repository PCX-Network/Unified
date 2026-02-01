/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.condition;

import sh.pcx.unified.condition.TemporaryCondition;
import sh.pcx.unified.event.condition.TemporaryConditionCancelledEvent;
import sh.pcx.unified.event.condition.TemporaryConditionExpiredEvent;
import sh.pcx.unified.event.condition.TemporaryConditionExtendedEvent;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Implementation of {@link TemporaryCondition}.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
final class TemporaryConditionImpl implements TemporaryCondition {

    private final DefaultConditionService service;
    private final String name;
    private final UUID playerId;
    private final UnifiedPlayer player;
    private final Duration duration;
    private final Instant appliedAt;
    private volatile Instant expiresAt;
    private final boolean persistent;
    private final Map<String, Object> metadata;
    private final Consumer<UnifiedPlayer> onApply;
    private final Consumer<UnifiedPlayer> onExpire;
    private final Consumer<UnifiedPlayer> onCancel;

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private ScheduledFuture<?> expiryTask;

    TemporaryConditionImpl(
            DefaultConditionService service,
            String name,
            UnifiedPlayer player,
            Duration duration,
            boolean persistent,
            Map<String, Object> metadata,
            Consumer<UnifiedPlayer> onApply,
            Consumer<UnifiedPlayer> onExpire,
            Consumer<UnifiedPlayer> onCancel
    ) {
        this.service = service;
        this.name = name;
        this.playerId = player.getUniqueId();
        this.player = player;
        this.duration = duration;
        this.appliedAt = Instant.now();
        this.expiresAt = appliedAt.plus(duration);
        this.persistent = persistent;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
        this.onApply = onApply;
        this.onExpire = onExpire;
        this.onCancel = onCancel;

        // Execute on-apply callback
        if (onApply != null) {
            try {
                onApply.accept(player);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    @Override
    public @NotNull Instant getAppliedAt() {
        return appliedAt;
    }

    @Override
    public @NotNull Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public @NotNull Duration getDuration() {
        return duration;
    }

    @Override
    public @NotNull Duration getRemaining() {
        if (!active.get()) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    @Override
    public @NotNull Duration getElapsed() {
        return Duration.between(appliedAt, Instant.now());
    }

    @Override
    public double getProgress() {
        if (!active.get()) {
            return 1.0;
        }
        long elapsed = Duration.between(appliedAt, Instant.now()).toMillis();
        long total = Duration.between(appliedAt, expiresAt).toMillis();
        if (total <= 0) return 1.0;
        return Math.min(1.0, (double) elapsed / total);
    }

    @Override
    public boolean isActive() {
        return active.get() && !isExpired() && !isCancelled();
    }

    @Override
    public boolean isExpired() {
        return !active.get() && !cancelled.get();
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public @NotNull Optional<Object> getMetadata(@NotNull String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> getMetadata(@NotNull String key, @NotNull Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Instant extend(@NotNull Duration extension) {
        if (!active.get()) {
            throw new IllegalStateException("Cannot extend an inactive temporary condition");
        }

        Instant previousExpiry = expiresAt;
        expiresAt = expiresAt.plus(extension);

        // Fire event
        service.getEventBus().fire(new TemporaryConditionExtendedEvent(
                this, extension, previousExpiry, expiresAt
        ));

        return expiresAt;
    }

    @Override
    public void setExpiresAt(@NotNull Instant newExpiresAt) {
        Objects.requireNonNull(newExpiresAt, "expiresAt cannot be null");
        if (!active.get()) {
            throw new IllegalStateException("Cannot modify an inactive temporary condition");
        }
        if (newExpiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expiration time cannot be in the past");
        }

        Instant previousExpiry = expiresAt;
        Duration extension = Duration.between(previousExpiry, newExpiresAt);
        expiresAt = newExpiresAt;

        service.getEventBus().fire(new TemporaryConditionExtendedEvent(
                this, extension, previousExpiry, expiresAt
        ));
    }

    @Override
    public void cancel() {
        if (!active.compareAndSet(true, false)) {
            throw new IllegalStateException("Temporary condition is already inactive");
        }

        cancelled.set(true);
        Duration remaining = getRemaining();

        // Cancel expiry task if any
        if (expiryTask != null) {
            expiryTask.cancel(false);
        }

        // Execute on-cancel callback
        if (onCancel != null) {
            try {
                onCancel.accept(player);
            } catch (Exception e) {
                // Log but don't fail
            }
        }

        // Fire event
        service.getEventBus().fire(new TemporaryConditionCancelledEvent(this, remaining));

        // Remove from service
        service.removeTemporary(this);
    }

    @Override
    public void cancelSilently() {
        if (!active.compareAndSet(true, false)) {
            throw new IllegalStateException("Temporary condition is already inactive");
        }

        cancelled.set(true);

        // Cancel expiry task if any
        if (expiryTask != null) {
            expiryTask.cancel(false);
        }

        // Remove from service
        service.removeTemporary(this);
    }

    /**
     * Called when the condition expires naturally.
     */
    void expire() {
        if (!active.compareAndSet(true, false)) {
            return; // Already inactive
        }

        // Execute on-expire callback
        if (onExpire != null) {
            try {
                onExpire.accept(player);
            } catch (Exception e) {
                // Log but don't fail
            }
        }

        // Fire event
        service.getEventBus().fire(new TemporaryConditionExpiredEvent(this));

        // Remove from service
        service.removeTemporary(this);
    }

    @Override
    public String toString() {
        return "TemporaryCondition{" +
                "name='" + name + '\'' +
                ", playerId=" + playerId +
                ", duration=" + duration +
                ", remaining=" + getRemaining() +
                ", active=" + isActive() +
                '}';
    }
}
