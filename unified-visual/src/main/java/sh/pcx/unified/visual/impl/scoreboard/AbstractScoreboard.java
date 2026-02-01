/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.scoreboard;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.core.LegacyScoreboard;
import sh.pcx.unified.visual.scoreboard.core.ScoreboardLineUpdater;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Abstract base implementation of {@link LegacyScoreboard}.
 *
 * <p>Provides common scoreboard functionality. Subclasses implement
 * platform-specific rendering logic.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractScoreboard implements LegacyScoreboard {

    protected final UUID id;
    protected final UnifiedPlayer player;
    protected volatile Component title;
    protected final Map<Integer, Component> lines;
    protected final Map<Integer, Predicate<UnifiedPlayer>> conditions;
    protected final Map<Integer, ScoreboardLineUpdater> updaters;
    protected volatile int updateInterval;
    protected volatile boolean visible;
    protected volatile boolean removed;

    // Title animation state
    protected volatile List<Component> titleFrames;
    protected volatile int titleFrameIndex;
    protected volatile int titleTicksPerFrame;
    protected volatile boolean titleAnimating;

    /**
     * Constructs a new abstract scoreboard.
     *
     * @param player the scoreboard owner
     */
    protected AbstractScoreboard(@NotNull UnifiedPlayer player) {
        this.id = UUID.randomUUID();
        this.player = player;
        this.title = Component.empty();
        this.lines = new ConcurrentHashMap<>();
        this.conditions = new ConcurrentHashMap<>();
        this.updaters = new ConcurrentHashMap<>();
        this.updateInterval = 20;
        this.visible = false;
        this.removed = false;
        this.titleFrames = List.of();
        this.titleAnimating = false;
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public @NotNull UnifiedPlayer getPlayer() {
        return player;
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
    public void animateTitle(@NotNull List<Component> frames, int ticksPerFrame) {
        Objects.requireNonNull(frames, "frames cannot be null");
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("frames cannot be empty");
        }
        this.titleFrames = List.copyOf(frames);
        this.titleTicksPerFrame = ticksPerFrame;
        this.titleFrameIndex = 0;
        this.titleAnimating = true;
        this.title = frames.getFirst();
        startTitleAnimation();
    }

    @Override
    public void stopTitleAnimation() {
        titleAnimating = false;
        doStopTitleAnimation();
    }

    @Override
    public boolean isTitleAnimating() {
        return titleAnimating;
    }

    @Override
    public @NotNull Map<Integer, Component> getLines() {
        return Map.copyOf(lines);
    }

    @Override
    public @NotNull Optional<Component> getLine(int score) {
        return Optional.ofNullable(lines.get(score));
    }

    @Override
    public void setLine(int score, @NotNull Component content) {
        Objects.requireNonNull(content, "content cannot be null");
        lines.put(score, content);
        conditions.remove(score);
        if (visible) {
            updateLine(score);
        }
    }

    @Override
    public void setLine(int score, @NotNull Component content, @NotNull Predicate<UnifiedPlayer> condition) {
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(condition, "condition cannot be null");
        lines.put(score, content);
        conditions.put(score, condition);
        if (visible) {
            updateLine(score);
        }
    }

    @Override
    public void removeLine(int score) {
        lines.remove(score);
        conditions.remove(score);
        updaters.remove(score);
        if (visible) {
            doRemoveLine(score);
        }
    }

    @Override
    public void clearLines() {
        lines.clear();
        conditions.clear();
        updaters.clear();
        if (visible) {
            doClearLines();
        }
    }

    @Override
    public void setLines(@NotNull List<Component> lineList) {
        Objects.requireNonNull(lineList, "lines cannot be null");
        clearLines();
        int score = 15;
        for (Component line : lineList) {
            if (score < 1) break;
            lines.put(score--, line);
        }
        if (visible) {
            refreshLines();
        }
    }

    @Override
    public void setLines(@NotNull Map<Integer, Component> lineMap) {
        Objects.requireNonNull(lineMap, "lines cannot be null");
        clearLines();
        lines.putAll(lineMap);
        if (visible) {
            refreshLines();
        }
    }

    @Override
    public int getLineCount() {
        return lines.size();
    }

    @Override
    public void addLineUpdater(int score, @NotNull ScoreboardLineUpdater updater) {
        Objects.requireNonNull(updater, "updater cannot be null");
        updaters.put(score, updater);
    }

    @Override
    public void removeLineUpdater(int score) {
        updaters.remove(score);
    }

    @Override
    public int getUpdateInterval() {
        return updateInterval;
    }

    @Override
    public void setUpdateInterval(int ticks) {
        this.updateInterval = Math.max(1, ticks);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void show() {
        if (removed) {
            throw new IllegalStateException("Cannot show a removed scoreboard");
        }
        if (!visible) {
            visible = true;
            doShow();
        }
    }

    @Override
    public void hide() {
        if (visible) {
            visible = false;
            doHide();
        }
    }

    @Override
    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    @Override
    public void update() {
        if (!visible || removed) return;

        // Update dynamic lines
        for (var entry : updaters.entrySet()) {
            int score = entry.getKey();
            ScoreboardLineUpdater updater = entry.getValue();
            Component content = updater.update(player);
            lines.put(score, content);
            updateLine(score);
        }

        // Check conditions
        for (var entry : conditions.entrySet()) {
            int score = entry.getKey();
            Predicate<UnifiedPlayer> condition = entry.getValue();
            boolean shouldShow = condition.test(player);
            updateLineVisibility(score, shouldShow);
        }
    }

    @Override
    public void remove() {
        if (!removed) {
            removed = true;
            stopTitleAnimation();
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
     * Advances the title animation by one frame.
     */
    protected void advanceTitleFrame() {
        if (!titleAnimating || titleFrames.isEmpty()) return;

        titleFrameIndex = (titleFrameIndex + 1) % titleFrames.size();
        title = titleFrames.get(titleFrameIndex);
        if (visible) {
            updateTitle();
        }
    }

    // ==================== Abstract Methods ====================

    /**
     * Shows the scoreboard to the player.
     */
    protected abstract void doShow();

    /**
     * Hides the scoreboard from the player.
     */
    protected abstract void doHide();

    /**
     * Updates the scoreboard title.
     */
    protected abstract void updateTitle();

    /**
     * Updates a specific line.
     *
     * @param score the line score
     */
    protected abstract void updateLine(int score);

    /**
     * Updates the visibility of a line.
     *
     * @param score   the line score
     * @param visible whether the line should be visible
     */
    protected abstract void updateLineVisibility(int score, boolean visible);

    /**
     * Removes a line from the scoreboard.
     *
     * @param score the line score
     */
    protected abstract void doRemoveLine(int score);

    /**
     * Clears all lines from the scoreboard.
     */
    protected abstract void doClearLines();

    /**
     * Refreshes all lines.
     */
    protected abstract void refreshLines();

    /**
     * Starts the title animation task.
     */
    protected abstract void startTitleAnimation();

    /**
     * Stops the title animation task.
     */
    protected abstract void doStopTitleAnimation();

    /**
     * Called when the scoreboard is removed.
     */
    protected abstract void onRemove();
}
