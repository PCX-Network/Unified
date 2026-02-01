/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.line.ScoreboardLine;
import sh.pcx.unified.visual.scoreboard.title.ScoreboardTitle;
import sh.pcx.unified.visual.scoreboard.update.UpdateInterval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of the {@link Scoreboard} interface.
 *
 * <p>This class provides a thread-safe implementation of scoreboards with
 * support for dynamic content, animated titles, and flicker-free updates.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultScoreboard implements Scoreboard {

    private final String id;
    private volatile ScoreboardTitle title;
    private final CopyOnWriteArrayList<ScoreboardLine> lines;
    private final Set<UUID> viewers;
    private final Map<String, Object> metadata;
    private final UpdateInterval updateInterval;
    private final boolean flickerFree;
    private final boolean placeholdersEnabled;
    private volatile boolean destroyed;

    /**
     * Creates a new default scoreboard.
     *
     * @param id                  the scoreboard ID
     * @param title               the scoreboard title
     * @param lines               the initial lines
     * @param updateInterval      the update interval
     * @param flickerFree         whether flicker-free updates are enabled
     * @param placeholdersEnabled whether placeholders are enabled
     * @param metadata            initial metadata
     */
    public DefaultScoreboard(@NotNull String id,
                             @NotNull ScoreboardTitle title,
                             @NotNull List<ScoreboardLine> lines,
                             @NotNull UpdateInterval updateInterval,
                             boolean flickerFree,
                             boolean placeholdersEnabled,
                             @NotNull Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.lines = new CopyOnWriteArrayList<>(lines);
        this.viewers = ConcurrentHashMap.newKeySet();
        this.metadata = new ConcurrentHashMap<>(metadata);
        this.updateInterval = updateInterval;
        this.flickerFree = flickerFree;
        this.placeholdersEnabled = placeholdersEnabled;
        this.destroyed = false;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull ScoreboardTitle getTitle() {
        return title;
    }

    @Override
    public void setTitle(@NotNull ScoreboardTitle title) {
        checkNotDestroyed();
        this.title = title;
        updateTitle();
    }

    @Override
    public void setTitle(@NotNull Component title) {
        setTitle(ScoreboardTitle.of(title));
    }

    @Override
    public @NotNull Component getCurrentTitle(@NotNull UnifiedPlayer player) {
        return title.render(player);
    }

    @Override
    public @NotNull List<ScoreboardLine> getLines() {
        return Collections.unmodifiableList(new ArrayList<>(lines));
    }

    @Override
    public @NotNull Optional<ScoreboardLine> getLine(int index) {
        if (index < 0 || index >= lines.size()) {
            return Optional.empty();
        }
        return Optional.of(lines.get(index));
    }

    @Override
    public void setLine(int index, @NotNull ScoreboardLine line) {
        checkNotDestroyed();
        if (index < 0 || index >= MAX_LINES) {
            throw new IndexOutOfBoundsException("Line index must be between 0 and " + (MAX_LINES - 1));
        }

        // Expand list if necessary
        while (lines.size() <= index) {
            lines.add(ScoreboardLine.empty());
        }

        lines.set(index, line);
        updateLine(index);
    }

    @Override
    public void addLine(@NotNull ScoreboardLine line) {
        checkNotDestroyed();
        if (lines.size() >= MAX_LINES) {
            throw new IllegalStateException("Cannot add more than " + MAX_LINES + " lines");
        }
        lines.add(line);
        update();
    }

    @Override
    public void insertLine(int index, @NotNull ScoreboardLine line) {
        checkNotDestroyed();
        if (lines.size() >= MAX_LINES) {
            throw new IllegalStateException("Cannot add more than " + MAX_LINES + " lines");
        }
        if (index < 0 || index > lines.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + lines.size());
        }
        lines.add(index, line);
        update();
    }

    @Override
    public @NotNull Optional<ScoreboardLine> removeLine(int index) {
        checkNotDestroyed();
        if (index < 0 || index >= lines.size()) {
            return Optional.empty();
        }
        ScoreboardLine removed = lines.remove(index);
        update();
        return Optional.of(removed);
    }

    @Override
    public void clearLines() {
        checkNotDestroyed();
        lines.clear();
        update();
    }

    @Override
    public int getLineCount() {
        return lines.size();
    }

    @Override
    public void show(@NotNull UnifiedPlayer player) {
        checkNotDestroyed();
        viewers.add(player.getUniqueId());
        // Platform-specific display logic would be implemented here
        update(player);
    }

    @Override
    public void hide(@NotNull UnifiedPlayer player) {
        viewers.remove(player.getUniqueId());
        // Platform-specific hide logic would be implemented here
    }

    @Override
    public boolean isVisibleTo(@NotNull UnifiedPlayer player) {
        return viewers.contains(player.getUniqueId());
    }

    @Override
    public @NotNull List<UUID> getViewers() {
        return new ArrayList<>(viewers);
    }

    @Override
    public int getViewerCount() {
        return viewers.size();
    }

    @Override
    public void update() {
        checkNotDestroyed();
        // Platform-specific update logic would iterate through viewers
        // and update their scoreboards
    }

    @Override
    public void update(@NotNull UnifiedPlayer player) {
        checkNotDestroyed();
        if (!isVisibleTo(player)) {
            return;
        }
        // Platform-specific update logic for a single player
    }

    @Override
    public void updateLine(int index) {
        checkNotDestroyed();
        // Platform-specific line update logic
    }

    @Override
    public void updateLine(@NotNull UnifiedPlayer player, int index) {
        checkNotDestroyed();
        if (!isVisibleTo(player)) {
            return;
        }
        // Platform-specific line update logic for a single player
    }

    @Override
    public void updateTitle() {
        checkNotDestroyed();
        // Platform-specific title update logic
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;

        // Clear all viewers
        viewers.clear();
        lines.clear();
        metadata.clear();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getMetadata(@NotNull String key) {
        return (T) metadata.get(key);
    }

    @Override
    public <T> void setMetadata(@NotNull String key, @Nullable T value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }

    @Override
    public boolean hasMetadata(@NotNull String key) {
        return metadata.containsKey(key);
    }

    /**
     * Returns the update interval for this scoreboard.
     *
     * @return the update interval
     * @since 1.0.0
     */
    public UpdateInterval getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Returns whether flicker-free updates are enabled.
     *
     * @return true if flicker-free updates are enabled
     * @since 1.0.0
     */
    public boolean isFlickerFree() {
        return flickerFree;
    }

    /**
     * Returns whether placeholder resolution is enabled.
     *
     * @return true if placeholders are enabled
     * @since 1.0.0
     */
    public boolean isPlaceholdersEnabled() {
        return placeholdersEnabled;
    }

    /**
     * Checks that the scoreboard is not destroyed.
     *
     * @throws IllegalStateException if the scoreboard is destroyed
     */
    private void checkNotDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("Scoreboard has been destroyed: " + id);
        }
    }

    @Override
    public String toString() {
        return "DefaultScoreboard{" +
                "id='" + id + '\'' +
                ", lines=" + lines.size() +
                ", viewers=" + viewers.size() +
                ", destroyed=" + destroyed +
                '}';
    }
}
