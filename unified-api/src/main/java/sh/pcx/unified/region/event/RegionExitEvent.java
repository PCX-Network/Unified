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
 * Event fired when a player exits a region.
 *
 * <p>This event is cancellable - cancelling it will prevent the player from
 * leaving the region (they will be pushed back into the region).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onRegionExit(RegionExitEvent event) {
 *     if (event.getRegion().getName().equals("jail")) {
 *         if (!event.getPlayer().hasPermission("myserver.jail.bypass")) {
 *             event.setCancelled(true);
 *             event.getPlayer().sendMessage(Component.text("You cannot leave jail!"));
 *         }
 *     }
 * }
 *
 * @EventHandler
 * public void onSpawnExit(RegionExitEvent event) {
 *     if (event.getRegion().getName().equals("spawn")) {
 *         event.getPlayer().sendMessage(Component.text("Leaving spawn area..."));
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RegionEnterEvent
 */
public class RegionExitEvent extends RegionEvent implements Cancellable {

    private final UnifiedPlayer player;
    private final UnifiedLocation from;
    private final UnifiedLocation to;
    private boolean cancelled;

    /**
     * Creates a new region exit event.
     *
     * @param region the region being exited
     * @param player the player exiting the region
     * @param from   the location the player is coming from (inside region)
     * @param to     the location the player is moving to (outside region)
     */
    public RegionExitEvent(
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
     * Returns the player exiting the region.
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
     * <p>This location is inside the region.
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
     * <p>This location is outside the region.
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
     * <p>If cancelled, the player will be prevented from leaving the region.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
