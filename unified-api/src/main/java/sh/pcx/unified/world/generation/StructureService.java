/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for registering and managing custom structures in world generation.
 *
 * <p>StructureService allows plugins to register custom structures from schematics
 * or procedurally generated templates. Structures can be placed during world
 * generation or manually at specific locations.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Inject
 * private StructureService structures;
 *
 * // Register structure from schematic
 * structures.register("myplugin:abandoned_tower")
 *     .schematic(getResource("schematics/tower.schem"))
 *     .biomes(Biome.PLAINS, Biome.FOREST, Biome.DARK_FOREST)
 *     .spacing(32)           // Minimum chunks between structures
 *     .separation(8)         // Additional separation
 *     .salt(123456)          // Seed modifier
 *     .terrainAdaptation(TerrainAdaptation.BEARD_THIN)
 *     .processors(processors -> {
 *         processors.add(new MossyStoneProcessor(0.3f));
 *         processors.add(new AirGapProcessor());
 *     })
 *     .onPlace((world, location, rotation) -> {
 *         world.spawnEntity(location.add(5, 1, 5), EntityType.WITCH);
 *     })
 *     .register();
 *
 * // Register jigsaw structure (like villages)
 * structures.registerJigsaw("myplugin:ruins")
 *     .startPool("myplugin:ruins/center")
 *     .maxDepth(5)
 *     .register();
 *
 * // Place structure manually
 * structures.place("myplugin:abandoned_tower", location, Rotation.NONE);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Structure
 * @see WorldService
 */
public interface StructureService extends Service {

    /**
     * Checks if custom structures are supported on this platform.
     *
     * @return true if custom structures are supported
     * @since 1.0.0
     */
    boolean isSupported();

    // ==================== Structure Registration ====================

    /**
     * Creates a builder for registering a structure from a schematic.
     *
     * @param key the structure key (e.g., "myplugin:abandoned_tower")
     * @return a new Structure.Builder
     * @since 1.0.0
     */
    @NotNull
    Structure.Builder register(@NotNull String key);

    /**
     * Creates a builder for registering a jigsaw structure.
     *
     * <p>Jigsaw structures are modular structures like villages that are
     * assembled from multiple pieces using jigsaw connections.
     *
     * @param key the structure key
     * @return a new JigsawStructure.Builder
     * @since 1.0.0
     */
    @NotNull
    JigsawStructure.Builder registerJigsaw(@NotNull String key);

    /**
     * Registers a structure piece class for jigsaw structures.
     *
     * @param key        the piece key
     * @param pieceClass the piece implementation class
     * @since 1.0.0
     */
    void registerPiece(@NotNull String key, @NotNull Class<? extends StructurePiece> pieceClass);

    /**
     * Gets a registered structure by key.
     *
     * @param key the structure key
     * @return the structure, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<Structure> getStructure(@NotNull String key);

    /**
     * Gets all registered structures.
     *
     * @return an unmodifiable collection of structures
     * @since 1.0.0
     */
    @NotNull
    Collection<Structure> getStructures();

    /**
     * Unregisters a structure.
     *
     * @param key the structure key
     * @return true if a structure was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String key);

    // ==================== Structure Placement ====================

    /**
     * Places a structure at the specified location.
     *
     * @param key      the structure key
     * @param location the center location
     * @param rotation the rotation to apply
     * @return true if the structure was placed successfully
     * @since 1.0.0
     */
    boolean place(@NotNull String key, @NotNull UnifiedLocation location, @NotNull Rotation rotation);

    /**
     * Places a structure at the specified location with options.
     *
     * @param key      the structure key
     * @param location the center location
     * @param options  placement options
     * @return true if the structure was placed successfully
     * @since 1.0.0
     */
    boolean place(@NotNull String key, @NotNull UnifiedLocation location, @NotNull PlacementOptions options);

    /**
     * Locates the nearest instance of a structure.
     *
     * @param key       the structure key
     * @param origin    the search origin
     * @param radius    the search radius in chunks
     * @param findUnexplored whether to include unexplored structures
     * @return the structure location, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedLocation> locate(@NotNull String key, @NotNull UnifiedLocation origin,
                                     int radius, boolean findUnexplored);

    // ==================== Schematic Loading ====================

    /**
     * Loads a schematic from a file path.
     *
     * @param path the path to the schematic file
     * @return the loaded schematic
     * @throws SchematicException if loading fails
     * @since 1.0.0
     */
    @NotNull
    Schematic loadSchematic(@NotNull Path path);

    /**
     * Loads a schematic from an input stream.
     *
     * @param stream the input stream
     * @param format the schematic format
     * @return the loaded schematic
     * @throws SchematicException if loading fails
     * @since 1.0.0
     */
    @NotNull
    Schematic loadSchematic(@NotNull InputStream stream, @NotNull SchematicFormat format);

    // ==================== Nested Types ====================

    /**
     * Structure rotation values.
     *
     * @since 1.0.0
     */
    enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        COUNTERCLOCKWISE_90;

