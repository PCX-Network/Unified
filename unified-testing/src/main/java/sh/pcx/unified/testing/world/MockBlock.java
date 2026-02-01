/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.world;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Mock implementation of a Minecraft block for testing purposes.
 *
 * <p>MockBlock represents a block in a MockWorld and tracks its type,
 * block state data, and block data for testing block-related functionality.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockWorld world = server.getMockWorld("world");
 * MockBlock block = world.getMockBlockAt(new UnifiedLocation(world, 10, 64, 10));
 *
 * // Set block type
 * block.setType("minecraft:stone");
 *
 * // Check block type
 * assertThat(block.getType()).isEqualTo("minecraft:stone");
 *
 * // Set block data
 * block.setProperty("facing", "north");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockWorld
 */
public final class MockBlock implements UnifiedBlock {

    private final MockWorld world;
    private final UnifiedLocation location;
    private String type;
    private final Map<String, String> properties = new HashMap<>();
    private int lightLevel = 0;
    private int skyLightLevel = 15;
    private int blockLightLevel = 0;
    private boolean powered = false;

    /**
     * Creates a new mock block.
     *
     * @param world    the containing world
     * @param location the block location
     * @param type     the initial block type
     */
    public MockBlock(
        @NotNull MockWorld world,
        @NotNull UnifiedLocation location,
        @NotNull String type
    ) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        return location;
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return location.getBlockX();
    }

    @Override
    public int getY() {
        return location.getBlockY();
    }

    @Override
    public int getZ() {
        return location.getBlockZ();
    }

    @Override
    @NotNull
    public String getType() {
        return type;
    }

    @Override
    public void setType(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        properties.clear(); // Clear properties when type changes
    }

    @Override
    public void setType(@NotNull String type, boolean physics) {
        setType(type);
        // In mock, we don't apply physics but could track if needed
    }

    @Override
    public boolean isEmpty() {
        return "minecraft:air".equals(type) ||
               "minecraft:cave_air".equals(type) ||
               "minecraft:void_air".equals(type);
    }

    @Override
    public boolean isLiquid() {
        return "minecraft:water".equals(type) ||
               "minecraft:lava".equals(type) ||
               type.contains("water") ||
               type.contains("lava");
    }

    @Override
    public boolean isSolid() {
        // Simplified check - in reality would check block properties
        return !isEmpty() && !isLiquid() && !isPassable();
    }

    @Override
    public boolean isTransparent() {
        return isEmpty() ||
               type.contains("glass") ||
               type.contains("ice") ||
               type.contains("leaves");
    }

    @Override
    public boolean isPassable() {
        return isEmpty() ||
               type.contains("air") ||
               type.contains("sapling") ||
               type.contains("flower") ||
               type.contains("torch") ||
               type.contains("sign") ||
               type.contains("pressure_plate") ||
               type.contains("button");
    }

    @Override
    public boolean isBurnable() {
        return type.contains("wood") ||
               type.contains("planks") ||
               type.contains("log") ||
               type.contains("leaves") ||
               type.contains("wool") ||
               type.contains("carpet") ||
               "minecraft:coal_block".equals(type);
    }

    @Override
    public boolean isReplaceable() {
        return isEmpty() ||
               type.equals("minecraft:grass") ||
               type.equals("minecraft:tall_grass") ||
               type.equals("minecraft:snow") ||
               type.contains("water") ||
               type.contains("lava");
    }

    @Override
    public int getLightLevel() {
        return lightLevel;
    }

    /**
     * Sets the light level.
     *
     * @param level the light level (0-15)
     */
    public void setLightLevel(int level) {
        this.lightLevel = Math.max(0, Math.min(15, level));
    }

    @Override
    public int getLightFromSky() {
        return skyLightLevel;
    }

    /**
     * Sets the sky light level.
     *
     * @param level the sky light level (0-15)
     */
    public void setSkyLightLevel(int level) {
        this.skyLightLevel = Math.max(0, Math.min(15, level));
    }

    @Override
    public int getLightFromBlocks() {
        return blockLightLevel;
    }

    /**
     * Sets the block light level.
     *
     * @param level the block light level (0-15)
     */
    public void setBlockLightLevel(int level) {
        this.blockLightLevel = Math.max(0, Math.min(15, level));
    }

    @Override
    @NotNull
    public UnifiedBlock getRelative(@NotNull BlockFace face) {
        return getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    @NotNull
    public UnifiedBlock getRelative(int dx, int dy, int dz) {
        return world.getBlockAt(
            location.getBlockX() + dx,
            location.getBlockY() + dy,
            location.getBlockZ() + dz
        );
    }

    @Override
    @NotNull
    public Optional<String> getProperty(@NotNull String property) {
        return Optional.ofNullable(properties.get(property));
    }

    @Override
    public boolean setProperty(@NotNull String property, @NotNull String value) {
        properties.put(
            Objects.requireNonNull(property, "property cannot be null"),
            Objects.requireNonNull(value, "value cannot be null")
        );
        return true;
    }

    @Override
    @NotNull
    public Collection<String> getPropertyNames() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    public boolean hasProperty(@NotNull String property) {
        return properties.containsKey(property);
    }

    @Override
    @NotNull
    public UnifiedChunk getChunk() {
        return world.getChunkAt(location);
    }

    @Override
    public boolean breakNaturally() {
        if (isEmpty()) {
            return false;
        }
        // Would drop items in full implementation
        setType("minecraft:air");
        return true;
    }

    @Override
    public boolean breakNaturally(@Nullable UnifiedItemStack tool) {
        return breakNaturally();
    }

    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops() {
        // Simplified - returns empty drops for mock
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops(@NotNull UnifiedItemStack tool) {
        // Simplified - returns empty drops for mock
        return Collections.emptyList();
    }

    @Override
    public boolean isPowered() {
        return powered;
    }

    /**
     * Sets whether this block is powered.
     *
     * @param powered the powered state
     */
    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    @Override
    public boolean isIndirectlyPowered() {
        // Simplified - would check adjacent blocks in full implementation
        return powered;
    }

    @Override
    public int getBlockPower() {
        return powered ? 15 : 0;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockBlock mockBlock = (MockBlock) o;
        return Objects.equals(location, mockBlock.location) &&
               Objects.equals(world.getUniqueId(), mockBlock.world.getUniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, world.getUniqueId());
    }

    @Override
    public String toString() {
        return "MockBlock{" +
            "type='" + type + '\'' +
            ", location=" + location +
            '}';
    }
}
