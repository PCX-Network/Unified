/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.condition;

import sh.pcx.unified.condition.Condition;
import sh.pcx.unified.condition.ConditionContext;
import sh.pcx.unified.condition.ConditionResult;
import sh.pcx.unified.condition.ConditionService;
import sh.pcx.unified.event.condition.ConditionChangeEvent;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Implementation of {@link ConditionService.ConditionWatch}.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
final class ConditionWatchImpl implements ConditionService.ConditionWatch {

    private final DefaultConditionService service;
    private final UnifiedPlayer player;
    private final UUID playerId;
    private final Condition condition;
    private final Duration interval;
    private final Consumer<ConditionResult> callback;

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicReference<ConditionResult> currentResult = new AtomicReference<>();
    private ScheduledFuture<?> checkTask;

    ConditionWatchImpl(
            DefaultConditionService service,
            UnifiedPlayer player,
            Condition condition,
            Duration interval,
            Consumer<ConditionResult> callback
    ) {
        this.service = service;
        this.player = player;
        this.playerId = player.getUniqueId();
        this.condition = condition;
        this.interval = interval;
        this.callback = callback;

        // Initial evaluation
        this.currentResult.set(service.evaluateWithResult(player, condition));
    }

    void start() {
        checkTask = service.getScheduler().scheduleAtFixedRate(
                this::check,
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void check() {
        if (!active.get()) {
            return;
        }

        try {
            ConditionContext context = ConditionContext.of(player);
            ConditionResult newResult = condition.evaluate(context);
            ConditionResult oldResult = currentResult.getAndSet(newResult);

            // Check if result changed
            if (oldResult != null && oldResult.passed() != newResult.passed()) {
                // Fire callback
                try {
                    callback.accept(newResult);
                } catch (Exception e) {
                    // Log but don't fail
                }

                // Fire event
                service.getEventBus().fire(new ConditionChangeEvent(player, condition, oldResult, newResult));
            }
        } catch (Exception e) {
            // Log but continue watching
        }
    }

    @Override
    public @NotNull Condition getCondition() {
        return condition;
    }

    @Override
    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    @Override
    public @NotNull ConditionResult getCurrentResult() {
        ConditionResult result = currentResult.get();
        return result != null ? result : ConditionResult.failure("Not evaluated");
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void stop() {
        if (active.compareAndSet(true, false)) {
            if (checkTask != null) {
                checkTask.cancel(false);
            }
            service.removeWatch(playerId, this);
        }
    }
}
