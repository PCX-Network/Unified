/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region.event;

import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.region.Region;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player enters a region.
 *
 * <p>This event is cancellable - cancelling it will prevent the player from
 * entering the region (they will be pushed back to their previous location).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onRegionEnter(RegionEnterEvent event) {
 *     if (event.getRegion().getName().equals("vip-area")) {
 *         if (!event.getPlayer().hasPermission("myserver.vip")) {
 *             event.setCancelled(true);
 *             event.getPlayer().sendMessage(Component.text("VIP area only!"));
 *         }
 *     }
 * }
 *
 * @EventHandler
 * public void onSpawnEnter(RegionEnterEvent event) {
 *     if (event.getRegion().getName().equals("spawn")) {
 *         event.getPlayer().sendMessage(Component.text("Welcome to spawn!"));
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RegionExitEvent
 */
public class RegionEnterEvent extends RegionEvent implements Cancellable {

    private final UnifiedPlayer player;
    private final UnifiedLocation from;
    private final UnifiedLocation to;
    private boolean cancelled;

    /**
     * Creates a new region enter event.
     *
     * @param region the region being entered
     * @param player the player entering the region
     * @param from   the location the player is coming from
     * @param to     the location the player is moving to
     */
    public RegionEnterEvent(
            @NotNull Region region,
            @NotNull UnifiedPlayer player,
            @NotNull UnifiedLocation from,
            @NotNull UnifiedLocation to
    ) {
        super(region);
        this.player = player;
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the player entering the region.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the location the player is coming from.
     *
     * <p>This location is outside the region.
     *
     * @return the from location
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation getFrom() {
        return from;
    }

    /**
     * Returns the location the player is moving to.
     *
     * <p>This location is inside the region.
     *
     * @return the to location
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation getTo() {
        return to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If cancelled, the player will be prevented from entering the region.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
