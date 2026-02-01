/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a biome type for world generation.
 *
 * <p>Biome provides a platform-agnostic way to reference biomes during
 * world generation. It supports both vanilla biomes via constants and
 * custom biomes via namespaced keys.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Use vanilla biomes
 * Biome plains = Biome.PLAINS;
 * Biome forest = Biome.FOREST;
 *
 * // Create custom biome reference
 * Biome customBiome = Biome.of("myplugin:crystal_forest");
 *
 * // Check biome category
 * if (biome.isOcean()) {
 *     // Handle ocean biomes
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see BiomeProvider
 * @see BiomeService
 */
public record Biome(@NotNull String key) {

    // ==================== Overworld Biomes ====================

    // Plains variants
    public static final Biome PLAINS = of("minecraft:plains");
    public static final Biome SUNFLOWER_PLAINS = of("minecraft:sunflower_plains");
    public static final Biome MEADOW = of("minecraft:meadow");
    public static final Biome CHERRY_GROVE = of("minecraft:cherry_grove");

    // Forest variants
    public static final Biome FOREST = of("minecraft:forest");
    public static final Biome FLOWER_FOREST = of("minecraft:flower_forest");
    public static final Biome BIRCH_FOREST = of("minecraft:birch_forest");
    public static final Biome OLD_GROWTH_BIRCH_FOREST = of("minecraft:old_growth_birch_forest");
    public static final Biome DARK_FOREST = of("minecraft:dark_forest");

    // Taiga variants
    public static final Biome TAIGA = of("minecraft:taiga");
    public static final Biome OLD_GROWTH_PINE_TAIGA = of("minecraft:old_growth_pine_taiga");
    public static final Biome OLD_GROWTH_SPRUCE_TAIGA = of("minecraft:old_growth_spruce_taiga");
    public static final Biome SNOWY_TAIGA = of("minecraft:snowy_taiga");

    // Jungle variants
    public static final Biome JUNGLE = of("minecraft:jungle");
    public static final Biome SPARSE_JUNGLE = of("minecraft:sparse_jungle");
    public static final Biome BAMBOO_JUNGLE = of("minecraft:bamboo_jungle");

    // Mountain variants
    public static final Biome WINDSWEPT_HILLS = of("minecraft:windswept_hills");
    public static final Biome WINDSWEPT_GRAVELLY_HILLS = of("minecraft:windswept_gravelly_hills");
    public static final Biome WINDSWEPT_FOREST = of("minecraft:windswept_forest");
    public static final Biome JAGGED_PEAKS = of("minecraft:jagged_peaks");
    public static final Biome FROZEN_PEAKS = of("minecraft:frozen_peaks");
    public static final Biome STONY_PEAKS = of("minecraft:stony_peaks");
    public static final Biome GROVE = of("minecraft:grove");
    public static final Biome SNOWY_SLOPES = of("minecraft:snowy_slopes");

    // Desert and savanna
    public static final Biome DESERT = of("minecraft:desert");
    public static final Biome SAVANNA = of("minecraft:savanna");
    public static final Biome SAVANNA_PLATEAU = of("minecraft:savanna_plateau");
    public static final Biome WINDSWEPT_SAVANNA = of("minecraft:windswept_savanna");
    public static final Biome BADLANDS = of("minecraft:badlands");
    public static final Biome ERODED_BADLANDS = of("minecraft:eroded_badlands");
    public static final Biome WOODED_BADLANDS = of("minecraft:wooded_badlands");

    // Cold biomes
    public static final Biome SNOWY_PLAINS = of("minecraft:snowy_plains");
    public static final Biome ICE_SPIKES = of("minecraft:ice_spikes");
    public static final Biome SNOWY_BEACH = of("minecraft:snowy_beach");
    public static final Biome FROZEN_RIVER = of("minecraft:frozen_river");

    // Swamp variants
    public static final Biome SWAMP = of("minecraft:swamp");
    public static final Biome MANGROVE_SWAMP = of("minecraft:mangrove_swamp");

    // Ocean variants
    public static final Biome OCEAN = of("minecraft:ocean");
    public static final Biome DEEP_OCEAN = of("minecraft:deep_ocean");
    public static final Biome COLD_OCEAN = of("minecraft:cold_ocean");
    public static final Biome DEEP_COLD_OCEAN = of("minecraft:deep_cold_ocean");
    public static final Biome FROZEN_OCEAN = of("minecraft:frozen_ocean");
    public static final Biome DEEP_FROZEN_OCEAN = of("minecraft:deep_frozen_ocean");
    public static final Biome LUKEWARM_OCEAN = of("minecraft:lukewarm_ocean");
    public static final Biome DEEP_LUKEWARM_OCEAN = of("minecraft:deep_lukewarm_ocean");
    public static final Biome WARM_OCEAN = of("minecraft:warm_ocean");

    // Beach and river
    public static final Biome BEACH = of("minecraft:beach");
    public static final Biome STONY_SHORE = of("minecraft:stony_shore");
    public static final Biome RIVER = of("minecraft:river");

    // Mushroom
    public static final Biome MUSHROOM_FIELDS = of("minecraft:mushroom_fields");

    // Cave biomes
    public static final Biome DRIPSTONE_CAVES = of("minecraft:dripstone_caves");
    public static final Biome LUSH_CAVES = of("minecraft:lush_caves");
    public static final Biome DEEP_DARK = of("minecraft:deep_dark");

    // ==================== Nether Biomes ====================

    public static final Biome NETHER_WASTES = of("minecraft:nether_wastes");
    public static final Biome SOUL_SAND_VALLEY = of("minecraft:soul_sand_valley");
    public static final Biome CRIMSON_FOREST = of("minecraft:crimson_forest");
    public static final Biome WARPED_FOREST = of("minecraft:warped_forest");
    public static final Biome BASALT_DELTAS = of("minecraft:basalt_deltas");

    // ==================== End Biomes ====================

    public static final Biome THE_END = of("minecraft:the_end");
    public static final Biome END_HIGHLANDS = of("minecraft:end_highlands");
    public static final Biome END_MIDLANDS = of("minecraft:end_midlands");
    public static final Biome END_BARRENS = of("minecraft:end_barrens");
    public static final Biome SMALL_END_ISLANDS = of("minecraft:small_end_islands");

    // ==================== Special ====================

    public static final Biome THE_VOID = of("minecraft:the_void");

    /**
     * Creates a biome from a namespaced key.
     *
     * @param key the namespaced key (e.g., "minecraft:plains")
     * @return the biome
     * @since 1.0.0
     */
    @NotNull
    public static Biome of(@NotNull String key) {
        return new Biome(key);
    }

    /**
     * Creates a biome from namespace and path.
     *
     * @param namespace the namespace (e.g., "minecraft")
     * @param path      the path (e.g., "plains")
     * @return the biome
     * @since 1.0.0
     */
    @NotNull
    public static Biome of(@NotNull String namespace, @NotNull String path) {
        return new Biome(namespace + ":" + path);
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
     * Checks if this is a vanilla Minecraft biome.
     *
     * @return true if this is a vanilla biome
     * @since 1.0.0
     */
    public boolean isVanilla() {
        return "minecraft".equals(namespace());
    }

    /**
     * Checks if this is a custom (non-vanilla) biome.
     *
     * @return true if this is a custom biome
     * @since 1.0.0
     */
    public boolean isCustom() {
        return !isVanilla();
    }

    /**
     * Checks if this is an ocean biome.
     *
     * @return true if this is an ocean biome
     * @since 1.0.0
     */
    public boolean isOcean() {
        String p = path();
        return p.contains("ocean");
    }

    /**
     * Checks if this is a river biome.
     *
     * @return true if this is a river biome
     * @since 1.0.0
     */
    public boolean isRiver() {
        return path().contains("river");
    }

    /**
     * Checks if this is a beach biome.
     *
     * @return true if this is a beach biome
     * @since 1.0.0
     */
    public boolean isBeach() {
        return path().contains("beach");
    }

    /**
     * Checks if this is a mountain/hills biome.
     *
     * @return true if this is a mountain biome
     * @since 1.0.0
     */
    public boolean isMountain() {
        String p = path();
        return p.contains("peak") || p.contains("hills") || p.contains("slopes");
    }

    /**
     * Checks if this is a forest biome.
     *
     * @return true if this is a forest biome
     * @since 1.0.0
     */
    public boolean isForest() {
        return path().contains("forest") || path().contains("grove");
    }

    /**
     * Checks if this is a snowy/cold biome.
     *
     * @return true if this is a cold biome
     * @since 1.0.0
     */
    public boolean isCold() {
        String p = path();
        return p.contains("snowy") || p.contains("frozen") || p.contains("ice");
    }

    /**
     * Checks if this is a nether biome.
     *
     * @return true if this is a nether biome
     * @since 1.0.0
     */
    public boolean isNether() {
        return this.equals(NETHER_WASTES) ||
               this.equals(SOUL_SAND_VALLEY) ||
               this.equals(CRIMSON_FOREST) ||
               this.equals(WARPED_FOREST) ||
               this.equals(BASALT_DELTAS);
    }

    /**
     * Checks if this is an end biome.
     *
     * @return true if this is an end biome
     * @since 1.0.0
     */
    public boolean isEnd() {
        return path().startsWith("the_end") || path().startsWith("end_") || path().equals("small_end_islands");
    }

    /**
     * Checks if this is a cave biome.
     *
     * @return true if this is a cave biome
     * @since 1.0.0
     */
    public boolean isCave() {
        String p = path();
        return p.contains("caves") || p.equals("deep_dark");
    }

    @Override
    public String toString() {
        return key;
    }
}
