/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.condition;

import sh.pcx.unified.condition.ConditionContext;
import sh.pcx.unified.condition.ConditionResult;
import sh.pcx.unified.condition.RegionCondition;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Implementation of {@link RegionCondition} that integrates with region plugins.
 *
 * <p>This implementation can be configured with a custom region checker that
 * integrates with plugins like WorldGuard, Residence, or GriefPrevention.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RegionConditionImpl implements RegionCondition {

    private final String region;
    private final BiFunction<UnifiedLocation, String, Boolean> regionChecker;

    private static volatile BiFunction<UnifiedLocation, String, Boolean> defaultChecker = null;

    /**
     * Creates a new region condition.
     *
     * @param region the region name to check
     */
    public RegionConditionImpl(@NotNull String region) {
        this(region, null);
    }

    /**
     * Creates a new region condition with a custom checker.
     *
     * @param region        the region name to check
     * @param regionChecker the function that checks if a location is in a region
     */
    public RegionConditionImpl(@NotNull String region, @Nullable BiFunction<UnifiedLocation, String, Boolean> regionChecker) {
        this.region = Objects.requireNonNull(region, "region cannot be null");
        this.regionChecker = regionChecker;
    }

    @Override
    public @NotNull String getRegion() {
        return region;
    }

    @Override
    public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
        Optional<UnifiedLocation> locationOpt = context.getLocation();
        if (locationOpt.isEmpty()) {
            return ConditionResult.failure("No location in context");
        }

        UnifiedLocation location = locationOpt.get();
        BiFunction<UnifiedLocation, String, Boolean> checker = getActiveChecker();

        if (checker == null) {
            return ConditionResult.failure("Region checking not available - no region plugin configured");
        }

        try {
            Boolean inRegion = checker.apply(location, region);
            if (inRegion != null && inRegion) {
                return ConditionResult.success("In region: " + region);
            }
            return ConditionResult.failure("Not in region: " + region);
        } catch (Exception e) {
            return ConditionResult.failure("Region check error: " + e.getMessage());
        }
    }

    private BiFunction<UnifiedLocation, String, Boolean> getActiveChecker() {
        if (regionChecker != null) {
            return regionChecker;
        }
        return defaultChecker;
    }

    /**
     * Sets the default region checker used by all RegionConditionImpl instances
     * that don't have a custom checker.
     *
     * <p>This should be called during plugin initialization to integrate with
     * the region plugin being used on the server.</p>
     *
     * <h2>Example WorldGuard Integration:</h2>
     * <pre>{@code
     * RegionConditionImpl.setDefaultChecker((location, regionName) -> {
     *     RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
     *     RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld().getHandle()));
     *     if (manager == null) return false;
     *
     *     BlockVector3 point = BlockVector3.at(location.getX(), location.getY(), location.getZ());
     *     ApplicableRegionSet set = manager.getApplicableRegions(point);
     *
     *     for (ProtectedRegion region : set) {
     *         if (region.getId().equalsIgnoreCase(regionName)) {
     *             return true;
     *         }
     *     }
     *     return false;
     * });
     * }</pre>
     *
     * @param checker the region checker function, or null to disable
     * @since 1.0.0
     */
    public static void setDefaultChecker(@Nullable BiFunction<UnifiedLocation, String, Boolean> checker) {
        defaultChecker = checker;
    }

    /**
     * Returns whether a default region checker is configured.
     *
     * @return true if region checking is available
     * @since 1.0.0
     */
    public static boolean isRegionCheckingAvailable() {
        return defaultChecker != null;
    }

    @Override
    public String toString() {
        return "RegionCondition{region='" + region + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionConditionImpl that = (RegionConditionImpl) o;
        return Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region);
    }
}
