/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.impl;

import sh.pcx.unified.content.loot.*;
import sh.pcx.unified.content.loot.LootTypes.CountRange;
import sh.pcx.unified.content.loot.LootTypes.LootTableType;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Note: LootEntryImpl is defined locally in this package
// It's not imported from the loot package to avoid conflicts with the API definition

/**
 * Default implementation of {@link LootTableService}.
 *
 * <p>This implementation manages custom loot tables with support for:
 * <ul>
 *   <li>Custom loot table creation and registration</li>
 *   <li>Loot table modification for vanilla and custom tables</li>
 *   <li>Loot generation with pools, conditions, and functions</li>
 *   <li>Block and entity drop registration</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class LootTableServiceImpl implements LootTableService {

    private final Map<String, LootTableImpl> lootTables = new ConcurrentHashMap<>();
    private final Map<String, LootTableModifierImpl> modifiers = new ConcurrentHashMap<>();
    private final Map<String, String> blockDrops = new ConcurrentHashMap<>();
    private final Map<String, String> entityDrops = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public LootTableBuilder create(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new LootTableBuilderImpl(key, this);
    }

    @Override
    @NotNull
    public Optional<LootTable> get(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(lootTables.get(key));
    }

    @Override
    @NotNull
    public Collection<LootTable> getAll() {
        return Collections.unmodifiableCollection(lootTables.values());
    }

    @Override
    public boolean unregister(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return lootTables.remove(key) != null;
    }

    @Override
    @NotNull
    public LootTableModifier modify(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return modifiers.computeIfAbsent(key, k -> new LootTableModifierImpl(k, this));
    }

    @Override
    @NotNull
    public LootTableModifier modify(@NotNull LootTables table) {
        Objects.requireNonNull(table, "Table cannot be null");
        return modify(table.getKey());
    }

    @Override
    @NotNull
    public List<UnifiedItemStack> generate(@NotNull LootTable table, @NotNull LootContext context) {
        Objects.requireNonNull(table, "Table cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        List<UnifiedItemStack> results = new ArrayList<>();
        Random random = context.getRandom();

        for (LootPool pool : table.getPools()) {
            if (!checkConditions(pool.getConditions(), context)) {
                continue;
            }

            int rolls = pool.getRolls().roll(random);
            int bonusRolls = pool.getBonusRolls()
                    .map(bonus -> bonus.calculate(context.getLuck()))
                    .orElse(0);

            for (int i = 0; i < rolls + bonusRolls; i++) {
                LootEntry selectedEntry = selectEntry(pool.getEntries(), context);
                if (selectedEntry != null) {
                    generateFromEntry(selectedEntry, context, results);
                }
            }
        }

        return results;
    }

    @Override
    @NotNull
    public List<UnifiedItemStack> generate(@NotNull String key, @NotNull LootContext context) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        LootTable table = lootTables.get(key);
        if (table == null) {
            return Collections.emptyList();
        }

        return generate(table, context);
    }

    @Override
    public void registerBlockDrop(@NotNull String blockType, @NotNull LootTable table) {
        Objects.requireNonNull(blockType, "Block type cannot be null");
        Objects.requireNonNull(table, "Table cannot be null");

        if (!lootTables.containsKey(table.getKey())) {
            registerTable((LootTableImpl) table);
        }
        blockDrops.put(blockType, table.getKey());
    }

    @Override
    public boolean unregisterBlockDrop(@NotNull String blockType) {
        Objects.requireNonNull(blockType, "Block type cannot be null");
        return blockDrops.remove(blockType) != null;
    }

    @Override
    public void registerEntityDrop(@NotNull String entityType, @NotNull LootTable table) {
        Objects.requireNonNull(entityType, "Entity type cannot be null");
        Objects.requireNonNull(table, "Table cannot be null");

        if (!lootTables.containsKey(table.getKey())) {
            registerTable((LootTableImpl) table);
        }
        entityDrops.put(entityType, table.getKey());
    }

    @Override
    public boolean unregisterEntityDrop(@NotNull String entityType) {
        Objects.requireNonNull(entityType, "Entity type cannot be null");
        return entityDrops.remove(entityType) != null;
    }

    @Override
    public void reloadModifications() {
        for (LootTableModifierImpl modifier : modifiers.values()) {
            modifier.apply();
        }
    }

    /**
     * Registers a loot table.
     */
    void registerTable(LootTableImpl table) {
        lootTables.put(table.getKey(), table);
    }

    /**
     * Gets a table for modification.
     */
    Optional<LootTableImpl> getTableForModification(String key) {
        return Optional.ofNullable(lootTables.get(key));
    }

    private boolean checkConditions(List<LootCondition> conditions, LootContext context) {
        for (LootCondition condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    private LootEntry selectEntry(List<LootEntry> entries, LootContext context) {
        if (entries.isEmpty()) {
            return null;
        }

        List<LootEntry> validEntries = new ArrayList<>();
        int totalWeight = 0;

        for (LootEntry entry : entries) {
            if (!checkConditions(entry.getConditions(), context)) {
                continue;
            }

            validEntries.add(entry);
            int weight = entry.getWeight() + (int) (entry.getQuality() * context.getLuck());
            totalWeight += Math.max(1, weight);
        }

        if (validEntries.isEmpty()) {
            return null;
        }

        int roll = context.getRandom().nextInt(totalWeight);
        int cumulative = 0;

        for (LootEntry entry : validEntries) {
            int weight = entry.getWeight() + (int) (entry.getQuality() * context.getLuck());
            cumulative += Math.max(1, weight);
            if (roll < cumulative) {
                return entry;
            }
        }

        return validEntries.getLast();
    }

    private void generateFromEntry(LootEntry entry, LootContext context, List<UnifiedItemStack> results) {
        if (entry instanceof LootEntryImpl impl) {
            switch (impl.getType()) {
                case EMPTY -> {
                    // No item generated
                }
                case ITEM -> {
                    UnifiedItemStack item = createItem(impl, context);
                    if (item != null) {
                        results.add(item);
                    }
                }
                case LOOT_TABLE -> {
                    String tableKey = impl.tableKey();
                    if (tableKey != null) {
                        results.addAll(generate(tableKey, context));
                    }
                }
                case TAG -> {
                    // Tag lookup would require platform implementation
                    // For now, treat as empty
                }
                default -> {
                    // Handle other entry types as needed
                }
            }
        }
    }

    private UnifiedItemStack createItem(LootEntryImpl entry, LootContext context) {
        UnifiedItemStack item;

        if (entry.item() != null) {
            item = entry.item().toBuilder().build();
        } else if (entry.itemType() != null) {
            // Platform would provide actual item creation
            // This is a placeholder that would be overridden
            item = createItemFromType(entry.itemType());
        } else {
            return null;
        }

        if (item == null) {
            return null;
        }

        // Apply count
        CountRange count = entry.count();
        if (count != null) {
            int amount = count.roll(context.getRandom());
            item = item.toBuilder().amount(amount).build();
        }

        // Apply functions
        for (LootFunction function : entry.getFunctions()) {
            item = function.apply(item, context);
        }

        return item;
    }

    /**
     * Creates an item from a type ID.
     * This method would be overridden by platform implementations.
     */
    protected UnifiedItemStack createItemFromType(String itemType) {
        // Platform implementations would override this
        return null;
    }

    private void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if (!key.contains(":")) {
            throw new IllegalArgumentException("Key must be namespaced (e.g., 'myplugin:boss_drops')");
        }
    }
}

