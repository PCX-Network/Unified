/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.api;

import sh.pcx.unified.service.Service;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for version-safe gamerule access.
 *
 * <p>Minecraft 1.21.11 changed gamerule naming conventions from camelCase to snake_case
 * in the registry. This service provides a consistent API that works across versions,
 * automatically handling the naming differences.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private GameRuleService gameRules;
 *
 * public void configureWorld(World world) {
 *     // Set gamerules with version-safe naming
 *     gameRules.set(world, GameRules.DO_FIRE_TICK, false);
 *     gameRules.set(world, GameRules.KEEP_INVENTORY, true);
 *     gameRules.set(world, GameRules.MOB_GRIEFING, false);
 *
 *     // Get gamerule values
 *     boolean keepInventory = gameRules.get(world, GameRules.KEEP_INVENTORY);
 *     int randomTickSpeed = gameRules.get(world, GameRules.RANDOM_TICK_SPEED);
 *
 *     // Reset to default
 *     gameRules.reset(world, GameRules.RANDOM_TICK_SPEED);
 * }
 * }</pre>
 *
 * <h2>Naming Convention Changes</h2>
 * <table>
 *   <tr><th>Pre-1.21.11</th><th>1.21.11+</th></tr>
 *   <tr><td>doFireTick</td><td>do_fire_tick</td></tr>
 *   <tr><td>keepInventory</td><td>keep_inventory</td></tr>
 *   <tr><td>mobGriefing</td><td>mob_griefing</td></tr>
 *   <tr><td>randomTickSpeed</td><td>random_tick_speed</td></tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods that modify world state should be called from the main thread.
 * Read operations are generally thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see GameRules
 */
public interface GameRuleService extends Service {

    /**
     * Gets a boolean gamerule value.
     *
     * @param world    the world
     * @param gameRule the gamerule
     * @return the gamerule value
     * @since 1.0.0
     */
    boolean getBoolean(@NotNull World world, @NotNull GameRule<Boolean> gameRule);

    /**
     * Gets an integer gamerule value.
     *
     * @param world    the world
     * @param gameRule the gamerule
     * @return the gamerule value
     * @since 1.0.0
     */
    int getInteger(@NotNull World world, @NotNull GameRule<Integer> gameRule);

    /**
     * Gets a gamerule value as a generic type.
     *
     * @param <T>      the gamerule type
     * @param world    the world
     * @param gameRule the gamerule
     * @return the gamerule value
     * @since 1.0.0
     */
    @NotNull
    <T> T getValue(@NotNull World world, @NotNull GameRule<T> gameRule);

    /**
     * Gets a gamerule value as an optional.
     *
     * @param <T>      the gamerule type
     * @param world    the world
     * @param gameRule the gamerule
     * @return the gamerule value, or empty if not supported
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> getOptional(@NotNull World world, @NotNull GameRule<T> gameRule);

    /**
     * Sets a boolean gamerule value.
     *
     * @param world    the world
     * @param gameRule the gamerule
     * @param value    the value to set
     * @since 1.0.0
     */
    void setBoolean(@NotNull World world, @NotNull GameRule<Boolean> gameRule, boolean value);

    /**
     * Sets an integer gamerule value.
     *
     * @param world    the world
     * @param gameRule the gamerule
     * @param value    the value to set
     * @since 1.0.0
     */
    void setInteger(@NotNull World world, @NotNull GameRule<Integer> gameRule, int value);

    /**
     * Sets a gamerule value with generic type.
     *
     * @param <T>      the gamerule type
     * @param world    the world
     * @param gameRule the gamerule
     * @param value    the value to set
     * @since 1.0.0
     */
    <T> void setValue(@NotNull World world, @NotNull GameRule<T> gameRule, @NotNull T value);

    /**
     * Resets a gamerule to its default value.
     *
     * @param world    the world
     * @param gameRule the gamerule
     * @since 1.0.0
     */
    void reset(@NotNull World world, @NotNull GameRule<?> gameRule);

    /**
     * Gets the default value for a gamerule.
     *
     * @param <T>      the gamerule type
     * @param gameRule the gamerule
     * @return the default value
     * @since 1.0.0
     */
    @NotNull
    <T> T getDefault(@NotNull GameRule<T> gameRule);

    /**
     * Checks if a gamerule exists on this server version.
     *
     * @param gameRule the gamerule
     * @return true if the gamerule exists
     * @since 1.0.0
     */
    boolean exists(@NotNull GameRule<?> gameRule);

