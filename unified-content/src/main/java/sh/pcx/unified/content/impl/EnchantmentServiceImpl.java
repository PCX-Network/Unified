/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.unified.content.enchantment.*;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * Default implementation of {@link EnchantmentService}.
 *
 * <p>This implementation manages custom enchantments with support for:
 * <ul>
 *   <li>Registration and unregistration of custom enchantments</li>
 *   <li>Applying/removing enchantments from items</li>
 *   <li>Trigger-based effect handlers</li>
 *   <li>Cooldown management per player</li>
 *   <li>Level-based chance and effect scaling</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class EnchantmentServiceImpl implements EnchantmentService {

    private static final String ENCHANTMENT_DATA_KEY = "unified:custom_enchants";

    private final Map<String, CustomEnchantmentImpl> enchantments = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Instant>> cooldowns = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public EnchantmentBuilder register(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new EnchantmentBuilderImpl(key, this);
    }

    @Override
    public boolean unregister(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return enchantments.remove(key) != null;
    }

    @Override
    @NotNull
    public Optional<CustomEnchantment> get(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(enchantments.get(key));
    }

    @Override
    @NotNull
    public Collection<CustomEnchantment> getAll() {
        return Collections.unmodifiableCollection(enchantments.values());
    }

    @Override
    public boolean isRegistered(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return enchantments.containsKey(key);
    }

    @Override
    public void apply(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment, int level) {
        Objects.requireNonNull(item, "Item cannot be null");
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");

        if (level < 1 || level > enchantment.getMaxLevel()) {
            throw new IllegalArgumentException(
                    "Level must be between 1 and " + enchantment.getMaxLevel());
        }

        Map<String, Integer> enchants = getEnchantmentData(item);
        enchants.put(enchantment.getKey(), level);
        setEnchantmentData(item, enchants);
        updateItemLore(item);
    }

    @Override
    public boolean apply(@NotNull UnifiedItemStack item, @NotNull String key, int level) {
        Objects.requireNonNull(item, "Item cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        CustomEnchantment enchantment = enchantments.get(key);
        if (enchantment == null) {
            return false;
        }

        apply(item, enchantment, level);
        return true;
    }

    @Override
    public boolean remove(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment) {
        return remove(item, enchantment.getKey());
    }

    @Override
    public boolean remove(@NotNull UnifiedItemStack item, @NotNull String key) {
        Objects.requireNonNull(item, "Item cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        Map<String, Integer> enchants = getEnchantmentData(item);
        boolean removed = enchants.remove(key) != null;

        if (removed) {
            setEnchantmentData(item, enchants);
            updateItemLore(item);
        }

        return removed;
    }

    @Override
    public boolean has(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment) {
        return has(item, enchantment.getKey());
    }

    @Override
    public boolean has(@NotNull UnifiedItemStack item, @NotNull String key) {
        Objects.requireNonNull(item, "Item cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        Map<String, Integer> enchants = getEnchantmentData(item);
        return enchants.containsKey(key);
    }

    @Override
    public int getLevel(@NotNull UnifiedItemStack item, @NotNull CustomEnchantment enchantment) {
        return getLevel(item, enchantment.getKey());
    }

    @Override
    public int getLevel(@NotNull UnifiedItemStack item, @NotNull String key) {
        Objects.requireNonNull(item, "Item cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        Map<String, Integer> enchants = getEnchantmentData(item);
        return enchants.getOrDefault(key, 0);
    }

    @Override
    @NotNull
    public Map<CustomEnchantment, Integer> getCustomEnchantments(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "Item cannot be null");

        Map<String, Integer> enchants = getEnchantmentData(item);
        Map<CustomEnchantment, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            CustomEnchantment enchantment = enchantments.get(entry.getKey());
            if (enchantment != null) {
                result.put(enchantment, entry.getValue());
            }
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public int clearCustomEnchantments(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "Item cannot be null");

        Map<String, Integer> enchants = getEnchantmentData(item);
        int count = enchants.size();

        if (count > 0) {
            enchants.clear();
            setEnchantmentData(item, enchants);
            updateItemLore(item);
        }

        return count;
    }

    @Override
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");

        return getRemainingCooldown(playerId, enchantment) > 0;
    }

    @Override
    public long getRemainingCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");

        Map<String, Instant> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0;
        }

        Instant expiry = playerCooldowns.get(enchantment.getKey());
        if (expiry == null) {
            return 0;
        }

        long remaining = Duration.between(Instant.now(), expiry).toMillis();
        return Math.max(0, remaining);
    }

    @Override
    public void setCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");

        Duration cooldownDuration = enchantment.getCooldown().orElse(Duration.ZERO);
        if (cooldownDuration.isZero()) {
            return;
        }

        Instant expiry = Instant.now().plus(cooldownDuration);
        cooldowns.computeIfAbsent(playerId, _ -> new ConcurrentHashMap<>())
                .put(enchantment.getKey(), expiry);
    }

    @Override
    public void clearCooldown(@NotNull UUID playerId, @NotNull CustomEnchantment enchantment) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");

        Map<String, Instant> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(enchantment.getKey());
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
        }
    }

    /**
     * Registers a built enchantment.
     */
    CustomEnchantmentImpl registerEnchantment(CustomEnchantmentImpl enchantment) {
        enchantments.put(enchantment.getKey(), enchantment);
        return enchantment;
    }

    /**
     * Gets the internal enchantment implementation for trigger handling.
     */
    Optional<CustomEnchantmentImpl> getImpl(String key) {
        return Optional.ofNullable(enchantments.get(key));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getEnchantmentData(UnifiedItemStack item) {
        Optional<Map> data = item.getPersistentData(ENCHANTMENT_DATA_KEY, Map.class);
        if (data.isPresent()) {
            return new HashMap<>((Map<String, Integer>) data.get());
        }
        return new HashMap<>();
    }

    private void setEnchantmentData(UnifiedItemStack item, Map<String, Integer> enchants) {
        item.toBuilder().persistentData(ENCHANTMENT_DATA_KEY, new HashMap<>(enchants)).build();
    }

    private void updateItemLore(UnifiedItemStack item) {
        Map<CustomEnchantment, Integer> enchants = getCustomEnchantments(item);
        List<Component> loreLines = new ArrayList<>();

        for (Map.Entry<CustomEnchantment, Integer> entry : enchants.entrySet()) {
            loreLines.add(entry.getKey().formatLore(entry.getValue()));
        }

        item.toBuilder().lore(loreLines).build();
    }

    private void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if (!key.contains(":")) {
            throw new IllegalArgumentException("Key must be namespaced (e.g., 'myplugin:lifesteal')");
        }
    }
}

