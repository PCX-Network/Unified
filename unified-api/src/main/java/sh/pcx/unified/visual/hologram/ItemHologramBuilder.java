/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Builder for creating item holograms.
 *
 * <p>Use this builder to configure item hologram properties before creation.
 * Obtain a builder from {@link HologramService#createItem}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ItemHologram hologram = hologramService.createItem(location)
 *     .item(new ItemStack(Material.DIAMOND))
 *     .spin(true, 2.0f)
 *     .bob(true)
 *     .bobHeight(0.2f)
 *     .bobSpeed(1.0f)
 *     .glowing(true)
 *     .scale(1.5f)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see HologramService
 * @see ItemHologram
 */
public interface ItemHologramBuilder {

    /**
     * Sets the name of the hologram.
     *
     * @param name the hologram name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder name(@NotNull String name);

    /**
     * Sets the item to display.
     *
     * @param item the item to display
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder item(@NotNull UnifiedItemStack item);

    /**
     * Enables spinning with the specified speed.
     *
     * @param spin          true to enable spinning
     * @param degreesPerTick rotation speed in degrees per tick
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder spin(boolean spin, float degreesPerTick);

    /**
     * Enables spinning with default speed.
     *
     * @param spin true to enable spinning
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ItemHologramBuilder spin(boolean spin) {
        return spin(spin, 2.0f);
    }

    /**
     * Enables bobbing animation.
     *
     * @param bob true to enable bobbing
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder bob(boolean bob);

    /**
     * Sets the bobbing height.
     *
     * @param height the bobbing height in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder bobHeight(float height);

    /**
     * Sets the bobbing speed.
     *
     * @param speed the bobbing speed multiplier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder bobSpeed(float speed);

    /**
     * Sets the billboard mode.
     *
     * @param billboard the billboard mode
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder billboard(@NotNull Billboard billboard);

    /**
     * Sets the scale of the item display.
     *
     * @param scale the scale factor (1.0 = normal size)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder scale(float scale);

    /**
     * Sets whether the item appears glowing.
     *
     * @param glowing true to enable glow
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder glowing(boolean glowing);

    /**
     * Sets whether the hologram persists across server restarts.
     *
     * @param persistent true to persist
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder persistent(boolean persistent);

    /**
     * Sets the hologram to only be visible to specific players.
     *
     * @param players the players who can see the hologram
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder viewers(@NotNull UnifiedPlayer... players);

    /**
     * Sets the hologram to only be visible to specific players.
     *
     * @param players the players who can see the hologram
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder viewers(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Sets the click handler for the hologram.
     *
     * @param handler the click handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder onClick(@NotNull Consumer<HologramClickEvent> handler);

    /**
     * Sets the view range for this hologram.
     *
     * @param range the view range in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder viewRange(double range);

    /**
     * Builds and spawns the item hologram.
     *
     * @return the created item hologram
     * @since 1.0.0
     */
    @NotNull
    ItemHologram build();

    /**
     * Builds the item hologram without spawning it.
     *
     * @return the created item hologram (not spawned)
     * @since 1.0.0
     */
    @NotNull
    ItemHologram buildWithoutSpawning();
}
