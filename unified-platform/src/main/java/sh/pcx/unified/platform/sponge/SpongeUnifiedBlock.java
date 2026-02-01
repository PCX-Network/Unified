/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.state.State;
import org.spongepowered.api.state.StateProperty;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sponge implementation of the {@link UnifiedBlock} interface.
 *
 * <p>This class wraps a Sponge {@link ServerLocation} to provide block
 * operations through the unified API.
 *
 * <h2>Block State System</h2>
 * <p>Sponge uses a powerful block state system with typed properties.
 * This implementation handles conversion between string-based properties
 * and Sponge's typed {@link StateProperty} system.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are generally thread-safe. Write operations (setting type,
 * breaking blocks) should be performed on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedBlock
 */
public final class SpongeUnifiedBlock implements UnifiedBlock {

    private final ServerLocation location;
    private final SpongePlatformProvider provider;

    /**
     * Creates a new SpongeUnifiedBlock at the given location.
     *
     * @param location the Sponge ServerLocation
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeUnifiedBlock(@NotNull ServerLocation location, @NotNull SpongePlatformProvider provider) {
        this.location = location;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        return new UnifiedLocation(
                provider.getOrCreateWorld(location.world()),
                location.x(),
                location.y(),
                location.z()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return provider.getOrCreateWorld(location.world());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX() {
        return location.blockX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY() {
        return location.blockY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getZ() {
        return location.blockZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getType() {
        return location.block().type().key(org.spongepowered.api.registry.RegistryTypes.BLOCK_TYPE).asString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(@NotNull String type) {
        setType(type, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(@NotNull String type, boolean physics) {
        ResourceKey typeKey = ResourceKey.resolve(type);
        Optional<BlockType> blockType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.BLOCK_TYPE).findValue(typeKey);

        if (blockType.isEmpty()) {
            return;
        }

        BlockState newState = blockType.get().defaultState();
        if (physics) {
            location.setBlock(newState);
        } else {
            // In Sponge API 8+, use setBlockType for no-physics block placement
            location.setBlockType(blockType.get());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return location.block().type().equals(BlockTypes.AIR.get()) ||
               location.block().type().equals(BlockTypes.CAVE_AIR.get()) ||
               location.block().type().equals(BlockTypes.VOID_AIR.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLiquid() {
        return location.block().type().equals(BlockTypes.WATER.get()) ||
               location.block().type().equals(BlockTypes.LAVA.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSolid() {
        return location.block().get(Keys.IS_SOLID).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransparent() {
        // Check if the block allows light through
        return !location.block().get(Keys.IS_SOLID).orElse(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return location.block().get(Keys.IS_PASSABLE).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBurnable() {
        return location.block().get(Keys.IS_FLAMMABLE).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReplaceable() {
        return location.block().get(Keys.IS_REPLACEABLE).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLightLevel() {
        return location.world().light(location.blockPosition());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLightFromSky() {
        // Sponge combines these, estimate based on position
        return location.world().light(location.blockPosition());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLightFromBlocks() {
        return location.block().get(Keys.LIGHT_EMISSION).orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getRelative(@NotNull BlockFace face) {
        return getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getRelative(int dx, int dy, int dz) {
        ServerLocation relative = location.add(dx, dy, dz);
        return new SpongeUnifiedBlock(relative, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<String> getProperty(@NotNull String property) {
        BlockState state = location.block();

        for (StateProperty stateProperty : state.stateProperties()) {
            if (stateProperty.name().equalsIgnoreCase(property)) {
                Optional value = state.stateProperty(stateProperty);
                return value.map(Object::toString);
            }
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean setProperty(@NotNull String property, @NotNull String value) {
        BlockState state = location.block();

        for (StateProperty stateProperty : state.stateProperties()) {
            if (stateProperty.name().equalsIgnoreCase(property)) {
                // Try to parse the value for this property
                Optional parsedValue = stateProperty.parseValue(value);
                if (parsedValue.isPresent()) {
                    Optional<BlockState> newState = state.withStateProperty(stateProperty, (Comparable) parsedValue.get());
                    if (newState.isPresent()) {
                        location.setBlock(newState.get());
                        return true;
                    }
                }
                return false;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("rawtypes")
    public Collection<String> getPropertyNames() {
        return location.block().stateProperties().stream()
                .map(StateProperty::name)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean hasProperty(@NotNull String property) {
        for (StateProperty stateProperty : location.block().stateProperties()) {
            if (stateProperty.name().equalsIgnoreCase(property)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedChunk getChunk() {
        int chunkX = location.blockX() >> 4;
        int chunkZ = location.blockZ() >> 4;
        return new SpongeUnifiedChunk(location.world(), chunkX, chunkZ, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean breakNaturally() {
        return breakNaturally(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean breakNaturally(@Nullable UnifiedItemStack tool) {
        if (isEmpty()) {
            return false;
        }

        // Get drops first
        Collection<UnifiedItemStack> drops = getDrops(tool != null ? tool : UnifiedItemStack.empty());

        // Set to air
        location.setBlock(BlockTypes.AIR.get().defaultState());

        // Spawn drops
        for (UnifiedItemStack drop : drops) {
            if (!drop.isEmpty()) {
                org.spongepowered.api.entity.Item itemEntity = location.world().createEntity(
                        org.spongepowered.api.entity.EntityTypes.ITEM.get(),
                        location.position()
                );
                itemEntity.offer(Keys.ITEM_STACK_SNAPSHOT, ((ItemStack) drop.getHandle()).createSnapshot());
                location.world().spawnEntity(itemEntity);
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops() {
        return getDrops(UnifiedItemStack.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops(@NotNull UnifiedItemStack tool) {
        // Sponge doesn't have a direct API for this
        // Return an empty collection as a fallback
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowered() {
        return location.block().get(Keys.IS_POWERED).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIndirectlyPowered() {
        return location.block().get(Keys.IS_INDIRECTLY_POWERED).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBlockPower() {
        return location.block().get(Keys.POWER).orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) location;
    }

    /**
     * Returns the Sponge ServerLocation.
     *
     * @return the ServerLocation
     */
    @NotNull
    public ServerLocation getServerLocation() {
        return location;
    }

    /**
     * Returns the block state at this location.
     *
     * @return the BlockState
     */
    @NotNull
    public BlockState getBlockState() {
        return location.block();
    }

    /**
     * Returns a string representation of this block.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeUnifiedBlock{" +
                "type=" + getType() +
                ", x=" + getX() +
                ", y=" + getY() +
                ", z=" + getZ() +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpongeUnifiedBlock other)) return false;
        return location.blockPosition().equals(other.location.blockPosition()) &&
               location.world().uniqueId().equals(other.location.world().uniqueId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = location.blockPosition().hashCode();
        result = 31 * result + location.world().uniqueId().hashCode();
        return result;
    }
}
