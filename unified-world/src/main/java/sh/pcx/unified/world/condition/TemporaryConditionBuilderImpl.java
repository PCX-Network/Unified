/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.condition;

import sh.pcx.unified.condition.TemporaryCondition;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implementation of {@link TemporaryCondition.Builder}.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
final class TemporaryConditionBuilderImpl implements TemporaryCondition.Builder {

    private final DefaultConditionService service;
    private final UnifiedPlayer player;
    private final String name;

    private Duration duration;
    private Consumer<UnifiedPlayer> onApply;
    private Consumer<UnifiedPlayer> onExpire;
    private Consumer<UnifiedPlayer> onCancel;
    private boolean persistent = false;
    private boolean replaceExisting = false;
    private boolean extendExisting = false;
    private final Map<String, Object> metadata = new HashMap<>();

    TemporaryConditionBuilderImpl(DefaultConditionService service, UnifiedPlayer player, String name) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    @Override
    public TemporaryCondition.@NotNull Builder duration(@NotNull Duration duration) {
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder onApply(@Nullable Consumer<UnifiedPlayer> action) {
        this.onApply = action;
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder onExpire(@Nullable Consumer<UnifiedPlayer> action) {
        this.onExpire = action;
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder onCancel(@Nullable Consumer<UnifiedPlayer> action) {
        this.onCancel = action;
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder metadata(@NotNull String key, @NotNull Object value) {
        this.metadata.put(
                Objects.requireNonNull(key, "key cannot be null"),
                Objects.requireNonNull(value, "value cannot be null")
        );
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder replaceExisting(boolean replace) {
        this.replaceExisting = replace;
        return this;
    }

    @Override
    public TemporaryCondition.@NotNull Builder extendExisting(boolean extend) {
        this.extendExisting = extend;
        return this;
    }

    @Override
    public @NotNull TemporaryCondition apply() {
        if (duration == null) {
            throw new IllegalStateException("Duration must be set");
        }

        // Check for existing condition
        Optional<TemporaryCondition> existing = service.getTemporary(player.getUniqueId(), name);
        if (existing.isPresent()) {
            TemporaryCondition existingCondition = existing.get();

            if (extendExisting && existingCondition.isActive()) {
                existingCondition.extend(duration);
                return existingCondition;
            }

            if (replaceExisting) {
                service.cancelTemporary(player.getUniqueId(), name);
            } else {
                throw new IllegalArgumentException(
                        "Temporary condition '" + name + "' already exists for player " + player.getName()
                );
            }
        }

        TemporaryConditionImpl condition = new TemporaryConditionImpl(
                service,
                name,
                player,
                duration,
                persistent,
                metadata,
                onApply,
                onExpire,
                onCancel
        );

        return service.registerTemporary(condition);
    }

    @Override
    public @NotNull TemporaryCondition applyIfAbsent() {
        Optional<TemporaryCondition> existing = service.getTemporary(player.getUniqueId(), name);
        if (existing.isPresent()) {
            return existing.get();
        }
        return apply();
    }
}
