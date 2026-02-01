/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enumeration of boss bar flags.
 *
 * <p>Boss bar flags control additional visual effects that can be applied
 * when a boss bar is visible to a player. These effects mimic vanilla
 * Minecraft boss battle ambiance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an ominous boss bar with sky darkening
 * BossBar bossBar = BossBarBuilder.create("boss")
 *     .title(Component.text("Ender Dragon"))
 *     .color(BossBarColor.PURPLE)
 *     .flags(BossBarFlag.DARKEN_SKY, BossBarFlag.CREATE_FOG)
 *     .build();
 *
 * // Add a flag dynamically
 * bossBar.addFlag(BossBarFlag.PLAY_MUSIC);
 *
 * // Remove a flag
 * bossBar.removeFlag(BossBarFlag.CREATE_FOG);
 *
 * // Check if a flag is set
 * if (bossBar.hasFlag(BossBarFlag.DARKEN_SKY)) {
 *     // Sky is darkened
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBar
 * @see BossBarBuilder
 */
public enum BossBarFlag {

    /**
     * Darkens the sky while the boss bar is visible.
     * Used by the Ender Dragon and Wither in vanilla Minecraft.
     */
    DARKEN_SKY("darken_sky"),

    /**
     * Plays boss music while the boss bar is visible.
     * Used by the Ender Dragon in vanilla Minecraft.
     */
    PLAY_MUSIC("play_music"),

    /**
     * Creates thick fog around the player.
     * Used by the Ender Dragon and Wither in vanilla Minecraft.
     */
    CREATE_FOG("create_fog");

    private final String key;

    BossBarFlag(@NotNull String key) {
        this.key = key;
    }

    /**
     * Returns the key identifier for this flag.
     *
     * @return the flag key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Parses a flag from a string key.
     *
     * @param key the flag key (case-insensitive)
     * @return the matching flag
     * @throws IllegalArgumentException if the key doesn't match any flag
     * @since 1.0.0
     */
    @NotNull
    public static BossBarFlag fromKey(@NotNull String key) {
        for (BossBarFlag flag : values()) {
            if (flag.key.equalsIgnoreCase(key) || flag.name().equalsIgnoreCase(key)) {
                return flag;
            }
        }
        throw new IllegalArgumentException("Unknown boss bar flag: " + key);
    }

    /**
     * Creates an empty set of flags.
     *
     * @return an empty mutable flag set
     * @since 1.0.0
     */
    @NotNull
    public static Set<BossBarFlag> none() {
        return EnumSet.noneOf(BossBarFlag.class);
    }

    /**
     * Creates a set containing all flags.
     *
     * @return a mutable set with all flags
     * @since 1.0.0
     */
    @NotNull
    public static Set<BossBarFlag> all() {
        return EnumSet.allOf(BossBarFlag.class);
    }

    /**
     * Creates a set containing the specified flags.
     *
     * @param flags the flags to include
     * @return a mutable set with the specified flags
     * @since 1.0.0
     */
    @NotNull
    public static Set<BossBarFlag> of(@NotNull BossBarFlag... flags) {
        if (flags.length == 0) {
            return none();
        }
        return EnumSet.of(flags[0], flags);
    }
}
