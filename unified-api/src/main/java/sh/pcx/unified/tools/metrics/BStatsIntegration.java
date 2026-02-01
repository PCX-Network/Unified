/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Integration with bStats for plugin analytics.
 *
 * <p>bStats is a free and open-source service that provides analytics
 * for Minecraft plugins. This integration allows you to submit custom
 * charts and data to bStats.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * BStatsIntegration bstats = metrics.bstats();
 *
 * // Initialize with plugin ID
 * bstats.initialize(12345);
 *
 * // Add simple pie chart
 * bstats.addSimplePie("storage_type", () -> config.getStorageType());
 *
 * // Add advanced pie chart
 * bstats.addAdvancedPie("features", () -> Map.of(
 *     "feature1", featureCount1,
 *     "feature2", featureCount2
 * ));
 *
 * // Add drilldown pie chart
 * bstats.addDrilldownPie("versions", () -> Map.of(
 *     "1.20", Map.of("1.20.1", 10, "1.20.2", 20),
 *     "1.21", Map.of("1.21.0", 30)
 * ));
 *
 * // Add single line chart
 * bstats.addSingleLineChart("players", () -> Bukkit.getOnlinePlayers().size());
 *
 * // Add multi-line chart
 * bstats.addMultiLineChart("statistics", () -> Map.of(
 *     "active", activeCount,
 *     "total", totalCount
 * ));
 *
 * // Add simple bar chart
 * bstats.addSimpleBarChart("regions", () -> Map.of(
 *     "NA", naCount,
 *     "EU", euCount
 * ));
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * metrics:
 *   bstats:
 *     enabled: true
 *     pluginId: 12345
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MetricsService
 */
public interface BStatsIntegration {

    /**
     * Initializes bStats with the given plugin ID.
     *
     * @param pluginId the bStats plugin ID
     * @since 1.0.0
     */
    void initialize(int pluginId);

    /**
     * Checks if bStats is enabled.
     *
     * @return true if enabled
     * @since 1.0.0
     */
    boolean isEnabled();

    /**
     * Sets whether bStats is enabled.
     *
     * @param enabled true to enable
     * @since 1.0.0
     */
    void setEnabled(boolean enabled);

    /**
     * Adds a simple pie chart.
     *
     * <p>Simple pie charts display a single value.
     *
     * @param chartId  the chart ID
     * @param supplier the value supplier
     * @since 1.0.0
     */
    void addSimplePie(@NotNull String chartId, @NotNull Callable<String> supplier);

    /**
     * Adds an advanced pie chart.
     *
     * <p>Advanced pie charts display multiple values with counts.
     *
     * @param chartId  the chart ID
     * @param supplier the values supplier (map of value to count)
     * @since 1.0.0
     */
    void addAdvancedPie(@NotNull String chartId, @NotNull Callable<Map<String, Integer>> supplier);

    /**
     * Adds a drilldown pie chart.
     *
     * <p>Drilldown pie charts have two levels of data.
     *
     * @param chartId  the chart ID
     * @param supplier the values supplier (map of category to sub-values)
     * @since 1.0.0
     */
    void addDrilldownPie(@NotNull String chartId,
                          @NotNull Callable<Map<String, Map<String, Integer>>> supplier);

    /**
     * Adds a single line chart.
     *
     * <p>Single line charts show a single value over time.
     *
     * @param chartId  the chart ID
     * @param supplier the value supplier
     * @since 1.0.0
     */
    void addSingleLineChart(@NotNull String chartId, @NotNull Callable<Integer> supplier);

    /**
     * Adds a multi-line chart.
     *
     * <p>Multi-line charts show multiple values over time.
     *
     * @param chartId  the chart ID
     * @param supplier the values supplier (map of line name to value)
     * @since 1.0.0
     */
    void addMultiLineChart(@NotNull String chartId, @NotNull Callable<Map<String, Integer>> supplier);

    /**
     * Adds a simple bar chart.
     *
     * <p>Bar charts display multiple named values.
     *
     * @param chartId  the chart ID
     * @param supplier the values supplier
     * @since 1.0.0
     */
    void addSimpleBarChart(@NotNull String chartId, @NotNull Callable<Map<String, Integer>> supplier);

    /**
     * Adds an advanced bar chart.
     *
     * <p>Advanced bar charts have stacked bars.
     *
     * @param chartId  the chart ID
     * @param supplier the values supplier (map of bar to stack values)
     * @since 1.0.0
     */
    void addAdvancedBarChart(@NotNull String chartId,
                              @NotNull Callable<Map<String, int[]>> supplier);

    /**
     * Removes a chart.
     *
     * @param chartId the chart ID
     * @return true if the chart was removed
     * @since 1.0.0
     */
    boolean removeChart(@NotNull String chartId);

    /**
     * Submits data immediately instead of waiting for the next scheduled submission.
     *
     * @since 1.0.0
     */
    void submitData();

    /**
     * Returns the plugin ID.
     *
     * @return the plugin ID, or -1 if not initialized
     * @since 1.0.0
     */
    int getPluginId();

    /**
     * Shuts down the bStats integration.
     *
     * @since 1.0.0
     */
    void shutdown();
}
