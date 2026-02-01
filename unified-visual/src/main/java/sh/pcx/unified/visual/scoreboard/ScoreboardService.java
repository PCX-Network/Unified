/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import sh.pcx.unified.visual.scoreboard.player.PlayerScoreboard;
import sh.pcx.unified.visual.scoreboard.player.ScoreboardGroup;
import sh.pcx.unified.visual.scoreboard.team.TeamManager;
import sh.pcx.unified.visual.scoreboard.update.UpdateInterval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Main service for managing scoreboards across the server.
 *
 * <p>The ScoreboardService is the central hub for creating, registering, and
 * managing scoreboards. It handles the lifecycle of scoreboards and provides
 * methods for showing scoreboards to players.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Scoreboard creation and registration</li>
 *   <li>Per-player scoreboard management</li>
 *   <li>Scoreboard groups for shared displays</li>
 *   <li>Automatic update scheduling</li>
 *   <li>Team management integration</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the scoreboard service
 * ScoreboardService service = UnifiedAPI.getInstance()
 *     .services()
 *     .getOrThrow(ScoreboardService.class);
 *
 * // Create and register a scoreboard
 * Scoreboard scoreboard = service.createBuilder("lobby")
 *     .title(Component.text("Lobby"))
 *     .line(StaticLine.of(Component.text("Welcome!")))
 *     .line(DynamicLine.of(p -> Component.text("Online: " + getOnlineCount())))
 *     .updateInterval(UpdateInterval.FAST)
 *     .build();
 *
 * service.register(scoreboard);
 *
 * // Show to a player
 * service.show(player, "lobby");
 *
 * // Create per-player scoreboard
 * PlayerScoreboard playerBoard = service.getOrCreatePlayerScoreboard(player);
 * playerBoard.show();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Scoreboard
 * @see ScoreboardBuilder
 * @see PlayerScoreboard
 */
public interface ScoreboardService extends Service {

    /**
     * Creates a new scoreboard builder.
     *
     * @param id the unique identifier for the scoreboard
     * @return a new scoreboard builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder createBuilder(@NotNull String id);

    /**
     * Registers a scoreboard with the service.
     *
     * @param scoreboard the scoreboard to register
     * @throws IllegalArgumentException if a scoreboard with the same ID already exists
     * @since 1.0.0
     */
    void register(@NotNull Scoreboard scoreboard);

    /**
     * Unregisters a scoreboard from the service.
     *
     * <p>This also hides the scoreboard from all current viewers.
     *
     * @param id the scoreboard ID to unregister
     * @return true if the scoreboard was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String id);

    /**
     * Unregisters a scoreboard from the service.
     *
     * @param scoreboard the scoreboard to unregister
     * @return true if the scoreboard was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull Scoreboard scoreboard);

    /**
     * Retrieves a registered scoreboard by its ID.
     *
     * @param id the scoreboard ID
     * @return an Optional containing the scoreboard if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> get(@NotNull String id);

    /**
     * Retrieves a registered scoreboard or throws if not found.
     *
     * @param id the scoreboard ID
     * @return the scoreboard
     * @throws IllegalArgumentException if no scoreboard with the ID exists
     * @since 1.0.0
     */
    @NotNull
    default Scoreboard getOrThrow(@NotNull String id) {
        return get(id).orElseThrow(() ->
                new IllegalArgumentException("Scoreboard not found: " + id));
    }

    /**
     * Checks if a scoreboard with the given ID is registered.
     *
     * @param id the scoreboard ID
     * @return true if the scoreboard is registered
     * @since 1.0.0
     */
    boolean isRegistered(@NotNull String id);

    /**
     * Returns all registered scoreboards.
     *
     * @return an unmodifiable collection of all scoreboards
     * @since 1.0.0
     */
    @NotNull
    Collection<Scoreboard> getAll();

    /**
     * Returns the number of registered scoreboards.
     *
     * @return the count of registered scoreboards
     * @since 1.0.0
     */
    int getCount();

    /**
     * Shows a scoreboard to a player.
     *
     * @param player      the player to show the scoreboard to
     * @param scoreboardId the ID of the scoreboard to show
     * @throws IllegalArgumentException if the scoreboard ID is not registered
     * @since 1.0.0
     */
    void show(@NotNull UnifiedPlayer player, @NotNull String scoreboardId);

    /**
     * Hides the current scoreboard from a player.
     *
     * @param player the player to hide the scoreboard from
     * @since 1.0.0
     */
    void hide(@NotNull UnifiedPlayer player);

    /**
     * Returns the scoreboard currently shown to a player.
     *
     * @param player the player to check
     * @return an Optional containing the current scoreboard
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> getCurrentScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Returns the scoreboard currently shown to a player by UUID.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the current scoreboard
     * @since 1.0.0
     */
    @NotNull
    Optional<Scoreboard> getCurrentScoreboard(@NotNull UUID playerId);

