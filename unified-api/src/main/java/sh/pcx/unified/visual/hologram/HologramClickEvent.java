/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Event data for hologram click interactions.
 *
 * <p>This record contains information about a player's click on a hologram,
 * including the player, the hologram, and the click type.
 *
 * @param player    the player who clicked
 * @param hologram  the hologram that was clicked
 * @param clickType the type of click
 * @param lineIndex the index of the line clicked, or -1 for item holograms
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record HologramClickEvent(
        @NotNull UnifiedPlayer player,
        @NotNull Hologram hologram,
        @NotNull ClickType clickType,
        int lineIndex
) {

    /**
     * Creates a click event for an item hologram.
     *
     * @param player    the player who clicked
     * @param hologram  the hologram that was clicked
     * @param clickType the type of click
     */
    public HologramClickEvent(@NotNull UnifiedPlayer player, @NotNull Hologram hologram, @NotNull ClickType clickType) {
        this(player, hologram, clickType, -1);
    }

    /**
     * Returns the player who clicked.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the hologram that was clicked.
     *
     * @return the hologram
     * @since 1.0.0
     */
    @NotNull
    public Hologram getHologram() {
        return hologram;
    }

    /**
     * Returns the click type.
     *
     * @return the click type
     * @since 1.0.0
     */
    @NotNull
    public ClickType getClickType() {
        return clickType;
    }

    /**
     * Returns the index of the line clicked.
     *
     * @return the line index, or -1 for item holograms
     * @since 1.0.0
     */
    public int getLineIndex() {
        return lineIndex;
    }

    /**
     * Returns whether a specific line was clicked.
     *
     * @return true if a line was clicked
     * @since 1.0.0
     */
    public boolean hasLineIndex() {
        return lineIndex >= 0;
    }

    /**
     * Enumeration of click types.
     *
     * @since 1.0.0
     */
    public enum ClickType {
        /**
         * Left mouse button click.
         */
        LEFT,

        /**
         * Right mouse button click.
         */
        RIGHT,

        /**
         * Shift + left click.
         */
        SHIFT_LEFT,

        /**
         * Shift + right click.
         */
        SHIFT_RIGHT
    }
}
