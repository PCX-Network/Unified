/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.title;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * Service for sending titles, subtitles, and action bars to players.
 *
 * <p>The TitleService provides methods to display large titles on the player's
 * screen and action bar messages above the hotbar.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Title and subtitle display</li>
 *   <li>Configurable fade in/stay/fade out timing</li>
 *   <li>Action bar messages</li>
 *   <li>Persistent action bars with duration</li>
 *   <li>Dynamic action bar content</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private TitleService titles;
 *
 * // Send title
 * titles.send(player)
 *     .title(Component.text("VICTORY!", NamedTextColor.GOLD))
 *     .subtitle(Component.text("You won the game!"))
 *     .fadeIn(Duration.ofMillis(500))
 *     .stay(Duration.ofSeconds(3))
 *     .fadeOut(Duration.ofMillis(500))
 *     .send();
 *
 * // Action bar (persists until cleared)
 * titles.actionBar(player)
 *     .message(Component.text("Combat Mode Active", NamedTextColor.RED))
 *     .duration(Duration.ofSeconds(10))
 *     .send();
 *
 * // Clear
 * titles.clear(player);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TitleBuilder
 * @see ActionBarBuilder
 */
public interface TitleService extends Service {

    // ==================== Title Methods ====================

    /**
     * Creates a title builder for a player.
     *
     * @param player the player to send the title to
     * @return a title builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder send(@NotNull UnifiedPlayer player);

    /**
     * Creates a title builder for multiple players.
     *
     * @param players the players to send the title to
     * @return a title builder
     * @since 1.0.0
     */
    @NotNull
    TitleBuilder send(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Sends a simple title to a player.
     *
     * @param player the player
     * @param title  the title text
     * @since 1.0.0
     */
    default void sendTitle(@NotNull UnifiedPlayer player, @NotNull Component title) {
        send(player).title(title).send();
    }

    /**
     * Sends a title with subtitle to a player.
     *
     * @param player   the player
     * @param title    the title text
     * @param subtitle the subtitle text
     * @since 1.0.0
     */
    default void sendTitle(@NotNull UnifiedPlayer player, @NotNull Component title, @NotNull Component subtitle) {
        send(player).title(title).subtitle(subtitle).send();
    }

    /**
     * Sends a title with full timing configuration.
     *
     * @param player   the player
     * @param title    the title text
     * @param subtitle the subtitle text
     * @param fadeIn   the fade-in duration
     * @param stay     the stay duration
     * @param fadeOut  the fade-out duration
     * @since 1.0.0
     */
    default void sendTitle(@NotNull UnifiedPlayer player, @NotNull Component title,
                           @NotNull Component subtitle, @NotNull Duration fadeIn,
                           @NotNull Duration stay, @NotNull Duration fadeOut) {
        send(player)
                .title(title)
                .subtitle(subtitle)
                .times(fadeIn, stay, fadeOut)
                .send();
    }

    // ==================== Action Bar Methods ====================

    /**
     * Creates an action bar builder for a player.
     *
     * @param player the player to send the action bar to
     * @return an action bar builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder actionBar(@NotNull UnifiedPlayer player);

    /**
     * Creates an action bar builder for multiple players.
     *
     * @param players the players to send the action bar to
     * @return an action bar builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder actionBar(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Sends a simple action bar message.
     *
     * @param player  the player
     * @param message the message
     * @since 1.0.0
     */
    default void sendActionBar(@NotNull UnifiedPlayer player, @NotNull Component message) {
        actionBar(player).message(message).send();
    }

    /**
     * Sends a persistent action bar message.
     *
     * @param player   the player
     * @param message  the message
     * @param duration how long to display
     * @return the action bar ID for cancellation
     * @since 1.0.0
     */
    @NotNull
    default UUID sendActionBar(@NotNull UnifiedPlayer player, @NotNull Component message,
                               @NotNull Duration duration) {
        return actionBar(player).message(message).duration(duration).send();
    }

    // ==================== Clear Methods ====================

    /**
     * Clears the current title from a player's screen.
     *
     * @param player the player
     * @since 1.0.0
     */
    void clearTitle(@NotNull UnifiedPlayer player);

    /**
     * Clears the current title from multiple players' screens.
     *
     * @param players the players
     * @since 1.0.0
     */
    void clearTitle(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Resets title timing to defaults for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void resetTitleTimes(@NotNull UnifiedPlayer player);

    /**
     * Cancels a persistent action bar by its ID.
     *
     * @param actionBarId the action bar ID returned from send()
     * @return true if the action bar was cancelled
     * @since 1.0.0
     */
    boolean cancelActionBar(@NotNull UUID actionBarId);

    /**
     * Clears all persistent action bars for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void clearActionBars(@NotNull UnifiedPlayer player);

    /**
     * Clears all visual elements (title and action bars) for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    default void clear(@NotNull UnifiedPlayer player) {
        clearTitle(player);
        clearActionBars(player);
    }
}
