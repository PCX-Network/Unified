/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Paper/Spigot implementation of {@link ItemBuilder}.
 *
 * <p>This class provides a fluent builder API for creating and modifying Bukkit
 * {@link ItemStack} instances. It supports all standard item properties as well as
 * specialized metadata like skulls, leather armor colors, and potion effects.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a simple item
 * UnifiedItemStack item = new PaperItemBuilder("minecraft:diamond_sword")
 *     .name(Component.text("Excalibur"))
 *     .lore(Component.text("A legendary sword"))
 *     .enchant("minecraft:sharpness", 5)
 *     .unbreakable(true)
 *     .build();
 *
 * // Create a player head
 * UnifiedItemStack head = new PaperItemBuilder("minecraft:player_head")
 *     .skullOwner(playerUuid)
 *     .name(Component.text("Player's Head"))
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>ItemBuilder instances are NOT thread-safe. Create separate builders for each thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ItemBuilder
 * @see PaperUnifiedItemStack
 */
public final class PaperItemBuilder implements ItemBuilder {

    private ItemStack itemStack;
    private final List<Component> lore = new ArrayList<>();
    private boolean hasGlowEffect = false;

    /**
     * Creates a new PaperItemBuilder for the specified material type.
     *
     * @param type the material type ID (e.g., "minecraft:diamond")
     * @since 1.0.0
     */
    public PaperItemBuilder(@NotNull String type) {
        Material material = parseMaterial(type);
        if (material == null) {
            material = Material.STONE;
        }
        this.itemStack = new ItemStack(material);
    }

