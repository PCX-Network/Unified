/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.game;

/**
 * Represents the current phase of a game.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public enum GamePhase {

    /**
     * Waiting for players to join.
     */
    WAITING("Waiting for Players", "&e"),

    /**
     * Countdown before game starts.
     */
    STARTING("Starting", "&6"),

    /**
     * Game is actively running.
     */
    RUNNING("In Progress", "&a"),

    /**
     * Game has ended, showing results.
     */
    ENDING("Game Over", "&c");

    private final String displayName;
    private final String colorCode;

    GamePhase(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    /**
     * Returns the display name of this phase.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the color code for this phase.
     *
     * @return the color code
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * Returns the colored display name.
     *
     * @return the display name with color
     */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /**
     * Checks if this phase allows new players to join.
     *
     * @return true if players can join
     */
    public boolean allowsJoin() {
        return this == WAITING || this == STARTING;
    }

    /**
     * Checks if combat is enabled in this phase.
     *
     * @return true if combat is enabled
     */
    public boolean combatEnabled() {
        return this == RUNNING;
    }
}
