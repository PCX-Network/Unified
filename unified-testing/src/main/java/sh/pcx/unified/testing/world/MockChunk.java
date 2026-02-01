/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.world;

import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Mock implementation of a Minecraft chunk for testing purposes.
 *
 * <p>MockChunk represents a 16x16 chunk of blocks in a MockWorld and
 * tracks loading state and contained entities.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockWorld
 * @see MockBlock
 */
public final class MockChunk implements UnifiedChunk {

    private final MockWorld world;
    private final int x;
    private final int z;
    private boolean loaded = true;
    private boolean forceLoaded = false;
    private boolean generated = true;
    private boolean entitiesLoaded = true;
    private long inhabitedTime = 0;

    /**
     * Creates a new mock chunk.
     *
     * @param world the containing world
     * @param x     the chunk x coordinate
     * @param z     the chunk z coordinate
     */
    public MockChunk(@NotNull MockWorld world, int x, int z) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.x = x;
        this.z = z;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Override
    @NotNull
    public UnifiedBlock getBlock(int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            throw new IllegalArgumentException("Chunk-relative x and z must be 0-15");
        }
        // Convert chunk-relative coordinates to world coordinates
        int worldX = this.x * 16 + x;
        int worldZ = this.z * 16 + z;
        return world.getBlockAt(worldX, y, worldZ);
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Sets the loaded state of this chunk.
     *
     * @param loaded the loaded state
     */
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> load() {
        this.loaded = true;
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public boolean load(boolean generate) {
        this.loaded = true;
        return true;
    }

    @Override
    public boolean unload() {
        if (!forceLoaded) {
            this.loaded = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean unload(boolean save) {
        return unload();
    }

    @Override
    public boolean isForceLoaded() {
        return forceLoaded;
    }

    @Override
    public void setForceLoaded(boolean forceLoaded) {
        this.forceLoaded = forceLoaded;
        if (forceLoaded) {
            this.loaded = true;
        }
    }

    @Override
    public boolean isGenerated() {
        return generated;
    }

    /**
     * Sets whether this chunk has been generated.
     *
     * @param generated the generated state
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    @Override
    public long getInhabitedTime() {
        return inhabitedTime;
    }

    @Override
    public void setInhabitedTime(long time) {
        this.inhabitedTime = time;
    }

    @Override
    public boolean areEntitiesLoaded() {
        return entitiesLoaded;
    }

    /**
     * Sets whether entities are loaded in this chunk.
     *
     * @param entitiesLoaded the entities loaded state
     */
    public void setEntitiesLoaded(boolean entitiesLoaded) {
        this.entitiesLoaded = entitiesLoaded;
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
        MockChunk mockChunk = (MockChunk) o;
        return x == mockChunk.x && z == mockChunk.z &&
               Objects.equals(world.getUniqueId(), mockChunk.world.getUniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(world.getUniqueId(), x, z);
    }

    @Override
    public String toString() {
        return "MockChunk{" +
            "world=" + world.getName() +
            ", x=" + x +
            ", z=" + z +
            ", loaded=" + loaded +
            '}';
    }
}
