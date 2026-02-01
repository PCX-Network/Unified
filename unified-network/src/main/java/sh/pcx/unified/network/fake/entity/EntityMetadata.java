/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.fake.entity;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Interface for accessing and modifying entity metadata.
 *
 * <p>Entity metadata controls various visual properties of entities
 * such as visibility, custom names, poses, and entity-specific options.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * fakeEntity.updateMetadata(meta -> {
 *     meta.set(EntityData.CUSTOM_NAME, Component.text("Hello World"));
 *     meta.set(EntityData.CUSTOM_NAME_VISIBLE, true);
 *     meta.set(EntityData.INVISIBLE, true);
 *     meta.set(EntityData.NO_GRAVITY, true);
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FakeEntity
 * @see EntityData
 */
public interface EntityMetadata {

    /**
     * Sets a metadata value.
     *
     * @param <T>  the value type
     * @param key  the metadata key
     * @param value the value to set
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    <T> EntityMetadata set(@NotNull EntityData<T> key, @Nullable T value);

    /**
     * Gets a metadata value.
     *
     * @param <T>  the value type
     * @param key  the metadata key
     * @return the value, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    <T> Optional<T> get(@NotNull EntityData<T> key);

    /**
     * Gets a metadata value with a default.
     *
     * @param <T>          the value type
     * @param key          the metadata key
     * @param defaultValue the default value
     * @return the value or default
     * @since 1.0.0
     */
    @NotNull
    default <T> T getOrDefault(@NotNull EntityData<T> key, @NotNull T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Removes a metadata value.
     *
     * @param key the metadata key
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    EntityMetadata remove(@NotNull EntityData<?> key);

    /**
     * Checks if a metadata key is set.
     *
     * @param key the metadata key
     * @return true if set
     * @since 1.0.0
     */
    boolean has(@NotNull EntityData<?> key);

    /**
     * Clears all metadata.
     *
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    EntityMetadata clear();

    /**
     * Sets the custom name.
     *
     * @param name the custom name
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata customName(@Nullable Component name) {
        return set(EntityData.CUSTOM_NAME, name);
    }

    /**
     * Sets custom name visibility.
     *
     * @param visible true to show
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata customNameVisible(boolean visible) {
        return set(EntityData.CUSTOM_NAME_VISIBLE, visible);
    }

    /**
     * Sets invisibility.
     *
     * @param invisible true to make invisible
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata invisible(boolean invisible) {
        return set(EntityData.INVISIBLE, invisible);
    }

    /**
     * Sets glowing effect.
     *
     * @param glowing true to glow
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata glowing(boolean glowing) {
        return set(EntityData.GLOWING, glowing);
    }

    /**
     * Sets on fire status.
     *
     * @param onFire true to set on fire
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata onFire(boolean onFire) {
        return set(EntityData.ON_FIRE, onFire);
    }

    /**
     * Sets sneaking pose.
     *
     * @param sneaking true to sneak
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata sneaking(boolean sneaking) {
        return set(EntityData.SNEAKING, sneaking);
    }

    /**
     * Sets silent mode.
     *
     * @param silent true to make silent
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata silent(boolean silent) {
        return set(EntityData.SILENT, silent);
    }

    /**
     * Sets no gravity.
     *
     * @param noGravity true to disable gravity
     * @return this metadata for chaining
     * @since 1.0.0
     */
    @NotNull
    default EntityMetadata noGravity(boolean noGravity) {
        return set(EntityData.NO_GRAVITY, noGravity);
    }
}
