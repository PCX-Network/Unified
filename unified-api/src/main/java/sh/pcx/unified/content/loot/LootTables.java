/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.loot;

import org.jetbrains.annotations.NotNull;

/**
 * Constants for vanilla Minecraft loot tables.
 *
 * <p>Use these constants with {@link LootTableService#modify(LootTables)}
 * to add or modify entries in vanilla loot tables.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Add custom drop to zombie loot table
 * lootTables.modify(LootTables.ZOMBIE)
 *     .addPool(LootPool.builder()
 *         .name("custom_drops")
 *         .rolls(1)
 *         .condition(LootCondition.randomChance(0.1f))
 *         .entry(LootEntry.item("minecraft:emerald").weight(1))
 *         .build())
 *     .apply();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see LootTableService
 */
public enum LootTables {

    // === Entity Loot Tables ===

    /** Zombie drops */
    ZOMBIE("minecraft:entities/zombie"),
    /** Zombie Villager drops */
    ZOMBIE_VILLAGER("minecraft:entities/zombie_villager"),
    /** Drowned drops */
    DROWNED("minecraft:entities/drowned"),
    /** Husk drops */
    HUSK("minecraft:entities/husk"),
    /** Skeleton drops */
    SKELETON("minecraft:entities/skeleton"),
    /** Stray drops */
    STRAY("minecraft:entities/stray"),
    /** Wither Skeleton drops */
    WITHER_SKELETON("minecraft:entities/wither_skeleton"),
    /** Creeper drops */
    CREEPER("minecraft:entities/creeper"),
    /** Spider drops */
    SPIDER("minecraft:entities/spider"),
    /** Cave Spider drops */
    CAVE_SPIDER("minecraft:entities/cave_spider"),
    /** Enderman drops */
    ENDERMAN("minecraft:entities/enderman"),
    /** Blaze drops */
    BLAZE("minecraft:entities/blaze"),
    /** Ghast drops */
    GHAST("minecraft:entities/ghast"),
    /** Witch drops */
    WITCH("minecraft:entities/witch"),
    /** Slime drops */
    SLIME("minecraft:entities/slime"),
    /** Magma Cube drops */
    MAGMA_CUBE("minecraft:entities/magma_cube"),
    /** Phantom drops */
    PHANTOM("minecraft:entities/phantom"),
    /** Guardian drops */
    GUARDIAN("minecraft:entities/guardian"),
    /** Elder Guardian drops */
    ELDER_GUARDIAN("minecraft:entities/elder_guardian"),
    /** Shulker drops */
    SHULKER("minecraft:entities/shulker"),
    /** Pillager drops */
    PILLAGER("minecraft:entities/pillager"),
    /** Vindicator drops */
    VINDICATOR("minecraft:entities/vindicator"),
    /** Evoker drops */
    EVOKER("minecraft:entities/evoker"),
    /** Ravager drops */
    RAVAGER("minecraft:entities/ravager"),
    /** Piglin drops */
    PIGLIN("minecraft:entities/piglin"),
    /** Piglin Brute drops */
    PIGLIN_BRUTE("minecraft:entities/piglin_brute"),
    /** Zombified Piglin drops */
    ZOMBIFIED_PIGLIN("minecraft:entities/zombified_piglin"),
    /** Hoglin drops */
    HOGLIN("minecraft:entities/hoglin"),
    /** Zoglin drops */
    ZOGLIN("minecraft:entities/zoglin"),
    /** Warden drops */
    WARDEN("minecraft:entities/warden"),
    /** Wither Boss drops */
    WITHER("minecraft:entities/wither"),
    /** Ender Dragon drops */
    ENDER_DRAGON("minecraft:entities/ender_dragon"),

    // === Passive Mob Loot Tables ===

    /** Cow drops */
    COW("minecraft:entities/cow"),
    /** Pig drops */
    PIG("minecraft:entities/pig"),
    /** Sheep drops */
    SHEEP("minecraft:entities/sheep"),
    /** Chicken drops */
    CHICKEN("minecraft:entities/chicken"),
    /** Rabbit drops */
    RABBIT("minecraft:entities/rabbit"),
    /** Horse drops */
    HORSE("minecraft:entities/horse"),
    /** Llama drops */
    LLAMA("minecraft:entities/llama"),
    /** Wolf drops */
    WOLF("minecraft:entities/wolf"),
    /** Fox drops */
    FOX("minecraft:entities/fox"),
    /** Cat drops */
    CAT("minecraft:entities/cat"),
    /** Parrot drops */
    PARROT("minecraft:entities/parrot"),
    /** Turtle drops */
    TURTLE("minecraft:entities/turtle"),
    /** Dolphin drops */
    DOLPHIN("minecraft:entities/dolphin"),
    /** Squid drops */
    SQUID("minecraft:entities/squid"),
    /** Glow Squid drops */
    GLOW_SQUID("minecraft:entities/glow_squid"),
    /** Iron Golem drops */
    IRON_GOLEM("minecraft:entities/iron_golem"),
    /** Snow Golem drops */
    SNOW_GOLEM("minecraft:entities/snow_golem"),

