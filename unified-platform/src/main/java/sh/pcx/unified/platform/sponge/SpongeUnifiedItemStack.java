/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sponge implementation of the {@link UnifiedItemStack} interface.
 *
 * <p>This class wraps Sponge's {@link ItemStack} to provide item operations
 * through the unified API.
 *
 * <h2>Data Keys</h2>
 * <p>Sponge uses a data key system for item metadata. This implementation
 * maps common item properties to their corresponding Sponge data keys.
 *
 * <h2>Thread Safety</h2>
 * <p>ItemStack instances are NOT thread-safe. Create copies when passing
 * between threads.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedItemStack
 */
public final class SpongeUnifiedItemStack implements UnifiedItemStack {

    private final ItemStack itemStack;
    private final SpongePlatformProvider provider;

    /**
     * Creates a new SpongeUnifiedItemStack wrapping the given ItemStack.
     *
     * @param itemStack the Sponge ItemStack to wrap
     * @param provider  the platform provider
     * @since 1.0.0
     */
    public SpongeUnifiedItemStack(@NotNull ItemStack itemStack, @NotNull SpongePlatformProvider provider) {
        this.itemStack = itemStack;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getType() {
        return itemStack.type().key(org.spongepowered.api.registry.RegistryTypes.ITEM_TYPE).asString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAmount() {
        return itemStack.quantity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        itemStack.setQuantity(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack withAmount(int amount) {
        ItemStack copy = itemStack.copy();
        copy.setQuantity(amount);
        return new SpongeUnifiedItemStack(copy, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxStackSize() {
        return itemStack.maxStackQuantity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return itemStack.isEmpty() ||
               itemStack.type().equals(ItemTypes.AIR.get()) ||
               itemStack.quantity() <= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSimilar(@NotNull UnifiedItemStack other) {
        if (!(other instanceof SpongeUnifiedItemStack spongeOther)) {
            return false;
        }
        // Compare without quantity
        ItemStack copy1 = itemStack.copy();
        ItemStack copy2 = spongeOther.itemStack.copy();
        copy1.setQuantity(1);
        copy2.setQuantity(1);
        return copy1.equalTo(copy2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Component> getDisplayName() {
        return itemStack.get(Keys.CUSTOM_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDisplayName() {
        return itemStack.get(Keys.CUSTOM_NAME).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayName(@Nullable Component name) {
        if (name == null) {
            itemStack.remove(Keys.CUSTOM_NAME);
        } else {
            itemStack.offer(Keys.CUSTOM_NAME, name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<Component> getLore() {
        return itemStack.get(Keys.LORE).orElse(new ArrayList<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLore() {
        return itemStack.get(Keys.LORE).map(list -> !list.isEmpty()).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLore(@Nullable List<Component> lore) {
        if (lore == null || lore.isEmpty()) {
            itemStack.remove(Keys.LORE);
        } else {
            itemStack.offer(Keys.LORE, lore);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLoreLine(@NotNull Component line) {
        List<Component> lore = new ArrayList<>(getLore());
        lore.add(line);
        setLore(lore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Map<String, Integer> getEnchantments() {
        List<Enchantment> enchantments = itemStack.get(Keys.APPLIED_ENCHANTMENTS).orElse(new ArrayList<>());
        Map<String, Integer> result = new HashMap<>();

        for (Enchantment enchantment : enchantments) {
            String key = enchantment.type().key(org.spongepowered.api.registry.RegistryTypes.ENCHANTMENT_TYPE).asString();
            result.put(key, enchantment.level());
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnchantments() {
        return itemStack.get(Keys.APPLIED_ENCHANTMENTS).map(list -> !list.isEmpty()).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnchantment(@NotNull String enchantment) {
        return getEnchantmentLevel(enchantment) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEnchantmentLevel(@NotNull String enchantment) {
        ResourceKey enchantKey = ResourceKey.resolve(enchantment);
        Optional<EnchantmentType> enchantType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ENCHANTMENT_TYPE).findValue(enchantKey);

        if (enchantType.isEmpty()) {
            return 0;
        }

        return itemStack.get(Keys.APPLIED_ENCHANTMENTS)
                .orElse(new ArrayList<>())
                .stream()
                .filter(e -> e.type().equals(enchantType.get()))
                .findFirst()
                .map(Enchantment::level)
                .orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEnchantment(@NotNull String enchantment, int level) {
        ResourceKey enchantKey = ResourceKey.resolve(enchantment);
        Optional<EnchantmentType> enchantType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ENCHANTMENT_TYPE).findValue(enchantKey);

        if (enchantType.isEmpty()) {
            return;
        }

        List<Enchantment> enchantments = new ArrayList<>(itemStack.get(Keys.APPLIED_ENCHANTMENTS).orElse(new ArrayList<>()));

        // Remove existing enchantment of same type
        enchantments.removeIf(e -> e.type().equals(enchantType.get()));

        // Add new enchantment
        enchantments.add(Enchantment.of(enchantType.get(), level));

        itemStack.offer(Keys.APPLIED_ENCHANTMENTS, enchantments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeEnchantment(@NotNull String enchantment) {
        ResourceKey enchantKey = ResourceKey.resolve(enchantment);
        Optional<EnchantmentType> enchantType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ENCHANTMENT_TYPE).findValue(enchantKey);

        if (enchantType.isEmpty()) {
            return false;
        }

        List<Enchantment> enchantments = new ArrayList<>(itemStack.get(Keys.APPLIED_ENCHANTMENTS).orElse(new ArrayList<>()));
        boolean removed = enchantments.removeIf(e -> e.type().equals(enchantType.get()));

        if (removed) {
            itemStack.offer(Keys.APPLIED_ENCHANTMENTS, enchantments);
        }

        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDurability() {
        return itemStack.get(Keys.MAX_DURABILITY).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDamage() {
        int maxDurability = getMaxDamage();
        int currentDurability = itemStack.get(Keys.ITEM_DURABILITY).orElse(maxDurability);
        return maxDurability - currentDurability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDamage(int damage) {
        int maxDurability = getMaxDamage();
        int durability = maxDurability - damage;
        itemStack.offer(Keys.ITEM_DURABILITY, Math.max(0, durability));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxDamage() {
        return itemStack.get(Keys.MAX_DURABILITY).orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnbreakable() {
        return itemStack.get(Keys.IS_UNBREAKABLE).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUnbreakable(boolean unbreakable) {
        itemStack.offer(Keys.IS_UNBREAKABLE, unbreakable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Integer> getCustomModelData() {
        return itemStack.get(Keys.CUSTOM_MODEL_DATA);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCustomModelData(@Nullable Integer customModelData) {
        if (customModelData == null) {
            itemStack.remove(Keys.CUSTOM_MODEL_DATA);
        } else {
            itemStack.offer(Keys.CUSTOM_MODEL_DATA, customModelData);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasCustomModelData() {
        return itemStack.get(Keys.CUSTOM_MODEL_DATA).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<String> getHiddenFlags() {
        return itemStack.get(Keys.HIDE_ATTRIBUTES).orElse(false) ? List.of("HIDE_ATTRIBUTES") : new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHiddenFlag(@NotNull String flag) {
        if (flag.equalsIgnoreCase("HIDE_ATTRIBUTES")) {
            itemStack.offer(Keys.HIDE_ATTRIBUTES, true);
        } else if (flag.equalsIgnoreCase("HIDE_ENCHANTS")) {
            itemStack.offer(Keys.HIDE_ENCHANTMENTS, true);
        } else if (flag.equalsIgnoreCase("HIDE_UNBREAKABLE")) {
            itemStack.offer(Keys.HIDE_UNBREAKABLE, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeHiddenFlag(@NotNull String flag) {
        if (flag.equalsIgnoreCase("HIDE_ATTRIBUTES")) {
            itemStack.offer(Keys.HIDE_ATTRIBUTES, false);
        } else if (flag.equalsIgnoreCase("HIDE_ENCHANTS")) {
            itemStack.offer(Keys.HIDE_ENCHANTMENTS, false);
        } else if (flag.equalsIgnoreCase("HIDE_UNBREAKABLE")) {
            itemStack.offer(Keys.HIDE_UNBREAKABLE, false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasHiddenFlag(@NotNull String flag) {
        if (flag.equalsIgnoreCase("HIDE_ATTRIBUTES")) {
            return itemStack.get(Keys.HIDE_ATTRIBUTES).orElse(false);
        } else if (flag.equalsIgnoreCase("HIDE_ENCHANTS")) {
            return itemStack.get(Keys.HIDE_ENCHANTMENTS).orElse(false);
        } else if (flag.equalsIgnoreCase("HIDE_UNBREAKABLE")) {
            return itemStack.get(Keys.HIDE_UNBREAKABLE).orElse(false);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getPersistentData(@NotNull String key, @NotNull Class<T> type) {
        // Sponge uses custom data for persistent storage
        // This is a simplified implementation
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void setPersistentData(@NotNull String key, @NotNull T value) {
        // Sponge uses custom data for persistent storage
        // This is a simplified implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePersistentData(@NotNull String key) {
        // Sponge uses custom data for persistent storage
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPersistentData(@NotNull String key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte @NotNull [] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Simple serialization - type and quantity
            dos.writeUTF(getType());
            dos.writeInt(getAmount());

            // Serialize display name if present
            getDisplayName().ifPresent(name -> {
                try {
                    dos.writeBoolean(true);
                    // Would need proper component serialization
                } catch (Exception e) {
                    // Ignore
                }
            });

            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String toBase64() {
        return Base64.getEncoder().encodeToString(serialize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack clone() {
        return new SpongeUnifiedItemStack(itemStack.copy(), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder toBuilder() {
        return new SpongeItemBuilder(getType(), provider)
                .amount(getAmount())
                .apply(builder -> {
                    getDisplayName().ifPresent(builder::name);
                    if (hasLore()) {
                        builder.lore(getLore());
                    }
                    getEnchantments().forEach(builder::enchant);
                    if (hasDurability()) {
                        builder.damage(getDamage());
                    }
                    builder.unbreakable(isUnbreakable());
                    getCustomModelData().ifPresent(builder::customModelData);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) itemStack;
    }

    /**
     * Returns the wrapped Sponge ItemStack.
     *
     * @return the Sponge ItemStack
     */
    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Returns a string representation of this item.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeUnifiedItemStack{" +
                "type=" + getType() +
                ", amount=" + getAmount() +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpongeUnifiedItemStack other)) return false;
        return itemStack.equalTo(other.itemStack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return itemStack.hashCode();
    }
}
