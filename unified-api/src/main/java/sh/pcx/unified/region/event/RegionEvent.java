/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region.event;

import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.region.Region;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all region-related events.
 *
 * <p>Region events are fired when players interact with regions,
 * such as entering or exiting them.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RegionEnterEvent
 * @see RegionExitEvent
 */
public abstract class RegionEvent extends UnifiedEvent {

    private final Region region;

    /**
     * Creates a new region event.
     *
     * @param region the region involved in this event
     */
    protected RegionEvent(@NotNull Region region) {
        this.region = region;
    }

    /**
     * Creates a new async region event.
     *
     * @param region the region involved in this event
     * @param async  whether this event is async
     */
    protected RegionEvent(@NotNull Region region, boolean async) {
        super(async);
        this.region = region;
    }

    /**
     * Returns the region involved in this event.
     *
     * @return the region
     * @since 1.0.0
     */
    @NotNull
    public Region getRegion() {
        return region;
    }
}
