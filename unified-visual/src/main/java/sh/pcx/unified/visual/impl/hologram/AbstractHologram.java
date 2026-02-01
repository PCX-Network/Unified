/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.hologram;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.hologram.Billboard;
import sh.pcx.unified.visual.hologram.Hologram;
import sh.pcx.unified.visual.hologram.HologramAnimation;
import sh.pcx.unified.visual.hologram.HologramClickEvent;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Abstract base implementation of {@link Hologram}.
 *
 * <p>Provides common functionality for all hologram implementations.
 * Subclasses implement platform-specific rendering logic.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractHologram implements Hologram {

    protected final UUID id;
    protected final String name;
    protected volatile UnifiedLocation location;
    protected final List<Component> lines;
    protected volatile Billboard billboard;
    protected volatile boolean globallyVisible;
    protected volatile boolean persistent;
    protected volatile boolean spawned;
    protected volatile boolean removed;
    protected volatile float lineSpacing;

    protected final Set<UUID> viewers;
    protected final Set<UUID> hiddenFrom;
    protected final Map<HologramAnimation.Type, HologramAnimation> activeAnimations;
    protected volatile Consumer<HologramClickEvent> clickHandler;

    /**
     * Constructs a new abstract hologram.
     *
     * @param location the hologram location
     * @param name     the optional hologram name
     */
    protected AbstractHologram(@NotNull UnifiedLocation location, @Nullable String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.location = location;
        this.lines = new CopyOnWriteArrayList<>();
        this.billboard = Billboard.CENTER;
        this.globallyVisible = true;
        this.persistent = false;
        this.spawned = false;
        this.removed = false;
        this.lineSpacing = 0.25f;
        this.viewers = ConcurrentHashMap.newKeySet();
        this.hiddenFrom = ConcurrentHashMap.newKeySet();
        this.activeAnimations = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public @NotNull Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public @NotNull UnifiedLocation getLocation() {
        return location;
    }

    @Override
    public void teleport(@NotNull UnifiedLocation location) {
        Objects.requireNonNull(location, "location cannot be null");
        this.location = location;
        if (spawned) {
            updateLocation();
        }
    }

    @Override
    public @NotNull List<Component> getLines() {
        return List.copyOf(lines);
    }

    @Override
    public @NotNull Component getLine(int index) {
        return lines.get(index);
    }

    @Override
    public void setLine(int index, @NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        lines.set(index, text);
        if (spawned) {
            updateLine(index);
        }
    }

    @Override
    public void addLine(@NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        lines.add(text);
        if (spawned) {
            respawnLines();
        }
    }

    @Override
    public void insertLine(int index, @NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        lines.add(index, text);
        if (spawned) {
            respawnLines();
        }
    }

    @Override
    public void removeLine(int index) {
        lines.remove(index);
        if (spawned) {
            respawnLines();
        }
    }

    @Override
    public void clearLines() {
        lines.clear();
        if (spawned) {
            despawnLines();
        }
    }

    @Override
    public int getLineCount() {
        return lines.size();
    }

    @Override
    public @NotNull Billboard getBillboard() {
        return billboard;
    }

    @Override
    public void setBillboard(@NotNull Billboard billboard) {
        Objects.requireNonNull(billboard, "billboard cannot be null");
        this.billboard = billboard;
        if (spawned) {
            updateBillboard();
        }
    }

    @Override
    public boolean isGloballyVisible() {
        return globallyVisible;
    }

    @Override
    public void setGloballyVisible(boolean visible) {
        this.globallyVisible = visible;
        if (spawned) {
            refreshVisibility();
        }
    }

    @Override
    public @NotNull Collection<UnifiedPlayer> getViewers() {
        // Implementation should return actual online players
        return List.of();
    }

    @Override
    public void showTo(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        viewers.add(player.getUniqueId());
        hiddenFrom.remove(player.getUniqueId());
        if (spawned) {
            spawnFor(player);
        }
    }

    @Override
    public void showTo(@NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(players, "players cannot be null");
        players.forEach(this::showTo);
    }

    @Override
    public void hideFrom(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        hiddenFrom.add(player.getUniqueId());
        viewers.remove(player.getUniqueId());
        if (spawned) {
            despawnFor(player);
        }
    }

    @Override
    public void hideFrom(@NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(players, "players cannot be null");
        players.forEach(this::hideFrom);
    }

    @Override
    public boolean isVisibleTo(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        UUID playerId = player.getUniqueId();
        if (hiddenFrom.contains(playerId)) {
            return false;
        }
        return globallyVisible || viewers.contains(playerId);
    }

    @Override
    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public void spawn() {
        if (removed) {
            throw new IllegalStateException("Cannot spawn a removed hologram");
        }
        if (!spawned) {
            spawned = true;
            doSpawn();
        }
    }

    @Override
    public void despawn() {
        if (spawned) {
            spawned = false;
            doDespawn();
        }
    }

    @Override
    public void remove() {
        if (!removed) {
            removed = true;
            stopAllAnimations();
            if (spawned) {
                doDespawn();
                spawned = false;
            }
            onRemove();
        }
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    @Override
    public void setClickHandler(@Nullable Consumer<HologramClickEvent> handler) {
        this.clickHandler = handler;
    }

    @Override
    public @NotNull Optional<Consumer<HologramClickEvent>> getClickHandler() {
        return Optional.ofNullable(clickHandler);
    }

    @Override
    public void startAnimation(@NotNull HologramAnimation animation) {
        Objects.requireNonNull(animation, "animation cannot be null");
        stopAnimation(animation.getType());
        activeAnimations.put(animation.getType(), animation);
        doStartAnimation(animation);
    }

    @Override
    public void stopAnimation(@NotNull HologramAnimation.Type animationType) {
        Objects.requireNonNull(animationType, "animationType cannot be null");
        HologramAnimation animation = activeAnimations.remove(animationType);
        if (animation != null) {
            doStopAnimation(animation);
        }
    }

    @Override
    public void stopAllAnimations() {
        for (HologramAnimation.Type type : List.copyOf(activeAnimations.keySet())) {
            stopAnimation(type);
        }
    }

    @Override
    public boolean isAnimating(@NotNull HologramAnimation.Type animationType) {
        return activeAnimations.containsKey(animationType);
    }

    @Override
    public void update() {
        if (spawned) {
            doUpdate();
        }
    }

    /**
     * Returns the line spacing.
     *
     * @return the line spacing in blocks
     */
    public float getLineSpacing() {
        return lineSpacing;
    }

    /**
     * Sets the line spacing.
     *
     * @param spacing the line spacing in blocks
     */
    public void setLineSpacing(float spacing) {
        this.lineSpacing = spacing;
        if (spawned) {
            respawnLines();
        }
    }

    // ==================== Abstract Methods ====================

    /**
     * Spawns the hologram entities.
     */
    protected abstract void doSpawn();

    /**
     * Despawns the hologram entities.
     */
    protected abstract void doDespawn();

    /**
     * Updates all hologram entities.
     */
    protected abstract void doUpdate();

    /**
     * Updates the location of hologram entities.
     */
    protected abstract void updateLocation();

    /**
     * Updates a specific line.
     *
     * @param index the line index
     */
    protected abstract void updateLine(int index);

    /**
     * Updates the billboard mode.
     */
    protected abstract void updateBillboard();

    /**
     * Respawns all line entities.
     */
    protected abstract void respawnLines();

    /**
     * Despawns all line entities.
     */
    protected abstract void despawnLines();

    /**
     * Refreshes visibility for all players.
     */
    protected abstract void refreshVisibility();

    /**
     * Spawns the hologram for a specific player.
     *
     * @param player the player
     */
    protected abstract void spawnFor(@NotNull UnifiedPlayer player);

    /**
     * Despawns the hologram for a specific player.
     *
     * @param player the player
     */
    protected abstract void despawnFor(@NotNull UnifiedPlayer player);

    /**
     * Starts an animation.
     *
     * @param animation the animation
     */
    protected abstract void doStartAnimation(@NotNull HologramAnimation animation);

    /**
     * Stops an animation.
     *
     * @param animation the animation
     */
    protected abstract void doStopAnimation(@NotNull HologramAnimation animation);

    /**
     * Called when the hologram is removed.
     */
    protected abstract void onRemove();
}
