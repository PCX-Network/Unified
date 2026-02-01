/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Represents a custom trigger for advancement criteria.
 *
 * <p>CustomTrigger allows creating plugin-specific triggers that can be
 * fired programmatically when certain conditions are met.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a custom trigger
 * CustomTrigger bossKilled = advancements.createTrigger("myplugin:boss_killed");
 *
 * // Use in advancement
 * CustomAdvancement dragonSlayer = advancements.create("myplugin:dragon_slayer")
 *     .criteria("kill_boss", bossKilled.matching(data ->
 *         "dragon".equals(data.getString("boss_type"))))
 *     .register();
 *
 * // Fire the trigger when boss is killed
 * public void onBossKill(Player player, String bossType) {
 *     bossKilled.trigger(player, TriggerData.of("boss_type", bossType));
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AdvancementService
 * @see Trigger
 */
public interface CustomTrigger extends Trigger {

    /**
     * Returns the trigger key.
     *
     * @return the namespaced key (e.g., "myplugin:boss_killed")
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Creates a trigger instance with a data matcher.
     *
     * @param predicate the predicate to match trigger data
     * @return a Trigger for use in advancement criteria
     * @since 1.0.0
     */
    @NotNull
    Trigger matching(@NotNull Predicate<TriggerData> predicate);

    /**
     * Creates a trigger instance that matches any data.
     *
     * @return a Trigger for use in advancement criteria
     * @since 1.0.0
     */
    @NotNull
    default Trigger any() {
        return matching(unused -> true);
    }

    /**
     * Fires this trigger for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void trigger(@NotNull UnifiedPlayer player);

    /**
     * Fires this trigger for a player with data.
     *
     * @param player the player
     * @param data   the trigger data
     * @since 1.0.0
     */
    void trigger(@NotNull UnifiedPlayer player, @NotNull TriggerData data);

    @Override
    @NotNull
    default String getType() {
        return getKey();
    }
}
