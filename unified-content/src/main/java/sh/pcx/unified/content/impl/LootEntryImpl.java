/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.impl;

import sh.pcx.unified.content.loot.*;
import sh.pcx.unified.content.loot.LootTypes.CountRange;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link LootEntry}.
 *
 * <p>This record-based implementation represents an entry in a loot pool
 * that defines what items can be selected and how they should be modified.
 *
 * @since 1.0.0
 * @author Supatuck
 */
record LootEntryImpl(
        LootEntryType type,
        String itemType,
        UnifiedItemStack item,
        String tag,
        String tableKey,
        int weight,
        int quality,
        CountRange count,
        List<LootCondition> conditions,
        List<LootFunction> functions
) implements LootEntry {

    /**
     * Compact constructor for validation.
     */
    public LootEntryImpl {
        Objects.requireNonNull(type, "Type cannot be null");
        conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        functions = Objects.requireNonNull(functions, "Functions cannot be null");
    }

    @Override
    @NotNull
    public LootEntryType getType() {
        return type;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    @NotNull
    public List<LootCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    @Override
    @NotNull
    public List<LootFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }
}
