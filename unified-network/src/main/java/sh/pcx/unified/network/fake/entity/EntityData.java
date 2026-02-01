/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.fake.entity;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Type-safe keys for entity metadata.
 *
 * <p>EntityData provides strongly-typed access to entity metadata values,
 * ensuring type safety when reading and writing metadata.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Using built-in keys
 * metadata.set(EntityData.CUSTOM_NAME, Component.text("Name"));
 * metadata.set(EntityData.INVISIBLE, true);
 *
 * // Reading values
 * Optional<Component> name = metadata.get(EntityData.CUSTOM_NAME);
 * boolean invisible = metadata.getOrDefault(EntityData.INVISIBLE, false);
 * }</pre>
 *
 * @param <T> the value type for this data key
 * @since 1.0.0
 * @author Supatuck
 * @see EntityMetadata
 */
public final class EntityData<T> {

    // =========================================================================
    // Base Entity Flags
    // =========================================================================

    /** Whether the entity is on fire. */
    public static final EntityData<Boolean> ON_FIRE = new EntityData<>("on_fire", Boolean.class, 0, 0x01);

    /** Whether the entity is sneaking. */
    public static final EntityData<Boolean> SNEAKING = new EntityData<>("sneaking", Boolean.class, 0, 0x02);

    /** Whether the entity is sprinting. */
    public static final EntityData<Boolean> SPRINTING = new EntityData<>("sprinting", Boolean.class, 0, 0x08);

    /** Whether the entity is swimming. */
    public static final EntityData<Boolean> SWIMMING = new EntityData<>("swimming", Boolean.class, 0, 0x10);

    /** Whether the entity is invisible. */
    public static final EntityData<Boolean> INVISIBLE = new EntityData<>("invisible", Boolean.class, 0, 0x20);

    /** Whether the entity is glowing. */
    public static final EntityData<Boolean> GLOWING = new EntityData<>("glowing", Boolean.class, 0, 0x40);

    /** Whether the entity is flying with elytra. */
    public static final EntityData<Boolean> ELYTRA_FLYING = new EntityData<>("elytra_flying", Boolean.class, 0, 0x80);

    // =========================================================================
    // Air & Health
    // =========================================================================

    /** The entity's remaining air ticks. */
    public static final EntityData<Integer> AIR_TICKS = new EntityData<>("air_ticks", Integer.class, 1);

    /** The entity's custom name. */
    public static final EntityData<Component> CUSTOM_NAME = new EntityData<>("custom_name", Component.class, 2);

    /** Whether the custom name is visible. */
    public static final EntityData<Boolean> CUSTOM_NAME_VISIBLE = new EntityData<>("custom_name_visible", Boolean.class, 3);

    /** Whether the entity is silent. */
    public static final EntityData<Boolean> SILENT = new EntityData<>("silent", Boolean.class, 4);

    /** Whether the entity has no gravity. */
    public static final EntityData<Boolean> NO_GRAVITY = new EntityData<>("no_gravity", Boolean.class, 5);

    /** The entity's pose. */
    public static final EntityData<Pose> POSE = new EntityData<>("pose", Pose.class, 6);

    /** Frozen ticks (for powder snow). */
    public static final EntityData<Integer> FROZEN_TICKS = new EntityData<>("frozen_ticks", Integer.class, 7);

    // =========================================================================
    // Living Entity
    // =========================================================================

    /** Living entity hand states (active hand, etc). */
    public static final EntityData<Byte> HAND_STATES = new EntityData<>("hand_states", Byte.class, 8);

    /** Living entity health. */
    public static final EntityData<Float> HEALTH = new EntityData<>("health", Float.class, 9);

    /** Potion effect color. */
    public static final EntityData<Integer> POTION_EFFECT_COLOR = new EntityData<>("potion_effect_color", Integer.class, 10);

    /** Whether potion effects are ambient. */
    public static final EntityData<Boolean> POTION_EFFECT_AMBIENT = new EntityData<>("potion_effect_ambient", Boolean.class, 11);

    /** Number of arrows stuck in entity. */
    public static final EntityData<Integer> ARROWS_IN_BODY = new EntityData<>("arrows_in_body", Integer.class, 12);

    /** Number of bee stingers in entity. */
    public static final EntityData<Integer> BEE_STINGERS = new EntityData<>("bee_stingers", Integer.class, 13);

    /** Location of bed being used. */
    public static final EntityData<Object> BED_LOCATION = new EntityData<>("bed_location", Object.class, 14);

    // =========================================================================
    // Armor Stand
    // =========================================================================

