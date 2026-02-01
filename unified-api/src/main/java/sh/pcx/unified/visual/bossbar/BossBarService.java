/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.bossbar;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating and managing boss bars.
 *
 * <p>The BossBarService provides methods to create boss bars with support
 * for multiple bars per player, progress animations, and countdown timers.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Multiple boss bars per player</li>
 *   <li>Progress animations</li>
 *   <li>Color and overlay customization</li>
 *   <li>Per-player targeting</li>
 *   <li>Countdown timers with callbacks</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private BossBarService bossBars;
 *
 * // Create boss bar
 * BossBarDisplay bar = bossBars.create()
 *     .title(Component.text("World Boss: Dragon", NamedTextColor.RED))
 *     .color(BossBarColor.RED)
 *     .overlay(BossBarOverlay.PROGRESS)
 *     .progress(1.0f)
 *     .addPlayer(player)
 *     .build();
 *
 * // Update progress
 * bar.setProgress(boss.getHealth() / boss.getMaxHealth());
 *
 * // Animated countdown
 * bossBars.countdown(player)
 *     .title(Component.text("Game starts in..."))
 *     .duration(Duration.ofSeconds(30))
 *     .onComplete(() -> startGame())
 *     .start();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BossBarDisplay
 * @see BossBarBuilder
 * @see CountdownBuilder
 */
public interface BossBarService extends Service {

    /**
     * Creates a builder for a new boss bar.
     *
     * @return a boss bar builder
     * @since 1.0.0
     */
    @NotNull
    BossBarBuilder create();

    /**
     * Creates a countdown boss bar builder for a player.
     *
     * @param player the player to show the countdown to
     * @return a countdown builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder countdown(@NotNull UnifiedPlayer player);

    /**
     * Creates a countdown boss bar builder for multiple players.
     *
     * @param players the players to show the countdown to
     * @return a countdown builder
     * @since 1.0.0
     */
    @NotNull
    CountdownBuilder countdown(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Returns a boss bar by its unique ID.
     *
     * @param id the boss bar's unique ID
     * @return an Optional containing the boss bar if found
     * @since 1.0.0
     */
    @NotNull
    Optional<BossBarDisplay> getById(@NotNull UUID id);

    /**
     * Returns all registered boss bars.
     *
     * @return an unmodifiable collection of all boss bars
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    Collection<BossBarDisplay> getAll();

    /**
     * Returns all boss bars visible to a player.
     *
     * @param player the player
     * @return a collection of boss bars visible to the player
     * @since 1.0.0
     */
    @NotNull
    Collection<BossBarDisplay> getBarsFor(@NotNull UnifiedPlayer player);

    /**
     * Returns the number of registered boss bars.
     *
     * @return the boss bar count
     * @since 1.0.0
     */
    int getCount();

    /**
     * Removes a boss bar by its unique ID.
     *
     * @param id the boss bar's unique ID
     * @return true if the boss bar was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UUID id);

    /**
     * Removes all boss bars.
     *
     * @since 1.0.0
     */
    void removeAll();

    /**
     * Removes all boss bars visible to a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void removeAllFor(@NotNull UnifiedPlayer player);
}
