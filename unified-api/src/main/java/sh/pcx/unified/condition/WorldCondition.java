/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * A condition that checks if the context is in one of specified worlds.
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Check for a single world
 * Condition inSurvival = Condition.world("survival");
 *
 * // Check for multiple worlds
 * Condition inNether = Condition.world("survival_nether", "world_nether");
 *
 * // Check for any survival dimension
 * Condition inSurvivalDimension = Condition.world("survival", "survival_nether", "survival_end");
 *
 * // Negate world check
 * Condition notInHub = Condition.not(Condition.world("hub"));
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public interface WorldCondition extends Condition {

    /**
     * Returns the set of world names this condition matches.
     *
     * @return the world names
     * @since 1.0.0
     */
    @NotNull
    Set<String> getWorlds();

    @Override
    @NotNull
    default String getName() {
        return "world:" + String.join(",", getWorlds());
    }

    @Override
    @NotNull
    default String getType() {
        return "world";
    }

    @Override
    @NotNull
    default String getDescription() {
        Set<String> worlds = getWorlds();
        if (worlds.size() == 1) {
            return "Must be in world: " + worlds.iterator().next();
        }
        return "Must be in one of: " + String.join(", ", worlds);
    }

    @Override
    @NotNull
    default ConditionResult evaluate(@NotNull ConditionContext context) {
        return context.getWorldName()
                .map(worldName -> {
                    if (getWorlds().contains(worldName)) {
                        return ConditionResult.success("In world: " + worldName);
                    }
                    return ConditionResult.failure("Not in required world. Current: " + worldName +
                            ", Required: " + String.join(" or ", getWorlds()));
                })
                .orElse(ConditionResult.failure("No world in context"));
    }

    /**
     * Creates a world condition.
     *
     * @param worlds the world names to match (any of them)
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static WorldCondition of(@NotNull String... worlds) {
        Objects.requireNonNull(worlds, "worlds cannot be null");
        if (worlds.length == 0) {
            throw new IllegalArgumentException("At least one world must be specified");
        }
        Set<String> worldSet = Set.copyOf(Arrays.asList(worlds));
        return () -> worldSet;
    }

    /**
     * Creates a world condition from a set.
     *
     * @param worlds the world names to match
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static WorldCondition of(@NotNull Set<String> worlds) {
        Objects.requireNonNull(worlds, "worlds cannot be null");
        if (worlds.isEmpty()) {
            throw new IllegalArgumentException("At least one world must be specified");
        }
        Set<String> worldSet = Set.copyOf(worlds);
        return () -> worldSet;
    }
}
