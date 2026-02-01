/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.world;

import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event fired when a block is broken by a player.
 *
 * <p>This event is fired when a player breaks a block. It provides information
 * about the block, the player who broke it, the tool used, and the expected drops.
 * The event can be cancelled to prevent the block from being broken.
 *
 * <h2>Platform Mapping</h2>
 * <table>
 *   <caption>Platform-specific event mapping</caption>
 *   <tr><th>Platform</th><th>Native Event</th></tr>
 *   <tr><td>Paper/Spigot</td><td>{@code BlockBreakEvent}</td></tr>
 *   <tr><td>Sponge</td><td>{@code ChangeBlockEvent.Break}</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onBlockBreak(BlockBreakEvent event) {
 *     UnifiedBlock block = event.getBlock();
 *     UnifiedPlayer player = event.getPlayer();
 *
 *     // Protect blocks in spawn
 *     if (isInSpawn(block.getLocation())) {
 *         if (!player.hasPermission("spawn.break")) {
 *             event.setCancelled(true);
 *             player.sendMessage(Component.text("You cannot break blocks in spawn!"));
 *             return;
 *         }
 *     }
 *
 *     // Modify drops
 *     if (block.getType().equals("minecraft:diamond_ore")) {
 *         event.setDropExperience(true);
 *         event.setExperienceAmount(10);
 *
 *         // Double drops for VIPs
 *         if (player.hasPermission("vip.drops")) {
 *             List<UnifiedItemStack> drops = new ArrayList<>(event.getDrops());
 *             event.getDrops().addAll(drops); // Double the drops
 *         }
 *     }
 *
 *     // Disable drops
 *     if (shouldVoidDrops(block)) {
 *         event.setDropItems(false);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BlockEvent
 * @see BlockPlaceEvent
 * @see Cancellable
 */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private final UnifiedPlayer player;
    private final UnifiedItemStack tool;
    private final List<UnifiedItemStack> drops;
    private boolean dropItems;
    private boolean dropExperience;
    private int experienceAmount;
    private boolean cancelled;

    /**
     * Constructs a new block break event.
     *
     * @param block   the block being broken
     * @param player  the player breaking the block
     * @param tool    the tool used to break the block, or null if none
     * @param drops   the items that will drop, or null for default drops
     * @since 1.0.0
     */
    public BlockBreakEvent(
            @NotNull UnifiedBlock block,
            @NotNull UnifiedPlayer player,
            @Nullable UnifiedItemStack tool,
            @Nullable Collection<UnifiedItemStack> drops
    ) {
        super(block);
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.tool = tool;
        this.drops = drops != null ? new ArrayList<>(drops) : new ArrayList<>();
        this.dropItems = true;
        this.dropExperience = true;
        this.experienceAmount = 0;
        this.cancelled = false;
    }

    /**
     * Constructs a new block break event with default values.
     *
     * @param block  the block being broken
     * @param player the player breaking the block
     * @since 1.0.0
     */
    public BlockBreakEvent(@NotNull UnifiedBlock block, @NotNull UnifiedPlayer player) {
        this(block, player, null, null);
    }

    /**
     * Returns the player who is breaking the block.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the tool used to break the block.
     *
     * @return the tool, or empty if no tool was used (fist)
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedItemStack> getTool() {
        return Optional.ofNullable(tool);
    }

    /**
     * Returns the items that will drop when the block is broken.
     *
     * <p>The returned list is mutable. Add or remove items to modify
     * what drops.
     *
     * @return the mutable list of drops
     * @since 1.0.0
     */
    @NotNull
    public List<UnifiedItemStack> getDrops() {
        return drops;
    }

    /**
     * Sets the items that will drop when the block is broken.
     *
     * @param drops the items to drop
     * @since 1.0.0
     */
    public void setDrops(@NotNull Collection<UnifiedItemStack> drops) {
        this.drops.clear();
        this.drops.addAll(drops);
    }

    /**
     * Clears all drops.
     *
     * @since 1.0.0
     */
    public void clearDrops() {
        drops.clear();
    }

    /**
     * Adds an item to the drops.
     *
     * @param item the item to add
     * @since 1.0.0
     */
    public void addDrop(@NotNull UnifiedItemStack item) {
        drops.add(item);
    }

    /**
     * Returns an immutable view of the drops.
     *
     * @return immutable collection of drops
     * @since 1.0.0
     */
    @NotNull
    public Collection<UnifiedItemStack> getDropsView() {
        return Collections.unmodifiableList(drops);
    }

    /**
     * Returns whether items will drop from this block.
     *
     * @return true if items will drop
     * @since 1.0.0
     */
    public boolean willDropItems() {
        return dropItems;
    }

    /**
     * Sets whether items will drop from this block.
     *
     * @param dropItems true to enable drops
     * @since 1.0.0
     */
    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    /**
     * Returns whether experience will drop from this block.
     *
     * @return true if experience will drop
     * @since 1.0.0
     */
    public boolean willDropExperience() {
        return dropExperience;
    }

    /**
     * Sets whether experience will drop from this block.
     *
     * @param dropExperience true to enable experience drops
     * @since 1.0.0
     */
    public void setDropExperience(boolean dropExperience) {
        this.dropExperience = dropExperience;
    }

    /**
     * Returns the amount of experience that will drop.
     *
     * @return the experience amount
     * @since 1.0.0
     */
    public int getExperienceAmount() {
        return experienceAmount;
    }

    /**
     * Sets the amount of experience that will drop.
     *
     * @param experienceAmount the experience amount
     * @since 1.0.0
     */
    public void setExperienceAmount(int experienceAmount) {
        this.experienceAmount = Math.max(0, experienceAmount);
    }

    /**
     * Returns whether the player used a tool (not bare hands).
     *
     * @return true if a tool was used
     * @since 1.0.0
     */
    public boolean usedTool() {
        return tool != null && !tool.isEmpty();
    }

    /**
     * Checks if the break was done in creative mode.
     *
     * @return true if in creative mode
     * @since 1.0.0
     */
    public boolean isCreativeMode() {
        return player.getGameMode() == UnifiedPlayer.GameMode.CREATIVE;
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
        return "BlockBreakEvent[block=" + getBlockType()
                + ", player=" + player.getName()
                + ", tool=" + (tool != null ? tool.getType() : "none")
                + ", drops=" + drops.size()
                + ", cancelled=" + cancelled
                + "]";
    }
}