    /**
     * Gets the actual gamerule name used by the server.
     *
     * <p>Returns the version-appropriate name (camelCase or snake_case).
     *
     * @param gameRule the gamerule
     * @return the actual name used by the server
     * @since 1.0.0
     */
    @NotNull
    String getActualName(@NotNull GameRule<?> gameRule);

    /**
     * Gets all available gamerules on this server version.
     *
     * @return set of available gamerules
     * @since 1.0.0
     */
    @NotNull
    Set<GameRule<?>> getAvailable();

    /**
     * Gets all gamerule values for a world.
     *
     * @param world the world
     * @return map of gamerule to value
     * @since 1.0.0
     */
    @NotNull
    Map<GameRule<?>, Object> getAll(@NotNull World world);

    /**
     * Copies all gamerules from one world to another.
     *
     * @param source      the source world
     * @param destination the destination world
     * @since 1.0.0
     */
    void copyAll(@NotNull World source, @NotNull World destination);

    /**
     * Looks up a gamerule by name.
     *
     * <p>Accepts both camelCase and snake_case names.
     *
     * @param name the gamerule name
     * @return the gamerule, or null if not found
     * @since 1.0.0
     */
    @Nullable
    GameRule<?> byName(@NotNull String name);

    /**
     * Checks if the server uses registry-based gamerules (1.21.11+).
     *
     * @return true if using registry gamerules
     * @since 1.0.0
     */
    boolean usesRegistryGameRules();

    @Override
    default String getServiceName() {
        return "GameRuleService";
    }

    /**
     * Type-safe gamerule reference.
     *
     * @param <T> the value type (Boolean or Integer)
     * @since 1.0.0
     */
    interface GameRule<T> {
        /**
         * Returns the legacy (camelCase) name.
         *
         * @return the legacy name
         */
        @NotNull
        String getLegacyName();

        /**
         * Returns the modern (snake_case) name.
         *
         * @return the modern name
         */
        @NotNull
        String getModernName();

        /**
         * Returns the value type.
         *
         * @return the value class
         */
        @NotNull
        Class<T> getType();

        /**
         * Returns the default value.
         *
         * @return the default value
         */
        @NotNull
        T getDefaultValue();

        /**
         * Returns the description of this gamerule.
         *
         * @return the description
         */
        @NotNull
        String getDescription();
    }

    /**
     * Standard Minecraft gamerules with type-safe access.
     *
     * @since 1.0.0
     */
    final class GameRules {

        private GameRules() {
            // Utility class
        }

        // ===== Boolean GameRules =====

        /** Whether fire should spread and naturally extinguish. */
        public static final GameRule<Boolean> DO_FIRE_TICK = booleanRule(
                "doFireTick", "do_fire_tick", true, "Whether fire should spread");

        /** Whether mobs should drop items. */
        public static final GameRule<Boolean> DO_MOB_LOOT = booleanRule(
                "doMobLoot", "do_mob_loot", true, "Whether mobs drop loot");

        /** Whether mobs should naturally spawn. */
        public static final GameRule<Boolean> DO_MOB_SPAWNING = booleanRule(
                "doMobSpawning", "do_mob_spawning", true, "Whether mobs spawn naturally");

        /** Whether blocks should drop as items when broken. */
        public static final GameRule<Boolean> DO_TILE_DROPS = booleanRule(
                "doTileDrops", "do_tile_drops", true, "Whether blocks drop items");

        /** Whether players should keep inventory on death. */
        public static final GameRule<Boolean> KEEP_INVENTORY = booleanRule(
                "keepInventory", "keep_inventory", false, "Keep inventory on death");

        /** Whether mobs can grief the world. */
        public static final GameRule<Boolean> MOB_GRIEFING = booleanRule(
                "mobGriefing", "mob_griefing", true, "Whether mobs can grief");

        /** Whether the daylight cycle should progress. */
        public static final GameRule<Boolean> DO_DAYLIGHT_CYCLE = booleanRule(
                "doDaylightCycle", "do_daylight_cycle", true, "Whether time progresses");

        /** Whether weather should change. */
        public static final GameRule<Boolean> DO_WEATHER_CYCLE = booleanRule(
                "doWeatherCycle", "do_weather_cycle", true, "Whether weather changes");

        /** Whether entities should take damage from falling. */
        public static final GameRule<Boolean> FALL_DAMAGE = booleanRule(
                "fallDamage", "fall_damage", true, "Whether entities take fall damage");

        /** Whether fire should damage entities. */
        public static final GameRule<Boolean> FIRE_DAMAGE = booleanRule(
                "fireDamage", "fire_damage", true, "Whether fire damages entities");

