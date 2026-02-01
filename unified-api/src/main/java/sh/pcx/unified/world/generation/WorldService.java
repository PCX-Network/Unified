/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for world generation and management.
 *
 * <p>The WorldService provides centralized access to all world-related functionality
 * including world creation, loading, unloading, deletion, and custom generation.
 * It serves as the primary entry point for programmatic world management.
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Programmatic world creation with fluent API</li>
 *   <li>Custom chunk generators</li>
 *   <li>World templates for quick setup</li>
 *   <li>Async world loading/unloading</li>
 *   <li>World groups for shared data</li>
 *   <li>Biome and structure customization</li>
 *   <li>World cloning and importing</li>
 *   <li>Scheduled unloading for temporary worlds</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Inject
 * private WorldService worlds;
 *
 * // Create a new world with vanilla generation
 * World survival = worlds.create("survival")
 *     .environment(World.Environment.NORMAL)
 *     .type(WorldType.NORMAL)
 *     .seed(12345L)
 *     .generateStructures(true)
 *     .hardcore(false)
 *     .create();
 *
 * // Create void world
 * World lobby = worlds.create("lobby")
 *     .environment(World.Environment.NORMAL)
 *     .generator(VoidGenerator.class)
 *     .spawnLocation(new Location(null, 0, 64, 0))
 *     .create();
 *
 * // Create world from template
 * World arena = worlds.createFromTemplate("arena", WorldTemplates.VOID_FLAT);
 *
 * // Import existing world folder
 * World imported = worlds.importWorld("backup_world")
 *     .copyToName("restored_world")
 *     .doImport();
 *
 * // Clone world
 * World clone = worlds.clone("survival", "survival_backup");
 *
 * // Unload world safely
 * worlds.unload("my_world")
 *     .save(true)
 *     .teleportPlayersTo(lobbySpawn)
 *     .onComplete(() -> log.info("World unloaded"))
 *     .unload();
 *
 * // Delete world
 * worlds.delete("temp_world")
 *     .requireConfirmation(true)
 *     .delete();
 *
 * // Scheduled unload (e.g., for minigame arenas)
 * worlds.scheduleUnload("arena-1", Duration.ofMinutes(5));
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldCreator
 * @see WorldTemplate
 * @see WorldUnloader
 * @see BiomeService
 * @see StructureService
 */
public interface WorldService extends Service {

    // ==================== World Creation ====================

    /**
     * Creates a new WorldCreator for building a world.
     *
     * @param name the world name
     * @return a new WorldCreator instance
     * @since 1.0.0
     */
    @NotNull
    WorldCreator create(@NotNull String name);

    /**
     * Creates a world from a pre-defined template.
     *
     * @param name     the world name
     * @param template the world template
     * @return a future that completes with the created world
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedWorld> createFromTemplate(@NotNull String name, @NotNull WorldTemplate template);

    /**
     * Creates a world importer for importing an existing world folder.
     *
     * @param sourceWorldName the name of the world folder to import
     * @return a new WorldImporter instance
     * @since 1.0.0
     */
    @NotNull
    WorldImporter importWorld(@NotNull String sourceWorldName);

    /**
     * Clones an existing world.
     *
     * @param sourceName the source world name
     * @param targetName the target world name
     * @return a future that completes with the cloned world
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedWorld> clone(@NotNull String sourceName, @NotNull String targetName);

    // ==================== World Access ====================

    /**
     * Gets a loaded world by name.
     *
     * @param name the world name
     * @return an Optional containing the world if loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull String name);

    /**
     * Gets a loaded world by UUID.
     *
     * @param uuid the world UUID
     * @return an Optional containing the world if loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull UUID uuid);

    /**
     * Gets all currently loaded worlds.
     *
     * @return an unmodifiable collection of loaded worlds
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedWorld> getWorlds();

    /**
     * Gets the names of all available worlds (loaded or not).
     *
     * @return a collection of world names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getAvailableWorldNames();

    /**
     * Checks if a world with the given name exists (loaded or as files).
     *
     * @param name the world name
     * @return true if the world exists
     * @since 1.0.0
     */
    boolean worldExists(@NotNull String name);

    /**
     * Checks if a world is currently loaded.
     *
     * @param name the world name
     * @return true if the world is loaded
     * @since 1.0.0
     */
    boolean isWorldLoaded(@NotNull String name);