/**
 * Implementation of {@link LootTableBuilder}.
 */
class LootTableBuilderImpl implements LootTableBuilder {

    private final String key;
    private final LootTableServiceImpl service;
    private LootTableType type = LootTableType.GENERIC;
    private final List<LootPool> pools = new ArrayList<>();

    LootTableBuilderImpl(String key, LootTableServiceImpl service) {
        this.key = key;
        this.service = service;
    }

    @Override
    @NotNull
    public LootTableBuilder key(@NotNull String key) {
        // Key is already set in constructor
        return this;
    }

    @Override
    @NotNull
    public LootTableBuilder type(@NotNull LootTableType type) {
        this.type = Objects.requireNonNull(type);
        return this;
    }

    @Override
    @NotNull
    public LootTableBuilder pool(@NotNull LootPool pool) {
        pools.add(Objects.requireNonNull(pool));
        return this;
    }

    @Override
    @NotNull
    public LootTableBuilder pools(@NotNull LootPool... pools) {
        Collections.addAll(this.pools, pools);
        return this;
    }

    @Override
    @NotNull
    public LootTable build() {
        return new LootTableImpl(key, List.copyOf(pools), type);
    }

    @Override
    @NotNull
    public LootTable register() {
        LootTableImpl table = new LootTableImpl(key, List.copyOf(pools), type);
        service.registerTable(table);
        return table;
    }
}

