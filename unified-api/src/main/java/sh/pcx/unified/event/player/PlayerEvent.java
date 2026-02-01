/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.player;

import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Base class for all player-related events.
 *
 * <p>This abstract class provides the foundation for events that involve a player.
 * All player events have access to the {@link UnifiedPlayer} that triggered or
 * is the subject of the event.
 *
 * <h2>Platform Bridging</h2>
 * <p>Player events are automatically bridged from platform-specific events:
 * <ul>
 *   <li>Paper/Spigot: {@code org.bukkit.event.player.PlayerEvent}</li>
 *   <li>Sponge: Various events involving {@code ServerPlayer}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class MyListener implements EventListener {
 *
 *     @EventHandler
 *     public void onAnyPlayerEvent(PlayerEvent event) {
 *         UnifiedPlayer player = event.getPlayer();
 *         logger.info("Player event: " + event.getEventName()
 *             + " for " + player.getName());
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer
 * @see PlayerJoinEvent
 * @see PlayerQuitEvent
 * @see PlayerChatEvent
 * @see PlayerMoveEvent
 */
public abstract class PlayerEvent extends UnifiedEvent {

    private final UnifiedPlayer player;

    /**
     * Constructs a new player event.
     *
     * @param player the player involved in this event
     * @throws NullPointerException if player is null
     * @since 1.0.0
     */
    protected PlayerEvent(@NotNull UnifiedPlayer player) {
        super();
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Constructs a new player event with async flag.
     *
     * @param player the player involved in this event
     * @param async  whether this event is fired asynchronously
     * @throws NullPointerException if player is null
     * @since 1.0.0
     */
    protected PlayerEvent(@NotNull UnifiedPlayer player, boolean async) {
        super(async);
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Returns the player involved in this event.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return getEventName() + "[player=" + player.getName() + ", id=" + getEventId() + "]";
    }
}
