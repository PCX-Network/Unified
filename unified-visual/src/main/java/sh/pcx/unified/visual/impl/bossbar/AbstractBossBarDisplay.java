/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.bossbar;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.bossbar.BossBarColor;
import sh.pcx.unified.visual.bossbar.BossBarDisplay;
import sh.pcx.unified.visual.bossbar.BossBarOverlay;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link BossBarDisplay}.
 *
 * <p>Provides common boss bar functionality. Subclasses implement
 * platform-specific rendering logic.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractBossBarDisplay implements BossBarDisplay {

    protected final UUID id;
    protected volatile Component title;
    protected volatile float progress;
    protected volatile BossBarColor color;
    protected volatile BossBarOverlay overlay;
    protected volatile boolean darkenSky;
    protected volatile boolean playBossMusic;
    protected volatile boolean createFog;
    protected volatile boolean visible;
    protected volatile boolean removed;

    protected final Set<UUID> playerIds;

    // Progress animation state
    protected volatile boolean animatingProgress;
    protected volatile float animationStartProgress;
    protected volatile float animationEndProgress;
    protected volatile long animationStartTime;
    protected volatile long animationDuration;

    /**
     * Constructs a new abstract boss bar display.
     */
    protected AbstractBossBarDisplay() {
        this.id = UUID.randomUUID();
        this.title = Component.empty();
        this.progress = 1.0f;
        this.color = BossBarColor.PURPLE;
        this.overlay = BossBarOverlay.PROGRESS;
        this.darkenSky = false;
        this.playBossMusic = false;
        this.createFog = false;
        this.visible = true;
        this.removed = false;
        this.playerIds = ConcurrentHashMap.newKeySet();
        this.animatingProgress = false;
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public void setTitle(@NotNull Component title) {
        Objects.requireNonNull(title, "title cannot be null");
        this.title = title;
        if (visible) {
            updateTitle();
        }
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public void setProgress(float progress) {
        if (progress < 0.0f || progress > 1.0f) {
            throw new IllegalArgumentException("Progress must be between 0.0 and 1.0");
        }
        this.progress = progress;
        if (visible) {
            updateProgress();
        }
    }

    @Override
    public void animateProgress(float target, @NotNull Duration duration) {
        animateProgress(this.progress, target, duration);
    }

    @Override
    public void animateProgress(float start, float end, @NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        if (start < 0.0f || start > 1.0f || end < 0.0f || end > 1.0f) {
            throw new IllegalArgumentException("Progress values must be between 0.0 and 1.0");
        }

        stopProgressAnimation();

        this.animationStartProgress = start;
        this.animationEndProgress = end;
        this.animationStartTime = System.currentTimeMillis();
        this.animationDuration = duration.toMillis();
        this.animatingProgress = true;
        this.progress = start;

        startProgressAnimationTask();
    }

    @Override
    public void stopProgressAnimation() {
        animatingProgress = false;
        doStopProgressAnimation();
    }

    @Override
    public boolean isAnimatingProgress() {
        return animatingProgress;
    }

    @Override
    public @NotNull BossBarColor getColor() {
        return color;
    }

    @Override
    public void setColor(@NotNull BossBarColor color) {
        Objects.requireNonNull(color, "color cannot be null");
        this.color = color;
        if (visible) {
            updateColor();
        }
    }

    @Override
    public @NotNull BossBarOverlay getOverlay() {
        return overlay;
    }

    @Override
    public void setOverlay(@NotNull BossBarOverlay overlay) {
        Objects.requireNonNull(overlay, "overlay cannot be null");
        this.overlay = overlay;
        if (visible) {
            updateOverlay();
        }
    }

    @Override
    public boolean isDarkenSky() {
        return darkenSky;
    }

    @Override
    public void setDarkenSky(boolean darkenSky) {
        this.darkenSky = darkenSky;
        if (visible) {
            updateFlags();
        }
    }

    @Override
    public boolean isPlayBossMusic() {
        return playBossMusic;
    }

    @Override
    public void setPlayBossMusic(boolean playMusic) {
        this.playBossMusic = playMusic;
        if (visible) {
            updateFlags();
        }
    }

    @Override
    public boolean isCreateFog() {
        return createFog;
    }

    @Override
    public void setCreateFog(boolean createFog) {
        this.createFog = createFog;
        if (visible) {
            updateFlags();
        }
    }

    @Override
    public @NotNull Collection<UnifiedPlayer> getPlayers() {
        // Implementation should return actual online players
        return List.of();
    }

    @Override
    public void addPlayer(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        if (playerIds.add(player.getUniqueId()) && visible) {
            doAddPlayer(player);
        }
    }

    @Override
    public void addPlayers(@NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(players, "players cannot be null");
        players.forEach(this::addPlayer);
    }

    @Override
    public void removePlayer(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        if (playerIds.remove(player.getUniqueId()) && visible) {
            doRemovePlayer(player);
        }
    }

    @Override
    public void removeAllPlayers() {
        Collection<UnifiedPlayer> currentPlayers = getPlayers();
        playerIds.clear();
        if (visible) {
            currentPlayers.forEach(this::doRemovePlayer);
        }
    }

    @Override
    public boolean hasPlayer(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return playerIds.contains(player.getUniqueId());
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            if (visible) {
                doShow();
            } else {
                doHide();
            }
        }
    }

    @Override
    public void remove() {
        if (!removed) {
            removed = true;
            stopProgressAnimation();
            if (visible) {
                doHide();
                visible = false;
            }
            onRemove();
        }
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    /**
     * Updates the progress during animation.
     */
    protected void tickProgressAnimation() {
        if (!animatingProgress) return;

        long elapsed = System.currentTimeMillis() - animationStartTime;
        float t = Math.min(1.0f, (float) elapsed / animationDuration);

        progress = animationStartProgress + (animationEndProgress - animationStartProgress) * t;

        if (visible) {
            updateProgress();
        }

        if (t >= 1.0f) {
            animatingProgress = false;
            doStopProgressAnimation();
        }
    }

    // ==================== Abstract Methods ====================

    /**
     * Shows the boss bar to all players.
     */
    protected abstract void doShow();

    /**
     * Hides the boss bar from all players.
     */
    protected abstract void doHide();

    /**
     * Updates the title display.
     */
    protected abstract void updateTitle();

    /**
     * Updates the progress display.
     */
    protected abstract void updateProgress();

    /**
     * Updates the color display.
     */
    protected abstract void updateColor();

    /**
     * Updates the overlay display.
     */
    protected abstract void updateOverlay();

    /**
     * Updates the flag settings (darken sky, music, fog).
     */
    protected abstract void updateFlags();

    /**
     * Adds a player to the boss bar.
     *
     * @param player the player
     */
    protected abstract void doAddPlayer(@NotNull UnifiedPlayer player);

    /**
     * Removes a player from the boss bar.
     *
     * @param player the player
     */
    protected abstract void doRemovePlayer(@NotNull UnifiedPlayer player);

    /**
     * Starts the progress animation task.
     */
    protected abstract void startProgressAnimationTask();

    /**
     * Stops the progress animation task.
     */
    protected abstract void doStopProgressAnimation();

    /**
     * Called when the boss bar is removed.
     */
    protected abstract void onRemove();
}
