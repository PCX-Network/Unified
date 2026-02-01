/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Function;

/**
 * Builder for creating countdown boss bars.
 *
 * <p>Countdown boss bars automatically update their progress and title
 * over a specified duration, optionally calling a callback on completion.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * BossBarDisplay countdown = bossBarService.countdown(player)
 *     .title(Component.text("Game starts in..."))
 *     .duration(Duration.ofSeconds(30))
 *     .color(BossBarColor.GREEN)
 *     .titleUpdater(remaining -> Component.text(
 *         "Game starts in " + remaining.toSeconds() + "s"))
 *     .onComplete(() -> startGame())
 *     .start();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBarService
 * @see BossBarDisplay
 */
public interface CountdownBuilder {

    /**
     * Sets the initial title of the countdown bar.
     *
     * @param title the title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder title(@NotNull Component title);

    /**
     * Sets a dynamic title updater called each tick.
     *
     * @param updater function that receives remaining time and returns the title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder titleUpdater(@NotNull Function<Duration, Component> updater);

    /**
     * Sets the countdown duration.
     *
     * @param duration the countdown duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder duration(@NotNull Duration duration);

    /**
     * Sets the color of the countdown bar.
     *
     * @param color the bar color
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder color(@NotNull BossBarColor color);

    /**
     * Sets the overlay style.
     *
     * @param overlay the overlay style
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder overlay(@NotNull BossBarOverlay overlay);

    /**
     * Sets the callback to run when the countdown completes.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder onComplete(@NotNull Runnable callback);

    /**
     * Sets whether progress fills up (false) or drains down (true).
     *
     * @param reverse true for progress to start full and drain
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder reverse(boolean reverse);

    /**
     * Sets whether to remove the bar automatically on completion.
     *
     * @param autoRemove true to auto-remove (default: true)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder autoRemove(boolean autoRemove);

    /**
     * Starts the countdown and returns the boss bar.
     *
     * @return the countdown boss bar
     * @since 1.0.0
     */
    @NotNull
    BossBarDisplay start();
}
