/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A condition that checks if the context location is within a specific region.
 *
 * <p>Region checking requires integration with a region plugin (e.g., WorldGuard).
 * If no region plugin is available, this condition will always fail.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Check if in spawn region
 * Condition inSpawn = Condition.region("spawn");
 *
 * // Check if not in PvP arena
 * Condition notInPvp = Condition.not(Condition.region("pvp_arena"));
 *
 * // Combine with other conditions
 * Condition canBuild = Condition.all(
 *     Condition.permission("build.use"),
 *     Condition.not(Condition.region("protected"))
 * );
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Condition
 */
public interface RegionCondition extends Condition {

    /**
     * Returns the region name being checked.
     *
     * @return the region name
     * @since 1.0.0
     */
    @NotNull
    String getRegion();

    @Override
    @NotNull
    default String getName() {
        return "region:" + getRegion();
    }

    @Override
    @NotNull
    default String getType() {
        return "region";
    }

    @Override
    @NotNull
    default String getDescription() {
        return "Must be in region: " + getRegion();
    }

    /**
     * Creates a region condition.
     *
     * <p>Note: The actual evaluation logic is provided by the implementation
     * in unified-world module, which has access to region plugins.</p>
     *
     * @param region the region name to check
     * @return the condition
     * @since 1.0.0
     */
    @NotNull
    static RegionCondition of(@NotNull String region) {
        Objects.requireNonNull(region, "region cannot be null");
        return new RegionCondition() {
            @Override
            public @NotNull String getRegion() {
                return region;
            }

            @Override
            public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
                // Default implementation - actual region checking is done by the
                // RegionConditionImpl in unified-world which has access to region plugins
                return context.getLocation()
                        .map(location -> {
                            // This is a placeholder - actual implementation in unified-world
                            // will override this with proper region checking
                            return ConditionResult.failure("Region checking not available");
                        })
                        .orElse(ConditionResult.failure("No location in context"));
            }
        };
    }
}
