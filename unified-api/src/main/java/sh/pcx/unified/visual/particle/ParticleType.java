/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.particle;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of all Minecraft particle types.
 *
 * <p>This enum provides a platform-independent way to reference particle types.
 * The implementation will map these to the appropriate platform-specific particles.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum ParticleType {

    // Basic particles
    EXPLOSION_LARGE("explosion"),
    EXPLOSION("explosion_emitter"),
    FIREWORK("firework"),
    BUBBLE("bubble"),
    SPLASH("splash"),
    WAKE("fishing"),
    SUSPENDED("underwater"),
    CRIT("crit"),
    CRIT_MAGIC("enchanted_hit"),
    SMOKE_NORMAL("smoke"),
    SMOKE_LARGE("large_smoke"),
    SPELL("effect"),
    SPELL_INSTANT("instant_effect"),
    SPELL_MOB("entity_effect"),
    SPELL_WITCH("witch"),
    DRIP_WATER("dripping_water"),
    DRIP_LAVA("dripping_lava"),
    VILLAGER_ANGRY("angry_villager"),
    VILLAGER_HAPPY("happy_villager"),
    NOTE("note"),
    PORTAL("portal"),
    ENCHANTMENT_TABLE("enchant"),
    FLAME("flame"),
    LAVA("lava"),
    CLOUD("cloud"),
    REDSTONE("dust"),
    SNOWBALL("item_snowball"),
    SLIME("item_slime"),
    HEART("heart"),
    ITEM_CRACK("item"),
    BLOCK_CRACK("block"),
    BLOCK_DUST("block"),
    WATER_DROP("rain"),
    MOB_APPEARANCE("elder_guardian"),
    DRAGON_BREATH("dragon_breath"),
    END_ROD("end_rod"),
    DAMAGE_INDICATOR("damage_indicator"),
    SWEEP_ATTACK("sweep_attack"),
    FALLING_DUST("falling_dust"),
    TOTEM("totem_of_undying"),
    SPIT("spit"),

    // 1.13+ particles
    BUBBLE_COLUMN_UP("bubble_column_up"),
    BUBBLE_POP("bubble_pop"),
    CURRENT_DOWN("current_down"),
    SQUID_INK("squid_ink"),
    NAUTILUS("nautilus"),
    DOLPHIN("dolphin"),

    // 1.14+ particles
    SNEEZE("sneeze"),
    CAMPFIRE_COSY_SMOKE("campfire_cosy_smoke"),
    CAMPFIRE_SIGNAL_SMOKE("campfire_signal_smoke"),
    COMPOSTER("composter"),
    FLASH("flash"),
    FALLING_LAVA("falling_lava"),
    LANDING_LAVA("landing_lava"),
    FALLING_WATER("falling_water"),

    // 1.15+ particles
    DRIPPING_HONEY("dripping_honey"),
    FALLING_HONEY("falling_honey"),
    LANDING_HONEY("landing_honey"),
    FALLING_NECTAR("falling_nectar"),

    // 1.16+ particles
    SOUL_FIRE_FLAME("soul_fire_flame"),
    ASH("ash"),
    CRIMSON_SPORE("crimson_spore"),
    WARPED_SPORE("warped_spore"),
    SOUL("soul"),
    DRIPPING_OBSIDIAN_TEAR("dripping_obsidian_tear"),
    FALLING_OBSIDIAN_TEAR("falling_obsidian_tear"),
    LANDING_OBSIDIAN_TEAR("landing_obsidian_tear"),
    REVERSE_PORTAL("reverse_portal"),
    WHITE_ASH("white_ash"),

    // 1.17+ particles
    DUST_COLOR_TRANSITION("dust_color_transition"),
    VIBRATION("vibration"),
    FALLING_SPORE_BLOSSOM("falling_spore_blossom"),
    SPORE_BLOSSOM_AIR("spore_blossom_air"),
    SMALL_FLAME("small_flame"),
    SNOWFLAKE("snowflake"),
    DRIPPING_DRIPSTONE_LAVA("dripping_dripstone_lava"),
    FALLING_DRIPSTONE_LAVA("falling_dripstone_lava"),
    DRIPPING_DRIPSTONE_WATER("dripping_dripstone_water"),
    FALLING_DRIPSTONE_WATER("falling_dripstone_water"),
    GLOW_SQUID_INK("glow_squid_ink"),
    GLOW("glow"),
    WAX_ON("wax_on"),
    WAX_OFF("wax_off"),
    ELECTRIC_SPARK("electric_spark"),
    SCRAPE("scrape"),

    // 1.18+ particles
    BLOCK_MARKER("block_marker"),

    // 1.19+ particles
    SONIC_BOOM("sonic_boom"),
    SCULK_SOUL("sculk_soul"),
    SCULK_CHARGE("sculk_charge"),
    SCULK_CHARGE_POP("sculk_charge_pop"),
    SHRIEK("shriek"),

    // 1.20+ particles
    CHERRY_LEAVES("cherry_leaves"),
    EGG_CRACK("egg_crack"),

    // 1.21+ particles
    DUST_PLUME("dust_plume"),
    TRIAL_SPAWNER_DETECTION("trial_spawner_detection"),
    TRIAL_SPAWNER_DETECTION_OMINOUS("trial_spawner_detection_ominous"),
    VAULT_CONNECTION("vault_connection"),
    DUST_PILLAR("dust_pillar"),
    OMINOUS_SPAWNING("ominous_spawning"),
    RAID_OMEN("raid_omen"),
    TRIAL_OMEN("trial_omen"),

    // Special particles
    DUST("dust"),
    ITEM("item"),
    BLOCK("block");

    private final String key;

    ParticleType(String key) {
        this.key = key;
    }

    /**
     * Returns the Minecraft key for this particle.
     *
     * @return the particle key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Returns whether this particle supports color customization.
     *
     * @return true if color can be set
     * @since 1.0.0
     */
    public boolean supportsColor() {
        return this == DUST || this == DUST_COLOR_TRANSITION || this == SPELL_MOB;
    }

    /**
     * Returns whether this particle requires additional data.
     *
     * @return true if data is required
     * @since 1.0.0
     */
    public boolean requiresData() {
        return this == ITEM || this == BLOCK || this == BLOCK_DUST ||
               this == FALLING_DUST || this == BLOCK_MARKER;
    }
}