/**
 * Implementation of {@link LootTable}.
 */
record LootTableImpl(
        String key,
        List<LootPool> pools,
        LootTableType type
) implements LootTable {

    @Override
    @NotNull
    public String getKey() {
        return key;
    }

    @Override
    @NotNull
    public List<LootPool> getPools() {
        return pools;
    }

    @Override
    @NotNull
    public Optional<LootPool> getPool(@NotNull String name) {
        return pools.stream()
                .filter(pool -> pool.getName().map(n -> n.equals(name)).orElse(false))
                .findFirst();
    }

    @Override
    @NotNull
    public LootTableType getType() {
        return type;
    }

    @Override
    @NotNull
    public LootTableBuilder toBuilder() {
        LootTableBuilder builder = LootTableBuilder.create()
                .key(key)
                .type(type);
        for (LootPool pool : pools) {
            builder.pool(pool);
        }
        return builder;
    }
}

/**
 * Implementation of {@link LootTableModifier}.
 */
class LootTableModifierImpl implements LootTableModifier {

    private final String tableKey;
    private final LootTableServiceImpl service;

    private final List<LootPool> poolsToAdd = new ArrayList<>();
    private final Set<String> poolsToRemove = new HashSet<>();
    private final Map<String, LootPool> poolsToReplace = new HashMap<>();
    private final Map<String, List<LootEntry>> entriesToAdd = new HashMap<>();
    private final Set<String> entriesToRemove = new HashSet<>();
    private final Map<String, Set<String>> poolEntriesToRemove = new HashMap<>();
    private final List<LootCondition> conditionsToAdd = new ArrayList<>();
    private final Map<String, List<LootCondition>> poolConditionsToAdd = new HashMap<>();
    private final List<LootFunction> functionsToAdd = new ArrayList<>();
    private final Map<String, List<LootFunction>> entryFunctionsToAdd = new HashMap<>();
    private float weightMultiplier = 1.0f;
    private int bonusRolls = 0;

    private LootTableImpl originalTable;

    LootTableModifierImpl(String tableKey, LootTableServiceImpl service) {
        this.tableKey = tableKey;
        this.service = service;
    }

    @Override
    @NotNull
    public String getTableKey() {
        return tableKey;
    }

