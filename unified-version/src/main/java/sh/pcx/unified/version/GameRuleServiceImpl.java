/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version;

import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.version.api.GameRuleService;
import sh.pcx.unified.version.api.VersionProvider;
import sh.pcx.unified.version.detection.VersionConstants;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Default implementation of {@link GameRuleService}.
 *
 * <p>This implementation handles the gamerule naming convention changes between versions:
 * <ul>
 *   <li>Pre-1.21.11: camelCase (doFireTick, keepInventory)</li>
 *   <li>1.21.11+: snake_case in registries (do_fire_tick, keep_inventory)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * GameRuleService gameRules = new GameRuleServiceImpl(versionProvider);
 *
 * // Set gamerules - works on any version
 * gameRules.set(world, GameRules.KEEP_INVENTORY, true);
 * gameRules.set(world, GameRules.DO_FIRE_TICK, false);
 *
 * // Get gamerule values
 * boolean keepInventory = gameRules.get(world, GameRules.KEEP_INVENTORY);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class GameRuleServiceImpl implements GameRuleService {

    private final VersionProvider versionProvider;
    private final boolean useModernNames;

    // Cache of GameRule objects for lookup by name
    private static final Map<String, GameRule<?>> BY_LEGACY_NAME = new HashMap<>();
    private static final Map<String, GameRule<?>> BY_MODERN_NAME = new HashMap<>();
    private static final Set<GameRule<?>> ALL_RULES = new HashSet<>();

    static {
        // Register all standard game rules
        registerRule(GameRules.DO_FIRE_TICK);
        registerRule(GameRules.DO_MOB_LOOT);
        registerRule(GameRules.DO_MOB_SPAWNING);
        registerRule(GameRules.DO_TILE_DROPS);
        registerRule(GameRules.KEEP_INVENTORY);
        registerRule(GameRules.MOB_GRIEFING);
        registerRule(GameRules.DO_DAYLIGHT_CYCLE);
        registerRule(GameRules.DO_WEATHER_CYCLE);
        registerRule(GameRules.FALL_DAMAGE);
        registerRule(GameRules.FIRE_DAMAGE);
        registerRule(GameRules.DROWNING_DAMAGE);
        registerRule(GameRules.FREEZE_DAMAGE);
        registerRule(GameRules.DO_IMMEDIATE_RESPAWN);
        registerRule(GameRules.SHOW_DEATH_MESSAGES);
        registerRule(GameRules.ANNOUNCE_ADVANCEMENTS);
        registerRule(GameRules.DO_LIMITED_CRAFTING);
        registerRule(GameRules.DO_INSOMNIA);
        registerRule(GameRules.DO_PATROL_SPAWNING);
        registerRule(GameRules.DO_TRADER_SPAWNING);
        registerRule(GameRules.DO_WARDEN_SPAWNING);
        registerRule(GameRules.UNIVERSAL_ANGER);
        registerRule(GameRules.FORGIVE_DEAD_PLAYERS);
        registerRule(GameRules.LOG_ADMIN_COMMANDS);
        registerRule(GameRules.REDUCED_DEBUG_INFO);
        registerRule(GameRules.SPAWN_RADIUS);
        registerRule(GameRules.COMMAND_BLOCK_OUTPUT);
        registerRule(GameRules.SEND_COMMAND_FEEDBACK);
        registerRule(GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK);
        registerRule(GameRules.DISABLE_RAIDS);
        registerRule(GameRules.BLOCK_EXPLOSION_DROP_DECAY);
        registerRule(GameRules.MOB_EXPLOSION_DROP_DECAY);
        registerRule(GameRules.TNT_EXPLOSION_DROP_DECAY);
        registerRule(GameRules.WATER_SOURCE_CONVERSION);
        registerRule(GameRules.LAVA_SOURCE_CONVERSION);
        registerRule(GameRules.DO_VINES_SPREAD);
        registerRule(GameRules.ENDER_PEARLS_VANISH_ON_DEATH);
        registerRule(GameRules.GLOBAL_SOUND_EVENTS);
        registerRule(GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
        registerRule(GameRules.RANDOM_TICK_SPEED);
        registerRule(GameRules.MAX_COMMAND_CHAIN_LENGTH);
        registerRule(GameRules.MAX_ENTITY_CRAMMING);
        registerRule(GameRules.SPAWN_PROTECTION);
        registerRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        registerRule(GameRules.SNOW_ACCUMULATION_HEIGHT);
        registerRule(GameRules.SPAWN_CHUNK_RADIUS);
        registerRule(GameRules.MAX_COMMAND_FORK_COUNT);
    }

    private static void registerRule(GameRule<?> rule) {
        BY_LEGACY_NAME.put(rule.getLegacyName(), rule);
        BY_MODERN_NAME.put(rule.getModernName(), rule);
        ALL_RULES.add(rule);
    }

    /**
     * Creates a new gamerule service.
     *
     * @param versionProvider the version provider for determining naming conventions
     */
    public GameRuleServiceImpl(@NotNull VersionProvider versionProvider) {
        this.versionProvider = versionProvider;
        this.useModernNames = versionProvider.current().isAtLeast(VersionConstants.V1_21_11);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean getBoolean(@NotNull World world, @NotNull GameRule<Boolean> gameRule) {
        String name = getActualName(gameRule);
        org.bukkit.GameRule<?> rawRule = org.bukkit.GameRule.getByName(name);
        if (rawRule == null) {
            return gameRule.getDefaultValue();
        }
        org.bukkit.GameRule<Boolean> bukkitRule = (org.bukkit.GameRule<Boolean>) rawRule;
        Boolean value = world.getGameRuleValue(bukkitRule);
        return value != null ? value : gameRule.getDefaultValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getInteger(@NotNull World world, @NotNull GameRule<Integer> gameRule) {
        String name = getActualName(gameRule);
        org.bukkit.GameRule<?> rawRule = org.bukkit.GameRule.getByName(name);
        if (rawRule == null) {
            return gameRule.getDefaultValue();
        }
        org.bukkit.GameRule<Integer> bukkitRule = (org.bukkit.GameRule<Integer>) rawRule;
        Integer value = world.getGameRuleValue(bukkitRule);
        return value != null ? value : gameRule.getDefaultValue();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getValue(@NotNull World world, @NotNull GameRule<T> gameRule) {
        if (gameRule.getType() == Boolean.class) {
            return (T) (Boolean) getBoolean(world, (GameRule<Boolean>) gameRule);
        } else if (gameRule.getType() == Integer.class) {
            return (T) (Integer) getInteger(world, (GameRule<Integer>) gameRule);
        }
        return gameRule.getDefaultValue();
    }

    @Override
    @NotNull
    public <T> Optional<T> getOptional(@NotNull World world, @NotNull GameRule<T> gameRule) {
        if (!exists(gameRule)) {
            return Optional.empty();
        }
        return Optional.of(getValue(world, gameRule));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setBoolean(@NotNull World world, @NotNull GameRule<Boolean> gameRule, boolean value) {
        String name = getActualName(gameRule);
        org.bukkit.GameRule<?> rawRule = org.bukkit.GameRule.getByName(name);
        if (rawRule != null) {
            org.bukkit.GameRule<Boolean> bukkitRule = (org.bukkit.GameRule<Boolean>) rawRule;
            world.setGameRule(bukkitRule, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setInteger(@NotNull World world, @NotNull GameRule<Integer> gameRule, int value) {
        String name = getActualName(gameRule);
        org.bukkit.GameRule<?> rawRule = org.bukkit.GameRule.getByName(name);
        if (rawRule != null) {
            org.bukkit.GameRule<Integer> bukkitRule = (org.bukkit.GameRule<Integer>) rawRule;
            world.setGameRule(bukkitRule, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void setValue(@NotNull World world, @NotNull GameRule<T> gameRule, @NotNull T value) {
        if (gameRule.getType() == Boolean.class) {
            setBoolean(world, (GameRule<Boolean>) gameRule, (Boolean) value);
        } else if (gameRule.getType() == Integer.class) {
            setInteger(world, (GameRule<Integer>) gameRule, (Integer) value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reset(@NotNull World world, @NotNull GameRule<?> gameRule) {
        // Type-safe reset using the generic setValue
        resetTyped(world, (GameRule<Object>) gameRule);
    }

    private <T> void resetTyped(World world, GameRule<T> gameRule) {
        setValue(world, gameRule, gameRule.getDefaultValue());
    }

    @Override
    @NotNull
    public <T> T getDefault(@NotNull GameRule<T> gameRule) {
        return gameRule.getDefaultValue();
    }

    @Override
    public boolean exists(@NotNull GameRule<?> gameRule) {
        String name = getActualName(gameRule);
        return org.bukkit.GameRule.getByName(name) != null;
    }

    @Override
    @NotNull
    public String getActualName(@NotNull GameRule<?> gameRule) {
        // Try legacy name first on older versions, modern name on newer
        if (useModernNames) {
            // 1.21.11+ - try modern name first
            String modern = gameRule.getModernName();
            if (org.bukkit.GameRule.getByName(modern) != null) {
                return modern;
            }
            // Fall back to legacy
            return gameRule.getLegacyName();
        } else {
            // Pre-1.21.11 - use legacy name
            return gameRule.getLegacyName();
        }
    }

    @Override
    @NotNull
    public Set<GameRule<?>> getAvailable() {
        Set<GameRule<?>> available = new HashSet<>();
        for (GameRule<?> rule : ALL_RULES) {
            if (exists(rule)) {
                available.add(rule);
            }
        }
        return Collections.unmodifiableSet(available);
    }

    @Override
    @NotNull
    public Map<GameRule<?>, Object> getAll(@NotNull World world) {
        Map<GameRule<?>, Object> values = new LinkedHashMap<>();
        for (GameRule<?> rule : ALL_RULES) {
            if (exists(rule)) {
                values.put(rule, getValue(world, rule));
            }
        }
        return Collections.unmodifiableMap(values);
    }

    @Override
    public void copyAll(@NotNull World source, @NotNull World destination) {
        for (GameRule<?> rule : ALL_RULES) {
            if (exists(rule)) {
                Object value = getValue(source, rule);
                setValueUnchecked(destination, rule, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void setValueUnchecked(World world, GameRule<T> rule, Object value) {
        setValue(world, rule, (T) value);
    }

    @Override
    @Nullable
    public GameRule<?> byName(@NotNull String name) {
        // Try both naming conventions
        GameRule<?> rule = BY_LEGACY_NAME.get(name);
        if (rule == null) {
            rule = BY_MODERN_NAME.get(name);
        }
        return rule;
    }

    @Override
    public boolean usesRegistryGameRules() {
        return useModernNames;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