    /**
     * Gets or creates a per-player scoreboard.
     *
     * @param player the player
     * @return the player's scoreboard
     * @since 1.0.0
     */
    @NotNull
    PlayerScoreboard getOrCreatePlayerScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Gets a player's per-player scoreboard if it exists.
     *
     * @param player the player
     * @return an Optional containing the player scoreboard
     * @since 1.0.0
     */
    @NotNull
    Optional<PlayerScoreboard> getPlayerScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Gets a player's per-player scoreboard if it exists.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the player scoreboard
     * @since 1.0.0
     */
    @NotNull
    Optional<PlayerScoreboard> getPlayerScoreboard(@NotNull UUID playerId);

    /**
     * Removes a player's per-player scoreboard.
     *
     * @param player the player
     * @return true if the scoreboard was removed
     * @since 1.0.0
     */
    boolean removePlayerScoreboard(@NotNull UnifiedPlayer player);

    /**
     * Creates a new scoreboard group.
     *
     * @param id         the group ID
     * @param scoreboard the scoreboard for the group
     * @return the created group
     * @since 1.0.0
     */
    @NotNull
    ScoreboardGroup createGroup(@NotNull String id, @NotNull Scoreboard scoreboard);

    /**
     * Gets a scoreboard group by ID.
     *
     * @param id the group ID
     * @return an Optional containing the group if found
     * @since 1.0.0
     */
    @NotNull
    Optional<ScoreboardGroup> getGroup(@NotNull String id);

    /**
     * Removes a scoreboard group.
     *
     * @param id the group ID
     * @return true if the group was removed
     * @since 1.0.0
     */
    boolean removeGroup(@NotNull String id);

    /**
     * Returns all scoreboard groups.
     *
     * @return an unmodifiable collection of all groups
     * @since 1.0.0
     */
    @NotNull
    Collection<ScoreboardGroup> getAllGroups();

    /**
     * Returns the team manager.
     *
     * @return the team manager
     * @since 1.0.0
     */
    @NotNull
    TeamManager getTeamManager();

    /**
     * Sets the default update interval for new scoreboards.
     *
     * @param interval the default update interval
     * @since 1.0.0
     */
    void setDefaultUpdateInterval(@NotNull UpdateInterval interval);

    /**
     * Returns the default update interval.
     *
     * @return the default update interval
     * @since 1.0.0
     */
    @NotNull
    UpdateInterval getDefaultUpdateInterval();

    /**
     * Triggers an update for all registered scoreboards.
     *
     * @since 1.0.0
     */
    void updateAll();

    /**
     * Registers a placeholder resolver for use in scoreboard content.
     *
     * <p>Placeholders are resolved when rendering scoreboard lines.
     * The format is {@code %placeholder%} in the text.
     *
     * @param placeholder the placeholder name (without % symbols)
     * @param resolver    the function to resolve the placeholder value
     * @since 1.0.0
     */
    void registerPlaceholder(@NotNull String placeholder,
                             @NotNull java.util.function.Function<UnifiedPlayer, String> resolver);

    /**
     * Unregisters a placeholder resolver.
     *
     * @param placeholder the placeholder name
     * @return true if the placeholder was unregistered
     * @since 1.0.0
     */
    boolean unregisterPlaceholder(@NotNull String placeholder);

    /**
     * Resolves placeholders in a string for a player.
     *
     * @param player the player context
     * @param text   the text containing placeholders
     * @return the text with placeholders resolved
     * @since 1.0.0
     */
    @NotNull
    String resolvePlaceholders(@NotNull UnifiedPlayer player, @NotNull String text);

    /**
     * Cleans up resources for a player.
     *
     * <p>This should be called when a player disconnects.
     *
     * @param player the player to clean up
     * @since 1.0.0
     */
    void cleanupPlayer(@NotNull UnifiedPlayer player);

    /**
     * Cleans up resources for a player by UUID.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void cleanupPlayer(@NotNull UUID playerId);

    /**
     * Shuts down the scoreboard service.
     *
     * <p>This removes all scoreboards and stops all update tasks.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Adds a listener to be notified when a scoreboard is shown to a player.
     *
     * @param listener the listener
     * @since 1.0.0
     */
    void addShowListener(@NotNull Consumer<ScoreboardShowEvent> listener);

    /**
     * Adds a listener to be notified when a scoreboard is hidden from a player.
     *
     * @param listener the listener
     * @since 1.0.0
     */
    void addHideListener(@NotNull Consumer<ScoreboardHideEvent> listener);

    /**
     * Event fired when a scoreboard is shown to a player.
     *
     * @param player     the player
     * @param scoreboard the scoreboard shown
     * @since 1.0.0
     */
    record ScoreboardShowEvent(@NotNull UnifiedPlayer player, @NotNull Scoreboard scoreboard) {}

    /**
     * Event fired when a scoreboard is hidden from a player.
     *
     * @param player     the player
     * @param scoreboard the scoreboard that was hidden, or null if cleared
     * @since 1.0.0
     */
    record ScoreboardHideEvent(@NotNull UnifiedPlayer player, @Nullable Scoreboard scoreboard) {}
}
