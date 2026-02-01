/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Base interface for session lifecycle events.
 *
 * <p>Session events are fired at key points in a player's session lifecycle:
 * <ul>
 *   <li>{@link Start} - When a player logs in and their session begins</li>
 *   <li>{@link End} - When a player logs out and their session ends</li>
 *   <li>{@link ServerSwitch} - When a player switches servers on a network</li>
 *   <li>{@link DataChange} - When session or profile data is modified</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * sessionManager.onSessionStart(event -> {
 *     PlayerProfile profile = event.getProfile();
 *     SessionData session = event.getSessionData();
 *
 *     // Welcome message
 *     if (profile.getFirstJoin().equals(profile.getLastJoin())) {
 *         broadcast("Welcome " + profile.getLastKnownName() + " for the first time!");
 *     }
 *
 *     // Track join time in session
 *     session.set("joinTime", event.getTimestamp());
 * });
 *
 * sessionManager.onSessionEnd(event -> {
 *     Duration playTime = event.getSessionDuration();
 *
 *     // Update total play time statistics
 *     int minutes = (int) playTime.toMinutes();
 *     event.getProfile().compute(PLAY_TIME_MINUTES, old -> old + minutes);
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SessionManager
 */
public interface SessionEvent {

    /**
     * Returns the player's UUID.
     *
     * @return the player UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getPlayerId();

    /**
     * Returns the session ID.
     *
     * @return the unique session identifier
     * @since 1.0.0
     */
    @NotNull
    String getSessionId();

    /**
     * Returns when this event occurred.
     *
     * @return the event timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getTimestamp();

    /**
     * Returns the server name where this event occurred.
     *
     * @return the server name
     * @since 1.0.0
     */
    @NotNull
    String getServerName();

    // ==================== Event Types ====================

    /**
     * Event fired when a player's session starts.
     *
     * <p>At this point, the player's profile data has been loaded and the
     * session data structure is available for use.
     *
     * @since 1.0.0
     */
    interface Start extends SessionEvent {

        /**
         * Returns the player's profile.
         *
         * @return the player profile
         * @since 1.0.0
         */
        @NotNull
        PlayerProfile getProfile();

        /**
         * Returns the session data container.
         *
         * @return the session data
         * @since 1.0.0
         */
        @NotNull
        SessionData getSessionData();

        /**
         * Returns the player's IP address.
         *
         * @return the IP address
         * @since 1.0.0
         */
        @NotNull
        String getIpAddress();

        /**
         * Returns whether this is the player's first time joining.
         *
         * @return true if this is the first join
         * @since 1.0.0
         */
        boolean isFirstJoin();

        /**
         * Returns the time since the player last logged in.
         *
         * <p>Returns null for first-time players.
         *
         * @return duration since last login, or null if first join
         * @since 1.0.0
         */
        @Nullable
        Duration getTimeSinceLastLogin();
    }

    /**
     * Event fired when a player's session ends.
     *
     * <p>This is fired before the player's data is saved to the database.
     * Any final modifications to the profile should be made in this handler.
     *
     * @since 1.0.0
     */
    interface End extends SessionEvent {

        /**
         * Returns the player's profile.
         *
         * <p>This is the last chance to modify the profile before it's saved.
         *
         * @return the player profile
         * @since 1.0.0
         */
        @NotNull
        PlayerProfile getProfile();

        /**
         * Returns the session data.
         *
         * <p>This data will be cleared after the event completes.
         *
         * @return the session data
         * @since 1.0.0
         */
        @NotNull
        SessionData getSessionData();

        /**
         * Returns how long the session lasted.
         *
         * @return the session duration
         * @since 1.0.0
         */
        @NotNull
        Duration getSessionDuration();

        /**
         * Returns the reason the session ended.
         *
         * @return the end reason
         * @since 1.0.0
         */
        @NotNull
        EndReason getReason();

        /**
         * Returns the kick/disconnect message, if any.
         *
         * @return the message, or null if not kicked
         * @since 1.0.0
         */
        @Nullable
        String getDisconnectMessage();
    }

    /**
     * Event fired when a player switches servers on a network.
     *
     * <p>This is only fired on networks (BungeeCord, Velocity) when a player
     * moves between backend servers.
     *
     * @since 1.0.0
     */
    interface ServerSwitch extends SessionEvent {

        /**
         * Returns the player's profile.
         *
         * @return the player profile
         * @since 1.0.0
         */
        @NotNull
        PlayerProfile getProfile();

        /**
         * Returns the session data.
         *
         * <p>Data marked as clearOnServerSwitch will be cleared after this event.
         *
         * @return the session data
         * @since 1.0.0
         */
        @NotNull
        SessionData getSessionData();

        /**
         * Returns the server the player is switching from.
         *
         * @return the previous server name
         * @since 1.0.0
         */
        @NotNull
        String getFromServer();

        /**
         * Returns the server the player is switching to.
         *
         * @return the new server name
         * @since 1.0.0
         */
        @NotNull
        String getToServer();
    }

    /**
     * Event fired when player data is modified.
     *
     * <p>This can be fired for local changes or for changes synchronized
     * from other servers.
     *
     * @since 1.0.0
     */
    interface DataChange extends SessionEvent {

        /**
         * Returns the player's profile.
         *
         * @return the player profile
         * @since 1.0.0
         */
        @NotNull
        PlayerProfile getProfile();

        /**
         * Returns the data keys that were changed.
         *
         * @return the set of changed keys
         * @since 1.0.0
         */
        @NotNull
        Set<DataKey<?>> getChangedKeys();

        /**
         * Checks if a specific key was changed.
         *
         * @param key the data key
         * @return true if the key was changed
         * @since 1.0.0
         */
        boolean hasChanged(@NotNull DataKey<?> key);

        /**
         * Returns whether this change was from a remote server.
         *
         * @return true if synchronized from another server
         * @since 1.0.0
         */
        boolean isRemoteChange();

        /**
         * Returns the source server for remote changes.
         *
         * @return the source server name, or null if local change
         * @since 1.0.0
         */
        @Nullable
        String getSourceServer();
    }

    // ==================== Enums ====================

    /**
     * Reasons why a session ended.
     *
     * @since 1.0.0
     */
    enum EndReason {
        /**
         * Player disconnected normally.
         */
        DISCONNECT,

        /**
         * Player was kicked by an operator or plugin.
         */
        KICKED,

        /**
         * Player was banned.
         */
        BANNED,

        /**
         * Connection timed out.
         */
        TIMEOUT,

        /**
         * Server is shutting down.
         */
        SERVER_SHUTDOWN,

        /**
         * Player switched to another server (network only).
         */
        SERVER_SWITCH,

        /**
         * Session ended for an unknown reason.
         */
        UNKNOWN
    }
}