    /**
     * Gets the default world.
     *
     * @return the default world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld getDefaultWorld();

    // ==================== World Loading ====================

    /**
     * Loads a world by name.
     *
     * @param name the world name
     * @return a future that completes with the loaded world
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedWorld> load(@NotNull String name);

    /**
     * Loads a world with custom configuration.
     *
     * @param name   the world name
     * @param loader the world loader configuration
     * @return a future that completes with the loaded world
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedWorld> load(@NotNull String name, @NotNull WorldLoader loader);

    // ==================== World Unloading ====================

    /**
     * Creates a world unloader for safely unloading a world.
     *
     * @param name the world name
     * @return a new WorldUnloader instance
     * @since 1.0.0
     */
    @NotNull
    WorldUnloader unload(@NotNull String name);

    /**
     * Unloads a world immediately.
     *
     * @param world the world to unload
     * @param save  whether to save the world before unloading
     * @return a future that completes with true if successful
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> unloadWorld(@NotNull UnifiedWorld world, boolean save);

    /**
     * Schedules a world to be unloaded after a delay.
     *
     * @param name  the world name
     * @param delay the delay before unloading
     * @since 1.0.0
     */
    void scheduleUnload(@NotNull String name, @NotNull Duration delay);

    /**
     * Cancels a scheduled unload.
     *
     * @param name the world name
     * @return true if a scheduled unload was cancelled
     * @since 1.0.0
     */
    boolean cancelScheduledUnload(@NotNull String name);

    // ==================== World Deletion ====================

    /**
     * Creates a world deleter for safely deleting a world.
     *
     * @param name the world name
     * @return a new WorldDeleter instance
     * @since 1.0.0
     */
    @NotNull
    WorldDeleter delete(@NotNull String name);

    // ==================== Generators ====================

    /**
     * Registers a custom chunk generator.
     *
     * @param id        the generator identifier
     * @param generator the chunk generator
     * @since 1.0.0
     */
    void registerGenerator(@NotNull String id, @NotNull ChunkGenerator generator);

    /**
     * Registers a chunk generator class for lazy instantiation.
     *
     * @param id             the generator identifier
     * @param generatorClass the chunk generator class
     * @since 1.0.0
     */
    void registerGenerator(@NotNull String id, @NotNull Class<? extends ChunkGenerator> generatorClass);

    /**
     * Gets a registered chunk generator by ID.
     *
     * @param id the generator identifier
     * @return the chunk generator, or null if not registered
     * @since 1.0.0
     */
    @Nullable
    ChunkGenerator getGenerator(@NotNull String id);

    /**
     * Gets all registered generator IDs.
     *
     * @return a collection of generator identifiers
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getRegisteredGenerators();

    // ==================== Templates ====================

    /**
     * Registers a world template.
     *
     * @param template the world template
     * @since 1.0.0
     */
    void registerTemplate(@NotNull WorldTemplate template);

    /**
     * Gets a registered template by ID.
     *
     * @param id the template identifier
     * @return an Optional containing the template if registered
     * @since 1.0.0
     */
    @NotNull
    Optional<WorldTemplate> getTemplate(@NotNull String id);

    /**
     * Gets all registered templates.
     *
     * @return an unmodifiable collection of templates
     * @since 1.0.0
     */
    @NotNull
    Collection<WorldTemplate> getTemplates();

    // ==================== Sub-Services ====================

    /**
     * Gets the biome service for custom biome management.
     *
     * @return the biome service
     * @since 1.0.0
     */
    @NotNull
    BiomeService getBiomeService();

    /**
     * Gets the structure service for custom structure management.
     *
     * @return the structure service
     * @since 1.0.0
     */
    @NotNull
    StructureService getStructureService();

    /**
     * Gets the world group manager.
     *
     * @return the group manager
     * @since 1.0.0
     */
    @NotNull
    WorldGroupManager getGroupManager();

    // ==================== Utilities ====================

    /**
     * Gets the worlds directory path.
     *
     * @return the path to the worlds directory
     * @since 1.0.0
     */
    @NotNull
    Path getWorldsDirectory();

    /**
     * Saves all loaded worlds.
     *
     * @return a future that completes when all worlds are saved
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAllWorlds();

    /**
     * Teleports a player to a world's spawn location.
     *
     * @param player the player UUID
     * @param world  the target world
     * @return a future that completes when the teleport is complete
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> teleportToWorld(@NotNull UUID player, @NotNull UnifiedWorld world);

    // ==================== Lifecycle ====================

    /**
     * Checks if the world service is enabled.
     *
     * @return true if the service is enabled
     * @since 1.0.0
     */
    boolean isEnabled();

    /**
     * Shuts down the world service, saving and unloading all worlds.
     *
     * @return a future that completes when shutdown is complete
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> shutdown();
}
