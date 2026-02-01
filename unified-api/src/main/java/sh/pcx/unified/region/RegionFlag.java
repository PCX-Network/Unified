/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.region;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a typed flag that can be set on regions to control behavior.
 *
 * <p>Region flags are typed values that define what actions are allowed or
 * denied within a region. Each flag has a name, a value type, and a default
 * value that applies when the flag is not explicitly set.
 *
 * <h2>Built-in Flags</h2>
 * <p>Common flags are provided as constants:
 * <ul>
 *   <li>{@link #PVP} - Whether player vs player combat is allowed</li>
 *   <li>{@link #BUILD} - Whether block placement is allowed</li>
 *   <li>{@link #BREAK} - Whether block breaking is allowed</li>
 *   <li>{@link #INTERACT} - Whether block/entity interaction is allowed</li>
 *   <li>{@link #USE} - Whether item use is allowed</li>
 *   <li>{@link #ENTRY} - Whether players can enter the region</li>
 *   <li>{@link #EXIT} - Whether players can exit the region</li>
 *   <li>{@link #MOB_SPAWNING} - Whether mobs can spawn</li>
 *   <li>{@link #MOB_DAMAGE} - Whether mobs can deal damage</li>
 *   <li>{@link #HUNGER} - Whether hunger depletes</li>
 *   <li>{@link #FALL_DAMAGE} - Whether fall damage is applied</li>
 *   <li>{@link #FIRE_SPREAD} - Whether fire spreads</li>
 *   <li>{@link #EXPLOSIONS} - Whether explosions deal damage</li>
 * </ul>
 *
 * <h2>Custom Flags</h2>
 * <pre>{@code
 * // Define a custom integer flag
 * public static final RegionFlag<Integer> MAX_PLAYERS =
 *     RegionFlag.of("max-players", Integer.class, -1);
 *
 * // Define a custom string flag
 * public static final RegionFlag<String> GREETING_MESSAGE =
 *     RegionFlag.of("greeting", String.class, "Welcome!");
 *
 * // Use in region
 * Region arena = regions.cuboid("arena")
 *     .flag(MAX_PLAYERS, 10)
 *     .flag(GREETING_MESSAGE, "Welcome to the arena!")
 *     .create();
 *
 * // Query flag value
 * int maxPlayers = regions.queryFlag(location, MAX_PLAYERS, -1);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>RegionFlag instances are immutable and thread-safe. Flag registration
 * is handled by a concurrent registry.
 *
 * @param <T> the type of value this flag holds
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class RegionFlag<T> {

    private static final Map<String, RegionFlag<?>> REGISTRY = new ConcurrentHashMap<>();

    // =========================================================================
    // Built-in Boolean Flags
    // =========================================================================

    /** Whether PvP combat is allowed. Default: true */
    public static final RegionFlag<Boolean> PVP = of("pvp", Boolean.class, true);

    /** Whether block placement is allowed. Default: true */
    public static final RegionFlag<Boolean> BUILD = of("build", Boolean.class, true);

    /** Whether block breaking is allowed. Default: true */
    public static final RegionFlag<Boolean> BREAK = of("break", Boolean.class, true);

    /** Whether block/entity interaction is allowed. Default: true */
    public static final RegionFlag<Boolean> INTERACT = of("interact", Boolean.class, true);

    /** Whether item use is allowed. Default: true */
    public static final RegionFlag<Boolean> USE = of("use", Boolean.class, true);

    /** Whether players can enter the region. Default: true */
    public static final RegionFlag<Boolean> ENTRY = of("entry", Boolean.class, true);

    /** Whether players can exit the region. Default: true */
    public static final RegionFlag<Boolean> EXIT = of("exit", Boolean.class, true);

    /** Whether mobs can spawn. Default: true */
    public static final RegionFlag<Boolean> MOB_SPAWNING = of("mob-spawning", Boolean.class, true);

    /** Whether mobs can deal damage. Default: true */
    public static final RegionFlag<Boolean> MOB_DAMAGE = of("mob-damage", Boolean.class, true);

    /** Whether hunger depletes. Default: true */
    public static final RegionFlag<Boolean> HUNGER = of("hunger", Boolean.class, true);

    /** Whether fall damage is applied. Default: true */
    public static final RegionFlag<Boolean> FALL_DAMAGE = of("fall-damage", Boolean.class, true);

    /** Whether fire spreads. Default: true */
    public static final RegionFlag<Boolean> FIRE_SPREAD = of("fire-spread", Boolean.class, true);

    /** Whether explosions deal damage. Default: true */
    public static final RegionFlag<Boolean> EXPLOSIONS = of("explosions", Boolean.class, true);

    /** Whether TNT can be ignited. Default: true */
    public static final RegionFlag<Boolean> TNT = of("tnt", Boolean.class, true);

    /** Whether lava flow is allowed. Default: true */
    public static final RegionFlag<Boolean> LAVA_FLOW = of("lava-flow", Boolean.class, true);

    /** Whether water flow is allowed. Default: true */
    public static final RegionFlag<Boolean> WATER_FLOW = of("water-flow", Boolean.class, true);

    /** Whether pistons can operate. Default: true */
    public static final RegionFlag<Boolean> PISTONS = of("pistons", Boolean.class, true);

    /** Whether leaf decay occurs. Default: true */
    public static final RegionFlag<Boolean> LEAF_DECAY = of("leaf-decay", Boolean.class, true);

    /** Whether snow falls/melts. Default: true */
    public static final RegionFlag<Boolean> SNOW_FALL = of("snow-fall", Boolean.class, true);

    /** Whether ice forms/melts. Default: true */
    public static final RegionFlag<Boolean> ICE_FORM = of("ice-form", Boolean.class, true);

    /** Whether endermen can pick up blocks. Default: true */
    public static final RegionFlag<Boolean> ENDERMAN_GRIEF = of("enderman-grief", Boolean.class, true);

    /** Whether creepers can damage blocks. Default: true */
    public static final RegionFlag<Boolean> CREEPER_EXPLOSION = of("creeper-explosion", Boolean.class, true);

    /** Whether item drops are allowed. Default: true */
    public static final RegionFlag<Boolean> ITEM_DROP = of("item-drop", Boolean.class, true);

    /** Whether item pickup is allowed. Default: true */
    public static final RegionFlag<Boolean> ITEM_PICKUP = of("item-pickup", Boolean.class, true);

    /** Whether experience orbs spawn. Default: true */
    public static final RegionFlag<Boolean> EXP_DROP = of("exp-drop", Boolean.class, true);

    /** Whether vehicles can be placed/used. Default: true */
    public static final RegionFlag<Boolean> VEHICLE_USE = of("vehicle-use", Boolean.class, true);

    /** Whether sleeping is allowed. Default: true */
    public static final RegionFlag<Boolean> SLEEP = of("sleep", Boolean.class, true);

    /** Whether respawning is allowed in the region. Default: true */
    public static final RegionFlag<Boolean> RESPAWN = of("respawn", Boolean.class, true);

    /** Whether health regeneration is enabled. Default: true */
    public static final RegionFlag<Boolean> HEALTH_REGEN = of("health-regen", Boolean.class, true);

    /** Whether players are invincible. Default: false */
    public static final RegionFlag<Boolean> INVINCIBLE = of("invincible", Boolean.class, false);

    /** Whether players can fly. Default: false */
    public static final RegionFlag<Boolean> FLY = of("fly", Boolean.class, false);

    /** Whether commands can be used. Default: true */
    public static final RegionFlag<Boolean> COMMANDS = of("commands", Boolean.class, true);

    /** Whether chat messages are sent. Default: true */
    public static final RegionFlag<Boolean> SEND_CHAT = of("send-chat", Boolean.class, true);

    /** Whether chat messages are received. Default: true */
    public static final RegionFlag<Boolean> RECEIVE_CHAT = of("receive-chat", Boolean.class, true);

    // =========================================================================
    // Message Flags
    // =========================================================================

    /** Greeting message when entering. Default: empty */
    public static final RegionFlag<String> GREETING = of("greeting", String.class, "");

    /** Farewell message when exiting. Default: empty */
    public static final RegionFlag<String> FAREWELL = of("farewell", String.class, "");

    /** Deny message for blocked actions. Default: empty */
    public static final RegionFlag<String> DENY_MESSAGE = of("deny-message", String.class, "");

    // =========================================================================
    // Instance Fields
    // =========================================================================

    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    /**
     * Creates a new region flag.
     *
     * @param name         the unique flag name
     * @param type         the value type class
     * @param defaultValue the default value when not set
     */
    private RegionFlag(@NotNull String name, @NotNull Class<T> type, @Nullable T defaultValue) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.defaultValue = defaultValue;
    }

    /**
     * Creates and registers a new region flag.
     *
     * <p>If a flag with the same name already exists, an exception is thrown.
     *
     * @param name         the unique flag name
     * @param type         the value type class
     * @param defaultValue the default value when not set
     * @param <T>          the value type
     * @return the created flag
     * @throws IllegalArgumentException if a flag with the name already exists
     * @since 1.0.0
     */
    @NotNull
    public static <T> RegionFlag<T> of(
            @NotNull String name,
            @NotNull Class<T> type,
            @Nullable T defaultValue
    ) {
        var flag = new RegionFlag<>(name, type, defaultValue);
        var existing = REGISTRY.putIfAbsent(name.toLowerCase(), flag);
        if (existing != null && existing != flag) {
            throw new IllegalArgumentException("Flag already registered: " + name);
        }
        return flag;
    }

    /**
     * Gets a registered flag by name.
     *
     * @param name the flag name
     * @return the flag, or null if not registered
     * @since 1.0.0
     */
    @Nullable
    public static RegionFlag<?> getByName(@NotNull String name) {
        return REGISTRY.get(name.toLowerCase());
    }

    /**
     * Gets a registered flag by name with type checking.
     *
     * @param name the flag name
     * @param type the expected type
     * @param <T>  the value type
     * @return the flag, or null if not registered or wrong type
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> RegionFlag<T> getByName(@NotNull String name, @NotNull Class<T> type) {
        RegionFlag<?> flag = REGISTRY.get(name.toLowerCase());
        if (flag != null && flag.type.equals(type)) {
            return (RegionFlag<T>) flag;
        }
        return null;
    }

    /**
     * Returns all registered flags.
     *
     * @return an unmodifiable map of flag names to flags
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, RegionFlag<?>> getRegisteredFlags() {
        return Map.copyOf(REGISTRY);
    }

    /**
     * Returns the unique name of this flag.
     *
     * @return the flag name
     * @since 1.0.0
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the value type of this flag.
     *
     * @return the type class
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getType() {
        return type;
    }

    /**
     * Returns the default value of this flag.
     *
     * @return the default value, may be null
     * @since 1.0.0
     */
    @Nullable
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Casts a raw value to this flag's type.
     *
     * @param value the value to cast
     * @return the typed value, or default if null or wrong type
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T cast(@Nullable Object value) {
        if (value == null) {
            return defaultValue;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        // Attempt basic type conversions
        if (type == Boolean.class && value instanceof String s) {
            return (T) Boolean.valueOf(s);
        }
        if (type == Integer.class && value instanceof Number n) {
            return (T) Integer.valueOf(n.intValue());
        }
        if (type == Double.class && value instanceof Number n) {
            return (T) Double.valueOf(n.doubleValue());
        }
        if (type == String.class) {
            return (T) value.toString();
        }
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionFlag<?> that)) return false;
        return name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "RegionFlag[" + name + ", type=" + type.getSimpleName() + ", default=" + defaultValue + "]";
    }
}
