/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.arena;

/**
 * Represents the current state of an arena.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public enum ArenaState {

    /**
     * The arena is disabled and cannot be used.
     */
    DISABLED("Disabled", "&8"),

    /**
     * The arena is waiting for players to join.
     */
    WAITING("Waiting", "&a"),

    /**
     * The arena has enough players and the countdown is starting.
     */
    STARTING("Starting", "&e"),

    /**
     * A game is currently in progress in the arena.
     */
    IN_GAME("In Game", "&c"),

    /**
     * The game has ended and the arena is resetting.
     */
    RESETTING("Resetting", "&6"),

    /**
     * The arena is under maintenance and cannot be used.
     */
    MAINTENANCE("Maintenance", "&7");

    private final String displayName;
    private final String colorCode;

    ArenaState(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    /**
     * Returns the display name of this state.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the color code for this state.
     *
     * @return the color code (e.g., "&a" for green)
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * Returns the colored display name.
     *
     * @return the display name with color code prefix
     */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /**
     * Checks if the arena can accept new players in this state.
     *
     * @return true if players can join
     */
    public boolean canJoin() {
        return this == WAITING || this == STARTING;
    }

    /**
     * Checks if the arena is active (has a game starting or in progress).
     *
     * @return true if the arena is active
     */
    public boolean isActive() {
        return this == STARTING || this == IN_GAME;
    }
}