    // === Chest Loot Tables ===

    /** Simple dungeon chest */
    SIMPLE_DUNGEON("minecraft:chests/simple_dungeon"),
    /** Abandoned mineshaft chest */
    ABANDONED_MINESHAFT("minecraft:chests/abandoned_mineshaft"),
    /** Buried treasure chest */
    BURIED_TREASURE("minecraft:chests/buried_treasure"),
    /** Desert pyramid chest */
    DESERT_PYRAMID("minecraft:chests/desert_pyramid"),
    /** End city treasure chest */
    END_CITY_TREASURE("minecraft:chests/end_city_treasure"),
    /** Igloo chest */
    IGLOO_CHEST("minecraft:chests/igloo_chest"),
    /** Jungle temple chest */
    JUNGLE_TEMPLE("minecraft:chests/jungle_temple"),
    /** Nether bridge (fortress) chest */
    NETHER_BRIDGE("minecraft:chests/nether_bridge"),
    /** Pillager outpost chest */
    PILLAGER_OUTPOST("minecraft:chests/pillager_outpost"),
    /** Shipwreck map chest */
    SHIPWRECK_MAP("minecraft:chests/shipwreck_map"),
    /** Shipwreck supply chest */
    SHIPWRECK_SUPPLY("minecraft:chests/shipwreck_supply"),
    /** Shipwreck treasure chest */
    SHIPWRECK_TREASURE("minecraft:chests/shipwreck_treasure"),
    /** Stronghold corridor chest */
    STRONGHOLD_CORRIDOR("minecraft:chests/stronghold_corridor"),
    /** Stronghold crossing chest */
    STRONGHOLD_CROSSING("minecraft:chests/stronghold_crossing"),
    /** Stronghold library chest */
    STRONGHOLD_LIBRARY("minecraft:chests/stronghold_library"),
    /** Underwater ruin big chest */
    UNDERWATER_RUIN_BIG("minecraft:chests/underwater_ruin_big"),
    /** Underwater ruin small chest */
    UNDERWATER_RUIN_SMALL("minecraft:chests/underwater_ruin_small"),
    /** Village weaponsmith chest */
    VILLAGE_WEAPONSMITH("minecraft:chests/village/village_weaponsmith"),
    /** Village toolsmith chest */
    VILLAGE_TOOLSMITH("minecraft:chests/village/village_toolsmith"),
    /** Village armorer chest */
    VILLAGE_ARMORER("minecraft:chests/village/village_armorer"),
    /** Village cartographer chest */
    VILLAGE_CARTOGRAPHER("minecraft:chests/village/village_cartographer"),
    /** Village temple chest */
    VILLAGE_TEMPLE("minecraft:chests/village/village_temple"),
    /** Woodland mansion chest */
    WOODLAND_MANSION("minecraft:chests/woodland_mansion"),
    /** Ancient city chest */
    ANCIENT_CITY("minecraft:chests/ancient_city"),
    /** Ancient city ice box */
    ANCIENT_CITY_ICE_BOX("minecraft:chests/ancient_city_ice_box"),
    /** Bastion bridge chest */
    BASTION_BRIDGE("minecraft:chests/bastion_bridge"),
    /** Bastion hoglin stable chest */
    BASTION_HOGLIN_STABLE("minecraft:chests/bastion_hoglin_stable"),
    /** Bastion other chest */
    BASTION_OTHER("minecraft:chests/bastion_other"),
    /** Bastion treasure chest */
    BASTION_TREASURE("minecraft:chests/bastion_treasure"),
    /** Ruined portal chest */
    RUINED_PORTAL("minecraft:chests/ruined_portal"),
    /** Trial chambers corridor chest */
    TRIAL_CHAMBERS_CORRIDOR("minecraft:chests/trial_chambers/corridor"),
    /** Trial chambers reward chest */
    TRIAL_CHAMBERS_REWARD("minecraft:chests/trial_chambers/reward"),

    // === Fishing Loot Tables ===

    /** Fishing loot */
    FISHING("minecraft:gameplay/fishing"),
    /** Fishing - Fish category */
    FISHING_FISH("minecraft:gameplay/fishing/fish"),
    /** Fishing - Junk category */
    FISHING_JUNK("minecraft:gameplay/fishing/junk"),
    /** Fishing - Treasure category */
    FISHING_TREASURE("minecraft:gameplay/fishing/treasure"),

    // === Other Gameplay Loot Tables ===

    /** Piglin bartering */
    PIGLIN_BARTERING("minecraft:gameplay/piglin_bartering"),
    /** Cat morning gift */
    CAT_MORNING_GIFT("minecraft:gameplay/cat_morning_gift"),
    /** Hero of the Village gift */
    HERO_OF_THE_VILLAGE("minecraft:gameplay/hero_of_the_village"),
    /** Sniffer digging */
    SNIFFER_DIGGING("minecraft:gameplay/sniffer_digging");

    private final String key;

    LootTables(String key) {
        this.key = key;
    }

    /**
     * Returns the loot table key.
     *
     * @return the namespaced key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }
}
