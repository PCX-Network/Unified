/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Configuration for loading a world.
 *
 * <p>WorldLoader provides options for customizing how a world is loaded,
 * including generator settings, spawn loading, and timeout options.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Load with custom options
 * WorldLoader loader = WorldLoader.builder()
 *     .keepSpawnLoaded(false)
 *     .generateSpawnOnLoad(true)
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * worlds.load("my_world", loader).thenAccept(world -> {
 *     System.out.println("World loaded: " + world.getName());
 * });
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 */
public interface WorldLoader {

    /**
     * Gets the custom generator to use, if any.
     *
     * @return the generator, or null for saved/default generator
     * @since 1.0.0
     */
    @Nullable
    ChunkGenerator getGenerator();

    /**
     * Gets the generator ID to use, if any.
     *
     * @return the generator ID, or null for saved/default generator
     * @since 1.0.0
     */
    @Nullable
    String getGeneratorId();

    /**
     * Checks if the spawn area should be kept loaded.
     *
     * @return true to keep spawn loaded
     * @since 1.0.0
     */
    boolean shouldKeepSpawnLoaded();

    /**
     * Checks if spawn should be generated immediately on load.
     *
     * @return true to generate spawn on load
     * @since 1.0.0
     */
    boolean shouldGenerateSpawnOnLoad();

    /**
     * Gets the load timeout in milliseconds.
     *
     * @return the timeout, or 0 for no timeout
     * @since 1.0.0
     */
    long getTimeoutMillis();

    /**
     * Creates a default loader with standard settings.
     *
     * @return a default loader
     * @since 1.0.0
     */
    @NotNull
    static WorldLoader defaults() {
        return builder().build();
    }

    /**
     * Creates a new loader builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new WorldLoaderBuilder();
    }

    /**
     * Builder for WorldLoader.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the custom generator to use.
         *
         * @param generator the generator
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder generator(@NotNull ChunkGenerator generator);

        /**
         * Sets the generator ID to use.
         *
         * @param generatorId the generator ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder generator(@NotNull String generatorId);

        /**
         * Sets whether to keep the spawn area loaded.
         *
         * @param keepLoaded true to keep spawn loaded
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder keepSpawnLoaded(boolean keepLoaded);

        /**
         * Sets whether to generate spawn on load.
         *
         * @param generate true to generate spawn
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder generateSpawnOnLoad(boolean generate);

        /**
         * Sets the load timeout.
         *
         * @param timeout the timeout duration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder timeout(@NotNull java.time.Duration timeout);

        /**
         * Sets the load timeout in milliseconds.
         *
         * @param millis the timeout in milliseconds
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder timeoutMillis(long millis);

        /**
         * Builds the loader.
         *
         * @return the built loader
         * @since 1.0.0
         */
        @NotNull
        WorldLoader build();
    }
}

/**
 * Default implementation of WorldLoader.Builder.
 */
final class WorldLoaderBuilder implements WorldLoader.Builder {
    private ChunkGenerator generator;
    private String generatorId;
    private boolean keepSpawnLoaded = true;
    private boolean generateSpawnOnLoad = true;
    private long timeoutMillis = 0;

    @Override
    @NotNull
    public WorldLoader.Builder generator(@NotNull ChunkGenerator generator) {
        this.generator = generator;
        this.generatorId = null;
        return this;
    }

    @Override
    @NotNull
    public WorldLoader.Builder generator(@NotNull String generatorId) {
        this.generatorId = generatorId;
        this.generator = null;
        return this;
    }

    @Override
    @NotNull
    public WorldLoader.Builder keepSpawnLoaded(boolean keepLoaded) {
        this.keepSpawnLoaded = keepLoaded;
        return this;
    }

    @Override
    @NotNull
    public WorldLoader.Builder generateSpawnOnLoad(boolean generate) {
        this.generateSpawnOnLoad = generate;
        return this;
    }

    @Override
    @NotNull
    public WorldLoader.Builder timeout(@NotNull java.time.Duration timeout) {
        this.timeoutMillis = timeout.toMillis();
        return this;
    }

    @Override
    @NotNull
    public WorldLoader.Builder timeoutMillis(long millis) {
        this.timeoutMillis = millis;
        return this;
    }

    @Override
    @NotNull
    public WorldLoader build() {
        return new WorldLoaderImpl(generator, generatorId, keepSpawnLoaded, generateSpawnOnLoad, timeoutMillis);
    }

    private record WorldLoaderImpl(
            ChunkGenerator generator,
            String generatorId,
            boolean keepSpawnLoaded,
            boolean generateSpawnOnLoad,
            long timeoutMillis
    ) implements WorldLoader {

        @Override
        @Nullable
        public ChunkGenerator getGenerator() {
            return generator;
        }

        @Override
        @Nullable
        public String getGeneratorId() {
            return generatorId;
        }

        @Override
        public boolean shouldKeepSpawnLoaded() {
            return keepSpawnLoaded;
        }

        @Override
        public boolean shouldGenerateSpawnOnLoad() {
            return generateSpawnOnLoad;
        }

        @Override
        public long getTimeoutMillis() {
            return timeoutMillis;
        }
    }
}