        /** Whether entities should take drowning damage. */
        public static final GameRule<Boolean> DROWNING_DAMAGE = booleanRule(
                "drowningDamage", "drowning_damage", true, "Whether entities drown");

        /** Whether entities should take freezing damage. */
        public static final GameRule<Boolean> FREEZE_DAMAGE = booleanRule(
                "freezeDamage", "freeze_damage", true, "Whether entities take freeze damage");

        /** Whether the player should respawn immediately. */
        public static final GameRule<Boolean> DO_IMMEDIATE_RESPAWN = booleanRule(
                "doImmediateRespawn", "do_immediate_respawn", false, "Instant respawn");

        /** Whether to show death messages. */
        public static final GameRule<Boolean> SHOW_DEATH_MESSAGES = booleanRule(
                "showDeathMessages", "show_death_messages", true, "Show death messages");

        /** Whether to announce advancements. */
        public static final GameRule<Boolean> ANNOUNCE_ADVANCEMENTS = booleanRule(
                "announceAdvancements", "announce_advancements", true, "Announce advancements");

        /** Whether beds explode in the nether/end. */
        public static final GameRule<Boolean> DO_LIMITED_CRAFTING = booleanRule(
                "doLimitedCrafting", "do_limited_crafting", false, "Recipe book only crafting");

        /** Whether insomnia spawns phantoms. */
        public static final GameRule<Boolean> DO_INSOMNIA = booleanRule(
                "doInsomnia", "do_insomnia", true, "Whether phantoms spawn");

        /** Whether patrols spawn. */
        public static final GameRule<Boolean> DO_PATROL_SPAWNING = booleanRule(
                "doPatrolSpawning", "do_patrol_spawning", true, "Whether patrols spawn");

        /** Whether trader spawns. */
        public static final GameRule<Boolean> DO_TRADER_SPAWNING = booleanRule(
                "doTraderSpawning", "do_trader_spawning", true, "Whether traders spawn");

        /** Whether wardens spawn. */
        public static final GameRule<Boolean> DO_WARDEN_SPAWNING = booleanRule(
                "doWardenSpawning", "do_warden_spawning", true, "Whether wardens spawn");

        /** Whether hostile mobs can attack players. */
        public static final GameRule<Boolean> UNIVERSAL_ANGER = booleanRule(
                "universalAnger", "universal_anger", false, "Mobs share anger");

        /** Forgive dead players. */
        public static final GameRule<Boolean> FORGIVE_DEAD_PLAYERS = booleanRule(
                "forgiveDeadPlayers", "forgive_dead_players", true, "Forgive dead players");

        /** Whether to log admin commands. */
        public static final GameRule<Boolean> LOG_ADMIN_COMMANDS = booleanRule(
                "logAdminCommands", "log_admin_commands", true, "Log admin commands");

        /** Whether to show coordinates in the F3 screen. */
        public static final GameRule<Boolean> REDUCED_DEBUG_INFO = booleanRule(
                "reducedDebugInfo", "reduced_debug_info", false, "Reduced F3 info");

        /** Whether to use the natural world spawn. */
        public static final GameRule<Boolean> SPAWN_RADIUS = booleanRule(
                "spectatorsGenerateChunks", "spectators_generate_chunks", true,
                "Spectators generate chunks");

        /** Whether command blocks are enabled. */
        public static final GameRule<Boolean> COMMAND_BLOCK_OUTPUT = booleanRule(
                "commandBlockOutput", "command_block_output", true, "Command block output");

        /** Whether to send command feedback. */
        public static final GameRule<Boolean> SEND_COMMAND_FEEDBACK = booleanRule(
                "sendCommandFeedback", "send_command_feedback", true, "Command feedback");

        /** Whether to disable elytra movement check. */
        public static final GameRule<Boolean> DISABLE_ELYTRA_MOVEMENT_CHECK = booleanRule(
                "disableElytraMovementCheck", "disable_elytra_movement_check", false,
                "Disable elytra check");

        /** Whether to disable raids. */
        public static final GameRule<Boolean> DISABLE_RAIDS = booleanRule(
                "disableRaids", "disable_raids", false, "Disable raids");

        /** Whether block explosion destroys loot. */
        public static final GameRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY = booleanRule(
                "blockExplosionDropDecay", "block_explosion_drop_decay", true,
                "Block explosion drop decay");

        /** Whether mob explosion destroys loot. */
        public static final GameRule<Boolean> MOB_EXPLOSION_DROP_DECAY = booleanRule(
                "mobExplosionDropDecay", "mob_explosion_drop_decay", true,
                "Mob explosion drop decay");

