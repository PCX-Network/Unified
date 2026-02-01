/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Sponge implementation of the {@link UnifiedChunk} interface.
 *
 * <p>This class provides chunk operations for Sponge servers. Unlike some other
 * implementations, Sponge's chunk API works with async operations by default.
 *
 * <h2>Chunk Coordinates</h2>
 * <p>Chunk coordinates are calculated by dividing block coordinates by 16.
 * Each chunk is a 16x16 column of blocks from the world's minimum to maximum height.
 *
 * <h2>Thread Safety</h2>
 * <p>Chunk loading operations can be performed asynchronously. Block access
 * within chunks should be done on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedChunk
 */
public final class SpongeUnifiedChunk implements UnifiedChunk {

    private final ServerWorld world;
    private final int chunkX;
    private final int chunkZ;
    private final SpongePlatformProvider provider;

    /**
     * Creates a new SpongeUnifiedChunk for the given coordinates.
     *
     * @param world    the Sponge ServerWorld
     * @param chunkX   the chunk X coordinate
     * @param chunkZ   the chunk Z coordinate
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeUnifiedChunk(@NotNull ServerWorld world, int chunkX, int chunkZ,
                              @NotNull SpongePlatformProvider provider) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX() {
        return chunkX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getZ() {
        return chunkZ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return provider.getOrCreateWorld(world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getBlock(int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            throw new IllegalArgumentException("Chunk-relative coordinates must be 0-15, got: x=" + x + ", z=" + z);
        }

        int worldX = (chunkX << 4) + x;
        int worldZ = (chunkZ << 4) + z;

        ServerLocation location = world.location(worldX, y, worldZ);
        return new SpongeUnifiedBlock(location, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoaded() {
        return world.isChunkLoaded(chunkX, 0, chunkZ, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> load() {
        return CompletableFuture.supplyAsync(() -> {
            Optional<WorldChunk> chunk = world.loadChunk(chunkX, 0, chunkZ, true);
            return chunk.isPresent();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean load(boolean generate) {
        Optional<WorldChunk> chunk = world.loadChunk(chunkX, 0, chunkZ, generate);
        return chunk.isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unload(boolean save) {
        // Sponge API 8+ uses WorldChunk for unloading
        Optional<WorldChunk> chunkOpt = world.loadChunk(chunkX, 0, chunkZ, false);
        if (chunkOpt.isPresent()) {
            return world.unloadChunk(chunkOpt.get());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unload() {
        return unload(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isForceLoaded() {
        // TODO: Force-loaded chunks API may differ in Sponge API 8+
        // For now, assume chunk is not force-loaded if we can't determine
        if (!isLoaded()) {
            return false;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setForceLoaded(boolean forceLoaded) {
        if (forceLoaded) {
            // Ensure chunk is loaded first
            load(true);
        }
        // TODO: Force-loaded chunks API may differ in Sponge API 8+
        // This is a no-op for now
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGenerated() {
        // If we can load without generating, we can check if it exists
        Optional<WorldChunk> chunk = world.loadChunk(chunkX, 0, chunkZ, false);
        return chunk.isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInhabitedTime() {
        // TODO: INHABITED_TIME key may not be available in Sponge API 8+
        // Return 0 as default
        return 0L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInhabitedTime(long time) {
        // TODO: INHABITED_TIME key may not be available in Sponge API 8+
        // This is a no-op for now
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areEntitiesLoaded() {
        // In Sponge, entities are loaded with the chunk
        return isLoaded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        // Return the WorldChunk if loaded, otherwise return the position info
        Optional<WorldChunk> chunk = world.loadChunk(chunkX, 0, chunkZ, false);
        if (chunk.isPresent()) {
            return (T) chunk.get();
        }
        // Return a vector representing the chunk position as fallback
        return (T) Vector3i.from(chunkX, 0, chunkZ);
    }

    /**
     * Returns the Sponge WorldChunk if loaded.
     *
     * @return an Optional containing the WorldChunk if loaded
     */
    @NotNull
    public Optional<WorldChunk> getChunk() {
        if (!isLoaded()) {
            return Optional.empty();
        }
        return world.loadChunk(chunkX, 0, chunkZ, false);
    }

    /**
     * Returns a string representation of this chunk.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeUnifiedChunk{" +
                "world=" + world.key().asString() +
                ", x=" + chunkX +
                ", z=" + chunkZ +
                ", loaded=" + isLoaded() +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpongeUnifiedChunk other)) return false;
        return chunkX == other.chunkX &&
               chunkZ == other.chunkZ &&
               world.uniqueId().equals(other.world.uniqueId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = chunkX;
        result = 31 * result + chunkZ;
        result = 31 * result + world.uniqueId().hashCode();
        return result;
    }
}
