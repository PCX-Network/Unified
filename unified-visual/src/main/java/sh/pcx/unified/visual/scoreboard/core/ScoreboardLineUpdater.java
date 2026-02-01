/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.core;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for dynamically updating scoreboard lines.
 *
 * <p>Line updaters are called periodically to provide updated content
 * for scoreboard lines based on the player's current state.
 *
 * @since 1.0.0
 * @author Supatuck
 */
@FunctionalInterface
public interface ScoreboardLineUpdater {

    /**
     * Updates the line content for the given player.
     *
     * @param player the player viewing the scoreboard
     * @return the updated line content
     * @since 1.0.0
     */
    @NotNull
    Component update(@NotNull UnifiedPlayer player);
}
