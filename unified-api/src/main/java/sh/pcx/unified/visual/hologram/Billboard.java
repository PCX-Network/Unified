/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

/**
 * Defines how a hologram faces relative to the viewer.
 *
 * <p>Billboard modes control the rotation behavior of display entities,
 * determining whether and how they rotate to face the player.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum Billboard {

    /**
     * No rotation - the hologram maintains its fixed orientation.
     */
    FIXED,

    /**
     * Rotates only on the vertical axis to face the player.
     * The hologram will turn left/right but not up/down.
     */
    VERTICAL,

    /**
     * Rotates only on the horizontal axis to face the player.
     * The hologram will tilt up/down but not turn left/right.
     */
    HORIZONTAL,

    /**
     * Fully rotates to always face the player on all axes.
     * This is the most common mode for text holograms.
     */
    CENTER
}
