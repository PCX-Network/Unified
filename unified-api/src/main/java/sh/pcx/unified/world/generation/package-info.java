/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * World generation API interfaces for the UnifiedPlugin framework.
 *
 * <p>This package contains the core interfaces for world generation and management,
 * including world creation, templates, custom generators, biomes, and structures.
 *
 * <h2>Key Interfaces:</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.world.generation.WorldService} - Main service for world management</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldCreator} - Fluent builder for world creation</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldTemplate} - Pre-configured world templates</li>
 *   <li>{@link sh.pcx.unified.world.generation.ChunkGenerator} - Base class for custom generators</li>
 *   <li>{@link sh.pcx.unified.world.generation.BiomeService} - Custom biome management</li>
 *   <li>{@link sh.pcx.unified.world.generation.StructureService} - Custom structure registration</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldGroup} - World grouping for shared data</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldGroupManager} - Managing world groups</li>
 * </ul>
 *
 * <h2>World Lifecycle:</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.world.generation.WorldLoader} - Configuration for loading worlds</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldUnloader} - Safe world unloading</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldDeleter} - Safe world deletion</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldImporter} - Import existing worlds</li>
 * </ul>
 *
 * <h2>Generation Components:</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.world.generation.ChunkData} - Block data during generation</li>
 *   <li>{@link sh.pcx.unified.world.generation.WorldInfo} - World metadata during generation</li>
 *   <li>{@link sh.pcx.unified.world.generation.BiomeProvider} - Custom biome distribution</li>
 *   <li>{@link sh.pcx.unified.world.generation.BlockPopulator} - Post-generation modification</li>
 *   <li>{@link sh.pcx.unified.world.generation.LimitedRegion} - Safe cross-chunk access</li>
 * </ul>
 *
 * <h2>Block and Biome Types:</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.world.generation.BlockType} - Block type references</li>
 *   <li>{@link sh.pcx.unified.world.generation.BlockData} - Block state data</li>
 *   <li>{@link sh.pcx.unified.world.generation.Biome} - Biome type references</li>
 *   <li>{@link sh.pcx.unified.world.generation.CustomBiome} - Custom biome definitions</li>
 * </ul>
 *
 * <h2>Structures:</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.world.generation.Structure} - Structure definitions</li>
 *   <li>{@link sh.pcx.unified.world.generation.JigsawStructure} - Modular jigsaw structures</li>
 *   <li>{@link sh.pcx.unified.world.generation.StructurePiece} - Structure building blocks</li>
 *   <li>{@link sh.pcx.unified.world.generation.StructureProcessor} - Structure block processing</li>
 *   <li>{@link sh.pcx.unified.world.generation.Schematic} - Saved structure data</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@org.jetbrains.annotations.ApiStatus.Experimental
package sh.pcx.unified.world.generation;