/**
 * Implementation of {@link EnchantmentBuilder}.
 */
class EnchantmentBuilderImpl implements EnchantmentBuilder {

    private final String key;
    private final EnchantmentServiceImpl service;

    private Component displayName;
    private IntFunction<Component> displayNameFunction;
    private String description;
    private int maxLevel = 1;
    private int startLevel = 1;
    private EnchantmentRarity rarity = EnchantmentRarity.COMMON;
    private EnchantmentTarget target = EnchantmentTarget.ALL;
    private boolean treasure = false;
    private boolean tradeable = true;
    private boolean discoverable = true;
    private boolean curse = false;
    private final Set<String> conflicts = new HashSet<>();
    private Duration cooldown;
    private IntFunction<Double> chanceFunction = _ -> 1.0;
    private Integer tickInterval;

    // Trigger handlers
    private BiConsumer<EnchantmentContext.Hit, Integer> onHitHandler;
    private BiConsumer<EnchantmentContext.Damage, Integer> onDamageHandler;
    private BiConsumer<EnchantmentContext.BlockBreak, Integer> onBlockBreakHandler;
    private BiConsumer<EnchantmentContext.Experience, Integer> onExperienceHandler;
    private BiConsumer<EnchantmentContext.ItemUse, Integer> onItemUseHandler;
    private BiConsumer<EnchantmentContext.Equipped, Integer> whileEquippedHandler;
    private BiConsumer<EnchantmentContext.Shoot, Integer> onShootHandler;
    private BiConsumer<EnchantmentContext.ArrowHit, Integer> onArrowHitHandler;
    private LoreFormatter loreFormatter;

    EnchantmentBuilderImpl(String key, EnchantmentServiceImpl service) {
        this.key = key;
        this.service = service;
    }

