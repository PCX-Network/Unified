/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Fluent builder for importing existing world folders.
 *
 * <p>WorldImporter provides options for importing world folders from external
 * locations, optionally copying them and renaming them in the process.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Import and rename a world
 * World imported = worlds.importWorld("backup_world")
 *     .copyToName("restored_world")
 *     .doImport();
 *
 * // Import from external path
 * worlds.importWorld("external_world")
 *     .fromPath(Paths.get("/backups/world_backup"))
 *     .copyToName("imported_world")
 *     .preserveUUID(false)
 *     .doImportAsync()
 *     .thenAccept(world -> {
 *         log.info("Imported: " + world.getName());
 *     });
 *
 * // Import in place (no copy)
 * worlds.importWorld("existing_world")
 *     .loadAfterImport(true)
 *     .doImport();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 */
public interface WorldImporter {

    /**
     * Sets an external source path for the world.
     *
     * <p>If not set, the world is expected to be in the server's world directory.
     *
     * @param path the source path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter fromPath(@NotNull Path path);

    /**
     * Sets a new name for the imported world.
     *
     * <p>When set, the world folder will be copied to a new folder with
     * this name. If not set, the world is imported in place.
     *
     * @param name the new world name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter copyToName(@NotNull String name);

    /**
     * Sets whether to preserve the original world UUID.
     *
     * <p>If false, a new UUID will be generated for the imported world.
     * Default is true.
     *
     * @param preserve true to preserve the UUID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter preserveUUID(boolean preserve);

    /**
     * Sets whether to load the world after importing.
     *
     * <p>Default is true.
     *
     * @param load true to load after import
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter loadAfterImport(boolean load);

    /**
     * Sets a custom generator for the imported world.
     *
     * @param generatorId the generator ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter generator(@NotNull String generatorId);

    /**
     * Sets a callback to run on import progress.
     *
     * @param callback the progress callback (percentage 0-100)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter onProgress(@NotNull Consumer<Integer> callback);

    /**
     * Sets a callback to run when import is complete.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter onComplete(@NotNull Consumer<UnifiedWorld> callback);

    /**
     * Sets a callback to run if import fails.
     *
     * @param callback the failure callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldImporter onFailure(@NotNull Consumer<Throwable> callback);

    /**
     * Imports the world synchronously.
     *
     * @return the imported world, or null if import failed
     * @throws WorldCreationException if import fails
     * @since 1.0.0
     */
    @Nullable
    UnifiedWorld doImport();

    /**
     * Imports the world asynchronously.
     *
     * @return a future that completes with the imported world
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedWorld> doImportAsync();

    /**
     * Validates the source world without importing.
     *
     * @return true if the source is a valid world
     * @since 1.0.0
     */
    boolean validate();

    /**
     * Gets the estimated size of the world in bytes.
     *
     * @return the estimated size, or -1 if unknown
     * @since 1.0.0
     */
    long getEstimatedSize();

    /**
     * Gets the source world name.
     *
     * @return the source world name
     * @since 1.0.0
     */
    @NotNull
    String getSourceName();

    /**
     * Gets the target world name.
     *
     * @return the target name, or the source name if not copying
     * @since 1.0.0
     */
    @NotNull
    String getTargetName();
}
