/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.line;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a line in a scoreboard.
 *
 * <p>Scoreboard lines can be static, dynamic (per-player), or conditional.
 * Each line renders to a Component that is displayed to players.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface ScoreboardLine {

    /**
     * Creates an empty line (spacer).
     *
     * @return an empty line
     * @since 1.0.0
     */
    @NotNull
    static ScoreboardLine empty() {
        return StaticLine.empty();
    }

    /**
     * Renders this line for a specific player.
     *
     * @param player the player to render for
     * @return the rendered component
     * @since 1.0.0
     */
    @NotNull
    Component render(@NotNull UnifiedPlayer player);

    /**
     * Returns whether this line is dynamic (changes per-player or over time).
     *
     * @return true if the line is dynamic
     * @since 1.0.0
     */
    boolean isDynamic();

    /**
     * Returns whether this line is conditional.
     *
     * @return true if the line is conditional
     * @since 1.0.0
     */
    default boolean isConditional() {
        return false;
    }

    /**
     * Returns whether this line should be visible for the given player.
     *
     * <p>For non-conditional lines, this always returns true.
     *
     * @param player the player to check
     * @return true if the line should be visible
     * @since 1.0.0
     */
    default boolean isVisibleTo(@NotNull UnifiedPlayer player) {
        return true;
    }
}
