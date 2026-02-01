/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.world;

import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedBlock.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Event fired when a player places a block.
 *
 * <p>This event is fired when a player places a block. It provides information
 * about the block being placed, the player, the item used, and the block that
 * was clicked to place against. The event can be cancelled to prevent placement.
 *
 * <h2>Platform Mapping</h2>
 * <table>
 *   <caption>Platform-specific event mapping</caption>
 *   <tr><th>Platform</th><th>Native Event</th></tr>
 *   <tr><td>Paper/Spigot</td><td>{@code BlockPlaceEvent}</td></tr>
 *   <tr><td>Sponge</td><td>{@code ChangeBlockEvent.Place}</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onBlockPlace(BlockPlaceEvent event) {
 *     UnifiedBlock block = event.getBlock();
 *     UnifiedPlayer player = event.getPlayer();
 *
 *     // Prevent placing blocks in protected areas
 *     if (isProtected(block.getLocation())) {
 *         if (!player.hasPermission("protection.bypass")) {
 *             event.setCancelled(true);
 *             player.sendMessage(Component.text("You cannot place blocks here!"));
 *             return;
 *         }
 *     }
 *
 *     // Prevent placing certain blocks
 *     if (block.getType().equals("minecraft:tnt")) {
 *         if (!player.hasPermission("place.tnt")) {
 *             event.setCancelled(true);
 *             player.sendMessage(Component.text("TNT placement is disabled!"));
 *             return;
 *         }
 *     }
 *
 *     // Log block placements
 *     logger.info(player.getName() + " placed " + block.getType()
 *         + " at " + block.getLocation());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BlockEvent
 * @see BlockBreakEvent
 * @see Cancellable
 */
public class BlockPlaceEvent extends BlockEvent implements Cancellable {

    private final UnifiedPlayer player;
    private final UnifiedItemStack itemInHand;
    private final UnifiedBlock placedAgainst;
    private final BlockFace placedFace;
    private final String previousBlockType;
    private boolean canBuild;
    private boolean cancelled;

    /**
     * Constructs a new block place event.
     *
     * @param block            the block being placed
     * @param player           the player placing the block
     * @param itemInHand       the item used to place the block
     * @param placedAgainst    the block that was clicked to place against
     * @param placedFace       the face of the block that was clicked
     * @param previousBlockType the type of block that was replaced
     * @since 1.0.0
     */
    public BlockPlaceEvent(
            @NotNull UnifiedBlock block,
            @NotNull UnifiedPlayer player,
            @Nullable UnifiedItemStack itemInHand,
            @Nullable UnifiedBlock placedAgainst,
            @Nullable BlockFace placedFace,
            @Nullable String previousBlockType
    ) {
        super(block);
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.itemInHand = itemInHand;
        this.placedAgainst = placedAgainst;
        this.placedFace = placedFace;
        this.previousBlockType = previousBlockType;
        this.canBuild = true;
        this.cancelled = false;
    }

    /**
     * Constructs a new block place event with default values.
     *
     * @param block  the block being placed
     * @param player the player placing the block
     * @since 1.0.0
     */
    public BlockPlaceEvent(@NotNull UnifiedBlock block, @NotNull UnifiedPlayer player) {
        this(block, player, null, null, null, null);
    }

    /**
     * Returns the player who is placing the block.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the item that was used to place the block.
     *
     * @return the item in hand, or empty if not available
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedItemStack> getItemInHand() {
        return Optional.ofNullable(itemInHand);
    }

    /**
     * Returns the block that was clicked to place against.
     *
     * <p>This is the existing block that the player right-clicked on
     * to place the new block.
     *
     * @return the block placed against, or empty if not available
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedBlock> getPlacedAgainst() {
        return Optional.ofNullable(placedAgainst);
    }

    /**
     * Returns the face of the block that was clicked.
     *
     * @return the block face, or empty if not available
     * @since 1.0.0
     */
    @NotNull
    public Optional<BlockFace> getPlacedFace() {
        return Optional.ofNullable(placedFace);
    }

    /**
     * Returns the type of block that was previously at this location.
     *
     * <p>This is typically air, but could be a replaceable block like
     * grass or water.
     *
     * @return the previous block type, or empty if not available
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getPreviousBlockType() {
        return Optional.ofNullable(previousBlockType);
    }

    /**
     * Returns whether the block was replacing another block.
     *
     * @return true if a non-air block was replaced
     * @since 1.0.0
     */
    public boolean isReplacing() {
        return previousBlockType != null
                && !previousBlockType.equals("minecraft:air")
                && !previousBlockType.equals("minecraft:cave_air")
                && !previousBlockType.equals("minecraft:void_air");
    }

    /**
     * Returns whether building is allowed at this location.
     *
     * <p>This is a hint from the platform about whether building is
     * allowed (e.g., spawn protection). Even if this returns true,
     * the event can still be cancelled.
     *
     * @return true if building is allowed
     * @since 1.0.0
     */
    public boolean canBuild() {
        return canBuild;
    }

    /**
     * Sets whether building is allowed at this location.
     *
     * <p>This affects the platform's default behavior. Set to false
     * to indicate the placement should be blocked.
     *
     * @param canBuild true to allow building
     * @since 1.0.0
     */
    public void setBuild(boolean canBuild) {
        this.canBuild = canBuild;
    }

    /**
     * Checks if the placement was done in creative mode.
     *
     * @return true if in creative mode
     * @since 1.0.0
     */
    public boolean isCreativeMode() {
        return player.getGameMode() == UnifiedPlayer.GameMode.CREATIVE;
    }

    /**
     * Returns the type of block being placed.
     *
     * <p>This is equivalent to {@link #getBlockType()}.
     *
     * @return the block type being placed
     * @since 1.0.0
     */
    @NotNull
    public String getPlacedType() {
        return getBlockType();
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
        return "BlockPlaceEvent[block=" + getBlockType()
                + ", player=" + player.getName()
                + ", replacing=" + (previousBlockType != null ? previousBlockType : "nothing")
                + ", canBuild=" + canBuild
                + ", cancelled=" + cancelled
                + "]";
    }
}
