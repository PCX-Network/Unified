/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of boss bar colors.
 *
 * <p>Boss bars can be displayed in various colors to differentiate between
 * different bar types or to indicate status changes. The available colors
 * match the vanilla Minecraft boss bar colors.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a red health bar
 * BossBar healthBar = BossBarBuilder.create("health")
 *     .title(Component.text("Health"))
 *     .color(BossBarColor.RED)
 *     .build();
 *
 * // Change color based on health percentage
 * if (health > 0.5) {
 *     healthBar.setColor(BossBarColor.GREEN);
 * } else if (health > 0.25) {
 *     healthBar.setColor(BossBarColor.YELLOW);
 * } else {
 *     healthBar.setColor(BossBarColor.RED);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBar
 * @see BossBarBuilder
 */
public enum BossBarColor {

    /**
     * Pink/magenta color.
     */
    PINK("pink"),

    /**
     * Blue color.
     */
    BLUE("blue"),

    /**
     * Red color. Often used for health or danger indicators.
     */
    RED("red"),

    /**
     * Green color. Often used for positive status or completion.
     */
    GREEN("green"),

    /**
     * Yellow color. Often used for warnings or intermediate states.
     */
    YELLOW("yellow"),

    /**
     * Purple color.
     */
    PURPLE("purple"),

    /**
     * White color. The default boss bar color.
     */
    WHITE("white");

    private final String key;

    BossBarColor(@NotNull String key) {
        this.key = key;
    }

    /**
     * Returns the key identifier for this color.
     *
     * @return the color key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Returns the next color in the enumeration, cycling back to the first
     * when at the end.
     *
     * <p>This is useful for color cycling animations.
     *
     * @return the next color
     * @since 1.0.0
     */
    @NotNull
    public BossBarColor next() {
        BossBarColor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Returns the previous color in the enumeration, cycling to the last
     * when at the beginning.
     *
     * @return the previous color
     * @since 1.0.0
     */
    @NotNull
    public BossBarColor previous() {
        BossBarColor[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }

    /**
     * Parses a color from a string key.
     *
     * @param key the color key (case-insensitive)
     * @return the matching color
     * @throws IllegalArgumentException if the key doesn't match any color
     * @since 1.0.0
     */
    @NotNull
    public static BossBarColor fromKey(@NotNull String key) {
        for (BossBarColor color : values()) {
            if (color.key.equalsIgnoreCase(key) || color.name().equalsIgnoreCase(key)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown boss bar color: " + key);
    }
}
