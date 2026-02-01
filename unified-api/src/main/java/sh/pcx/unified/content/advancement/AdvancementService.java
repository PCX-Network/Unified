/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Service for creating and managing custom advancements.
 *
 * <p>AdvancementService provides a fluent API for defining custom advancements
 * with full GUI integration, triggers, rewards, progress tracking, and
 * persistence across restarts.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Custom Advancements</b> - Define new achievements with display</li>
 *   <li><b>Advancement Trees</b> - Parent/child relationships</li>
 *   <li><b>Triggers</b> - Built-in and custom trigger types</li>
 *   <li><b>Rewards</b> - XP, items, commands, and custom rewards</li>
 *   <li><b>Progress</b> - Multi-step advancements with tracking</li>
 *   <li><b>Hidden</b> - Secret advancements until unlocked</li>
 *   <li><b>Persistence</b> - Saved with player data</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private AdvancementService advancements;
 *
 * // Create root advancement (tab)
 * CustomAdvancement root = advancements.create("myplugin:root")
 *     .display(AdvancementDisplay.builder()
 *         .icon("minecraft:nether_star")
 *         .title(Component.text("My Plugin Adventures"))
 *         .description(Component.text("Begin your journey"))
 *         .background("minecraft:textures/gui/advancements/backgrounds/stone.png")
 *         .build())
 *     .criteria("join", Trigger.impossible())
 *     .register();
 *
 * // Create child advancement
 * CustomAdvancement firstKill = advancements.create("myplugin:first_kill")
 *     .parent(root)
 *     .display(AdvancementDisplay.builder()
 *         .icon("minecraft:iron_sword")
 *         .title(Component.text("First Blood"))
 *         .description(Component.text("Kill your first monster"))
 *         .frame(AdvancementFrame.TASK)
 *         .build())
 *     .criteria("kill", Trigger.playerKilledEntity(
 *         EntityPredicate.type("minecraft:zombie")))
 *     .reward(AdvancementReward.builder()
 *         .experience(50)
 *         .build())
 *     .register();
 *
 * // Grant advancement
 * advancements.grant(player, firstKill);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomAdvancement
 * @see AdvancementBuilder
 */
public interface AdvancementService extends Service {

    /**
     * Creates a new advancement builder with the specified key.
     *
     * @param key the unique advancement key (e.g., "myplugin:first_kill")
     * @return a new AdvancementBuilder
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if key format is invalid
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder create(@NotNull String key);

    /**
     * Gets a registered custom advancement by key.
     *
     * @param key the advancement key
     * @return an Optional containing the advancement if found
     * @since 1.0.0
     */
    @NotNull
    Optional<CustomAdvancement> get(@NotNull String key);

    /**
     * Returns all registered custom advancements.
     *
     * @return an unmodifiable collection of advancements
     * @since 1.0.0
     */
    @NotNull
    Collection<CustomAdvancement> getAll();

    /**
     * Unregisters a custom advancement.
     *
     * @param key the advancement key
     * @return true if the advancement was unregistered
     * @since 1.0.0
     */
    boolean unregister(@NotNull String key);

    /**
     * Creates a custom trigger for use in advancements.
     *
     * @param key the trigger key (e.g., "myplugin:boss_killed")
     * @return a new CustomTrigger
     * @since 1.0.0
     */
    @NotNull
    CustomTrigger createTrigger(@NotNull String key);

    /**
     * Grants an advancement to a player.
     *
     * @param player      the player
     * @param advancement the advancement to grant
     * @return true if the advancement was newly granted
     * @since 1.0.0
     */
    boolean grant(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement);

    /**
     * Grants an advancement by key.
     *
     * @param player the player
     * @param key    the advancement key
     * @return true if the advancement was newly granted
     * @since 1.0.0
     */
    boolean grant(@NotNull UnifiedPlayer player, @NotNull String key);

    /**
     * Grants a specific criterion of an advancement.
     *
     * @param player      the player
     * @param advancement the advancement
     * @param criterion   the criterion name
     * @return true if the criterion was newly completed
     * @since 1.0.0
     */
    boolean grantCriteria(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                          @NotNull String criterion);

    /**
     * Revokes an advancement from a player.
     *
     * @param player      the player
     * @param advancement the advancement to revoke
     * @return true if the advancement was revoked
     * @since 1.0.0
     */
    boolean revoke(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement);

    /**
     * Revokes an advancement by key.
     *
     * @param player the player
     * @param key    the advancement key
     * @return true if the advancement was revoked
     * @since 1.0.0
     */
    boolean revoke(@NotNull UnifiedPlayer player, @NotNull String key);

    /**
     * Revokes all advancements in a tree (from a root advancement).
     *
     * @param player the player
     * @param root   the root advancement
     * @return the number of advancements revoked
     * @since 1.0.0
     */
    int revokeAll(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement root);

    /**
     * Checks if a player has completed an advancement.
     *
     * @param player      the player
     * @param advancement the advancement
     * @return true if the player has completed the advancement
     * @since 1.0.0
     */
    boolean has(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement);

    /**
     * Checks if a player has completed an advancement by key.
     *
     * @param player the player
     * @param key    the advancement key
     * @return true if the player has completed the advancement
     * @since 1.0.0
     */
    boolean has(@NotNull UnifiedPlayer player, @NotNull String key);

    /**
     * Gets the progress of a criterion for a player.
     *
     * @param player      the player
     * @param advancement the advancement
     * @param criterion   the criterion name
     * @return the current progress count
     * @since 1.0.0
     */
    int getProgress(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                    @NotNull String criterion);

    /**
     * Gets the required count for a criterion.
     *
     * @param advancement the advancement
     * @param criterion   the criterion name
     * @return the required count
     * @since 1.0.0
     */
    int getRequired(@NotNull CustomAdvancement advancement, @NotNull String criterion);

    /**
     * Sets the progress of a criterion for a player.
     *
     * @param player      the player
     * @param advancement the advancement
     * @param criterion   the criterion name
     * @param progress    the new progress value
     * @since 1.0.0
     */
    void setProgress(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                     @NotNull String criterion, int progress);

    /**
     * Increments the progress of a criterion for a player.
     *
     * @param player      the player
     * @param advancement the advancement
     * @param criterion   the criterion name
     * @param amount      the amount to increment
     * @return the new progress value
     * @since 1.0.0
     */
    int incrementProgress(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                          @NotNull String criterion, int amount);

    /**
     * Gets all completed advancements for a player.
     *
     * @param player the player
     * @return an unmodifiable set of completed advancements
     * @since 1.0.0
     */
    @NotNull
    Set<CustomAdvancement> getCompleted(@NotNull UnifiedPlayer player);

    /**
     * Gets all advancements in progress for a player.
     *
     * @param player the player
     * @return an unmodifiable set of in-progress advancements
     * @since 1.0.0
     */
    @NotNull
    Set<CustomAdvancement> getInProgress(@NotNull UnifiedPlayer player);

    /**
     * Reloads all custom advancements.
     *
     * <p>Re-registers all advancements with the server. Use after
     * modifying advancement definitions.
     *
     * @since 1.0.0
     */
    void reload();
}
