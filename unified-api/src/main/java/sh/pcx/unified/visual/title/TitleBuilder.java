/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.title;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Builder for sending titles to players.
 *
 * <p>Use this builder to configure title display properties.
 * Obtain a builder from {@link TitleService#send}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * titleService.send(player)
 *     .title(Component.text("VICTORY!", NamedTextColor.GOLD))
 *     .subtitle(Component.text("You won the game!"))
 *     .fadeIn(Duration.ofMillis(500))
 *     .stay(Duration.ofSeconds(3))
 *     .fadeOut(Duration.ofMillis(500))
 *     .send();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TitleService
 */
public interface TitleBuilder {

    /**
     * Sets the main title text.
     *
     * @param title the title text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder title(@NotNull Component title);

    /**
     * Sets the subtitle text.
     *
     * @param subtitle the subtitle text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder subtitle(@NotNull Component subtitle);

    /**
     * Sets the fade-in duration.
     *
     * @param duration the fade-in duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder fadeIn(@NotNull Duration duration);

    /**
     * Sets the fade-in duration in ticks.
     *
     * @param ticks the fade-in duration in ticks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default TitleBuilder fadeInTicks(int ticks) {
        return fadeIn(Duration.ofMillis(ticks * 50L));
    }

    /**
     * Sets the stay duration.
     *
     * @param duration the stay duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder stay(@NotNull Duration duration);

    /**
     * Sets the stay duration in ticks.
     *
     * @param ticks the stay duration in ticks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default TitleBuilder stayTicks(int ticks) {
        return stay(Duration.ofMillis(ticks * 50L));
    }

    /**
     * Sets the fade-out duration.
     *
     * @param duration the fade-out duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder fadeOut(@NotNull Duration duration);

    /**
     * Sets the fade-out duration in ticks.
     *
     * @param ticks the fade-out duration in ticks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default TitleBuilder fadeOutTicks(int ticks) {
        return fadeOut(Duration.ofMillis(ticks * 50L));
    }

    /**
     * Sets all timing values at once.
     *
     * @param fadeIn  the fade-in duration
     * @param stay    the stay duration
     * @param fadeOut the fade-out duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder times(@NotNull Duration fadeIn, @NotNull Duration stay, @NotNull Duration fadeOut);

    /**
     * Sets all timing values at once in ticks.
     *
     * @param fadeInTicks  the fade-in duration in ticks
     * @param stayTicks    the stay duration in ticks
     * @param fadeOutTicks the fade-out duration in ticks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default TitleBuilder timesTicks(int fadeInTicks, int stayTicks, int fadeOutTicks) {
        return times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
    }

    /**
     * Sends the title to the player(s).
     *
     * @since 1.0.0
     */
    void send();
}
