/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for region persistence storage.
 *
 * <p>Implementations handle loading and saving regions to various backends
 * such as files (YAML, JSON) or databases.
 *
 * <h2>Built-in Implementations</h2>
 * <ul>
 *   <li>File-based storage (YAML or JSON per world)</li>
 *   <li>Database storage (SQL via unified-data)</li>
 * </ul>
 *
 * <h2>Custom Implementation</h2>
 * <pre>{@code
 * public class MyRegionStorage implements RegionStorage {
 *
 *     @Override
 *     public CompletableFuture<Collection<Region>> loadAll(UnifiedWorld world) {
 *         return CompletableFuture.supplyAsync(() -> {
 *             // Load regions from your storage
 *             return loadedRegions;
 *         });
 *     }
 *
 *     @Override
 *     public CompletableFuture<Void> save(Region region) {
 *         return CompletableFuture.runAsync(() -> {
 *             // Save region to your storage
 *         });
 *     }
 *
 *     // ... other methods
 * }
 *
 * // Register the custom storage
 * regionService.setStorage(new MyRegionStorage());
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RegionService
 */
public interface RegionStorage {

    /**
     * Loads all regions for a world.
     *
     * @param world the world
     * @return a future containing the loaded regions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Region>> loadAll(@NotNull UnifiedWorld world);

    /**
     * Loads all regions across all worlds.
     *
     * @return a future containing all loaded regions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Collection<Region>> loadAll();

    /**
     * Loads a specific region by ID.
     *
     * @param id the region UUID
     * @return a future containing the region if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Region>> load(@NotNull UUID id);

    /**
     * Loads a specific region by name and world.
     *
     * @param world the world
     * @param name  the region name
     * @return a future containing the region if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Region>> load(@NotNull UnifiedWorld world, @NotNull String name);

    /**
     * Saves a region.
     *
     * @param region the region to save
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull Region region);

    /**
     * Saves multiple regions.
     *
     * @param regions the regions to save
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAll(@NotNull Collection<Region> regions);

    /**
     * Saves all regions for a world.
     *
     * @param world   the world
     * @param regions the regions to save
     * @return a future that completes when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveWorld(@NotNull UnifiedWorld world, @NotNull Collection<Region> regions);

    /**
     * Deletes a region from storage.
     *
     * @param region the region to delete
     * @return a future that completes with true if deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull Region region);

    /**
     * Deletes a region by ID.
     *
     * @param id the region UUID
     * @return a future that completes with true if deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> delete(@NotNull UUID id);

    /**
     * Deletes all regions for a world.
     *
     * @param world the world
     * @return a future that completes with the number deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> deleteAll(@NotNull UnifiedWorld world);

    /**
     * Checks if a region exists in storage.
     *
     * @param id the region UUID
     * @return a future that completes with true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull UUID id);

    /**
     * Checks if a region exists by name and world.
     *
     * @param world the world
     * @param name  the region name
     * @return a future that completes with true if exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> exists(@NotNull UnifiedWorld world, @NotNull String name);

    /**
     * Gets the count of stored regions for a world.
     *
     * @param world the world
     * @return a future containing the count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> count(@NotNull UnifiedWorld world);

    /**
     * Gets the total count of stored regions.
     *
     * @return a future containing the total count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> count();

    /**
     * Initializes the storage backend.
     *
     * <p>Called when the storage is first set up.
     *
     * @return a future that completes when initialization is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> initialize();

    /**
     * Closes the storage backend.
     *
     * <p>Called when the service is shutting down.
     *
     * @return a future that completes when closed
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> close();

    /**
     * Gets the name of this storage provider.
     *
     * @return the storage name
     * @since 1.0.0
     */
    @NotNull
    String getName();
}
