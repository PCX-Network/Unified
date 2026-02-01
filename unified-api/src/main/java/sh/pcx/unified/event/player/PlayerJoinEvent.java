/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.player;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Event fired when a player joins the server.
 *
 * <p>This event is fired after the player has fully connected and is visible
 * to other players. At this point, the player's data has been loaded and they
 * are ready to interact with the server.
 *
 * <h2>Platform Mapping</h2>
 * <table>
 *   <caption>Platform-specific event mapping</caption>
 *   <tr><th>Platform</th><th>Native Event</th></tr>
 *   <tr><td>Paper/Spigot</td><td>{@code PlayerJoinEvent}</td></tr>
 *   <tr><td>Sponge</td><td>{@code ServerSideConnectionEvent.Join}</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPlayerJoin(PlayerJoinEvent event) {
 *     UnifiedPlayer player = event.getPlayer();
 *
 *     // Welcome message
 *     player.sendMessage(Component.text("Welcome to the server!"));
 *
 *     // Customize join message
 *     if (player.hasPermission("vip")) {
 *         event.setJoinMessage(Component.text()
 *             .append(Component.text("[VIP] ", NamedTextColor.GOLD))
 *             .append(Component.text(player.getName() + " has joined!"))
 *             .build());
 *     }
 *
 *     // Disable join message
 *     // event.setJoinMessage(null);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This event is fired synchronously on the main server thread.
 * It is safe to interact with game state directly from handlers.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerQuitEvent
 * @see PlayerEvent
 */
public class PlayerJoinEvent extends PlayerEvent {

    private Component joinMessage;
    private final boolean firstJoin;
    private final String hostname;

    /**
     * Constructs a new player join event.
     *
     * @param player      the player who joined
     * @param joinMessage the join message to broadcast, or null for no message
     * @param firstJoin   whether this is the player's first time joining
     * @param hostname    the hostname the player used to connect
     * @since 1.0.0
     */
    public PlayerJoinEvent(
            @NotNull UnifiedPlayer player,
            @Nullable Component joinMessage,
            boolean firstJoin,
            @Nullable String hostname
    ) {
        super(player);
        this.joinMessage = joinMessage;
        this.firstJoin = firstJoin;
        this.hostname = hostname;
    }

    /**
     * Constructs a new player join event with a default join message.
     *
     * @param player the player who joined
     * @since 1.0.0
     */
    public PlayerJoinEvent(@NotNull UnifiedPlayer player) {
        this(player, Component.text(player.getName() + " joined the game"), false, null);
    }

    /**
     * Returns the join message that will be broadcast to all players.
     *
     * @return the join message, or empty if no message will be broadcast
     * @since 1.0.0
     */
    @NotNull
    public Optional<Component> getJoinMessage() {
        return Optional.ofNullable(joinMessage);
    }

    /**
     * Sets the join message that will be broadcast to all players.
     *
     * <p>Set to null to disable the join message broadcast entirely.
     *
     * @param joinMessage the join message, or null for no message
     * @since 1.0.0
     */
    public void setJoinMessage(@Nullable Component joinMessage) {
        this.joinMessage = joinMessage;
    }

    /**
     * Checks if the join message is present.
     *
     * @return true if a join message will be broadcast
     * @since 1.0.0
     */
    public boolean hasJoinMessage() {
        return joinMessage != null;
    }

    /**
     * Disables the join message broadcast.
     *
     * <p>Equivalent to {@code setJoinMessage(null)}.
     *
     * @since 1.0.0
     */
    public void disableJoinMessage() {
        this.joinMessage = null;
    }

    /**
     * Returns whether this is the player's first time joining the server.
     *
     * <p>First join is determined by whether the player has played before
     * (has a saved player data file).
     *
     * @return true if this is the first join
     * @since 1.0.0
     */
    public boolean isFirstJoin() {
        return firstJoin;
    }

    /**
     * Returns the hostname the player used to connect.
     *
     * <p>This is useful for servers using domain-based routing or
     * virtual hosts.
     *
     * @return the hostname, or empty if not available
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getHostname() {
        return Optional.ofNullable(hostname);
    }

    @Override
    public String toString() {
        return "PlayerJoinEvent[player=" + getPlayer().getName()
                + ", firstJoin=" + firstJoin
                + ", hasMessage=" + hasJoinMessage()
                + "]";
    }
}