    /** Armor stand flags (small, arms, etc). */
    public static final EntityData<Byte> ARMOR_STAND_FLAGS = new EntityData<>("armor_stand_flags", Byte.class, 15);

    /** Whether armor stand is small. */
    public static final EntityData<Boolean> ARMOR_STAND_SMALL = new EntityData<>("armor_stand_small", Boolean.class, 15, 0x01);

    /** Whether armor stand has arms. */
    public static final EntityData<Boolean> ARMOR_STAND_ARMS = new EntityData<>("armor_stand_arms", Boolean.class, 15, 0x04);

    /** Whether armor stand has no base plate. */
    public static final EntityData<Boolean> ARMOR_STAND_NO_BASE = new EntityData<>("armor_stand_no_base", Boolean.class, 15, 0x08);

    /** Whether armor stand is a marker. */
    public static final EntityData<Boolean> ARMOR_STAND_MARKER = new EntityData<>("armor_stand_marker", Boolean.class, 15, 0x10);

    /** Armor stand head rotation. */
    public static final EntityData<Object> ARMOR_STAND_HEAD_ROTATION = new EntityData<>("armor_stand_head_rotation", Object.class, 16);

    /** Armor stand body rotation. */
    public static final EntityData<Object> ARMOR_STAND_BODY_ROTATION = new EntityData<>("armor_stand_body_rotation", Object.class, 17);

    /** Armor stand left arm rotation. */
    public static final EntityData<Object> ARMOR_STAND_LEFT_ARM_ROTATION = new EntityData<>("armor_stand_left_arm_rotation", Object.class, 18);

    /** Armor stand right arm rotation. */
    public static final EntityData<Object> ARMOR_STAND_RIGHT_ARM_ROTATION = new EntityData<>("armor_stand_right_arm_rotation", Object.class, 19);

    /** Armor stand left leg rotation. */
    public static final EntityData<Object> ARMOR_STAND_LEFT_LEG_ROTATION = new EntityData<>("armor_stand_left_leg_rotation", Object.class, 20);

    /** Armor stand right leg rotation. */
    public static final EntityData<Object> ARMOR_STAND_RIGHT_LEG_ROTATION = new EntityData<>("armor_stand_right_leg_rotation", Object.class, 21);

    // =========================================================================
    // Player Specific
    // =========================================================================

    /** Player additional hearts (absorption). */
    public static final EntityData<Float> ABSORPTION_HEARTS = new EntityData<>("absorption_hearts", Float.class, 15);

    /** Player score. */
    public static final EntityData<Integer> SCORE = new EntityData<>("score", Integer.class, 16);

    /** Player skin parts displayed. */
    public static final EntityData<Byte> SKIN_PARTS = new EntityData<>("skin_parts", Byte.class, 17);

    /** Player main hand (0 = left, 1 = right). */
    public static final EntityData<Byte> MAIN_HAND = new EntityData<>("main_hand", Byte.class, 18);

    // =========================================================================
    // Instance Fields
    // =========================================================================

    private final String name;
    private final Class<T> type;
    private final int index;
    private final int bitmask;

    private EntityData(String name, Class<T> type, int index) {
        this(name, type, index, 0);
    }

    private EntityData(String name, Class<T> type, int index, int bitmask) {
        this.name = name;
        this.type = type;
        this.index = index;
        this.bitmask = bitmask;
    }

    /**
     * Returns the name of this data key.
     *
     * @return the key name
     * @since 1.0.0
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the value type.
     *
     * @return the type class
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getType() {
        return type;
    }

    /**
     * Returns the metadata index.
     *
     * @return the index
     * @since 1.0.0
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the bitmask for flag values.
     *
     * @return the bitmask, or 0 if not a flag
     * @since 1.0.0
     */
    public int getBitmask() {
        return bitmask;
    }

    /**
     * Checks if this is a flag value.
     *
     * @return true if this uses a bitmask
     * @since 1.0.0
     */
    public boolean isFlag() {
        return bitmask != 0;
    }

    @Override
    public String toString() {
        return "EntityData{name='" + name + "', type=" + type.getSimpleName() + ", index=" + index + "}";
    }

    /**
     * Entity pose enumeration.
     *
     * @since 1.0.0
     */
    public enum Pose {
        STANDING,
        FALL_FLYING,
        SLEEPING,
        SWIMMING,
        SPIN_ATTACK,
        SNEAKING,
        LONG_JUMPING,
        DYING,
        CROAKING,
        USING_TONGUE,
        SITTING,
        ROARING,
        SNIFFING,
        EMERGING,
        DIGGING,
        SLIDING,
        SHOOTING,
        INHALING
    }
}
