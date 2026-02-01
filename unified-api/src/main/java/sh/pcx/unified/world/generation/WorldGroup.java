/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a group of related worlds that can share data.
 *
 * <p>World groups allow multiple worlds to be treated as a single logical unit,
 * with options to share inventories, ender chests, and player data between
 * worlds in the group.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a world group for survival dimensions
 * WorldGroup survivalWorlds = worlds.getGroupManager()
 *     .createGroup("survival")
 *     .add("survival_overworld")
 *     .add("survival_nether")
 *     .add("survival_end")
 *     .sharedInventory(true)
 *     .sharedEnderChest(true)
 *     .sharedPlayerData(true)
 *     .build();
 *
 * // Check if a world is in the group
 * boolean isInGroup = survivalWorlds.contains("survival_nether");
 *
 * // Get all worlds in the group
 * Collection<UnifiedWorld> groupWorlds = survivalWorlds.getWorlds();
 *
 * // Teleport within group (inventory preserved)
 * player.teleport(survivalWorlds.getWorld("survival_nether").get().getSpawnLocation());
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldGroupManager
 * @see WorldService
 */
public interface WorldGroup {

    /**
     * Gets the name of this group.
     *
     * @return the group name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Gets all world names in this group.
     *
     * @return an unmodifiable collection of world names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getWorldNames();

    /**
     * Gets all loaded worlds in this group.
     *
     * @return an unmodifiable collection of loaded worlds
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedWorld> getWorlds();

    /**
     * Gets a specific world from this group by name.
     *
     * @param name the world name
     * @return the world, or empty if not in group or not loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull String name);

    /**
     * Checks if a world is in this group.
     *
     * @param name the world name
     * @return true if the world is in this group
     * @since 1.0.0
     */
    boolean contains(@NotNull String name);

    /**
     * Checks if a world is in this group.
     *
     * @param world the world
     * @return true if the world is in this group
     * @since 1.0.0
     */
    boolean contains(@NotNull UnifiedWorld world);

    /**
     * Gets the number of worlds in this group.
     *
     * @return the world count
     * @since 1.0.0
     */
    int size();

    /**
     * Checks if this group is empty.
     *
     * @return true if the group has no worlds
     * @since 1.0.0
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    // ==================== Shared Data Settings ====================

    /**
     * Checks if inventories are shared across worlds in this group.
     *
     * @return true if inventories are shared
     * @since 1.0.0
     */
    boolean hasSharedInventory();

    /**
     * Checks if ender chests are shared across worlds in this group.
     *
     * @return true if ender chests are shared
     * @since 1.0.0
     */
    boolean hasSharedEnderChest();

    /**
     * Checks if player data (health, hunger, etc.) is shared.
     *
     * @return true if player data is shared
     * @since 1.0.0
     */
    boolean hasSharedPlayerData();

    /**
     * Checks if experience is shared across worlds in this group.
     *
     * @return true if experience is shared
     * @since 1.0.0
     */
    boolean hasSharedExperience();

    /**
     * Checks if potion effects persist across worlds in this group.
     *
     * @return true if potion effects are shared
     * @since 1.0.0
     */
    boolean hasSharedPotionEffects();

    // ==================== Group Modification ====================

    /**
     * Adds a world to this group.
     *
     * @param worldName the world name to add
     * @return true if the world was added
     * @since 1.0.0
     */
    boolean addWorld(@NotNull String worldName);

    /**
     * Removes a world from this group.
     *
     * @param worldName the world name to remove
     * @return true if the world was removed
     * @since 1.0.0
     */
    boolean removeWorld(@NotNull String worldName);

    // ==================== Builder ====================

    /**
     * Builder for creating world groups.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Adds a world to the group.
         *
         * @param worldName the world name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder add(@NotNull String worldName);

        /**
         * Adds multiple worlds to the group.
         *
         * @param worldNames the world names
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder add(@NotNull String... worldNames);

        /**
         * Sets whether inventories are shared.
         *
         * @param shared true to share inventories
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder sharedInventory(boolean shared);

        /**
         * Sets whether ender chests are shared.
         *
         * @param shared true to share ender chests
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder sharedEnderChest(boolean shared);

        /**
         * Sets whether player data is shared.
         *
         * @param shared true to share player data
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder sharedPlayerData(boolean shared);

        /**
         * Sets whether experience is shared.
         *
         * @param shared true to share experience
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder sharedExperience(boolean shared);

        /**
         * Sets whether potion effects are shared.
         *
         * @param shared true to share potion effects
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder sharedPotionEffects(boolean shared);

        /**
         * Enables all sharing options.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        default Builder shareAll() {
            return sharedInventory(true)
                    .sharedEnderChest(true)
                    .sharedPlayerData(true)
                    .sharedExperience(true)
                    .sharedPotionEffects(true);
        }

        /**
         * Disables all sharing options.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        default Builder shareNone() {
            return sharedInventory(false)
                    .sharedEnderChest(false)
                    .sharedPlayerData(false)
                    .sharedExperience(false)
                    .sharedPotionEffects(false);
        }

        /**
         * Builds the world group.
         *
         * @return the built world group
         * @since 1.0.0
         */
        @NotNull
        WorldGroup build();
    }
}
