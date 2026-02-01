/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.item;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder interface for creating {@link UnifiedItemStack} instances.
 *
 * <p>This interface provides a clean, chainable API for constructing items
 * with various properties. Implementations handle platform-specific details
 * while providing a consistent builder pattern.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple item
 * UnifiedItemStack diamond = ItemBuilder.of("minecraft:diamond")
 *     .amount(64)
 *     .build();
 *
 * // Item with display name and lore
 * UnifiedItemStack sword = ItemBuilder.of("minecraft:diamond_sword")
 *     .name(Component.text("Excalibur"))
 *     .lore(
 *         Component.text("A legendary blade"),
 *         Component.text("Forged in ancient times")
 *     )
 *     .build();
 *
 * // Enchanted item with custom model data
 * UnifiedItemStack enchantedBow = ItemBuilder.of("minecraft:bow")
 *     .name(Component.text("Power Bow"))
 *     .enchant("minecraft:power", 5)
 *     .enchant("minecraft:infinity", 1)
 *     .unbreakable(true)
 *     .customModelData(1001)
 *     .build();
 *
 * // Item with persistent data
 * UnifiedItemStack customItem = ItemBuilder.of("minecraft:stick")
 *     .name(Component.text("Magic Wand"))
 *     .persistentData("myplugin:wand_type", "fire")
 *     .persistentData("myplugin:power_level", 10)
 *     .build();
 *
 * // Skull with player texture
 * UnifiedItemStack playerHead = ItemBuilder.skull()
 *     .skullOwner(uuid)
 *     .name(Component.text("Player's Head"))
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>ItemBuilder instances are NOT thread-safe. Create separate builders
 * for each thread or synchronize access.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedItemStack
 */
public interface ItemBuilder {

    /**
     * Creates a new ItemBuilder for the specified item type.
     *
     * @param type the item type ID (e.g., "minecraft:diamond")
     * @return a new ItemBuilder
     * @since 1.0.0
     */
    @NotNull
    static ItemBuilder of(@NotNull String type) {
        return ItemBuilderProvider.create(type);
    }

    /**
     * Creates a new ItemBuilder for a player skull.
     *
     * @return a new ItemBuilder for a player head
     * @since 1.0.0
     */
    @NotNull
    static ItemBuilder skull() {
        return of("minecraft:player_head");
    }

    /**
     * Creates a new ItemBuilder from an existing item.
     *
     * @param item the item to copy
     * @return a new ItemBuilder based on the item
     * @since 1.0.0
     */
    @NotNull
    static ItemBuilder from(@NotNull UnifiedItemStack item) {
        return item.toBuilder();
    }