    @Override
    @NotNull
    public EnchantmentBuilder displayName(@NotNull Component name) {
        this.displayName = Objects.requireNonNull(name);
        this.displayNameFunction = null;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder displayName(@NotNull IntFunction<Component> nameFunction) {
        this.displayNameFunction = Objects.requireNonNull(nameFunction);
        this.displayName = null;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder description(@NotNull String description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder maxLevel(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Max level must be at least 1");
        }
        this.maxLevel = maxLevel;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder startLevel(int startLevel) {
        this.startLevel = startLevel;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder rarity(@NotNull EnchantmentRarity rarity) {
        this.rarity = Objects.requireNonNull(rarity);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder target(@NotNull EnchantmentTarget target) {
        this.target = Objects.requireNonNull(target);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder treasure(boolean treasure) {
        this.treasure = treasure;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder tradeable(boolean tradeable) {
        this.tradeable = tradeable;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder discoverable(boolean discoverable) {
        this.discoverable = discoverable;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder curse(boolean curse) {
        this.curse = curse;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder conflictsWith(@NotNull String... keys) {
        Collections.addAll(conflicts, keys);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder cooldown(@NotNull Duration cooldown) {
        this.cooldown = Objects.requireNonNull(cooldown);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder chance(@NotNull IntFunction<Double> chanceFunction) {
        this.chanceFunction = Objects.requireNonNull(chanceFunction);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder tickInterval(int ticks) {
        this.tickInterval = ticks;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onHit(@NotNull BiConsumer<EnchantmentContext.Hit, Integer> handler) {
        this.onHitHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onDamage(@NotNull BiConsumer<EnchantmentContext.Damage, Integer> handler) {
        this.onDamageHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onBlockBreak(@NotNull BiConsumer<EnchantmentContext.BlockBreak, Integer> handler) {
        this.onBlockBreakHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onExperienceGain(@NotNull BiConsumer<EnchantmentContext.Experience, Integer> handler) {
        this.onExperienceHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onItemUse(@NotNull BiConsumer<EnchantmentContext.ItemUse, Integer> handler) {
        this.onItemUseHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder whileEquipped(@NotNull BiConsumer<EnchantmentContext.Equipped, Integer> handler) {
        this.whileEquippedHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onShoot(@NotNull BiConsumer<EnchantmentContext.Shoot, Integer> handler) {
        this.onShootHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder onArrowHit(@NotNull BiConsumer<EnchantmentContext.ArrowHit, Integer> handler) {
        this.onArrowHitHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentBuilder loreFormat(@NotNull LoreFormatter formatter) {
        this.loreFormatter = Objects.requireNonNull(formatter);
        return this;
    }

    @Override
    @NotNull
    public CustomEnchantment register() {
        if (displayName == null && displayNameFunction == null) {
            throw new IllegalStateException("Display name is required");
        }

        CustomEnchantmentImpl enchantment = new CustomEnchantmentImpl(
                key,
                displayName,
                displayNameFunction,
                description,
                maxLevel,
                startLevel,
                rarity,
                target,
                treasure,
                tradeable,
                discoverable,
                curse,
                Set.copyOf(conflicts),
                cooldown,
                chanceFunction,
                tickInterval,
                onHitHandler,
                onDamageHandler,
                onBlockBreakHandler,
                onExperienceHandler,
                onItemUseHandler,
                whileEquippedHandler,
                onShootHandler,
                onArrowHitHandler,
                loreFormatter
        );

        return service.registerEnchantment(enchantment);
    }
}

/**
 * Implementation of {@link CustomEnchantment}.
 */
record CustomEnchantmentImpl(
        String key,
        Component displayName,
        IntFunction<Component> displayNameFunction,
        String description,
        int maxLevel,
        int startLevel,
        EnchantmentRarity rarity,
        EnchantmentTarget target,
        boolean treasure,
        boolean tradeable,
        boolean discoverable,
        boolean curse,
        Set<String> conflicts,
        Duration cooldown,
        IntFunction<Double> chanceFunction,
        Integer tickInterval,
        BiConsumer<EnchantmentContext.Hit, Integer> onHitHandler,
        BiConsumer<EnchantmentContext.Damage, Integer> onDamageHandler,
        BiConsumer<EnchantmentContext.BlockBreak, Integer> onBlockBreakHandler,
        BiConsumer<EnchantmentContext.Experience, Integer> onExperienceHandler,
        BiConsumer<EnchantmentContext.ItemUse, Integer> onItemUseHandler,
        BiConsumer<EnchantmentContext.Equipped, Integer> whileEquippedHandler,
        BiConsumer<EnchantmentContext.Shoot, Integer> onShootHandler,
        BiConsumer<EnchantmentContext.ArrowHit, Integer> onArrowHitHandler,
        EnchantmentBuilder.LoreFormatter loreFormatter
) implements CustomEnchantment {

    @Override
    @NotNull
    public String getKey() {
        return key;
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        if (displayNameFunction != null) {
            return displayNameFunction.apply(1);
        }
        return displayName != null ? displayName : Component.text(key);
    }

    @Override
    @NotNull
    public Component getDisplayName(int level) {
        if (displayNameFunction != null) {
            return displayNameFunction.apply(level);
        }
        return getDisplayName();
    }

    @Override
    @NotNull
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getStartLevel() {
        return startLevel;
    }

    @Override
    @NotNull
    public EnchantmentRarity getRarity() {
        return rarity;
    }

    @Override
    @NotNull
    public EnchantmentTarget getTarget() {
        return target;
    }

    @Override
    public boolean canEnchantItem(@NotNull String itemType) {
        return target.includes(itemType);
    }

    @Override
    @NotNull
    public Set<String> getConflicts() {
        return conflicts;
    }

    @Override
    public boolean conflictsWith(@NotNull String otherKey) {
        return conflicts.contains(otherKey);
    }

    @Override
    public boolean isTreasure() {
        return treasure;
    }

    @Override
    public boolean isTradeable() {
        return tradeable;
    }

    @Override
    public boolean isDiscoverable() {
        return discoverable;
    }

    @Override
    public boolean isCurse() {
        return curse;
    }

    @Override
    @NotNull
    public Optional<Duration> getCooldown() {
        return Optional.ofNullable(cooldown);
    }

    @Override
    @NotNull
    public IntFunction<Double> getChanceFunction() {
        return chanceFunction;
    }

    @Override
    public double getChance(int level) {
        return chanceFunction.apply(level);
    }

    @Override
    @NotNull
    public Optional<Integer> getTickInterval() {
        return Optional.ofNullable(tickInterval);
    }

    @Override
    @NotNull
    public Component formatLore(int level) {
        Component name = getDisplayName(level);

        if (loreFormatter != null) {
            return loreFormatter.format(name, level);
        }

        if (maxLevel == 1) {
            return name.color(curse ? NamedTextColor.RED : NamedTextColor.GRAY);
        }

        return name.append(Component.text(" " + CustomEnchantment.toRoman(level)))
                .color(curse ? NamedTextColor.RED : NamedTextColor.GRAY);
    }

    /**
     * Handles a hit trigger.
     */
    void handleHit(EnchantmentContext.Hit context, int level) {
        if (onHitHandler != null) {
            onHitHandler.accept(context, level);
        }
    }

    /**
     * Handles a damage trigger.
     */
    void handleDamage(EnchantmentContext.Damage context, int level) {
        if (onDamageHandler != null) {
            onDamageHandler.accept(context, level);
        }
    }

    /**
     * Handles a block break trigger.
     */
    void handleBlockBreak(EnchantmentContext.BlockBreak context, int level) {
        if (onBlockBreakHandler != null) {
            onBlockBreakHandler.accept(context, level);
        }
    }

    /**
     * Handles an experience gain trigger.
     */
    void handleExperience(EnchantmentContext.Experience context, int level) {
        if (onExperienceHandler != null) {
            onExperienceHandler.accept(context, level);
        }
    }

    /**
     * Handles an item use trigger.
     */
    void handleItemUse(EnchantmentContext.ItemUse context, int level) {
        if (onItemUseHandler != null) {
            onItemUseHandler.accept(context, level);
        }
    }

    /**
     * Handles a while equipped trigger.
     */
    void handleEquipped(EnchantmentContext.Equipped context, int level) {
        if (whileEquippedHandler != null) {
            whileEquippedHandler.accept(context, level);
        }
    }

    /**
     * Handles a shoot trigger.
     */
    void handleShoot(EnchantmentContext.Shoot context, int level) {
        if (onShootHandler != null) {
            onShootHandler.accept(context, level);
        }
    }

    /**
     * Handles an arrow hit trigger.
     */
    void handleArrowHit(EnchantmentContext.ArrowHit context, int level) {
        if (onArrowHitHandler != null) {
            onArrowHitHandler.accept(context, level);
        }
    }
}
