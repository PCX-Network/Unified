/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of boss bar styles.
 *
 * <p>Boss bar styles determine how the progress bar is visually divided.
 * The bar can be displayed as a solid bar or segmented into 6, 10, 12,
 * or 20 notches.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Solid progress bar
 * BossBar progressBar = BossBarBuilder.create("progress")
 *     .title(Component.text("Loading..."))
 *     .style(BossBarStyle.SOLID)
 *     .build();
 *
 * // Segmented bar for phases (6 segments)
 * BossBar phaseBar = BossBarBuilder.create("phase")
 *     .title(Component.text("Phase 1"))
 *     .style(BossBarStyle.SEGMENTED_6)
 *     .build();
 *
 * // 20 segments for percentage display
 * BossBar percentBar = BossBarBuilder.create("percent")
 *     .title(Component.text("0%"))
 *     .style(BossBarStyle.SEGMENTED_20)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBar
 * @see BossBarBuilder
 */
public enum BossBarStyle {

    /**
     * A solid, continuous progress bar with no divisions.
     */
    SOLID("solid", 1),

    /**
     * A progress bar divided into 6 segments.
     * Useful for phase-based progress or a small number of steps.
     */
    SEGMENTED_6("segmented_6", 6),

    /**
     * A progress bar divided into 10 segments.
     * Useful for decimal percentage display.
     */
    SEGMENTED_10("segmented_10", 10),

    /**
     * A progress bar divided into 12 segments.
     * Useful for monthly or hourly displays.
     */
    SEGMENTED_12("segmented_12", 12),

    /**
     * A progress bar divided into 20 segments.
     * Useful for 5% increments.
     */
    SEGMENTED_20("segmented_20", 20);

    private final String key;
    private final int segments;

    BossBarStyle(@NotNull String key, int segments) {
        this.key = key;
        this.segments = segments;
    }

    /**
     * Returns the key identifier for this style.
     *
     * @return the style key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Returns the number of segments in this style.
     *
     * @return the segment count (1 for solid)
     * @since 1.0.0
     */
    public int getSegments() {
        return segments;
    }

    /**
     * Checks if this style has visible segment dividers.
     *
     * @return true if segmented, false if solid
     * @since 1.0.0
     */
    public boolean isSegmented() {
        return segments > 1;
    }

    /**
     * Parses a style from a string key.
     *
     * @param key the style key (case-insensitive)
     * @return the matching style
     * @throws IllegalArgumentException if the key doesn't match any style
     * @since 1.0.0
     */
    @NotNull
    public static BossBarStyle fromKey(@NotNull String key) {
        for (BossBarStyle style : values()) {
            if (style.key.equalsIgnoreCase(key) || style.name().equalsIgnoreCase(key)) {
                return style;
            }
        }
        throw new IllegalArgumentException("Unknown boss bar style: " + key);
    }

    /**
     * Returns the style with the specified number of segments.
     *
     * @param segments the number of segments (1, 6, 10, 12, or 20)
     * @return the matching style
     * @throws IllegalArgumentException if no style has that segment count
     * @since 1.0.0
     */
    @NotNull
    public static BossBarStyle forSegments(int segments) {
        for (BossBarStyle style : values()) {
            if (style.segments == segments) {
                return style;
            }
        }
        throw new IllegalArgumentException("No boss bar style with " + segments + " segments");
    }
}