    /**
     * Creates a new PaperItemBuilder from an existing ItemStack.
     *
     * @param itemStack the item stack to modify
     * @since 1.0.0
     */
    public PaperItemBuilder(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        // Copy existing lore
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasLore()) {
            try {
                List<Component> existingLore = meta.lore();
                if (existingLore != null) {
                    this.lore.addAll(existingLore);
                }
            } catch (NoSuchMethodError e) {
                // Spigot fallback
                List<String> legacyLore = meta.getLore();
                if (legacyLore != null) {
                    legacyLore.stream()
                            .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                            .forEach(this.lore::add);
                }
            }
        }
    }

    /**
     * Parses a namespaced material ID to a Bukkit Material.
     *
     * @param type the material type ID
     * @return the Material, or null if not found
     */
    @Nullable
    private Material parseMaterial(@NotNull String type) {
        String name = type.replace("minecraft:", "").toUpperCase();

        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            try {
                NamespacedKey key = NamespacedKey.fromString(type);
                if (key != null) {
                    return Registry.MATERIAL.get(key);
                }
            } catch (Exception ex) {
                // Failed to parse
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder type(@NotNull String type) {
        Material material = parseMaterial(type);
        if (material != null) {
            ItemStack newStack = new ItemStack(material, itemStack.getAmount());
            // Copy meta if possible
            if (itemStack.hasItemMeta()) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    newStack.setItemMeta(meta);
                }
            }
            this.itemStack = newStack;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder name(@NotNull Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                meta.displayName(name);
            } catch (NoSuchMethodError e) {
                meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));
            }
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder lore(@NotNull Component... lines) {
        this.lore.clear();
        this.lore.addAll(Arrays.asList(lines));
        applyLore();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder lore(@NotNull List<Component> lines) {
        this.lore.clear();
        this.lore.addAll(lines);
        applyLore();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder addLore(@NotNull Component line) {
        this.lore.add(line);
        applyLore();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder clearLore() {
        this.lore.clear();
        applyLore();
        return this;
    }

    /**
     * Applies the current lore list to the item meta.
     */
    private void applyLore() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                meta.lore(this.lore.isEmpty() ? null : new ArrayList<>(this.lore));
            } catch (NoSuchMethodError e) {
                if (this.lore.isEmpty()) {
                    meta.setLore(null);
                } else {
                    List<String> legacyLore = this.lore.stream()
                            .map(line -> LegacyComponentSerializer.legacySection().serialize(line))
                            .collect(Collectors.toList());
                    meta.setLore(legacyLore);
                }
            }
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder enchant(@NotNull String enchantment, int level) {
        Enchantment ench = parseEnchantment(enchantment);
        if (ench != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.addEnchant(ench, level, true);
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder removeEnchant(@NotNull String enchantment) {
        Enchantment ench = parseEnchantment(enchantment);
        if (ench != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.removeEnchant(ench);
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder clearEnchants() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            for (Enchantment ench : meta.getEnchants().keySet()) {
                meta.removeEnchant(ench);
            }
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Parses an enchantment from a namespaced ID.
     *
     * @param enchantment the enchantment ID
     * @return the Enchantment, or null if not found
     */
    @Nullable
    private Enchantment parseEnchantment(@NotNull String enchantment) {
        NamespacedKey key = NamespacedKey.fromString(enchantment);
        if (key != null) {
            Enchantment ench = Enchantment.getByKey(key);
            if (ench != null) {
                return ench;
            }
        }
        String name = enchantment.replace("minecraft:", "").toUpperCase();
        return Enchantment.getByName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder glowing(boolean glowing) {
        this.hasGlowEffect = glowing;
        if (glowing && itemStack.getEnchantments().isEmpty()) {
            // Add a dummy enchantment and hide it
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                try {
                    meta.setEnchantmentGlintOverride(true);
                } catch (NoSuchMethodError e) {
                    // Fallback for older versions - use luck of the sea enchantment
                    Enchantment glowEnchant = getGlowEnchantment();
                    if (glowEnchant != null) {
                        meta.addEnchant(glowEnchant, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                }
                itemStack.setItemMeta(meta);
            }
        } else if (!glowing) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                try {
                    meta.setEnchantmentGlintOverride(null);
                } catch (NoSuchMethodError e) {
                    // Remove the dummy enchantment
                    Enchantment glowEnchant = getGlowEnchantment();
                    if (glowEnchant != null) {
                        meta.removeEnchant(glowEnchant);
                    }
                }
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * Gets an enchantment suitable for creating a glow effect.
     *
     * @return an enchantment that can be used for glowing, or null if none found
     */
    @Nullable
    @SuppressWarnings("deprecation")
    private Enchantment getGlowEnchantment() {
        // Try various enchantment names that have existed across versions
        String[] enchantmentNames = {"LUCK_OF_THE_SEA", "LURE", "UNBREAKING"};
        for (String name : enchantmentNames) {
            try {
                Enchantment ench = Enchantment.getByName(name);
                if (ench != null) {
                    return ench;
                }
            } catch (Exception e) {
                // Try next one
            }
        }
        // Try by key
        try {
            NamespacedKey key = NamespacedKey.minecraft("luck_of_the_sea");
            return Registry.ENCHANTMENT.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder damage(int damage) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(damage);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder unbreakable(boolean unbreakable) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder customModelData(int customModelData) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder hideFlag(@NotNull String flag) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                meta.addItemFlags(itemFlag);
                itemStack.setItemMeta(meta);
            } catch (IllegalArgumentException e) {
                // Unknown flag
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder hideAllFlags() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder showFlag(@NotNull String flag) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                meta.removeItemFlags(itemFlag);
                itemStack.setItemMeta(meta);
            } catch (IllegalArgumentException e) {
                // Unknown flag
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> ItemBuilder persistentData(@NotNull String key, @NotNull T value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            NamespacedKey nsKey = createKey(key);
            PersistentDataType dataType = getDataType(value.getClass());
            if (dataType != null) {
                meta.getPersistentDataContainer().set(nsKey, dataType, value);
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder removePersistentData(@NotNull String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(createKey(key));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Creates a NamespacedKey for persistent data.
     *
     * @param key the key string
     * @return the NamespacedKey
     */
    @NotNull
    private NamespacedKey createKey(@NotNull String key) {
        if (key.contains(":")) {
            NamespacedKey nsKey = NamespacedKey.fromString(key);
            if (nsKey != null) {
                return nsKey;
            }
        }
        return new NamespacedKey("unifiedpluginapi", key.toLowerCase().replace(".", "_"));
    }

    /**
     * Gets the PersistentDataType for a Java class.
     *
     * @param type the Java class
     * @return the PersistentDataType, or null if not supported
     */
    @Nullable
    @SuppressWarnings("rawtypes")
    private PersistentDataType getDataType(@NotNull Class<?> type) {
        if (type == String.class) return PersistentDataType.STRING;
        if (type == Integer.class || type == int.class) return PersistentDataType.INTEGER;
        if (type == Long.class || type == long.class) return PersistentDataType.LONG;
        if (type == Double.class || type == double.class) return PersistentDataType.DOUBLE;
        if (type == Float.class || type == float.class) return PersistentDataType.FLOAT;
        if (type == Byte.class || type == byte.class) return PersistentDataType.BYTE;
        if (type == Boolean.class || type == boolean.class) return PersistentDataType.BOOLEAN;
        if (type == byte[].class) return PersistentDataType.BYTE_ARRAY;
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder skullOwner(@NotNull UUID ownerUuid) {
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof SkullMeta skullMeta) {
                try {
                    // Paper API
                    PlayerProfile profile = Bukkit.createProfile(ownerUuid);
                    skullMeta.setPlayerProfile(profile);
                } catch (NoSuchMethodError | NoClassDefFoundError e) {
                    // Spigot fallback
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
                }
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder skullTexture(@NotNull String texture) {
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof SkullMeta skullMeta) {
                try {
                    // Paper API
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                    profile.setProperty(new ProfileProperty("textures", texture));
                    skullMeta.setPlayerProfile(profile);
                    itemStack.setItemMeta(meta);
                } catch (NoSuchMethodError | NoClassDefFoundError e) {
                    // Spigot doesn't have easy texture setting
                    // Would need reflection to set the GameProfile
                }
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder leatherColor(int red, int green, int blue) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(red, green, blue));
            itemStack.setItemMeta(meta);
        }
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
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(Color.fromRGB(red, green, blue));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder potionEffect(@NotNull String effectType, int duration, int amplifier,
                                     boolean ambient, boolean particles, boolean icon) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            PotionEffectType type = parsePotionEffectType(effectType);
            if (type != null) {
                PotionEffect effect = new PotionEffect(type, duration, amplifier, ambient, particles, icon);
                potionMeta.addCustomEffect(effect, true);
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * Parses a potion effect type from a namespaced ID.
     *
     * @param effectType the effect type ID
     * @return the PotionEffectType, or null if not found
     */
    @Nullable
    private PotionEffectType parsePotionEffectType(@NotNull String effectType) {
        NamespacedKey key = NamespacedKey.fromString(effectType);
        if (key != null) {
            try {
                return Registry.EFFECT.get(key);
            } catch (NoSuchFieldError e) {
                // Older version
            }
        }
        String name = effectType.replace("minecraft:", "").toUpperCase();
        return PotionEffectType.getByName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack build() {
        return new PaperUnifiedItemStack(itemStack.clone());
    }

    /**
     * Returns a string representation of this builder.
     *
     * @return a string containing the current item type
     */
    @Override
    public String toString() {
        return "PaperItemBuilder{" +
                "type=" + itemStack.getType() +
                ", amount=" + itemStack.getAmount() +
                '}';
    }
}
