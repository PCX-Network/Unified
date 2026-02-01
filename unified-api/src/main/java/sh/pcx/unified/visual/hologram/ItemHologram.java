/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a hologram that displays a floating item.
 *
 * <p>Item holograms can display any item with optional animations
 * such as spinning and bobbing.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ItemHologram itemHologram = hologramService.createItem(location)
 *     .item(new ItemStack(Material.DIAMOND))
 *     .spin(true, 2.0f)
 *     .bob(true)
 *     .glowing(true)
 *     .build();
 *
 * // Change the displayed item
 * itemHologram.setItem(new ItemStack(Material.EMERALD));
 *
 * // Toggle spinning
 * itemHologram.setSpinning(false);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Hologram
 * @see HologramService#createItem
 */
public interface ItemHologram extends Hologram {

    /**
     * Returns the item being displayed.
     *
     * @return the displayed item
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getItem();

    /**
     * Sets the item to display.
     *
     * @param item the item to display
     * @since 1.0.0
     */
    void setItem(@NotNull UnifiedItemStack item);

    /**
     * Returns whether the item is spinning.
     *
     * @return true if spinning
     * @since 1.0.0
     */
    boolean isSpinning();

    /**
     * Sets whether the item spins.
     *
     * @param spinning true to enable spinning
     * @since 1.0.0
     */
    void setSpinning(boolean spinning);

    /**
     * Returns the spin speed in degrees per tick.
     *
     * @return the spin speed
     * @since 1.0.0
     */
    float getSpinSpeed();

    /**
     * Sets the spin speed.
     *
     * @param degreesPerTick the rotation speed in degrees per tick
     * @since 1.0.0
     */
    void setSpinSpeed(float degreesPerTick);

    /**
     * Returns whether the item has a bobbing animation.
     *
     * @return true if bobbing
     * @since 1.0.0
     */
    boolean isBobbing();

    /**
     * Sets whether the item has a bobbing animation.
     *
     * @param bobbing true to enable bobbing
     * @since 1.0.0
     */
    void setBobbing(boolean bobbing);

    /**
     * Returns the bobbing height amplitude.
     *
     * @return the bobbing height in blocks
     * @since 1.0.0
     */
    float getBobbingHeight();

    /**
     * Sets the bobbing height amplitude.
     *
     * @param height the bobbing height in blocks
     * @since 1.0.0
     */
    void setBobbingHeight(float height);

    /**
     * Returns the bobbing speed.
     *
     * @return the bobbing speed multiplier
     * @since 1.0.0
     */
    float getBobbingSpeed();

    /**
     * Sets the bobbing speed.
     *
     * @param speed the bobbing speed multiplier
     * @since 1.0.0
     */
    void setBobbingSpeed(float speed);

    /**
     * Returns whether the item appears glowing.
     *
     * @return true if glowing
     * @since 1.0.0
     */
    boolean isGlowing();

    /**
     * Sets whether the item appears glowing.
     *
     * @param glowing true to enable glow effect
     * @since 1.0.0
     */
    void setGlowing(boolean glowing);

    /**
     * Returns the scale of the item display.
     *
     * @return the scale factor (1.0 = normal size)
     * @since 1.0.0
     */
    float getScale();

    /**
     * Sets the scale of the item display.
     *
     * @param scale the scale factor (1.0 = normal size)
     * @since 1.0.0
     */
    void setScale(float scale);
}
