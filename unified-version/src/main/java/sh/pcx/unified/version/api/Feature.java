/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.api;

import sh.pcx.unified.server.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of version-gated features in Minecraft.
 *
 * <p>This enum defines features that were introduced or changed in specific Minecraft versions.
 * Use this with {@link VersionProvider#supports(Feature)} to check if a feature is available
 * on the current server version.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private VersionProvider version;
 *
 * public void doSomething() {
 *     if (version.supports(Feature.COMPONENT_ITEMS)) {
 *         // Use component-based items (1.20.5+)
 *         useComponentItems();
 *     } else {
 *         // Use legacy NBT-based items
 *         useLegacyItems();
 *     }
 *
 *     if (version.supports(Feature.REGISTRY_GAMERULES)) {
 *         // Use registry-based gamerules (1.21.11+)
 *         useRegistryGameRules();
 *     }
 * }
 * }</pre>
 *
 * <h2>Feature Matrix</h2>
 * <table>
 *   <tr><th>Feature</th><th>1.20.5</th><th>1.21.0</th><th>1.21.4</th><th>1.21.11</th></tr>
 *   <tr><td>COMPONENT_ITEMS</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>DATA_COMPONENTS</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>TRIAL_CHAMBERS</td><td>No</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>BUNDLES_FULL</td><td>No</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>HARDCORE_BANNER</td><td>No</td><td>No</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>PALE_GARDEN</td><td>No</td><td>No</td><td>Yes</td><td>Yes</td></tr>
 *   <tr><td>REGISTRY_GAMERULES</td><td>No</td><td>No</td><td>No</td><td>Yes</td></tr>
 *   <tr><td>MOJANG_MAPPINGS</td><td>No</td><td>No</td><td>No</td><td>Yes</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see VersionProvider#supports(Feature)
 */
public enum Feature {

    /**
     * Component-based items introduced in 1.20.5.
     *
     * <p>Items now use data components instead of NBT tags for storage.
     * This affects item serialization, comparison, and modification.
     */
    COMPONENT_ITEMS(MinecraftVersion.V1_20_5, "Component-based item system"),

    /**
     * Data components API for item stacks.
     *
     * <p>Allows direct manipulation of item data components without
     * going through NBT.
     */
    DATA_COMPONENTS(MinecraftVersion.V1_20_5, "Data component API"),

    /**
     * Trial Chambers structure introduced in 1.21.
     *
     * <p>New underground structure with trial spawners and vaults.
     */
    TRIAL_CHAMBERS(MinecraftVersion.V1_21, "Trial Chambers structure"),

    /**
     * Full bundle functionality in 1.21.
     *
     * <p>Bundles are fully implemented and functional.
     */
    BUNDLES_FULL(MinecraftVersion.V1_21, "Full bundle functionality"),

    /**
     * Breeze mob introduced in 1.21.
     *
     * <p>New hostile mob found in Trial Chambers.
     */
    BREEZE_MOB(MinecraftVersion.V1_21, "Breeze mob"),

    /**
     * Wind Charge item and entity in 1.21.
     *
     * <p>Projectile that creates wind bursts.
     */
    WIND_CHARGE(MinecraftVersion.V1_21, "Wind charge projectile"),

    /**
     * Mace weapon introduced in 1.21.
     *
     * <p>New weapon with smash attack mechanics.
     */
    MACE_WEAPON(MinecraftVersion.V1_21, "Mace weapon"),

    /**
     * Hardcore banner pattern in 1.21.4.
     *
     * <p>Special banner pattern for hardcore mode.
     */
    HARDCORE_BANNER(MinecraftVersion.V1_21_4, "Hardcore banner pattern"),

    /**
     * Pale Garden biome in 1.21.4.
     *
     * <p>New biome with pale oak trees and creaking mob.
     */
    PALE_GARDEN(MinecraftVersion.V1_21_4, "Pale Garden biome"),

    /**
     * Creaking mob in 1.21.4.
     *
     * <p>New hostile mob found in Pale Garden.
     */
    CREAKING_MOB(MinecraftVersion.V1_21_4, "Creaking mob"),

    /**
     * Pale Oak wood type in 1.21.4.
     *
     * <p>New wood type from the Pale Garden biome.
     */
    PALE_OAK(MinecraftVersion.V1_21_4, "Pale Oak wood type"),

    /**
     * Resin block and items in 1.21.4.
     *
     * <p>New decorative block from Pale Garden.
     */
    RESIN_BLOCKS(MinecraftVersion.V1_21_4, "Resin blocks"),

    /**
     * Registry-based gamerules in 1.21.11.
     *
     * <p>GameRules now use snake_case naming in registries.
     * Commands still use camelCase for backwards compatibility.
     */
    REGISTRY_GAMERULES(MinecraftVersion.V1_21_11, "Registry-based gamerule naming"),

    /**
     * Mojang mappings in Paper 1.21.11+.
     *
     * <p>Paper uses Mojang-mapped internals instead of Spigot mappings.
     * This affects NMS access patterns.
     */
    MOJANG_MAPPINGS(MinecraftVersion.V1_21_11, "Paper Mojang mappings"),

    /**
     * Improved entity tracking in 1.21.11.
     *
     * <p>Entity tracking has been optimized and some APIs changed.
     */
    IMPROVED_ENTITY_TRACKING(MinecraftVersion.V1_21_11, "Improved entity tracking"),

    /**
     * Attribute modifier improvements in 1.21.
     *
     * <p>Attribute modifiers use namespaced keys instead of UUIDs.
     */
    NAMESPACED_ATTRIBUTE_MODIFIERS(MinecraftVersion.V1_21, "Namespaced attribute modifiers"),

    /**
     * Enchantment registry changes in 1.21.
     *
     * <p>Enchantments moved to data-driven registry.
     */
    DATA_DRIVEN_ENCHANTMENTS(MinecraftVersion.V1_21, "Data-driven enchantments"),

    /**
     * Painting variant registry in 1.21.
     *
     * <p>Paintings use variant registry instead of hardcoded values.
     */
    PAINTING_VARIANTS(MinecraftVersion.V1_21, "Painting variant registry"),

    /**
     * Wolf variants in 1.21.
     *
     * <p>Wolves can have different appearances based on biome.
     */
    WOLF_VARIANTS(MinecraftVersion.V1_21, "Wolf biome variants");

    private final MinecraftVersion minimumVersion;
    private final String description;

    /**
     * Constructs a feature with its minimum required version.
     *
     * @param minimumVersion the minimum Minecraft version required
     * @param description    a human-readable description of the feature
     */
    Feature(@NotNull MinecraftVersion minimumVersion, @NotNull String description) {
        this.minimumVersion = minimumVersion;
        this.description = description;
    }

    /**
     * Returns the minimum Minecraft version required for this feature.
     *
     * @return the minimum required version
     * @since 1.0.0
     */
    @NotNull
    public MinecraftVersion getMinimumVersion() {
        return minimumVersion;
    }

    /**
     * Returns a human-readable description of this feature.
     *
     * @return the feature description
     * @since 1.0.0
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this feature is available on the specified version.
     *
     * @param version the Minecraft version to check
     * @return true if the feature is available
     * @since 1.0.0
     */
    public boolean isAvailableOn(@NotNull MinecraftVersion version) {
        return version.isAtLeast(minimumVersion);
    }

    /**
     * Returns a string representation of this feature.
     *
     * @return string in format "FEATURE_NAME (1.x.x+): description"
     */
    @Override
    public String toString() {
        return name() + " (" + minimumVersion + "+): " + description;
    }
}