        /** Whether TNT explosion destroys loot. */
        public static final GameRule<Boolean> TNT_EXPLOSION_DROP_DECAY = booleanRule(
                "tntExplosionDropDecay", "tnt_explosion_drop_decay", false,
                "TNT explosion drop decay");

        /** Whether water source blocks are infinite. */
        public static final GameRule<Boolean> WATER_SOURCE_CONVERSION = booleanRule(
                "waterSourceConversion", "water_source_conversion", true,
                "Water source conversion");

        /** Whether lava source blocks are infinite. */
        public static final GameRule<Boolean> LAVA_SOURCE_CONVERSION = booleanRule(
                "lavaSourceConversion", "lava_source_conversion", false,
                "Lava source conversion");

        /** Whether snow should accumulate. */
        public static final GameRule<Boolean> DO_VINES_SPREAD = booleanRule(
                "doVinesSpread", "do_vines_spread", true, "Whether vines spread");

        /** Whether entities collide. */
        public static final GameRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH = booleanRule(
                "enderPearlsVanishOnDeath", "ender_pearls_vanish_on_death", true,
                "Ender pearls vanish on death");

        /** Global sound events. */
        public static final GameRule<Boolean> GLOBAL_SOUND_EVENTS = booleanRule(
                "globalSoundEvents", "global_sound_events", true, "Global sound events");

        /** Whether projectiles can break decorated pots. */
        public static final GameRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS = booleanRule(
                "projectilesCanBreakBlocks", "projectiles_can_break_blocks", true,
                "Projectiles break decorated pots");

        // ===== Integer GameRules =====

        /** How often random ticks occur. */
        public static final GameRule<Integer> RANDOM_TICK_SPEED = integerRule(
                "randomTickSpeed", "random_tick_speed", 3, "Random tick speed");

        /** Maximum number of commands in a function. */
        public static final GameRule<Integer> MAX_COMMAND_CHAIN_LENGTH = integerRule(
                "maxCommandChainLength", "max_command_chain_length", 65536,
                "Max command chain length");

        /** Maximum entity cramming. */
        public static final GameRule<Integer> MAX_ENTITY_CRAMMING = integerRule(
                "maxEntityCramming", "max_entity_cramming", 24, "Max entity cramming");

        /** Spawn protection radius. */
        public static final GameRule<Integer> SPAWN_PROTECTION = integerRule(
                "spawnRadius", "spawn_radius", 10, "Spawn radius");

        /** Number of players required for sleeping. */
        public static final GameRule<Integer> PLAYERS_SLEEPING_PERCENTAGE = integerRule(
                "playersSleepingPercentage", "players_sleeping_percentage", 100,
                "Sleeping percentage");

        /** Snow accumulation height. */
        public static final GameRule<Integer> SNOW_ACCUMULATION_HEIGHT = integerRule(
                "snowAccumulationHeight", "snow_accumulation_height", 1,
                "Snow accumulation height");

        /** How many chunks from the player spawn. */
        public static final GameRule<Integer> SPAWN_CHUNK_RADIUS = integerRule(
                "spawnChunkRadius", "spawn_chunk_radius", 2, "Spawn chunk radius");

        /** Maximum command fork count. */
        public static final GameRule<Integer> MAX_COMMAND_FORK_COUNT = integerRule(
                "maxCommandForkCount", "max_command_fork_count", 65536, "Max command fork count");

        // ===== Factory Methods =====

        private static GameRule<Boolean> booleanRule(String legacy, String modern,
                                                     boolean defaultValue, String description) {
            return new GameRule<>() {
                @Override
                public @NotNull String getLegacyName() {
                    return legacy;
                }

                @Override
                public @NotNull String getModernName() {
                    return modern;
                }

                @Override
                public @NotNull Class<Boolean> getType() {
                    return Boolean.class;
                }

                @Override
                public @NotNull Boolean getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public @NotNull String getDescription() {
                    return description;
                }

                @Override
                public String toString() {
                    return modern + " (boolean)";
                }
            };
        }

        private static GameRule<Integer> integerRule(String legacy, String modern,
                                                     int defaultValue, String description) {
            return new GameRule<>() {
                @Override
                public @NotNull String getLegacyName() {
                    return legacy;
                }

                @Override
                public @NotNull String getModernName() {
                    return modern;
                }

                @Override
                public @NotNull Class<Integer> getType() {
                    return Integer.class;
                }

                @Override
                public @NotNull Integer getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public @NotNull String getDescription() {
                    return description;
                }

                @Override
                public String toString() {
                    return modern + " (integer)";
                }
            };
        }
    }
}