        /**
         * Gets a random rotation.
         *
         * @param random the random source
         * @return a random rotation
         * @since 1.0.0
         */
        @NotNull
        public static Rotation random(@NotNull java.util.Random random) {
            return values()[random.nextInt(values().length)];
        }
    }

    /**
     * Structure mirror values.
     *
     * @since 1.0.0
     */
    enum Mirror {
        NONE,
        LEFT_RIGHT,
        FRONT_BACK
    }

    /**
     * How the structure adapts to terrain.
     *
     * @since 1.0.0
     */
    enum TerrainAdaptation {
        /** No terrain adaptation. */
        NONE,
        /** Fills gaps below the structure. */
        BEARD_THIN,
        /** Fills larger gaps below the structure. */
        BEARD_BOX,
        /** Buries the structure partially. */
        BURY,
        /** Encases the structure in terrain. */
        ENCASE_FROZEN
    }

    /**
     * Supported schematic formats.
     *
     * @since 1.0.0
     */
    enum SchematicFormat {
        /** WorldEdit .schem format */
        SPONGE,
        /** Legacy WorldEdit .schematic format */
        LEGACY,
        /** Minecraft structure .nbt format */
        NBT
    }

    /**
     * Options for structure placement.
     *
     * @since 1.0.0
     */
    interface PlacementOptions {

        /**
         * Gets the rotation.
         *
         * @return the rotation
         * @since 1.0.0
         */
        @NotNull
        Rotation getRotation();

        /**
         * Gets the mirror mode.
         *
         * @return the mirror mode
         * @since 1.0.0
         */
        @NotNull
        Mirror getMirror();

        /**
         * Checks if air blocks should be included.
         *
         * @return true to include air
         * @since 1.0.0
         */
        boolean shouldIncludeAir();

        /**
         * Checks if entities should be included.
         *
         * @return true to include entities
         * @since 1.0.0
         */
        boolean shouldIncludeEntities();

        /**
         * Gets the integrity (block placement probability).
         *
         * @return the integrity (0.0-1.0)
         * @since 1.0.0
         */
        float getIntegrity();

        /**
         * Gets the integrity seed.
         *
         * @return the seed for integrity randomization
         * @since 1.0.0
         */
        long getIntegritySeed();

        /**
         * Creates a new options builder.
         *
         * @return a new builder
         * @since 1.0.0
         */
        @NotNull
        static Builder builder() {
            return new PlacementOptionsImpl.Builder();
        }

        /**
         * Builder for PlacementOptions.
         *
         * @since 1.0.0
         */
        interface Builder {
            @NotNull Builder rotation(@NotNull Rotation rotation);
            @NotNull Builder mirror(@NotNull Mirror mirror);
            @NotNull Builder includeAir(boolean include);
            @NotNull Builder includeEntities(boolean include);
            @NotNull Builder integrity(float integrity);
            @NotNull Builder integritySeed(long seed);
            @NotNull PlacementOptions build();
        }
    }
}

/**
 * Default implementation of PlacementOptions.
 */
record PlacementOptionsImpl(
        StructureService.Rotation rotation,
        StructureService.Mirror mirror,
        boolean includeAir,
        boolean includeEntities,
        float integrity,
        long integritySeed
) implements StructureService.PlacementOptions {

    @Override
    @NotNull
    public StructureService.Rotation getRotation() {
        return rotation;
    }

    @Override
    @NotNull
    public StructureService.Mirror getMirror() {
        return mirror;
    }

    @Override
    public boolean shouldIncludeAir() {
        return includeAir;
    }

    @Override
    public boolean shouldIncludeEntities() {
        return includeEntities;
    }

    @Override
    public float getIntegrity() {
        return integrity;
    }

    @Override
    public long getIntegritySeed() {
        return integritySeed;
    }

    static class Builder implements StructureService.PlacementOptions.Builder {
        private StructureService.Rotation rotation = StructureService.Rotation.NONE;
        private StructureService.Mirror mirror = StructureService.Mirror.NONE;
        private boolean includeAir = false;
        private boolean includeEntities = true;
        private float integrity = 1.0f;
        private long integritySeed = 0L;

        @Override
        @NotNull
        public StructureService.PlacementOptions.Builder rotation(@NotNull StructureService.Rotation rotation) {
            this.rotation = rotation;
            return this;
        }

        @Override
        @NotNull
        public StructureService.PlacementOptions.Builder mirror(@NotNull StructureService.Mirror mirror) {
            this.mirror = mirror;
            return this;
        }

        @Override
        @NotNull
        public StructureService.PlacementOptions.Builder includeAir(boolean include) {
            this.includeAir = include;
            return this;
        }

        @Override
        @NotNull
        public StructureService.PlacementOptions.Builder includeEntities(boolean include) {
            this.includeEntities = include;
            return this;
        }

        @Override
        @NotNull
        public StructureService.PlacementOptions.Builder integrity(float integrity) {
            this.integrity = Math.max(0.0f, Math.min(1.0f, integrity));
            return this;
        }

        @Override
        @NotNull
        public StructureService.PlacementOptions.Builder integritySeed(long seed) {
            this.integritySeed = seed;
            return this;
        }

        @Override
        @NotNull
        public StructureService.PlacementOptions build() {
            return new PlacementOptionsImpl(rotation, mirror, includeAir, includeEntities, integrity, integritySeed);
        }
    }
}
