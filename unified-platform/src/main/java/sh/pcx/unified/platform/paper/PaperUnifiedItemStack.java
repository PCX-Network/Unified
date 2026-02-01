/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Paper/Spigot implementation of {@link UnifiedItemStack}.
 *
 * <p>This class wraps a Bukkit {@link ItemStack} and provides a unified API for
 * item manipulation including display properties, enchantments, and persistent data.
 *
 * <h2>Adventure Integration</h2>
 * <p>On Paper servers with native Adventure support, display names and lore are
 * handled using Adventure components. On Spigot, legacy string conversion is used.
 *
 * <h2>Persistent Data</h2>
 * <p>Custom data is stored using Bukkit's PersistentDataContainer API, which
 * persists across restarts and supports multiple data types.
 *
 * <h2>Thread Safety</h2>
 * <p>ItemStack instances are NOT thread-safe. Create clones when passing between
 * threads using {@link #clone()}.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedItemStack
 * @see ItemStack
 */
public final class PaperUnifiedItemStack implements UnifiedItemStack {

    private static final NamespacedKey UNIFIED_NAMESPACE =
            new NamespacedKey("unifiedpluginapi", "data");

    private final ItemStack itemStack;

    /**
     * Creates a new PaperUnifiedItemStack wrapping the given Bukkit item stack.
     *
     * @param itemStack the Bukkit item stack to wrap
     * @since 1.0.0
     */
    public PaperUnifiedItemStack(@NotNull ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
    }

    /**
     * Creates a new PaperUnifiedItemStack from a material type.
     *
     * @param material the material type
     * @since 1.0.0
     */
    public PaperUnifiedItemStack(@NotNull Material material) {
        this(new ItemStack(material));
    }

    /**
     * Creates a new PaperUnifiedItemStack from a material type and amount.
     *
     * @param material the material type
     * @param amount   the stack amount
     * @since 1.0.0
     */
    public PaperUnifiedItemStack(@NotNull Material material, int amount) {
        this(new ItemStack(material, amount));
    }

    /**
     * Returns the underlying Bukkit item stack.
     *
     * @return the Bukkit item stack
     */
    @NotNull
    public ItemStack getBukkitItemStack() {
        return itemStack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getType() {
        return itemStack.getType().getKey().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAmount() {
        return itemStack.getAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        itemStack.setAmount(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack withAmount(int amount) {
        ItemStack clone = itemStack.clone();
        clone.setAmount(amount);
        return new PaperUnifiedItemStack(clone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxStackSize() {
        return itemStack.getMaxStackSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return itemStack.getType().isAir() || itemStack.getAmount() <= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSimilar(@NotNull UnifiedItemStack other) {
        if (other instanceof PaperUnifiedItemStack paperItem) {
            return itemStack.isSimilar(paperItem.itemStack);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Component> getDisplayName() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            try {
                // Paper native Adventure support
                return Optional.ofNullable(meta.displayName());
            } catch (NoSuchMethodError e) {
                // Spigot fallback
                String legacyName = meta.getDisplayName();
                return Optional.of(LegacyComponentSerializer.legacySection().deserialize(legacyName));
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDisplayName() {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayName(@Nullable Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                // Paper native Adventure support
                meta.displayName(name);
            } catch (NoSuchMethodError e) {
                // Spigot fallback
                if (name == null) {
                    meta.setDisplayName(null);
                } else {
                    meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));
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
    public List<Component> getLore() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasLore()) {
            try {
                // Paper native Adventure support
                List<Component> lore = meta.lore();
                return lore != null ? new ArrayList<>(lore) : new ArrayList<>();
            } catch (NoSuchMethodError e) {
                // Spigot fallback
                List<String> legacyLore = meta.getLore();
                if (legacyLore != null) {
                    return legacyLore.stream()
                            .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                            .collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLore() {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.hasLore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLore(@Nullable List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                // Paper native Adventure support
                meta.lore(lore);
            } catch (NoSuchMethodError e) {
                // Spigot fallback
                if (lore == null) {
                    meta.setLore(null);
                } else {
                    List<String> legacyLore = lore.stream()
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
    public void addLoreLine(@NotNull Component line) {
        List<Component> lore = getLore();
        lore.add(line);
        setLore(lore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Map<String, Integer> getEnchantments() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
            result.put(entry.getKey().getKey().toString(), entry.getValue());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnchantments() {
        return !itemStack.getEnchantments().isEmpty();
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
        Enchantment ench = parseEnchantment(enchantment);
        if (ench != null) {
            return itemStack.getEnchantmentLevel(ench);
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEnchantment(@NotNull String enchantment, int level) {
        Enchantment ench = parseEnchantment(enchantment);
        if (ench != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.addEnchant(ench, level, true);
                itemStack.setItemMeta(meta);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeEnchantment(@NotNull String enchantment) {
        Enchantment ench = parseEnchantment(enchantment);
        if (ench != null) {
            return itemStack.removeEnchantment(ench) > 0;
        }
        return false;
    }

    /**
     * Parses an enchantment from a namespaced ID.
     *
     * @param enchantment the enchantment ID
     * @return the Enchantment, or null if not found
     */
    @Nullable
    private Enchantment parseEnchantment(@NotNull String enchantment) {
        // Try direct key lookup
        NamespacedKey key = NamespacedKey.fromString(enchantment);
        if (key != null) {
            Enchantment ench = Enchantment.getByKey(key);
            if (ench != null) {
                return ench;
            }
        }
        // Try name lookup (without minecraft: prefix)
        String name = enchantment.replace("minecraft:", "").toUpperCase();
        return Enchantment.getByName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDurability() {
        ItemMeta meta = itemStack.getItemMeta();
        return meta instanceof Damageable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDamage() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            return damageable.getDamage();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDamage(int damage) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(damage);
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxDamage() {
        return itemStack.getType().getMaxDurability();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnbreakable() {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.isUnbreakable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUnbreakable(boolean unbreakable) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Integer> getCustomModelData() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) {
            return Optional.of(meta.getCustomModelData());
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCustomModelData(@Nullable Integer customModelData) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasCustomModelData() {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.hasCustomModelData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<String> getHiddenFlags() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            return meta.getItemFlags().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHiddenFlag(@NotNull String flag) {
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeHiddenFlag(@NotNull String flag) {
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasHiddenFlag(@NotNull String flag) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                return meta.hasItemFlag(itemFlag);
            } catch (IllegalArgumentException e) {
                // Unknown flag
            }
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
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey nsKey = createKey(key);

            PersistentDataType<?, T> dataType = getDataType(type);
            if (dataType != null && pdc.has(nsKey, dataType)) {
                return Optional.ofNullable(pdc.get(nsKey, dataType));
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> void setPersistentData(@NotNull String key, @NotNull T value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey nsKey = createKey(key);

            PersistentDataType dataType = getDataType(value.getClass());
            if (dataType != null) {
                pdc.set(nsKey, dataType, value);
                itemStack.setItemMeta(meta);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePersistentData(@NotNull String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.remove(createKey(key));
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPersistentData(@NotNull String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey nsKey = createKey(key);

            // Check common types
            return pdc.has(nsKey, PersistentDataType.STRING) ||
                   pdc.has(nsKey, PersistentDataType.INTEGER) ||
                   pdc.has(nsKey, PersistentDataType.LONG) ||
                   pdc.has(nsKey, PersistentDataType.DOUBLE) ||
                   pdc.has(nsKey, PersistentDataType.FLOAT) ||
                   pdc.has(nsKey, PersistentDataType.BYTE) ||
                   pdc.has(nsKey, PersistentDataType.BOOLEAN) ||
                   pdc.has(nsKey, PersistentDataType.BYTE_ARRAY);
        }
        return false;
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
    @SuppressWarnings("unchecked")
    private <T> PersistentDataType<?, T> getDataType(@NotNull Class<T> type) {
        if (type == String.class) return (PersistentDataType<?, T>) PersistentDataType.STRING;
        if (type == Integer.class || type == int.class) return (PersistentDataType<?, T>) PersistentDataType.INTEGER;
        if (type == Long.class || type == long.class) return (PersistentDataType<?, T>) PersistentDataType.LONG;
        if (type == Double.class || type == double.class) return (PersistentDataType<?, T>) PersistentDataType.DOUBLE;
        if (type == Float.class || type == float.class) return (PersistentDataType<?, T>) PersistentDataType.FLOAT;
        if (type == Byte.class || type == byte.class) return (PersistentDataType<?, T>) PersistentDataType.BYTE;
        if (type == Boolean.class || type == boolean.class) return (PersistentDataType<?, T>) PersistentDataType.BOOLEAN;
        if (type == byte[].class) return (PersistentDataType<?, T>) PersistentDataType.BYTE_ARRAY;
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte @NotNull [] serialize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(itemStack);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize item stack", e);
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
     * Deserializes an item stack from a byte array.
     *
     * @param data the serialized data
     * @return the deserialized item stack
     */
    @NotNull
    public static PaperUnifiedItemStack fromBytes(byte @NotNull [] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
            ItemStack itemStack = (ItemStack) ois.readObject();
            return new PaperUnifiedItemStack(itemStack);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize item stack", e);
        }
    }

    /**
     * Deserializes an item stack from a Base64 string.
     *
     * @param base64 the Base64-encoded data
     * @return the deserialized item stack
     */
    @NotNull
    public static PaperUnifiedItemStack fromBase64(@NotNull String base64) {
        return fromBytes(Base64.getDecoder().decode(base64));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack clone() {
        return new PaperUnifiedItemStack(itemStack.clone());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemBuilder toBuilder() {
        return new PaperItemBuilder(itemStack.clone());
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
     * Checks equality based on item similarity.
     *
     * @param o the object to compare
     * @return true if the items are similar
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperUnifiedItemStack that)) return false;
        return itemStack.equals(that.itemStack);
    }

    /**
     * Returns a hash code based on the item stack.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return itemStack.hashCode();
    }

    /**
     * Returns a string representation of this item stack.
     *
     * @return a string containing the item type and amount
     */
    @Override
    public String toString() {
        return "PaperUnifiedItemStack{" +
                "type=" + itemStack.getType() +
                ", amount=" + itemStack.getAmount() +
                '}';
    }
}
