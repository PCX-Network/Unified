/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.scoreboard;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.core.LegacyScoreboard;
import sh.pcx.unified.visual.scoreboard.core.LegacyScoreboardService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link LegacyScoreboardService}.
 *
 * <p>Provides common scoreboard management functionality. Subclasses
 * implement platform-specific scoreboard creation.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractScoreboardService implements LegacyScoreboardService {

    protected final Map<UUID, LegacyScoreboard> scoreboards = new ConcurrentHashMap<>();

    @Override
    public @NotNull Optional<LegacyScoreboard> getScoreboard(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return getScoreboard(player.getUniqueId());
    }

    @Override
    public @NotNull Optional<LegacyScoreboard> getScoreboard(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return Optional.ofNullable(scoreboards.get(playerId));
    }

    @Override
    public boolean hasScoreboard(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return scoreboards.containsKey(player.getUniqueId());
    }

    @Override
    public @NotNull Collection<LegacyScoreboard> getAll() {
        return List.copyOf(scoreboards.values());
    }

    @Override
    public int getCount() {
        return scoreboards.size();
    }

    @Override
    public boolean remove(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return remove(player.getUniqueId());
    }

    @Override
    public boolean remove(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        LegacyScoreboard scoreboard = scoreboards.remove(playerId);
        if (scoreboard != null) {
            scoreboard.remove();
            return true;
        }
        return false;
    }

    @Override
    public void removeAll() {
        List<LegacyScoreboard> toRemove = new ArrayList<>(scoreboards.values());
        scoreboards.clear();
        toRemove.forEach(LegacyScoreboard::remove);
    }

    @Override
    public void show(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        getScoreboard(player).ifPresent(LegacyScoreboard::show);
    }

    @Override
    public void hide(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        getScoreboard(player).ifPresent(LegacyScoreboard::hide);
    }

    @Override
    public void toggle(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        getScoreboard(player).ifPresent(LegacyScoreboard::toggle);
    }

    @Override
    public void updateAll() {
        scoreboards.values().forEach(LegacyScoreboard::update);
    }

    /**
     * Registers a scoreboard with the service.
     *
     * @param scoreboard the scoreboard to register
     */
    protected void registerScoreboard(@NotNull LegacyScoreboard scoreboard) {
        // Remove existing scoreboard for the player
        LegacyScoreboard existing = scoreboards.put(scoreboard.getPlayer().getUniqueId(), scoreboard);
        if (existing != null) {
            existing.remove();
        }
    }

    /**
     * Unregisters a scoreboard from the service.
     *
     * @param scoreboard the scoreboard to unregister
     */
    protected void unregisterScoreboard(@NotNull LegacyScoreboard scoreboard) {
        scoreboards.remove(scoreboard.getPlayer().getUniqueId(), scoreboard);
    }

    @Override
    public String getServiceName() {
        return "LegacyScoreboardService";
    }
}
