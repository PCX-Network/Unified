/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.world;

import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Base class for all block-related events.
 *
 * <p>This abstract class provides the foundation for events that involve
 * a specific block in the world. All block events have access to the
 * {@link UnifiedBlock} that is the subject of the event.
 *
 * <h2>Platform Bridging</h2>
 * <p>Block events are automatically bridged from platform-specific events:
 * <ul>
 *   <li>Paper/Spigot: {@code org.bukkit.event.block.BlockEvent}</li>
 *   <li>Sponge: Various {@code ChangeBlockEvent} subtypes</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedBlock
 * @see BlockBreakEvent
 * @see BlockPlaceEvent
 */
public abstract class BlockEvent extends UnifiedEvent {

    private final UnifiedBlock block;

    /**
     * Constructs a new block event.
     *
     * @param block the block involved in this event
     * @throws NullPointerException if block is null
     * @since 1.0.0
     */
    protected BlockEvent(@NotNull UnifiedBlock block) {
        super();
        this.block = Objects.requireNonNull(block, "block cannot be null");
    }

    /**
     * Returns the block involved in this event.
     *
     * @return the block
     * @since 1.0.0
     */
    @NotNull
    public UnifiedBlock getBlock() {
        return block;
    }

    /**
     * Returns the location of the block.
     *
     * @return the block's location
     * @since 1.0.0
     */
    @NotNull
    public UnifiedLocation getLocation() {
        return block.getLocation();
    }

    /**
     * Returns the world containing the block.
     *
     * @return the block's world
     * @since 1.0.0
     */
    @NotNull
    public UnifiedWorld getWorld() {
        return block.getWorld();
    }

    /**
     * Returns the block type as a namespaced ID.
     *
     * @return the block type (e.g., "minecraft:stone")
     * @since 1.0.0
     */
    @NotNull
    public String getBlockType() {
        return block.getType();
    }

    @Override
    public String toString() {
        return getEventName() + "[block=" + block.getType()
                + ", location=" + formatLocation()
                + "]";
    }

    /**
     * Formats the location for display.
     */
    private String formatLocation() {
        UnifiedLocation loc = block.getLocation();
        String worldName = loc.getWorld().map(w -> w.getName()).orElse("unknown");
        return String.format("(%d, %d, %d in %s)",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                worldName);
    }
}
