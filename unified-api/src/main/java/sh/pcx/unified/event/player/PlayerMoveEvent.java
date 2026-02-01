/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.player;

import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Event fired when a player moves.
 *
 * <p>This event is fired whenever a player's location changes, including:
 * <ul>
 *   <li>Walking, running, swimming, flying</li>
 *   <li>Riding entities (unless handled separately)</li>
 *   <li>Being pushed by pistons or explosions</li>
 *   <li>Looking around (rotation changes)</li>
 * </ul>
 *
 * <p>Teleportation typically fires a separate event (PlayerTeleportEvent).
 *
 * <h2>Platform Mapping</h2>
 * <table>
 *   <caption>Platform-specific event mapping</caption>
 *   <tr><th>Platform</th><th>Native Event</th></tr>
 *   <tr><td>Paper/Spigot</td><td>{@code PlayerMoveEvent}</td></tr>
 *   <tr><td>Sponge</td><td>{@code MoveEntityEvent} (for players)</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onPlayerMove(PlayerMoveEvent event) {
 *     UnifiedLocation from = event.getFrom();
 *     UnifiedLocation to = event.getTo();
 *
 *     // Only check actual movement (not just head rotation)
 *     if (!event.hasActuallyMoved()) {
 *         return;
 *     }
 *
 *     // Prevent entering restricted area
 *     if (isRestricted(to) && !event.getPlayer().hasPermission("area.bypass")) {
 *         event.setCancelled(true);
 *         event.getPlayer().sendMessage(Component.text("You cannot enter this area!"));
 *         return;
 *     }
 *
 *     // Modify destination
 *     if (shouldRedirect(to)) {
 *         event.setTo(getRedirectLocation(to));
 *     }
 *
 *     // Check for border crossing
 *     if (crossedBorder(from, to)) {
 *         announceRegionChange(event.getPlayer(), from, to);
 *     }
 * }
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <p>This event fires very frequently (every tick a player moves). Keep handlers
 * lightweight and avoid:
 * <ul>
 *   <li>Expensive calculations</li>
 *   <li>Database operations</li>
 *   <li>Complex condition checks</li>
 * </ul>
 *
 * <p>Consider using cooldowns or caching for expensive operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerEvent
 * @see Cancellable
 * @see UnifiedLocation
 */
public class PlayerMoveEvent extends PlayerEvent implements Cancellable {

    private final UnifiedLocation from;
    private UnifiedLocation to;
    private boolean cancelled;

    /**
     * Constructs a new player move event.
     *
     * @param player the player who moved
     * @param from   the location the player moved from
     * @param to     the location the player moved to
     * @since 1.0.0
     */
    public PlayerMoveEvent(
            @NotNull UnifiedPlayer player,
            @NotNull UnifiedLocation from,
            @NotNull UnifiedLocation to
    ) {
        super(player);
        this.from = Objects.requireNonNull(from, "from location cannot be null");
        this.to = Objects.requireNonNull(to, "to location cannot be null");
        this.cancelled = false;
    }

    /**
     * Returns the location the player moved from.
     *
     * @return the origin location
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation getFrom() {
        return from;
    }

    /**
     * Returns the location the player is moving to.
     *
     * @return the destination location
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation getTo() {
        return to;
    }

    /**
     * Sets the location the player will move to.
     *
     * <p>Modifying this will change where the player ends up. This can be
     * used to redirect player movement.
     *
     * @param to the new destination location
     * @throws NullPointerException if to is null
     * @since 1.0.0
     */
    public void setTo(@NotNull UnifiedLocation to) {
        this.to = Objects.requireNonNull(to, "to location cannot be null");
    }

    /**
     * Checks if the player actually moved position (not just rotated).
     *
     * <p>Returns true if the X, Y, or Z coordinate changed. Returns false
     * if only yaw or pitch changed (player just looked around).
     *
     * @return true if the player's position changed
     * @since 1.0.0
     */
    public boolean hasActuallyMoved() {
        return from.x() != to.x()
                || from.y() != to.y()
                || from.z() != to.z();
    }

    /**
     * Checks if the player moved to a different block.
     *
     * <p>Returns true if the block coordinates changed. This is useful
     * for block-based checks like region boundaries.
     *
     * @return true if the player moved to a different block
     * @since 1.0.0
     */
    public boolean hasChangedBlock() {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    /**
     * Checks if the player moved to a different chunk.
     *
     * @return true if the player moved to a different chunk
     * @since 1.0.0
     */
    public boolean hasChangedChunk() {
        return (from.getBlockX() >> 4) != (to.getBlockX() >> 4)
                || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4);
    }

    /**
     * Checks if the player's rotation (yaw or pitch) changed.
     *
     * @return true if the player's view direction changed
     * @since 1.0.0
     */
    public boolean hasChangedRotation() {
        return from.yaw() != to.yaw() || from.pitch() != to.pitch();
    }

    /**
     * Checks if the player changed worlds.
     *
     * @return true if the player moved to a different world
     * @since 1.0.0
     */
    public boolean hasChangedWorld() {
        return !from.world().equals(to.world());
    }

    /**
     * Returns the distance the player moved.
     *
     * @return the distance in blocks
     * @since 1.0.0
     */
    public double getDistance() {
        return from.distance(to);
    }

    /**
     * Returns the squared distance the player moved.
     *
     * <p>This is more efficient than {@link #getDistance()} when you only
     * need to compare distances.
     *
     * @return the squared distance
     * @since 1.0.0
     */
    public double getDistanceSquared() {
        return from.distanceSquared(to);
    }

    /**
     * Returns the horizontal distance the player moved (ignoring Y).
     *
     * @return the horizontal distance in blocks
     * @since 1.0.0
     */
    public double getHorizontalDistance() {
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Returns the vertical distance the player moved.
     *
     * @return the vertical distance (positive = up, negative = down)
     * @since 1.0.0
     */
    public double getVerticalDistance() {
        return to.y() - from.y();
    }

    /**
     * Checks if the player is moving upward.
     *
     * @return true if the player is moving up
     * @since 1.0.0
     */
    public boolean isMovingUp() {
        return to.y() > from.y();
    }

    /**
     * Checks if the player is moving downward.
     *
     * @return true if the player is moving down
     * @since 1.0.0
     */
    public boolean isMovingDown() {
        return to.y() < from.y();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public String toString() {
        return "PlayerMoveEvent[player=" + getPlayer().getName()
                + ", from=" + formatLocation(from)
                + ", to=" + formatLocation(to)
                + ", distance=" + String.format("%.2f", getDistance())
                + ", cancelled=" + cancelled
                + "]";
    }

    private String formatLocation(UnifiedLocation loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.x(), loc.y(), loc.z());
    }
}
