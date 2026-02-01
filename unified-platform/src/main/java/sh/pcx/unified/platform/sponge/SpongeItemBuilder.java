/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sponge implementation of the {@link ItemBuilder} interface.
 *
 * <p>This class provides a fluent API for constructing Sponge ItemStacks
 * through the unified API.
 *
 * <h2>Builder Pattern</h2>
 * <p>All setter methods return {@code this} for method chaining.
 * Call {@link #build()} to create the final ItemStack.
 *
 * <h2>Thread Safety</h2>
 * <p>ItemBuilder instances are NOT thread-safe. Create separate builders
 * for each thread or synchronize access.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemBuilder
 */
public final class SpongeItemBuilder implements ItemBuilder {

    private final SpongePlatformProvider provider;
    private ItemType itemType;
    private int amount = 1;
    private Component displayName;
    private List<Component> lore = new ArrayList<>();
    private List<Enchantment> enchantments = new ArrayList<>();
    private boolean glowing = false;
    private int damage = 0;
    private boolean unbreakable = false;
    private Integer customModelData;
    private List<String> hiddenFlags = new ArrayList<>();
    private UUID skullOwner;
    private String skullTexture;
    private Color leatherColor;
    private Color potionColor;
    private List<PotionEffect> potionEffects = new ArrayList<>();

    /**
     * Creates a new SpongeItemBuilder for the given item type.
     *
     * @param type     the item type ID
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeItemBuilder(@NotNull String type, @NotNull SpongePlatformProvider provider) {
        this.provider = provider;
        this.itemType = resolveItemType(type);
    }

    /**
     * Resolves an item type from its string ID.
     *
     * @param type the item type ID
     * @return the ItemType
     */
    @NotNull
    private ItemType resolveItemType(@NotNull String type) {
        ResourceKey typeKey = ResourceKey.resolve(type);
        return Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ITEM_TYPE).findValue(typeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item type: " + type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder type(@NotNull String type) {
        this.itemType = resolveItemType(type);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder name(@NotNull Component name) {
        this.displayName = name;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder lore(@NotNull Component... lines) {
        this.lore = new ArrayList<>(List.of(lines));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder lore(@NotNull List<Component> lines) {
        this.lore = new ArrayList<>(lines);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder addLore(@NotNull Component line) {
        this.lore.add(line);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder clearLore() {
        this.lore.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder enchant(@NotNull String enchantment, int level) {
        ResourceKey enchantKey = ResourceKey.resolve(enchantment);
        Optional<EnchantmentType> enchantType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ENCHANTMENT_TYPE).findValue(enchantKey);

        enchantType.ifPresent(type -> {
            // Remove existing enchantment of same type
            enchantments.removeIf(e -> e.type().equals(type));
            enchantments.add(Enchantment.of(type, level));
        });

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder removeEnchant(@NotNull String enchantment) {
        ResourceKey enchantKey = ResourceKey.resolve(enchantment);
        Optional<EnchantmentType> enchantType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ENCHANTMENT_TYPE).findValue(enchantKey);

        enchantType.ifPresent(type -> enchantments.removeIf(e -> e.type().equals(type)));

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder clearEnchants() {
        this.enchantments.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder damage(int damage) {
        this.damage = damage;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder customModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder hideFlag(@NotNull String flag) {
        if (!hiddenFlags.contains(flag)) {
            hiddenFlags.add(flag);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder hideAllFlags() {
        hiddenFlags.add("HIDE_ATTRIBUTES");
        hiddenFlags.add("HIDE_ENCHANTS");
        hiddenFlags.add("HIDE_UNBREAKABLE");
        hiddenFlags.add("HIDE_DESTROYS");
        hiddenFlags.add("HIDE_PLACED_ON");
        hiddenFlags.add("HIDE_POTION_EFFECTS");
        hiddenFlags.add("HIDE_DYE");
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder showFlag(@NotNull String flag) {
        hiddenFlags.remove(flag);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public <T> ItemBuilder persistentData(@NotNull String key, @NotNull T value) {
        // Sponge persistent data would require custom data registration
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder removePersistentData(@NotNull String key) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder skullOwner(@NotNull UUID ownerUuid) {
        this.skullOwner = ownerUuid;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder skullTexture(@NotNull String texture) {
        this.skullTexture = texture;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder leatherColor(int red, int green, int blue) {
        this.leatherColor = Color.ofRgb(red, green, blue);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder leatherColor(int hex) {
        int red = (hex >> 16) & 0xFF;
        int green = (hex >> 8) & 0xFF;
        int blue = hex & 0xFF;
        return leatherColor(red, green, blue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder potionColor(int red, int green, int blue) {
        this.potionColor = Color.ofRgb(red, green, blue);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder potionEffect(@NotNull String effectType, int duration, int amplifier,
                                    boolean ambient, boolean particles, boolean icon) {
        ResourceKey effectKey = ResourceKey.resolve(effectType);
        Optional<PotionEffectType> potionType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.POTION_EFFECT_TYPE).findValue(effectKey);

        potionType.ifPresent(type -> {
            PotionEffect effect = PotionEffect.builder()
                    .potionType(type)
                    .duration(Ticks.of(duration))
                    .amplifier(amplifier)
                    .ambient(ambient)
                    .showParticles(particles)
                    .showIcon(icon)
                    .build();
            potionEffects.add(effect);
        });

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack build() {
        ItemStack itemStack = ItemStack.of(itemType, amount);

        // Apply display name
        if (displayName != null) {
            itemStack.offer(Keys.CUSTOM_NAME, displayName);
        }

        // Apply lore
        if (!lore.isEmpty()) {
            itemStack.offer(Keys.LORE, lore);
        }

        // Apply enchantments
        if (!enchantments.isEmpty()) {
            itemStack.offer(Keys.APPLIED_ENCHANTMENTS, enchantments);
        }

        // Apply glowing effect (use luck enchantment and hide it)
        if (glowing && enchantments.isEmpty()) {
            List<Enchantment> glowEnchants = new ArrayList<>();
            glowEnchants.add(Enchantment.of(EnchantmentTypes.LUCK_OF_THE_SEA.get(), 1));
            itemStack.offer(Keys.APPLIED_ENCHANTMENTS, glowEnchants);
            itemStack.offer(Keys.HIDE_ENCHANTMENTS, true);
        }

        // Apply damage/durability
        if (damage > 0) {
            itemStack.get(Keys.MAX_DURABILITY).ifPresent(maxDur -> {
                itemStack.offer(Keys.ITEM_DURABILITY, maxDur - damage);
            });
        }

        // Apply unbreakable
        itemStack.offer(Keys.IS_UNBREAKABLE, unbreakable);

        // Apply custom model data
        if (customModelData != null) {
            itemStack.offer(Keys.CUSTOM_MODEL_DATA, customModelData);
        }

        // Apply hidden flags
        for (String flag : hiddenFlags) {
            switch (flag.toUpperCase()) {
                case "HIDE_ATTRIBUTES" -> itemStack.offer(Keys.HIDE_ATTRIBUTES, true);
                case "HIDE_ENCHANTS" -> itemStack.offer(Keys.HIDE_ENCHANTMENTS, true);
                case "HIDE_UNBREAKABLE" -> itemStack.offer(Keys.HIDE_UNBREAKABLE, true);
            }
        }

        // Apply skull owner
        if (skullOwner != null) {
            GameProfile profile = GameProfile.of(skullOwner);
            itemStack.offer(Keys.GAME_PROFILE, profile);
        }

        // Apply leather color
        if (leatherColor != null) {
            itemStack.offer(Keys.COLOR, leatherColor);
        }

        // Apply potion color
        if (potionColor != null) {
            itemStack.offer(Keys.COLOR, potionColor);
        }

        // Apply potion effects
        if (!potionEffects.isEmpty()) {
            itemStack.offer(Keys.POTION_EFFECTS, potionEffects);
        }

        return new SpongeUnifiedItemStack(itemStack, provider);
    }

    /**
     * Returns a string representation of this builder.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeItemBuilder{" +
                "type=" + itemType.key(org.spongepowered.api.registry.RegistryTypes.ITEM_TYPE).asString() +
                ", amount=" + amount +
                '}';
    }
}
