/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a block type for use in world generation.
 *
 * <p>BlockType provides a platform-agnostic way to reference block types
 * during chunk generation. It supports both namespaced keys and common
 * pre-defined constants.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Use pre-defined constants
 * chunkData.setBlock(x, y, z, BlockType.STONE);
 * chunkData.setBlock(x, y + 1, z, BlockType.GRASS_BLOCK);
 *
 * // Use namespaced key
 * BlockType customBlock = BlockType.of("mymod:custom_stone");
 * chunkData.setBlock(x, y, z, customBlock);
 *
 * // Check block type
 * if (chunkData.getBlock(x, y, z).equals(BlockType.AIR)) {
 *     // Block is air
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public record BlockType(@NotNull String key) {

    // ==================== Common Block Types ====================

    public static final BlockType AIR = of("minecraft:air");
    public static final BlockType CAVE_AIR = of("minecraft:cave_air");
    public static final BlockType VOID_AIR = of("minecraft:void_air");

    // Stone variants
    public static final BlockType STONE = of("minecraft:stone");
    public static final BlockType GRANITE = of("minecraft:granite");
    public static final BlockType DIORITE = of("minecraft:diorite");
    public static final BlockType ANDESITE = of("minecraft:andesite");
    public static final BlockType DEEPSLATE = of("minecraft:deepslate");
    public static final BlockType COBBLESTONE = of("minecraft:cobblestone");
    public static final BlockType COBBLED_DEEPSLATE = of("minecraft:cobbled_deepslate");
    public static final BlockType SMOOTH_STONE = of("minecraft:smooth_stone");
    public static final BlockType MOSSY_COBBLESTONE = of("minecraft:mossy_cobblestone");

    // Dirt variants
    public static final BlockType DIRT = of("minecraft:dirt");
    public static final BlockType COARSE_DIRT = of("minecraft:coarse_dirt");
    public static final BlockType ROOTED_DIRT = of("minecraft:rooted_dirt");
    public static final BlockType GRASS_BLOCK = of("minecraft:grass_block");
    public static final BlockType PODZOL = of("minecraft:podzol");
    public static final BlockType MYCELIUM = of("minecraft:mycelium");
    public static final BlockType MUD = of("minecraft:mud");
    public static final BlockType FARMLAND = of("minecraft:farmland");
    public static final BlockType DIRT_PATH = of("minecraft:dirt_path");

    // Sand variants
    public static final BlockType SAND = of("minecraft:sand");
    public static final BlockType RED_SAND = of("minecraft:red_sand");
    public static final BlockType GRAVEL = of("minecraft:gravel");
    public static final BlockType SANDSTONE = of("minecraft:sandstone");
    public static final BlockType RED_SANDSTONE = of("minecraft:red_sandstone");

    // Wood types
    public static final BlockType OAK_LOG = of("minecraft:oak_log");
    public static final BlockType SPRUCE_LOG = of("minecraft:spruce_log");
    public static final BlockType BIRCH_LOG = of("minecraft:birch_log");
    public static final BlockType JUNGLE_LOG = of("minecraft:jungle_log");
    public static final BlockType ACACIA_LOG = of("minecraft:acacia_log");
    public static final BlockType DARK_OAK_LOG = of("minecraft:dark_oak_log");
    public static final BlockType CHERRY_LOG = of("minecraft:cherry_log");
    public static final BlockType MANGROVE_LOG = of("minecraft:mangrove_log");

    // Leaves
    public static final BlockType OAK_LEAVES = of("minecraft:oak_leaves");
    public static final BlockType SPRUCE_LEAVES = of("minecraft:spruce_leaves");
    public static final BlockType BIRCH_LEAVES = of("minecraft:birch_leaves");
    public static final BlockType JUNGLE_LEAVES = of("minecraft:jungle_leaves");
    public static final BlockType ACACIA_LEAVES = of("minecraft:acacia_leaves");
    public static final BlockType DARK_OAK_LEAVES = of("minecraft:dark_oak_leaves");
    public static final BlockType CHERRY_LEAVES = of("minecraft:cherry_leaves");
    public static final BlockType MANGROVE_LEAVES = of("minecraft:mangrove_leaves");

    // Ores
    public static final BlockType COAL_ORE = of("minecraft:coal_ore");
    public static final BlockType IRON_ORE = of("minecraft:iron_ore");
    public static final BlockType COPPER_ORE = of("minecraft:copper_ore");
    public static final BlockType GOLD_ORE = of("minecraft:gold_ore");
    public static final BlockType REDSTONE_ORE = of("minecraft:redstone_ore");
    public static final BlockType EMERALD_ORE = of("minecraft:emerald_ore");
    public static final BlockType LAPIS_ORE = of("minecraft:lapis_ore");
    public static final BlockType DIAMOND_ORE = of("minecraft:diamond_ore");
    public static final BlockType DEEPSLATE_COAL_ORE = of("minecraft:deepslate_coal_ore");
    public static final BlockType DEEPSLATE_IRON_ORE = of("minecraft:deepslate_iron_ore");
    public static final BlockType DEEPSLATE_COPPER_ORE = of("minecraft:deepslate_copper_ore");
    public static final BlockType DEEPSLATE_GOLD_ORE = of("minecraft:deepslate_gold_ore");
    public static final BlockType DEEPSLATE_REDSTONE_ORE = of("minecraft:deepslate_redstone_ore");
    public static final BlockType DEEPSLATE_EMERALD_ORE = of("minecraft:deepslate_emerald_ore");
    public static final BlockType DEEPSLATE_LAPIS_ORE = of("minecraft:deepslate_lapis_ore");
    public static final BlockType DEEPSLATE_DIAMOND_ORE = of("minecraft:deepslate_diamond_ore");

    // Liquids
    public static final BlockType WATER = of("minecraft:water");
    public static final BlockType LAVA = of("minecraft:lava");

    // Special blocks
    public static final BlockType BEDROCK = of("minecraft:bedrock");
    public static final BlockType OBSIDIAN = of("minecraft:obsidian");
    public static final BlockType END_STONE = of("minecraft:end_stone");
    public static final BlockType NETHERRACK = of("minecraft:netherrack");
    public static final BlockType SOUL_SAND = of("minecraft:soul_sand");
    public static final BlockType SOUL_SOIL = of("minecraft:soul_soil");
    public static final BlockType BASALT = of("minecraft:basalt");
    public static final BlockType BLACKSTONE = of("minecraft:blackstone");
    public static final BlockType CRYING_OBSIDIAN = of("minecraft:crying_obsidian");
    public static final BlockType GLOWSTONE = of("minecraft:glowstone");
    public static final BlockType SHROOMLIGHT = of("minecraft:shroomlight");

    // Ice and snow
    public static final BlockType ICE = of("minecraft:ice");
    public static final BlockType PACKED_ICE = of("minecraft:packed_ice");
    public static final BlockType BLUE_ICE = of("minecraft:blue_ice");
    public static final BlockType SNOW = of("minecraft:snow");
    public static final BlockType SNOW_BLOCK = of("minecraft:snow_block");
    public static final BlockType POWDER_SNOW = of("minecraft:powder_snow");

    // Clay and terracotta
    public static final BlockType CLAY = of("minecraft:clay");
    public static final BlockType TERRACOTTA = of("minecraft:terracotta");

    // Plants
    public static final BlockType GRASS = of("minecraft:grass");
    public static final BlockType TALL_GRASS = of("minecraft:tall_grass");
    public static final BlockType FERN = of("minecraft:fern");
    public static final BlockType LARGE_FERN = of("minecraft:large_fern");
    public static final BlockType DEAD_BUSH = of("minecraft:dead_bush");
    public static final BlockType SEAGRASS = of("minecraft:seagrass");
    public static final BlockType TALL_SEAGRASS = of("minecraft:tall_seagrass");
    public static final BlockType KELP = of("minecraft:kelp");
    public static final BlockType CACTUS = of("minecraft:cactus");
    public static final BlockType SUGAR_CANE = of("minecraft:sugar_cane");

    // Flowers
    public static final BlockType DANDELION = of("minecraft:dandelion");
    public static final BlockType POPPY = of("minecraft:poppy");
    public static final BlockType BLUE_ORCHID = of("minecraft:blue_orchid");
    public static final BlockType ALLIUM = of("minecraft:allium");
    public static final BlockType AZURE_BLUET = of("minecraft:azure_bluet");
    public static final BlockType RED_TULIP = of("minecraft:red_tulip");
    public static final BlockType CORNFLOWER = of("minecraft:cornflower");
    public static final BlockType LILY_OF_THE_VALLEY = of("minecraft:lily_of_the_valley");
    public static final BlockType SUNFLOWER = of("minecraft:sunflower");
    public static final BlockType LILAC = of("minecraft:lilac");
    public static final BlockType ROSE_BUSH = of("minecraft:rose_bush");
    public static final BlockType PEONY = of("minecraft:peony");

    /**
     * Creates a block type from a namespaced key.
     *
     * @param key the namespaced key (e.g., "minecraft:stone")
     * @return the block type
     * @since 1.0.0
     */
    @NotNull
    public static BlockType of(@NotNull String key) {
        return new BlockType(key);
    }

    /**
     * Creates a block type from namespace and path.
     *
     * @param namespace the namespace (e.g., "minecraft")
     * @param path      the path (e.g., "stone")
     * @return the block type
     * @since 1.0.0
     */
    @NotNull
    public static BlockType of(@NotNull String namespace, @NotNull String path) {
        return new BlockType(namespace + ":" + path);
    }

    /**
     * Gets the namespace portion of the key.
     *
     * @return the namespace
     * @since 1.0.0
     */
    @NotNull
    public String namespace() {
        int colonIndex = key.indexOf(':');
        return colonIndex > 0 ? key.substring(0, colonIndex) : "minecraft";
    }

    /**
     * Gets the path portion of the key.
     *
     * @return the path
     * @since 1.0.0
     */
    @NotNull
    public String path() {
        int colonIndex = key.indexOf(':');
        return colonIndex > 0 ? key.substring(colonIndex + 1) : key;
    }

    /**
     * Checks if this is an air block type.
     *
     * @return true if this is any type of air
     * @since 1.0.0
     */
    public boolean isAir() {
        return this.equals(AIR) || this.equals(CAVE_AIR) || this.equals(VOID_AIR);
    }

    /**
     * Checks if this is a liquid block type.
     *
     * @return true if this is water or lava
     * @since 1.0.0
     */
    public boolean isLiquid() {
        return this.equals(WATER) || this.equals(LAVA);
    }

    /**
     * Checks if this is a solid block type.
     *
     * @return true if this is generally solid
     * @since 1.0.0
     */
    public boolean isSolid() {
        return !isAir() && !isLiquid();
    }

    @Override
    public String toString() {
        return key;
    }
}
