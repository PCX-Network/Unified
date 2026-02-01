/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.title;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Represents a single frame in an animated scoreboard title.
 *
 * <p>Each frame has content and a duration defining how long it should
 * be displayed before transitioning to the next frame.
 *
 * @param content  the frame content
 * @param duration the duration to display this frame
 * @since 1.0.0
 * @author Supatuck
 */
public record TitleFrame(@NotNull Component content, @NotNull Duration duration) {

    /**
     * Creates a new title frame.
     *
     * @param content  the frame content
     * @param duration the duration to display
     * @return a new title frame
     * @since 1.0.0
     */
    @NotNull
    public static TitleFrame of(@NotNull Component content, @NotNull Duration duration) {
        return new TitleFrame(content, duration);
    }

    /**
     * Creates a new title frame with a default duration.
     *
     * @param content the frame content
     * @return a new title frame with 200ms duration
     * @since 1.0.0
     */
    @NotNull
    public static TitleFrame of(@NotNull Component content) {
        return new TitleFrame(content, Duration.ofMillis(200));
    }
}
