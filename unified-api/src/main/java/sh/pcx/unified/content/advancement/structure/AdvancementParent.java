/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement.structure;

import sh.pcx.unified.content.advancement.CustomAdvancement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a reference to a parent advancement in the advancement tree.
 *
 * <p>AdvancementParent can reference either a custom advancement created through
 * this API or a vanilla Minecraft advancement by its namespaced key. This allows
 * custom advancements to be integrated into existing advancement trees.
 *
 * <h2>Parent Types</h2>
 * <ul>
 *   <li><b>Custom Parent</b> - Reference to a {@link CustomAdvancement} object</li>
 *   <li><b>Vanilla Parent</b> - Reference to a vanilla advancement by key</li>
 *   <li><b>Root</b> - No parent (this advancement is a tree root)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Reference a custom advancement
 * CustomAdvancement parent = advancementService.create("myplugin:getting_started").build();
 * AdvancementParent customParent = AdvancementParent.of(parent);
 *
 * // Reference a vanilla advancement
 * AdvancementParent vanillaParent = AdvancementParent.ofVanilla("minecraft:story/iron_tools");
 *
 * // Create a root advancement (no parent)
 * AdvancementParent root = AdvancementParent.root();
 *
 * // Use in advancement builder
 * CustomAdvancement child = advancementService.create("myplugin:next_step")
 *     .parent(customParent)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomAdvancement
 */
public final class AdvancementParent {

    private static final AdvancementParent ROOT = new AdvancementParent(null, null);

    private final CustomAdvancement customParent;
    private final String vanillaKey;

    /**
     * Private constructor - use factory methods to create instances.
     */
    private AdvancementParent(@Nullable CustomAdvancement customParent, @Nullable String vanillaKey) {
        this.customParent = customParent;
        this.vanillaKey = vanillaKey;
    }

    /**
     * Creates a parent reference from a custom advancement.
     *
     * @param advancement the parent advancement
     * @return an AdvancementParent referencing the custom advancement
     * @throws NullPointerException if advancement is null
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementParent of(@NotNull CustomAdvancement advancement) {
        Objects.requireNonNull(advancement, "Advancement cannot be null");
        return new AdvancementParent(advancement, null);
    }

    /**
     * Creates a parent reference from a vanilla advancement key.
     *
     * <p>The key should be a fully qualified namespaced key, such as
     * "minecraft:story/iron_tools" or "minecraft:nether/find_fortress".
     *
     * @param key the vanilla advancement key
     * @return an AdvancementParent referencing the vanilla advancement
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if key is empty
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementParent ofVanilla(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        return new AdvancementParent(null, key);
    }

    /**
     * Creates a parent reference for a root advancement (no parent).
     *
     * <p>Root advancements are displayed at the top level of an advancement
     * tab and typically have a background texture set in their display.
     *
     * @return an AdvancementParent representing no parent (root)
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementParent root() {
        return ROOT;
    }

    /**
     * Checks if this is a root advancement (has no parent).
     *
     * @return true if this represents a root advancement
     * @since 1.0.0
     */
    public boolean isRoot() {
        return customParent == null && vanillaKey == null;
    }

    /**
     * Checks if this references a custom advancement.
     *
     * @return true if this references a custom advancement
     * @since 1.0.0
     */
    public boolean isCustom() {
        return customParent != null;
    }

    /**
     * Checks if this references a vanilla advancement.
     *
     * @return true if this references a vanilla advancement by key
     * @since 1.0.0
     */
    public boolean isVanilla() {
        return vanillaKey != null;
    }

    /**
     * Returns the custom parent advancement, if this references one.
     *
     * @return an Optional containing the custom advancement if present
     * @since 1.0.0
     */
    @NotNull
    public Optional<CustomAdvancement> getCustomParent() {
        return Optional.ofNullable(customParent);
    }

    /**
     * Returns the vanilla advancement key, if this references one.
     *
     * @return an Optional containing the vanilla key if present
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getVanillaKey() {
        return Optional.ofNullable(vanillaKey);
    }

    /**
     * Returns the key of the parent advancement.
     *
     * <p>For custom advancements, returns the advancement's key.
     * For vanilla advancements, returns the vanilla key.
     * For root advancements, returns an empty Optional.
     *
     * @return an Optional containing the parent key if not root
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getKey() {
        if (customParent != null) {
            return Optional.of(customParent.getKey());
        }
        return Optional.ofNullable(vanillaKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdvancementParent that = (AdvancementParent) o;
        return Objects.equals(customParent, that.customParent) &&
               Objects.equals(vanillaKey, that.vanillaKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customParent, vanillaKey);
    }

    @Override
    public String toString() {
        if (isRoot()) {
            return "AdvancementParent{root}";
        } else if (isCustom()) {
            return "AdvancementParent{custom=" + customParent.getKey() + "}";
        } else {
            return "AdvancementParent{vanilla=" + vanillaKey + "}";
        }
    }
}
