/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.title;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.title.ActionBarBuilder;
import sh.pcx.unified.visual.title.TitleBuilder;
import sh.pcx.unified.visual.title.TitleService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link TitleService}.
 *
 * <p>Provides common title management functionality. Subclasses
 * implement platform-specific title rendering.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractTitleService implements TitleService {

    /**
     * Active action bars mapped by their unique ID.
     */
    protected final Map<UUID, ActionBarState> activeActionBars = new ConcurrentHashMap<>();

    /**
     * Action bars by player UUID for quick lookup.
     */
    protected final Map<UUID, Set<UUID>> playerActionBars = new ConcurrentHashMap<>();

    @Override
    public @NotNull TitleBuilder send(@NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(players, "players cannot be null");
        return createTitleBuilder(List.copyOf(players));
    }

    @Override
    public @NotNull ActionBarBuilder actionBar(@NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(players, "players cannot be null");
        return createActionBarBuilder(List.copyOf(players));
    }

    @Override
    public void clearTitle(@NotNull Collection<? extends UnifiedPlayer> players) {
        Objects.requireNonNull(players, "players cannot be null");
        players.forEach(this::clearTitle);
    }

    @Override
    public boolean cancelActionBar(@NotNull UUID actionBarId) {
        Objects.requireNonNull(actionBarId, "actionBarId cannot be null");
        ActionBarState state = activeActionBars.remove(actionBarId);
        if (state != null) {
            state.cancel();
            for (UUID playerId : state.playerIds()) {
                Set<UUID> bars = playerActionBars.get(playerId);
                if (bars != null) {
                    bars.remove(actionBarId);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void clearActionBars(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        Set<UUID> bars = playerActionBars.remove(player.getUniqueId());
        if (bars != null) {
            bars.forEach(id -> {
                ActionBarState state = activeActionBars.remove(id);
                if (state != null) {
                    state.cancel();
                }
            });
        }
    }

    /**
     * Registers an active action bar.
     *
     * @param id       the action bar ID
     * @param state    the action bar state
     * @param players  the target players
     */
    protected void registerActionBar(@NotNull UUID id, @NotNull ActionBarState state,
                                     @NotNull Collection<? extends UnifiedPlayer> players) {
        activeActionBars.put(id, state);
        for (UnifiedPlayer player : players) {
            playerActionBars.computeIfAbsent(player.getUniqueId(), unused -> ConcurrentHashMap.newKeySet())
                    .add(id);
        }
    }

    /**
     * Creates a title builder for the given players.
     *
     * @param players the target players
     * @return a title builder
     */
    protected abstract TitleBuilder createTitleBuilder(@NotNull List<UnifiedPlayer> players);

    /**
     * Creates an action bar builder for the given players.
     *
     * @param players the target players
     * @return an action bar builder
     */
    protected abstract ActionBarBuilder createActionBarBuilder(@NotNull List<UnifiedPlayer> players);

    @Override
    public String getServiceName() {
        return "TitleService";
    }

    /**
     * State holder for active action bars.
     *
     * @param playerIds        the player UUIDs
     * @param cancelRunnable   the cancellation runnable
     */
    protected record ActionBarState(@NotNull Set<UUID> playerIds, @NotNull Runnable cancelRunnable) {
        /**
         * Cancels this action bar state.
         */
        public void cancel() {
            cancelRunnable.run();
        }
    }
}
