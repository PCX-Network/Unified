/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.item;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Core implementation of the ItemBuilder interface with full NBT support.
 *
 * <p>This implementation provides a fluent API for creating item stacks with
 * various properties including display names, lore, enchantments, persistent
 * data, and item-type-specific attributes.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent builder pattern for item construction</li>
 *   <li>Full NBT data support via persistent data API</li>
 *   <li>Type-safe enchantment handling</li>
 *   <li>Support for all item metadata types (skulls, potions, leather armor, etc.)</li>
 *   <li>Immutable result - build() creates a new item each time</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * UnifiedItemStack sword = CoreItemBuilder.create("minecraft:diamond_sword")
 *     .name(Component.text("Excalibur").color(NamedTextColor.GOLD))
 *     .lore(
 *         Component.text("A legendary blade"),
 *         Component.text("Damage: +50")
 *     )
 *     .enchant("minecraft:sharpness", 5)
 *     .unbreakable(true)
 *     .persistentData("myplugin:owner", playerUuid.toString())
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class CoreItemBuilder implements ItemBuilder {

    // Item properties
    private String type;
    private int amount = 1;
    private Component displayName;
    private final List<Component> lore = new ArrayList<>();
    private final Map<String, Integer> enchantments = new LinkedHashMap<>();
    private final Set<String> hiddenFlags = new HashSet<>();
    private final Map<String, Object> persistentData = new LinkedHashMap<>();

    // State flags
    private boolean glowing = false;
    private int damage = 0;
    private boolean unbreakable = false;
    private Integer customModelData;

    // Skull data
    private UUID skullOwner;
    private String skullTexture;

    // Leather armor color
    private int[] leatherColor;

    // Potion data
    private int[] potionColor;
    private final List<PotionEffectData> potionEffects = new ArrayList<>();

    /**
     * Internal record for storing potion effect data.
     */
    private record PotionEffectData(
            String type,
            int duration,
            int amplifier,
            boolean ambient,
            boolean particles,
            boolean icon
    ) {}

    /**
     * Creates a new CoreItemBuilder for the specified item type.
     *
     * @param type the item type ID (e.g., "minecraft:diamond_sword")
     * @since 1.0.0
     */
    protected CoreItemBuilder(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    /**
     * Factory method to create a new CoreItemBuilder.
     *
     * @param type the item type ID
     * @return a new CoreItemBuilder
     * @since 1.0.0
     */
    @NotNull
    public static CoreItemBuilder create(@NotNull String type) {
        return new CoreItemBuilder(type);
    }

    /**
     * Creates a CoreItemBuilder for a player skull.
     *
     * @return a new CoreItemBuilder for a player head
     * @since 1.0.0
     */
    @NotNull
    public static CoreItemBuilder skull() {
        return create("minecraft:player_head");
    }

    // ==================== Basic Properties ====================

    @Override
    @NotNull
    public ItemBuilder type(@NotNull String type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder amount(int amount) {
        this.amount = Math.max(1, Math.min(amount, 64));
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder name(@NotNull Component name) {
        this.displayName = Objects.requireNonNull(name, "name cannot be null");
        return this;
    }

    // ==================== Lore ====================

    @Override
    @NotNull
    public ItemBuilder lore(@NotNull Component... lines) {
        this.lore.clear();
        Collections.addAll(this.lore, lines);
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder lore(@NotNull List<Component> lines) {
        this.lore.clear();
        this.lore.addAll(lines);
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder addLore(@NotNull Component line) {
        this.lore.add(Objects.requireNonNull(line, "line cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder clearLore() {
        this.lore.clear();
        return this;
    }

    /**
     * Sets lore from string lines (convenience method).
     *
     * @param lines the lore lines as strings
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ItemBuilder loreStrings(@NotNull String... lines) {
        this.lore.clear();
        for (String line : lines) {
            this.lore.add(Component.text(line));
        }
        return this;
    }

    /**
     * Adds a blank line to the lore.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ItemBuilder addLoreBlank() {
        return addLore(Component.empty());
    }

    // ==================== Enchantments ====================

    @Override
    @NotNull
    public ItemBuilder enchant(@NotNull String enchantment, int level) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");
        if (level > 0) {
            this.enchantments.put(normalizeId(enchantment), level);
        } else {
            this.enchantments.remove(normalizeId(enchantment));
        }
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder removeEnchant(@NotNull String enchantment) {
        this.enchantments.remove(normalizeId(enchantment));
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder clearEnchants() {
        this.enchantments.clear();
        return this;
    }

    /**
     * Adds multiple enchantments at once.
     *
     * @param enchantments the enchantments to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ItemBuilder enchantAll(@NotNull Map<String, Integer> enchantments) {
        enchantments.forEach(this::enchant);
        return this;
    }

    // ==================== Item Flags ====================

    @Override
    @NotNull
    public ItemBuilder glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder damage(int damage) {
        this.damage = Math.max(0, damage);
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder customModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder hideFlag(@NotNull String flag) {
        this.hiddenFlags.add(flag.toUpperCase());
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder hideAllFlags() {
        this.hiddenFlags.add("HIDE_ENCHANTS");
        this.hiddenFlags.add("HIDE_ATTRIBUTES");
        this.hiddenFlags.add("HIDE_UNBREAKABLE");
        this.hiddenFlags.add("HIDE_DESTROYS");
        this.hiddenFlags.add("HIDE_PLACED_ON");
        this.hiddenFlags.add("HIDE_ADDITIONAL_TOOLTIP");
        this.hiddenFlags.add("HIDE_DYE");
        this.hiddenFlags.add("HIDE_ARMOR_TRIM");
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder showFlag(@NotNull String flag) {
        this.hiddenFlags.remove(flag.toUpperCase());
        return this;
    }

    // ==================== Persistent Data ====================

    @Override
    @NotNull
    public <T> ItemBuilder persistentData(@NotNull String key, @NotNull T value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        this.persistentData.put(key, value);
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder removePersistentData(@NotNull String key) {
        this.persistentData.remove(key);
        return this;
    }

    /**
     * Sets multiple persistent data values at once.
     *
     * @param data the data map
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ItemBuilder persistentDataAll(@NotNull Map<String, Object> data) {
        this.persistentData.putAll(data);
        return this;
    }

    /**
     * Clears all persistent data.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ItemBuilder clearPersistentData() {
        this.persistentData.clear();
        return this;
    }

    // ==================== Skull Properties ====================

    @Override
    @NotNull
    public ItemBuilder skullOwner(@NotNull UUID ownerUuid) {
        this.skullOwner = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder skullTexture(@NotNull String texture) {
        this.skullTexture = Objects.requireNonNull(texture, "texture cannot be null");
        return this;
    }

    // ==================== Leather Armor ====================

    @Override
    @NotNull
    public ItemBuilder leatherColor(int red, int green, int blue) {
        this.leatherColor = new int[]{
                clampColor(red),
                clampColor(green),
                clampColor(blue)
        };
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder leatherColor(int hex) {
        return leatherColor(
                (hex >> 16) & 0xFF,
                (hex >> 8) & 0xFF,
                hex & 0xFF
        );
    }

    // ==================== Potion Properties ====================

    @Override
    @NotNull
    public ItemBuilder potionColor(int red, int green, int blue) {
        this.potionColor = new int[]{
                clampColor(red),
                clampColor(green),
                clampColor(blue)
        };
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder potionEffect(@NotNull String effectType, int duration, int amplifier,
                                     boolean ambient, boolean particles, boolean icon) {
        this.potionEffects.add(new PotionEffectData(
                normalizeId(effectType),
                Math.max(0, duration),
                Math.max(0, amplifier),
                ambient,
                particles,
                icon
        ));
        return this;
    }

    /**
     * Clears all potion effects.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ItemBuilder clearPotionEffects() {
        this.potionEffects.clear();
        return this;
    }

    // ==================== Conditional Modifications ====================

    @Override
    @NotNull
    public ItemBuilder apply(@NotNull Consumer<ItemBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    @Override
    @NotNull
    public ItemBuilder applyIf(boolean condition, @NotNull Consumer<ItemBuilder> consumer) {
        if (condition) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Applies a modification if the value is not null.
     *
     * @param value   the value to check
     * @param applier the modification to apply
     * @param <T>     the value type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <T> ItemBuilder applyIfPresent(@Nullable T value, @NotNull java.util.function.BiConsumer<ItemBuilder, T> applier) {
        if (value != null) {
            applier.accept(this, value);
        }
        return this;
    }

    // ==================== Build ====================

    @Override
    @NotNull
    public UnifiedItemStack build() {
        // Create the item stack data
        ItemData data = new ItemData(
                type,
                amount,
                displayName,
                List.copyOf(lore),
                Map.copyOf(enchantments),
                Set.copyOf(hiddenFlags),
                Map.copyOf(persistentData),
                glowing,
                damage,
                unbreakable,
                customModelData,
                skullOwner,
                skullTexture,
                leatherColor != null ? leatherColor.clone() : null,
                potionColor != null ? potionColor.clone() : null,
                List.copyOf(potionEffects)
        );

        // Delegate to platform-specific implementation
        return createItem(data);
    }

    /**
     * Creates the actual item stack from the collected data.
     *
     * <p>This method is meant to be overridden by platform-specific implementations.
     * The default implementation returns a placeholder.
     *
     * @param data the item data
     * @return the created item stack
     */
    @NotNull
    protected UnifiedItemStack createItem(@NotNull ItemData data) {
        // This would be overridden by platform implementations
        // For now, return a placeholder implementation
        return new PlaceholderItemStack(data);
    }

    // ==================== Helper Methods ====================

    /**
     * Normalizes an ID by ensuring it has a namespace.
     */
    private static String normalizeId(String id) {
        if (id.contains(":")) {
            return id.toLowerCase();
        }
        return "minecraft:" + id.toLowerCase();
    }

    /**
     * Clamps a color component to 0-255.
     */
    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    // ==================== Data Records ====================

    /**
     * Internal record containing all item data.
     */
    protected record ItemData(
            String type,
            int amount,
            Component displayName,
            List<Component> lore,
            Map<String, Integer> enchantments,
            Set<String> hiddenFlags,
            Map<String, Object> persistentData,
            boolean glowing,
            int damage,
            boolean unbreakable,
            Integer customModelData,
            UUID skullOwner,
            String skullTexture,
            int[] leatherColor,
            int[] potionColor,
            List<PotionEffectData> potionEffects
    ) {}

    /**
     * Placeholder implementation of UnifiedItemStack for testing.
     *
     * <p>This is a minimal implementation used when no platform-specific
     * implementation is available. Platform implementations should override
     * {@link CoreItemBuilder#createItem(ItemData)} to provide real items.
     */
    private static class PlaceholderItemStack implements UnifiedItemStack {
        private final ItemData data;
        private int mutableAmount;
        private Component mutableDisplayName;
        private final List<Component> mutableLore;
        private final Map<String, Integer> mutableEnchantments;
        private int mutableDamage;
        private boolean mutableUnbreakable;
        private Integer mutableCustomModelData;
        private final Set<String> mutableHiddenFlags;
        private final Map<String, Object> mutablePersistentData;

        PlaceholderItemStack(ItemData data) {
            this.data = data;
            this.mutableAmount = data.amount();
            this.mutableDisplayName = data.displayName();
            this.mutableLore = new ArrayList<>(data.lore());
            this.mutableEnchantments = new LinkedHashMap<>(data.enchantments());
            this.mutableDamage = data.damage();
            this.mutableUnbreakable = data.unbreakable();
            this.mutableCustomModelData = data.customModelData();
            this.mutableHiddenFlags = new HashSet<>(data.hiddenFlags());
            this.mutablePersistentData = new LinkedHashMap<>(data.persistentData());
        }

        @Override
        public @NotNull String getType() {
            return data.type();
        }

        @Override
        public int getAmount() {
            return mutableAmount;
        }

        @Override
        public void setAmount(int amount) {
            this.mutableAmount = Math.max(0, Math.min(amount, getMaxStackSize()));
        }

        @Override
        public @NotNull UnifiedItemStack withAmount(int amount) {
            PlaceholderItemStack copy = (PlaceholderItemStack) clone();
            copy.setAmount(amount);
            return copy;
        }

        @Override
        public int getMaxStackSize() {
            return 64; // Default max stack size
        }

        @Override
        public boolean isEmpty() {
            return mutableAmount <= 0 || "minecraft:air".equals(data.type());
        }

        @Override
        public boolean isSimilar(@NotNull UnifiedItemStack other) {
            if (!(other instanceof PlaceholderItemStack otherPlaceholder)) {
                return false;
            }
            return Objects.equals(data.type(), otherPlaceholder.data.type()) &&
                    Objects.equals(mutableDisplayName, otherPlaceholder.mutableDisplayName) &&
                    Objects.equals(mutableLore, otherPlaceholder.mutableLore) &&
                    Objects.equals(mutableEnchantments, otherPlaceholder.mutableEnchantments);
        }

        @Override
        public @NotNull Optional<Component> getDisplayName() {
            return Optional.ofNullable(mutableDisplayName);
        }

        @Override
        public boolean hasDisplayName() {
            return mutableDisplayName != null;
        }

        @Override
        public void setDisplayName(@Nullable Component name) {
            this.mutableDisplayName = name;
        }

        @Override
        public @NotNull List<Component> getLore() {
            return List.copyOf(mutableLore);
        }

        @Override
        public boolean hasLore() {
            return !mutableLore.isEmpty();
        }

        @Override
        public void setLore(@Nullable List<Component> lore) {
            mutableLore.clear();
            if (lore != null) {
                mutableLore.addAll(lore);
            }
        }

        @Override
        public void addLoreLine(@NotNull Component line) {
            mutableLore.add(line);
        }

        @Override
        public @NotNull Map<String, Integer> getEnchantments() {
            return Map.copyOf(mutableEnchantments);
        }

        @Override
        public boolean hasEnchantments() {
            return !mutableEnchantments.isEmpty();
        }

        @Override
        public boolean hasEnchantment(@NotNull String enchantment) {
            return mutableEnchantments.containsKey(normalizeId(enchantment));
        }

        @Override
        public int getEnchantmentLevel(@NotNull String enchantment) {
            return mutableEnchantments.getOrDefault(normalizeId(enchantment), 0);
        }

        @Override
        public void addEnchantment(@NotNull String enchantment, int level) {
            if (level > 0) {
                mutableEnchantments.put(normalizeId(enchantment), level);
            }
        }

        @Override
        public boolean removeEnchantment(@NotNull String enchantment) {
            return mutableEnchantments.remove(normalizeId(enchantment)) != null;
        }

        @Override
        public boolean hasDurability() {
            // Simple check - real implementation would check item type
            String type = data.type().toLowerCase();
            return type.contains("sword") || type.contains("pickaxe") ||
                    type.contains("axe") || type.contains("shovel") ||
                    type.contains("hoe") || type.contains("bow") ||
                    type.contains("armor") || type.contains("helmet") ||
                    type.contains("chestplate") || type.contains("leggings") ||
                    type.contains("boots") || type.contains("shield");
        }

        @Override
        public int getDamage() {
            return mutableDamage;
        }

        @Override
        public void setDamage(int damage) {
            this.mutableDamage = Math.max(0, damage);
        }

        @Override
        public int getMaxDamage() {
            return 100; // Default - real implementation would vary by item type
        }

        @Override
        public boolean isUnbreakable() {
            return mutableUnbreakable;
        }

        @Override
        public void setUnbreakable(boolean unbreakable) {
            this.mutableUnbreakable = unbreakable;
        }

        @Override
        public @NotNull Optional<Integer> getCustomModelData() {
            return Optional.ofNullable(mutableCustomModelData);
        }

        @Override
        public void setCustomModelData(@Nullable Integer customModelData) {
            this.mutableCustomModelData = customModelData;
        }

        @Override
        public boolean hasCustomModelData() {
            return mutableCustomModelData != null;
        }

        @Override
        public @NotNull List<String> getHiddenFlags() {
            return List.copyOf(mutableHiddenFlags);
        }

        @Override
        public void addHiddenFlag(@NotNull String flag) {
            mutableHiddenFlags.add(flag.toUpperCase());
        }

        @Override
        public void removeHiddenFlag(@NotNull String flag) {
            mutableHiddenFlags.remove(flag.toUpperCase());
        }

        @Override
        public boolean hasHiddenFlag(@NotNull String flag) {
            return mutableHiddenFlags.contains(flag.toUpperCase());
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull <T> Optional<T> getPersistentData(@NotNull String key, @NotNull Class<T> type) {
            Object value = mutablePersistentData.get(key);
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }

        @Override
        public <T> void setPersistentData(@NotNull String key, @NotNull T value) {
            mutablePersistentData.put(key, value);
        }

        @Override
        public void removePersistentData(@NotNull String key) {
            mutablePersistentData.remove(key);
        }

        @Override
        public boolean hasPersistentData(@NotNull String key) {
            return mutablePersistentData.containsKey(key);
        }

        @Override
        public byte @NotNull [] serialize() {
            // Placeholder - real implementation would serialize properly
            return new byte[0];
        }

        @Override
        public @NotNull String toBase64() {
            return java.util.Base64.getEncoder().encodeToString(serialize());
        }

        @Override
        public @NotNull UnifiedItemStack clone() {
            PlaceholderItemStack clone = new PlaceholderItemStack(data);
            clone.mutableAmount = this.mutableAmount;
            clone.mutableDisplayName = this.mutableDisplayName;
            clone.mutableLore.clear();
            clone.mutableLore.addAll(this.mutableLore);
            clone.mutableEnchantments.clear();
            clone.mutableEnchantments.putAll(this.mutableEnchantments);
            clone.mutableDamage = this.mutableDamage;
            clone.mutableUnbreakable = this.mutableUnbreakable;
            clone.mutableCustomModelData = this.mutableCustomModelData;
            clone.mutableHiddenFlags.clear();
            clone.mutableHiddenFlags.addAll(this.mutableHiddenFlags);
            clone.mutablePersistentData.clear();
            clone.mutablePersistentData.putAll(this.mutablePersistentData);
            return clone;
        }

        @Override
        public @NotNull ItemBuilder toBuilder() {
            CoreItemBuilder builder = new CoreItemBuilder(data.type());
            builder.amount = mutableAmount;
            builder.displayName = mutableDisplayName;
            builder.lore.addAll(mutableLore);
            builder.enchantments.putAll(mutableEnchantments);
            builder.hiddenFlags.addAll(mutableHiddenFlags);
            builder.persistentData.putAll(mutablePersistentData);
            builder.glowing = data.glowing();
            builder.damage = mutableDamage;
            builder.unbreakable = mutableUnbreakable;
            builder.customModelData = mutableCustomModelData;
            builder.skullOwner = data.skullOwner();
            builder.skullTexture = data.skullTexture();
            builder.leatherColor = data.leatherColor();
            builder.potionColor = data.potionColor();
            return builder;
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull <T> T getHandle() {
            throw new UnsupportedOperationException(
                    "PlaceholderItemStack has no platform handle. " +
                    "Use a platform-specific ItemBuilder implementation."
            );
        }

        @Override
        public String toString() {
            return "PlaceholderItemStack{" +
                    "type='" + data.type() + '\'' +
                    ", amount=" + mutableAmount +
                    ", displayName=" + mutableDisplayName +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CoreItemBuilder{" +
                "type='" + type + '\'' +
                ", amount=" + amount +
                ", displayName=" + displayName +
                ", enchantments=" + enchantments.size() +
                '}';
    }
}