    @Override
    @NotNull
    public LootTableModifier addPool(@NotNull LootPool pool) {
        poolsToAdd.add(Objects.requireNonNull(pool));
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier removePool(@NotNull String poolName) {
        poolsToRemove.add(Objects.requireNonNull(poolName));
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier replacePool(@NotNull String poolName, @NotNull LootPool pool) {
        poolsToReplace.put(Objects.requireNonNull(poolName), Objects.requireNonNull(pool));
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier addEntry(@NotNull String poolName, @NotNull LootEntry entry) {
        entriesToAdd.computeIfAbsent(poolName, unused -> new ArrayList<>()).add(entry);
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier removeEntry(@NotNull String itemType) {
        entriesToRemove.add(Objects.requireNonNull(itemType));
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier removeEntry(@NotNull String poolName, @NotNull String itemType) {
        poolEntriesToRemove.computeIfAbsent(poolName, unused -> new HashSet<>()).add(itemType);
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier addCondition(@NotNull LootCondition condition) {
        conditionsToAdd.add(Objects.requireNonNull(condition));
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier addCondition(@NotNull String poolName, @NotNull LootCondition condition) {
        poolConditionsToAdd.computeIfAbsent(poolName, unused -> new ArrayList<>()).add(condition);
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier addFunction(@NotNull LootFunction function) {
        functionsToAdd.add(Objects.requireNonNull(function));
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier addFunction(@NotNull String itemType, @NotNull LootFunction function) {
        entryFunctionsToAdd.computeIfAbsent(itemType, unused -> new ArrayList<>()).add(function);
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier scaleWeights(float multiplier) {
        this.weightMultiplier = multiplier;
        return this;
    }

    @Override
    @NotNull
    public LootTableModifier addBonusRolls(int bonusRolls) {
        this.bonusRolls = bonusRolls;
        return this;
    }

    @Override
    public void apply() {
        Optional<LootTableImpl> tableOpt = service.getTableForModification(tableKey);
        if (tableOpt.isEmpty()) {
            // Table doesn't exist yet, create a placeholder
            originalTable = new LootTableImpl(tableKey, List.of(), LootTableType.GENERIC);
        } else {
            originalTable = tableOpt.get();
        }

        List<LootPool> modifiedPools = new ArrayList<>();

        // Process existing pools
        for (LootPool pool : originalTable.pools()) {
            String poolName = pool.getName().orElse(null);

            // Skip removed pools
            if (poolName != null && poolsToRemove.contains(poolName)) {
                continue;
            }

            // Replace pool if specified
            if (poolName != null && poolsToReplace.containsKey(poolName)) {
                modifiedPools.add(poolsToReplace.get(poolName));
                continue;
            }

            // Modify pool
            modifiedPools.add(modifyPool(pool, poolName));
        }

        // Add new pools
        modifiedPools.addAll(poolsToAdd);

        // Create modified table
        LootTableImpl modifiedTable = new LootTableImpl(tableKey, modifiedPools, originalTable.type());
        service.registerTable(modifiedTable);
    }

    private LootPool modifyPool(LootPool pool, String poolName) {
        LootPool.Builder builder = LootPool.builder();

        if (poolName != null) {
            builder.name(poolName);
        }

        builder.rolls(pool.getRolls());
        pool.getBonusRolls().ifPresent(builder::bonusRolls);

        // Add entries (filtering removed ones)
        for (LootEntry entry : pool.getEntries()) {
            if (shouldKeepEntry(entry, poolName)) {
                builder.entry(modifyEntry(entry));
            }
        }

        // Add new entries
        if (poolName != null && entriesToAdd.containsKey(poolName)) {
            for (LootEntry entry : entriesToAdd.get(poolName)) {
                builder.entry(entry);
            }
        }

        // Add conditions
        for (LootCondition condition : pool.getConditions()) {
            builder.condition(condition);
        }
        for (LootCondition condition : conditionsToAdd) {
            builder.condition(condition);
        }
        if (poolName != null && poolConditionsToAdd.containsKey(poolName)) {
            for (LootCondition condition : poolConditionsToAdd.get(poolName)) {
                builder.condition(condition);
            }
        }

        return builder.build();
    }

    private boolean shouldKeepEntry(LootEntry entry, String poolName) {
        if (entry instanceof LootEntryImpl impl) {
            String itemType = impl.itemType();
            if (itemType != null) {
                if (entriesToRemove.contains(itemType)) {
                    return false;
                }
                if (poolName != null && poolEntriesToRemove.containsKey(poolName)) {
                    return !poolEntriesToRemove.get(poolName).contains(itemType);
                }
            }
        }
        return true;
    }

    private LootEntry modifyEntry(LootEntry entry) {
        if (!(entry instanceof LootEntryImpl impl)) {
            return entry;
        }

        String itemType = impl.itemType();
        List<LootFunction> additionalFunctions = new ArrayList<>(impl.getFunctions());

        // Add global functions
        additionalFunctions.addAll(functionsToAdd);

        // Add entry-specific functions
        if (itemType != null && entryFunctionsToAdd.containsKey(itemType)) {
            additionalFunctions.addAll(entryFunctionsToAdd.get(itemType));
        }

        // Apply weight multiplier
        int newWeight = (int) (impl.getWeight() * weightMultiplier);

        return new LootEntryImpl(
                impl.getType(),
                impl.itemType(),
                impl.item(),
                impl.tag(),
                impl.tableKey(),
                Math.max(1, newWeight),
                impl.getQuality(),
                impl.count(),
                impl.getConditions(),
                additionalFunctions
        );
    }

    @Override
    public void clear() {
        poolsToAdd.clear();
        poolsToRemove.clear();
        poolsToReplace.clear();
        entriesToAdd.clear();
        entriesToRemove.clear();
        poolEntriesToRemove.clear();
        conditionsToAdd.clear();
        poolConditionsToAdd.clear();
        functionsToAdd.clear();
        entryFunctionsToAdd.clear();
        weightMultiplier = 1.0f;
        bonusRolls = 0;
    }

    @Override
    public void revert() {
        if (originalTable != null) {
            service.registerTable(originalTable);
        }
        clear();
    }
}
