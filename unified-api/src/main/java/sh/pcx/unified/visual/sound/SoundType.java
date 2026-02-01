/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.sound;

import org.jetbrains.annotations.NotNull;

/**
 * Common Minecraft sound types.
 *
 * <p>This enum provides a subset of commonly used sounds. For the complete
 * list of sounds, use {@link SoundBuilder#custom(String)} with the sound key.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum SoundType {

    // UI Sounds
    UI_BUTTON_CLICK("ui.button.click"),
    UI_TOAST_IN("ui.toast.in"),
    UI_TOAST_OUT("ui.toast.out"),
    UI_TOAST_CHALLENGE_COMPLETE("ui.toast.challenge_complete"),

    // Player Sounds
    ENTITY_PLAYER_LEVELUP("entity.player.levelup"),
    ENTITY_PLAYER_HURT("entity.player.hurt"),
    ENTITY_PLAYER_DEATH("entity.player.death"),
    ENTITY_PLAYER_ATTACK_STRONG("entity.player.attack.strong"),
    ENTITY_PLAYER_ATTACK_SWEEP("entity.player.attack.sweep"),
    ENTITY_PLAYER_ATTACK_CRIT("entity.player.attack.crit"),
    ENTITY_PLAYER_BURP("entity.player.burp"),

    // Experience
    ENTITY_EXPERIENCE_ORB_PICKUP("entity.experience_orb.pickup"),
    ENTITY_EXPERIENCE_BOTTLE_THROW("entity.experience_bottle.throw"),

    // Item Sounds
    ENTITY_ITEM_PICKUP("entity.item.pickup"),
    ITEM_ARMOR_EQUIP_GENERIC("item.armor.equip_generic"),
    ITEM_ARMOR_EQUIP_CHAIN("item.armor.equip_chain"),
    ITEM_ARMOR_EQUIP_DIAMOND("item.armor.equip_diamond"),
    ITEM_ARMOR_EQUIP_NETHERITE("item.armor.equip_netherite"),
    ITEM_SHIELD_BLOCK("item.shield.block"),
    ITEM_SHIELD_BREAK("item.shield.break"),
    ITEM_TOTEM_USE("item.totem.use"),
    ITEM_TRIDENT_THROW("item.trident.throw"),
    ITEM_TRIDENT_RETURN("item.trident.return"),
    ITEM_CHORUS_FRUIT_TELEPORT("item.chorus_fruit.teleport"),
    ITEM_BOTTLE_FILL("item.bottle.fill"),
    ITEM_BUCKET_FILL("item.bucket.fill"),
    ITEM_BUCKET_EMPTY("item.bucket.empty"),
    ITEM_FLINTANDSTEEL_USE("item.flintandsteel.use"),

    // Block Sounds
    BLOCK_NOTE_BLOCK_PLING("block.note_block.pling"),
    BLOCK_NOTE_BLOCK_BELL("block.note_block.bell"),
    BLOCK_NOTE_BLOCK_CHIME("block.note_block.chime"),
    BLOCK_NOTE_BLOCK_XYLOPHONE("block.note_block.xylophone"),
    BLOCK_NOTE_BLOCK_HARP("block.note_block.harp"),
    BLOCK_ANVIL_USE("block.anvil.use"),
    BLOCK_ANVIL_LAND("block.anvil.land"),
    BLOCK_ANVIL_BREAK("block.anvil.break"),
    BLOCK_CHEST_OPEN("block.chest.open"),
    BLOCK_CHEST_CLOSE("block.chest.close"),
    BLOCK_ENDER_CHEST_OPEN("block.ender_chest.open"),
    BLOCK_ENDER_CHEST_CLOSE("block.ender_chest.close"),
    BLOCK_ENCHANTMENT_TABLE_USE("block.enchantment_table.use"),
    BLOCK_BEACON_ACTIVATE("block.beacon.activate"),
    BLOCK_BEACON_DEACTIVATE("block.beacon.deactivate"),
    BLOCK_BEACON_POWER_SELECT("block.beacon.power_select"),
    BLOCK_PORTAL_TRIGGER("block.portal.trigger"),
    BLOCK_PORTAL_TRAVEL("block.portal.travel"),
    BLOCK_END_PORTAL_SPAWN("block.end_portal.spawn"),
    BLOCK_RESPAWN_ANCHOR_CHARGE("block.respawn_anchor.charge"),
    BLOCK_RESPAWN_ANCHOR_SET_SPAWN("block.respawn_anchor.set_spawn"),
    BLOCK_BELL_USE("block.bell.use"),
    BLOCK_BELL_RESONATE("block.bell.resonate"),
    BLOCK_AMETHYST_BLOCK_CHIME("block.amethyst_block.chime"),
    BLOCK_AMETHYST_CLUSTER_BREAK("block.amethyst_cluster.break"),

    // Ambient/Environment
    AMBIENT_CAVE("ambient.cave"),
    AMBIENT_UNDERWATER_ENTER("ambient.underwater.enter"),
    AMBIENT_UNDERWATER_EXIT("ambient.underwater.exit"),
    WEATHER_RAIN("weather.rain"),
    WEATHER_RAIN_ABOVE("weather.rain.above"),
    ENTITY_LIGHTNING_BOLT_THUNDER("entity.lightning_bolt.thunder"),
    ENTITY_LIGHTNING_BOLT_IMPACT("entity.lightning_bolt.impact"),

    // Mob Sounds
    ENTITY_ENDER_DRAGON_GROWL("entity.ender_dragon.growl"),
    ENTITY_ENDER_DRAGON_DEATH("entity.ender_dragon.death"),
    ENTITY_ENDER_DRAGON_FLAP("entity.ender_dragon.flap"),
    ENTITY_WITHER_SPAWN("entity.wither.spawn"),
    ENTITY_WITHER_DEATH("entity.wither.death"),
    ENTITY_WITHER_AMBIENT("entity.wither.ambient"),
    ENTITY_ENDERMAN_TELEPORT("entity.enderman.teleport"),
    ENTITY_ENDERMAN_SCREAM("entity.enderman.scream"),
    ENTITY_ZOMBIE_VILLAGER_CURE("entity.zombie_villager.cure"),
    ENTITY_VILLAGER_YES("entity.villager.yes"),
    ENTITY_VILLAGER_NO("entity.villager.no"),
    ENTITY_VILLAGER_TRADE("entity.villager.trade"),
    ENTITY_VILLAGER_CELEBRATE("entity.villager.celebrate"),
    ENTITY_WOLF_HOWL("entity.wolf.howl"),
    ENTITY_CAT_PURR("entity.cat.purr"),
    ENTITY_BLAZE_SHOOT("entity.blaze.shoot"),
    ENTITY_GHAST_SHOOT("entity.ghast.shoot"),
    ENTITY_GHAST_SCREAM("entity.ghast.scream"),
    ENTITY_CREEPER_PRIMED("entity.creeper.primed"),
    ENTITY_GENERIC_EXPLODE("entity.generic.explode"),
    ENTITY_WARDEN_EMERGE("entity.warden.emerge"),
    ENTITY_WARDEN_ROAR("entity.warden.roar"),
    ENTITY_WARDEN_SONIC_BOOM("entity.warden.sonic_boom"),

    // Music Discs
    MUSIC_DISC_13("music_disc.13"),
    MUSIC_DISC_CAT("music_disc.cat"),
    MUSIC_DISC_BLOCKS("music_disc.blocks"),
    MUSIC_DISC_CHIRP("music_disc.chirp"),
    MUSIC_DISC_FAR("music_disc.far"),
    MUSIC_DISC_MALL("music_disc.mall"),
    MUSIC_DISC_MELLOHI("music_disc.mellohi"),
    MUSIC_DISC_STAL("music_disc.stal"),
    MUSIC_DISC_STRAD("music_disc.strad"),
    MUSIC_DISC_WARD("music_disc.ward"),
    MUSIC_DISC_11("music_disc.11"),
    MUSIC_DISC_WAIT("music_disc.wait"),
    MUSIC_DISC_PIGSTEP("music_disc.pigstep"),
    MUSIC_DISC_OTHERSIDE("music_disc.otherside"),
    MUSIC_DISC_5("music_disc.5"),
    MUSIC_DISC_RELIC("music_disc.relic"),

    // Misc
    ENTITY_FIREWORK_ROCKET_LAUNCH("entity.firework_rocket.launch"),
    ENTITY_FIREWORK_ROCKET_BLAST("entity.firework_rocket.blast"),
    ENTITY_FIREWORK_ROCKET_LARGE_BLAST("entity.firework_rocket.large_blast"),
    ENTITY_FIREWORK_ROCKET_TWINKLE("entity.firework_rocket.twinkle"),
    ENTITY_ARROW_HIT("entity.arrow.hit"),
    ENTITY_ARROW_HIT_PLAYER("entity.arrow.hit_player"),
    ENTITY_ARROW_SHOOT("entity.arrow.shoot"),
    ENTITY_SPLASH_POTION_BREAK("entity.splash_potion.break"),
    ENTITY_EVOKER_CAST_SPELL("entity.evoker.cast_spell"),
    ENTITY_ILLUSIONER_CAST_SPELL("entity.illusioner.cast_spell");

    private final String key;

    SoundType(String key) {
        this.key = key;
    }

    /**
     * Returns the Minecraft key for this sound.
     *
     * @return the sound key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }
}
