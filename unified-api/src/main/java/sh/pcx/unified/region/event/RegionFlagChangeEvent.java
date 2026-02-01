/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region.event;

import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.region.Region;
import sh.pcx.unified.region.RegionFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a region's flag value changes.
 *
 * <p>This event is fired before the flag is actually changed, allowing
 * listeners to cancel the change or observe it.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onFlagChange(RegionFlagChangeEvent event) {
 *     // Prevent PVP being enabled in spawn
 *     if (event.getRegion().getName().equals("spawn")) {
 *         if (event.getFlag().equals(RegionFlag.PVP)) {
 *             Boolean newValue = (Boolean) event.getNewValue();
 *             if (newValue != null && newValue) {
 *                 event.setCancelled(true);
 *                 // Notify the person making the change
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class RegionFlagChangeEvent extends RegionEvent implements Cancellable {

    private final RegionFlag<?> flag;
    private final Object oldValue;
    private final Object newValue;
    private boolean cancelled;

    /**
     * Creates a new flag change event.
     *
     * @param region   the region whose flag is changing
     * @param flag     the flag being changed
     * @param oldValue the old flag value (may be null)
     * @param newValue the new flag value (may be null if being removed)
     */
    public RegionFlagChangeEvent(
            @NotNull Region region,
            @NotNull RegionFlag<?> flag,
            @Nullable Object oldValue,
            @Nullable Object newValue
    ) {
        super(region);
        this.flag = flag;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Returns the flag being changed.
     *
     * @return the flag
     * @since 1.0.0
     */
    @NotNull
    public RegionFlag<?> getFlag() {
        return flag;
    }

    /**
     * Returns the old flag value.
     *
     * @return the old value, or null if not previously set
     * @since 1.0.0
     */
    @Nullable
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Returns the new flag value.
     *
     * @return the new value, or null if being removed
     * @since 1.0.0
     */
    @Nullable
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Returns the old value cast to the flag's type.
     *
     * @param <T> the flag value type
     * @return the typed old value
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getTypedOldValue() {
        return (T) oldValue;
    }

    /**
     * Returns the new value cast to the flag's type.
     *
     * @param <T> the flag value type
     * @return the typed new value
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getTypedNewValue() {
        return (T) newValue;
    }

    /**
     * Checks if the flag is being removed (new value is null).
     *
     * @return true if the flag is being removed
     * @since 1.0.0
     */
    public boolean isRemoval() {
        return newValue == null;
    }

    /**
     * Checks if this is a new flag being set (old value is null).
     *
     * @return true if this is a new flag
     * @since 1.0.0
     */
    public boolean isNewFlag() {
        return oldValue == null && newValue != null;
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
     * <p>If cancelled, the flag will not be changed.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
