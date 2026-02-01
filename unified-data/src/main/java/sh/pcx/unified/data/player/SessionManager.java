/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Manages player sessions and session lifecycle events.
 *
 * <p>The SessionManager tracks when players log in and out, assigns unique
 * session IDs, and provides hooks for session lifecycle events. It integrates
 * with the player data system to ensure data is loaded before session start
 * and saved after session end.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private SessionManager sessions;
 *
 * public void onEnable() {
 *     // Register session event handlers
 *     sessions.onSessionStart(event -> {
 *         PlayerProfile profile = event.getProfile();
 *         String sessionId = event.getSessionId();
 *         logger.info(profile.getLastKnownName() + " started session " + sessionId);
 *
 *         // Initialize session-specific data
 *         SessionData sessionData = event.getSessionData();
 *         sessionData.set("loginTime", Instant.now());
 *     });
 *
 *     sessions.onSessionEnd(event -> {
 *         Duration playTime = event.getSessionDuration();
 *         logger.info("Session ended after " + playTime.toMinutes() + " minutes");
 *     });
 * }
 *
 * // Get a player's current session
 * Optional<SessionData> session = sessions.getSession(player.getUniqueId());
 * session.ifPresent(s -> {
 *     Duration online = s.getDuration();
 *     // ...
 * });
 * }</pre>
 *
 * <h2>Session Lifecycle</h2>
 * <ol>
 *   <li><b>Pre-login:</b> Player data is loaded from database</li>
 *   <li><b>Session Start:</b> Session created, SessionEvent.START fired</li>
 *   <li><b>Active:</b> Player is online, session data available</li>
 *   <li><b>Session End:</b> SessionEvent.END fired, data saved</li>
 *   <li><b>Post-logout:</b> Session invalidated, cache may be cleared</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe. Event callbacks may be invoked from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SessionData
 * @see SessionEvent
 */
public interface SessionManager {

    /**
     * Gets the session for an online player.
     *
     * @param playerId the player's UUID
     * @return the session data if the player is online
     * @since 1.0.0
     */
    @NotNull
    Optional<SessionData> getSession(@NotNull UUID playerId);

    /**
     * Gets all active sessions.
     *
     * @return a collection of all active sessions
     * @since 1.0.0
     */
    @NotNull
    Collection<SessionData> getActiveSessions();

    /**
     * Gets the number of active sessions.
     *
     * @return the count of online players with sessions
     * @since 1.0.0
     */
    int getActiveSessionCount();

    /**
     * Checks if a player has an active session.
     *
     * @param playerId the player's UUID
     * @return true if the player has an active session
     * @since 1.0.0
     */
    boolean hasSession(@NotNull UUID playerId);

    /**
     * Gets a map of all active sessions keyed by player UUID.
     *
     * @return an unmodifiable map of sessions
     * @since 1.0.0
     */
    @NotNull
    Map<UUID, SessionData> getSessionMap();

    /**
     * Registers a handler for session start events.
     *
     * <p>The handler is called when a player's session begins, after their
     * profile data has been loaded. The session data is available for use.
     *
     * @param handler the event handler
     * @since 1.0.0
     */
    void onSessionStart(@NotNull Consumer<SessionEvent.Start> handler);

    /**
     * Registers a handler for session end events.
     *
     * <p>The handler is called when a player's session ends, before their
     * data is saved. This is the last chance to modify session data.
     *
     * @param handler the event handler
     * @since 1.0.0
     */
    void onSessionEnd(@NotNull Consumer<SessionEvent.End> handler);

    /**
     * Registers a handler for server switch events (on networks).
     *
     * <p>The handler is called when a player switches between backend servers.
     * Session data marked as clearOnServerSwitch will be cleared.
     *
     * @param handler the event handler
     * @since 1.0.0
     */
    void onServerSwitch(@NotNull Consumer<SessionEvent.ServerSwitch> handler);

    /**
     * Registers a handler for session data changes.
     *
     * <p>The handler is called when session data is modified, either locally
     * or via cross-server sync.
     *
     * @param handler the event handler
     * @since 1.0.0
     */
    void onDataChange(@NotNull Consumer<SessionEvent.DataChange> handler);

    /**
     * Starts a new session for a player.
     *
     * <p>This is typically called internally when a player joins. It creates
     * the session data structure and fires the session start event.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's username
     * @param serverName the server name
     * @param ipAddress  the player's IP address
     * @return the new session data
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<SessionData> startSession(@NotNull UUID playerId, @NotNull String playerName,
                                                 @NotNull String serverName, @NotNull String ipAddress);

    /**
     * Ends a player's session.
     *
     * <p>This is typically called internally when a player disconnects.
     * It fires the session end event and cleans up session data.
     *
     * @param playerId the player's UUID
     * @return the ended session data, or empty if no session existed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<SessionData>> endSession(@NotNull UUID playerId);

    /**
     * Updates the server for a player's session (on server switch).
     *
     * @param playerId      the player's UUID
     * @param newServerName the new server name
     * @since 1.0.0
     */
    void updateServer(@NotNull UUID playerId, @NotNull String newServerName);

    /**
     * Generates a new unique session ID.
     *
     * @return a unique session identifier
     * @since 1.0.0
     */
    @NotNull
    String generateSessionId();

    /**
     * Gets statistics about sessions.
     *
     * @return session statistics
     * @since 1.0.0
     */
    @NotNull
    SessionStatistics getStatistics();

    /**
     * Session statistics snapshot.
     *
     * @since 1.0.0
     */
    interface SessionStatistics {

        /**
         * Returns the current number of active sessions.
         *
         * @return active session count
         */
        int getActiveCount();

        /**
         * Returns the total sessions started since server start.
         *
         * @return total session count
         */
        long getTotalStarted();

        /**
         * Returns the total sessions ended since server start.
         *
         * @return total ended count
         */
        long getTotalEnded();

        /**
         * Returns the average session duration.
         *
         * @return average duration
         */
        @NotNull
        Duration getAverageDuration();

        /**
         * Returns the longest session duration.
         *
         * @return longest duration
         */
        @NotNull
        Duration getLongestDuration();

        /**
         * Returns when sessions were first tracked.
         *
         * @return tracking start time
         */
        @NotNull
        Instant getTrackingSince();

        /**
         * Returns the peak concurrent sessions.
         *
         * @return peak count
         */
        int getPeakConcurrent();
    }
}