    /**
     * Sets the item type.
     *
     * @param type the item type ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder type(@NotNull String type);

    /**
     * Sets the stack amount.
     *
     * @param amount the stack amount (1-64 typically)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder amount(int amount);

    /**
     * Sets the display name.
     *
     * @param name the display name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder name(@NotNull Component name);

    /**
     * Sets the display name from a string (no formatting).
     *
     * @param name the display name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ItemBuilder name(@NotNull String name) {
        return name(Component.text(name));
    }

    /**
     * Sets the lore from varargs.
     *
     * @param lines the lore lines
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder lore(@NotNull Component... lines);

    /**
     * Sets the lore from a list.
     *
     * @param lines the lore lines
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder lore(@NotNull List<Component> lines);

    /**
     * Adds a single lore line.
     *
     * @param line the lore line to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder addLore(@NotNull Component line);

    /**
     * Adds a lore line from a string.
     *
     * @param line the lore line to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ItemBuilder addLore(@NotNull String line) {
        return addLore(Component.text(line));
    }

    /**
     * Clears all lore.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder clearLore();

    /**
     * Adds an enchantment.
     *
     * @param enchantment the enchantment ID
     * @param level       the enchantment level
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder enchant(@NotNull String enchantment, int level);

    /**
     * Removes an enchantment.
     *
     * @param enchantment the enchantment ID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder removeEnchant(@NotNull String enchantment);

    /**
     * Clears all enchantments.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder clearEnchants();

    /**
     * Adds a glowing effect (enchantment glint without actual enchantments).
     *
     * @param glowing whether to add the glow effect
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder glowing(boolean glowing);

    /**
     * Sets the item damage (durability loss).
     *
     * @param damage the damage value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder damage(int damage);

    /**
     * Sets whether the item is unbreakable.
     *
     * @param unbreakable true for unbreakable
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder unbreakable(boolean unbreakable);

    /**
     * Sets the custom model data.
     *
     * @param customModelData the custom model data value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder customModelData(int customModelData);

    /**
     * Hides an item flag.
     *
     * @param flag the flag name (e.g., "HIDE_ENCHANTS", "HIDE_ATTRIBUTES")
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder hideFlag(@NotNull String flag);

    /**
     * Hides all item flags.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder hideAllFlags();

    /**
     * Shows a previously hidden item flag.
     *
     * @param flag the flag name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder showFlag(@NotNull String flag);

    /**
     * Sets persistent data on the item.
     *
     * @param key   the data key (namespaced recommended)
     * @param value the data value
     * @param <T>   the data type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    <T> ItemBuilder persistentData(@NotNull String key, @NotNull T value);

    /**
     * Removes persistent data from the item.
     *
     * @param key the data key
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder removePersistentData(@NotNull String key);

    /**
     * Sets the skull owner for player head items.
     *
     * @param ownerUuid the owner's UUID
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder skullOwner(@NotNull java.util.UUID ownerUuid);

    /**
     * Sets the skull texture for player head items.
     *
     * @param texture the Base64-encoded texture data
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder skullTexture(@NotNull String texture);

    /**
     * Sets the leather armor color.
     *
     * @param red   the red component (0-255)
     * @param green the green component (0-255)
     * @param blue  the blue component (0-255)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder leatherColor(int red, int green, int blue);

    /**
     * Sets the leather armor color from a hex value.
     *
     * @param hex the hex color (e.g., 0xFF5555)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder leatherColor(int hex);

    /**
     * Sets the potion color.
     *
     * @param red   the red component (0-255)
     * @param green the green component (0-255)
     * @param blue  the blue component (0-255)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder potionColor(int red, int green, int blue);

    /**
     * Adds a potion effect.
     *
     * @param effectType the effect type ID
     * @param duration   the duration in ticks
     * @param amplifier  the effect amplifier (0 = level 1)
     * @param ambient    whether particles are less visible
     * @param particles  whether to show particles
     * @param icon       whether to show the icon
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ItemBuilder potionEffect(@NotNull String effectType, int duration, int amplifier,
                             boolean ambient, boolean particles, boolean icon);

    /**
     * Adds a simple potion effect.
     *
     * @param effectType the effect type ID
     * @param duration   the duration in ticks
     * @param amplifier  the effect amplifier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ItemBuilder potionEffect(@NotNull String effectType, int duration, int amplifier) {
        return potionEffect(effectType, duration, amplifier, false, true, true);
    }

    /**
     * Applies a consumer function to modify this builder.
     *
     * <p>Useful for conditional modifications.
     *
     * @param consumer the consumer to apply
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ItemBuilder apply(@NotNull Consumer<ItemBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * Conditionally applies a consumer function.
     *
     * @param condition the condition to check
     * @param consumer  the consumer to apply if condition is true
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default ItemBuilder applyIf(boolean condition, @NotNull Consumer<ItemBuilder> consumer) {
        if (condition) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Builds the item stack.
     *
     * @return the constructed UnifiedItemStack
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack build();
}

/**
 * Internal provider for creating ItemBuilder instances.
 *
 * <p>This is used internally by the API to delegate to platform-specific
 * implementations. Plugin developers should use {@link ItemBuilder#of(String)}.
 *
 * @since 1.0.0
 */
interface ItemBuilderProvider {

    /**
     * The registered provider instance.
     */
    ItemBuilderProvider INSTANCE = null; // Set by platform implementation

    /**
     * Creates a new ItemBuilder for the specified type.
     *
     * @param type the item type ID
     * @return a new ItemBuilder
     */
    @NotNull
    static ItemBuilder create(@NotNull String type) {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "ItemBuilderProvider has not been initialized. " +
                    "Ensure UnifiedPluginAPI is loaded before using ItemBuilder."
            );
        }
        return INSTANCE.createBuilder(type);
    }

    /**
     * Creates a builder for the specified type.
     *
     * @param type the item type ID
     * @return a new ItemBuilder
     */
    @NotNull
    ItemBuilder createBuilder(@NotNull String type);
}
