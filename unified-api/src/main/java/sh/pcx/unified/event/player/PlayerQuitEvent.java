/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.player;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Event fired when a player quits the server.
 *
 * <p>This event is fired when a player disconnects from the server for any reason,
 * including voluntary disconnect, timeout, kick, or server shutdown.
 *
 * <h2>Platform Mapping</h2>
 * <table>
 *   <caption>Platform-specific event mapping</caption>
 *   <tr><th>Platform</th><th>Native Event</th></tr>
 *   <tr><td>Paper/Spigot</td><td>{@code PlayerQuitEvent}</td></tr>
 *   <tr><td>Sponge</td><td>{@code ServerSideConnectionEvent.Disconnect}</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPlayerQuit(PlayerQuitEvent event) {
 *     UnifiedPlayer player = event.getPlayer();
 *
 *     // Log session duration
 *     Duration sessionTime = event.getSessionDuration().orElse(Duration.ZERO);
 *     logger.info(player.getName() + " played for " + sessionTime.toMinutes() + " minutes");
 *
 *     // Customize quit message based on reason
 *     switch (event.getReason()) {
 *         case KICKED:
 *             event.setQuitMessage(Component.text(player.getName() + " was kicked!"));
 *             break;
 *         case TIMEOUT:
 *             event.setQuitMessage(Component.text(player.getName() + " timed out"));
 *             break;
 *         default:
 *             event.setQuitMessage(Component.text(player.getName() + " left the game"));
 *     }
 * }
 * }</pre>
 *
 * <h2>Important Notes</h2>
 * <ul>
 *   <li>Player data should be saved before this event fires</li>
 *   <li>The player may not be fully accessible depending on the platform</li>
 *   <li>Avoid performing expensive operations in this handler</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerJoinEvent
 * @see PlayerEvent
 */
public class PlayerQuitEvent extends PlayerEvent {

    private Component quitMessage;
    private final QuitReason reason;
    private final Instant joinTime;

    /**
     * Constructs a new player quit event.
     *
     * @param player      the player who quit
     * @param quitMessage the quit message to broadcast, or null for no message
     * @param reason      the reason for quitting
     * @param joinTime    the time when the player joined, or null if unknown
     * @since 1.0.0
     */
    public PlayerQuitEvent(
            @NotNull UnifiedPlayer player,
            @Nullable Component quitMessage,
            @NotNull QuitReason reason,
            @Nullable Instant joinTime
    ) {
        super(player);
        this.quitMessage = quitMessage;
        this.reason = reason;
        this.joinTime = joinTime;
    }

    /**
     * Constructs a new player quit event with default values.
     *
     * @param player the player who quit
     * @since 1.0.0
     */
    public PlayerQuitEvent(@NotNull UnifiedPlayer player) {
        this(player, Component.text(player.getName() + " left the game"), QuitReason.DISCONNECTED, null);
    }

    /**
     * Returns the quit message that will be broadcast to all players.
     *
     * @return the quit message, or empty if no message will be broadcast
     * @since 1.0.0
     */
    @NotNull
    public Optional<Component> getQuitMessage() {
        return Optional.ofNullable(quitMessage);
    }

    /**
     * Sets the quit message that will be broadcast to all players.
     *
     * <p>Set to null to disable the quit message broadcast entirely.
     *
     * @param quitMessage the quit message, or null for no message
     * @since 1.0.0
     */
    public void setQuitMessage(@Nullable Component quitMessage) {
        this.quitMessage = quitMessage;
    }

    /**
     * Checks if the quit message is present.
     *
     * @return true if a quit message will be broadcast
     * @since 1.0.0
     */
    public boolean hasQuitMessage() {
        return quitMessage != null;
    }

    /**
     * Disables the quit message broadcast.
     *
     * <p>Equivalent to {@code setQuitMessage(null)}.
     *
     * @since 1.0.0
     */
    public void disableQuitMessage() {
        this.quitMessage = null;
    }

    /**
     * Returns the reason for the player's disconnect.
     *
     * @return the quit reason
     * @since 1.0.0
     */
    @NotNull
    public QuitReason getReason() {
        return reason;
    }

    /**
     * Returns the time when the player joined the server.
     *
     * @return the join time, or empty if unknown
     * @since 1.0.0
     */
    @NotNull
    public Optional<Instant> getJoinTime() {
        return Optional.ofNullable(joinTime);
    }

    /**
     * Returns how long the player was connected.
     *
     * <p>This is calculated as the time between join and quit.
     *
     * @return the session duration, or empty if join time is unknown
     * @since 1.0.0
     */
    @NotNull
    public Optional<Duration> getSessionDuration() {
        if (joinTime == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(joinTime, Instant.now()));
    }

    /**
     * Checks if the player was kicked.
     *
     * @return true if the player was kicked
     * @since 1.0.0
     */
    public boolean wasKicked() {
        return reason == QuitReason.KICKED;
    }

    /**
     * Checks if the player timed out.
     *
     * @return true if the player timed out
     * @since 1.0.0
     */
    public boolean timedOut() {
        return reason == QuitReason.TIMEOUT;
    }

    @Override
    public String toString() {
        return "PlayerQuitEvent[player=" + getPlayer().getName()
                + ", reason=" + reason
                + ", hasMessage=" + hasQuitMessage()
                + "]";
    }

    /**
     * Reasons for a player quitting the server.
     *
     * @since 1.0.0
     */
    public enum QuitReason {

        /**
         * The player disconnected voluntarily.
         */
        DISCONNECTED,

        /**
         * The player was kicked from the server.
         */
        KICKED,

        /**
         * The player's connection timed out.
         */
        TIMEOUT,

        /**
         * The player was banned.
         */
        BANNED,

        /**
         * The server is shutting down.
         */
        SERVER_SHUTDOWN,

        /**
         * The player was transferred to another server.
         */
        TRANSFERRED,

        /**
         * The reason is unknown.
         */
        UNKNOWN
    }
}
