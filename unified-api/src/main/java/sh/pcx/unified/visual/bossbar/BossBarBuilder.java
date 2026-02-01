/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Builder for creating boss bars.
 *
 * <p>Use this builder to configure boss bar properties before creation.
 * Obtain a builder from {@link BossBarService#create()}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * BossBarDisplay bar = bossBarService.create()
 *     .title(Component.text("World Boss: Dragon", NamedTextColor.RED))
 *     .color(BossBarColor.RED)
 *     .overlay(BossBarOverlay.PROGRESS)
 *     .progress(1.0f)
 *     .darkenSky(true)
 *     .addPlayer(player)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBarService
 * @see BossBarDisplay
 */
public interface BossBarBuilder {

    /**
     * Sets the title of the boss bar.
     *
     * @param title the boss bar title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder title(@NotNull Component title);

    /**
     * Sets the color of the boss bar.
     *
     * @param color the boss bar color
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder color(@NotNull BossBarColor color);

    /**
     * Sets the overlay style of the boss bar.
     *
     * @param overlay the overlay style
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder overlay(@NotNull BossBarOverlay overlay);

    /**
     * Sets the initial progress of the boss bar.
     *
     * @param progress the progress (0.0 to 1.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder progress(float progress);

    /**
     * Sets whether the sky darkens when this bar is shown.
     *
     * @param darkenSky true to darken the sky
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder darkenSky(boolean darkenSky);

    /**
     * Sets whether boss music plays when this bar is shown.
     *
     * @param playMusic true to play music
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder playBossMusic(boolean playMusic);

    /**
     * Sets whether fog is created when this bar is shown.
     *
     * @param createFog true to create fog
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder createFog(boolean createFog);

    /**
     * Adds a player to view the boss bar.
     *
     * @param player the player to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder addPlayer(@NotNull UnifiedPlayer player);

    /**
     * Adds multiple players to view the boss bar.
     *
     * @param players the players to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder addPlayers(@NotNull UnifiedPlayer... players);

    /**
     * Adds multiple players to view the boss bar.
     *
     * @param players the players to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder addPlayers(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Sets whether the boss bar is initially visible.
     *
     * @param visible true to show immediately
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder visible(boolean visible);

    /**
     * Builds and shows the boss bar.
     *
     * @return the created boss bar
     * @since 1.0.0
     */
    @NotNull
    BossBarDisplay build();
}
