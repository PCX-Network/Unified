/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Paper/Spigot implementation of {@link UnifiedChunk}.
 *
 * <p>This class wraps a Bukkit {@link Chunk} and provides a unified API for
 * chunk operations including loading, unloading, and block access.
 *
 * <h2>Chunk Coordinates</h2>
 * <p>Chunk coordinates are world block coordinates divided by 16 (shifted right by 4).
 * A chunk covers a 16x16 area of blocks in the X and Z directions.
 *
 * <h2>Thread Safety</h2>
 * <p>Chunk loading and unloading operations should be performed on the main server
 * thread. On Paper, async chunk loading methods are available. On Folia, use
 * region-aware scheduling.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedChunk
 * @see Chunk
 */
public final class PaperUnifiedChunk implements UnifiedChunk {

    private final Chunk chunk;
    private final PaperPlatformProvider provider;

    /**
     * Creates a new PaperUnifiedChunk wrapping the given Bukkit chunk.
     *
     * @param chunk    the Bukkit chunk to wrap
     * @param provider the platform provider for creating related wrappers
     * @since 1.0.0
     */
    public PaperUnifiedChunk(@NotNull Chunk chunk, @NotNull PaperPlatformProvider provider) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /**
     * Returns the underlying Bukkit chunk.
     *
     * @return the Bukkit chunk
     */
    @NotNull
    public Chunk getBukkitChunk() {
        return chunk;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX() {
        return chunk.getX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getZ() {
        return chunk.getZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return provider.getOrCreateWorld(chunk.getWorld());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getBlock(int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            throw new IllegalArgumentException(
                    "Chunk-relative coordinates must be 0-15, got x=" + x + ", z=" + z
            );
        }
        return new PaperUnifiedBlock(chunk.getBlock(x, y, z), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoaded() {
        return chunk.isLoaded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> load() {
        try {
            // Paper supports async chunk loading
            return chunk.getWorld().getChunkAtAsync(chunk.getX(), chunk.getZ())
                    .thenApply(c -> true);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(load(true));
            return future;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean load(boolean generate) {
        return chunk.load(generate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unload(boolean save) {
        return chunk.unload(save);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unload() {
        return chunk.unload();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isForceLoaded() {
        return chunk.isForceLoaded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setForceLoaded(boolean forceLoaded) {
        chunk.setForceLoaded(forceLoaded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGenerated() {
        try {
            return chunk.getWorld().isChunkGenerated(chunk.getX(), chunk.getZ());
        } catch (NoSuchMethodError e) {
            // Fallback - if it's loaded, assume it's generated
            return chunk.isLoaded();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInhabitedTime() {
        return chunk.getInhabitedTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInhabitedTime(long time) {
        chunk.setInhabitedTime(time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean areEntitiesLoaded() {
        try {
            // Paper provides this method
            return chunk.isEntitiesLoaded();
        } catch (NoSuchMethodError e) {
            // Fallback - assume entities are loaded if chunk is loaded
            return chunk.isLoaded();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) chunk;
    }

    /**
     * Checks equality based on chunk coordinates and world.
     *
     * @param o the object to compare
     * @return true if the other object represents the same chunk
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperUnifiedChunk that)) return false;
        return chunk.getX() == that.chunk.getX() &&
               chunk.getZ() == that.chunk.getZ() &&
               chunk.getWorld().equals(that.chunk.getWorld());
    }

    /**
     * Returns a hash code based on chunk coordinates and world.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    /**
     * Returns a string representation of this chunk.
     *
     * @return a string containing the chunk's coordinates and world
     */
    @Override
    public String toString() {
        return "PaperUnifiedChunk{" +
                "x=" + chunk.getX() +
                ", z=" + chunk.getZ() +
                ", world=" + chunk.getWorld().getName() +
                ", loaded=" + chunk.isLoaded() +
                '}';
    }
}
