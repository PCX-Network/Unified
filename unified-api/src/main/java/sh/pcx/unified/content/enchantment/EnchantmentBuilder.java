/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.enchantment;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * Fluent builder for creating custom enchantments.
 *
 * <p>EnchantmentBuilder provides a chainable API for configuring all aspects
 * of a custom enchantment, including display properties, targeting, conflicts,
 * effects, and trigger handlers.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CustomEnchantment lifesteal = enchantments.register("myplugin:lifesteal")
 *     .displayName(Component.text("Lifesteal", NamedTextColor.RED))
 *     .description("Heal for a percentage of damage dealt")
 *     .maxLevel(5)
 *     .rarity(EnchantmentRarity.RARE)
 *     .target(EnchantmentTarget.WEAPON)
 *     .treasure(false)
 *     .tradeable(true)
 *     .discoverable(true)
 *     .conflictsWith("myplugin:vampirism", "minecraft:mending")
 *     .cooldown(Duration.ofSeconds(5))
 *     .chance(level -> 0.1 + (0.05 * level))
 *     .onHit((context, level) -> {
 *         // Handle hit event
 *     })
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomEnchantment
 * @see EnchantmentService
 */
public interface EnchantmentBuilder {

    /**
     * Sets the display name shown in item lore.
     *
     * @param name the display name component
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder displayName(@NotNull Component name);

    /**
     * Sets a level-dependent display name function.
     *
     * <p>This allows different colors or formatting per level.
     *
     * @param nameFunction function that takes level and returns the display name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder displayName(@NotNull IntFunction<Component> nameFunction);

    /**
     * Sets the enchantment description.
     *
     * @param description the description text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder description(@NotNull String description);

    /**
     * Sets the maximum enchantment level.
     *
     * @param maxLevel the maximum level (minimum 1)
     * @return this builder
     * @throws IllegalArgumentException if maxLevel is less than 1
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder maxLevel(int maxLevel);

    /**
     * Sets the starting enchantment level.
     *
     * @param startLevel the starting level (default 1)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder startLevel(int startLevel);

    /**
     * Sets the enchantment rarity.
     *
     * @param rarity the rarity level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder rarity(@NotNull EnchantmentRarity rarity);

    /**
     * Sets the enchantment target (which items can have this enchantment).
     *
     * @param target the enchantment target
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder target(@NotNull EnchantmentTarget target);

    /**
     * Sets whether this is a treasure enchantment.
     *
     * <p>Treasure enchantments cannot be obtained from enchanting tables
     * but can appear in loot and trades.
     *
     * @param treasure true for treasure enchantment
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder treasure(boolean treasure);

    /**
     * Sets whether villagers can trade this enchantment.
     *
     * @param tradeable true if tradeable
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder tradeable(boolean tradeable);

    /**
     * Sets whether this enchantment can appear in loot.
     *
     * @param discoverable true if discoverable in loot
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder discoverable(boolean discoverable);

    /**
     * Sets whether this enchantment is a curse.
     *
     * @param curse true for curse enchantment
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder curse(boolean curse);

    /**
     * Adds conflicting enchantments by key.
     *
     * <p>Conflicting enchantments cannot be combined on the same item.
     *
     * @param keys the conflicting enchantment keys
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder conflictsWith(@NotNull String... keys);

    /**
     * Sets the cooldown between enchantment activations.
     *
     * @param cooldown the cooldown duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder cooldown(@NotNull Duration cooldown);

    /**
     * Sets the activation chance function.
     *
     * @param chanceFunction function that takes level and returns chance (0.0-1.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder chance(@NotNull IntFunction<Double> chanceFunction);

    /**
     * Sets a fixed activation chance for all levels.
     *
     * @param chance the activation chance (0.0-1.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default EnchantmentBuilder chance(double chance) {
        return chance(level -> chance);
    }

    /**
     * Sets the tick interval for passive effects.
     *
     * <p>Used with {@link #whileEquipped(BiConsumer)} to define how often
     * the passive effect is applied.
     *
     * @param ticks the tick interval
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder tickInterval(int ticks);

    /**
     * Sets a handler for when the enchanted item hits an entity.
     *
     * @param handler the hit handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onHit(@NotNull BiConsumer<EnchantmentContext.Hit, Integer> handler);

    /**
     * Sets a handler for when the holder takes damage.
     *
     * @param handler the damage handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onDamage(@NotNull BiConsumer<EnchantmentContext.Damage, Integer> handler);

    /**
     * Sets a handler for when the holder breaks a block.
     *
     * @param handler the block break handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onBlockBreak(@NotNull BiConsumer<EnchantmentContext.BlockBreak, Integer> handler);

    /**
     * Sets a handler for when the holder gains experience.
     *
     * @param handler the experience handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onExperienceGain(@NotNull BiConsumer<EnchantmentContext.Experience, Integer> handler);

    /**
     * Sets a handler for when the enchanted item is used.
     *
     * @param handler the item use handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onItemUse(@NotNull BiConsumer<EnchantmentContext.ItemUse, Integer> handler);

    /**
     * Sets a handler for passive effects while the item is equipped.
     *
     * <p>Combine with {@link #tickInterval(int)} to control frequency.
     *
     * @param handler the equipped handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder whileEquipped(@NotNull BiConsumer<EnchantmentContext.Equipped, Integer> handler);

    /**
     * Sets a handler for when an arrow is shot.
     *
     * @param handler the shoot handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onShoot(@NotNull BiConsumer<EnchantmentContext.Shoot, Integer> handler);

    /**
     * Sets a handler for when an arrow hits a target.
     *
     * @param handler the arrow hit handler receiving (context, level)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder onArrowHit(@NotNull BiConsumer<EnchantmentContext.ArrowHit, Integer> handler);

    /**
     * Sets a custom lore format function.
     *
     * @param formatter function that takes (name, level) and returns formatted lore
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    EnchantmentBuilder loreFormat(@NotNull LoreFormatter formatter);

    /**
     * Registers the enchantment and returns it.
     *
     * @return the registered CustomEnchantment
     * @throws IllegalStateException if required properties are missing
     * @since 1.0.0
     */
    @NotNull
    CustomEnchantment register();

    /**
     * Functional interface for formatting enchantment lore.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface LoreFormatter {
        /**
         * Formats the enchantment name and level for item lore.
         *
         * @param name  the enchantment display name
         * @param level the enchantment level
         * @return the formatted lore component
         */
        @NotNull
        Component format(@NotNull Component name, int level);
    }
}
