/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.metrics;

import sh.pcx.unified.tools.metrics.BStatsIntegration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link BStatsIntegration}.
 *
 * <p>This is a placeholder implementation that stores chart definitions.
 * The actual bStats submission requires platform-specific integration
 * (e.g., with Bukkit's plugin metrics system).
 *
 * @since 1.0.0
 */
public final class DefaultBStatsIntegration implements BStatsIntegration {

    private volatile int pluginId = -1;
    private volatile boolean enabled = false;
    private final Map<String, ChartDefinition> charts = new ConcurrentHashMap<>();

    @Override
    public void initialize(int pluginId) {
        this.pluginId = pluginId;
        this.enabled = true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && pluginId > 0;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void addSimplePie(@NotNull String chartId, @NotNull Callable<String> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.SIMPLE_PIE, supplier));
    }

    @Override
    public void addAdvancedPie(@NotNull String chartId, @NotNull Callable<Map<String, Integer>> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.ADVANCED_PIE, supplier));
    }

    @Override
    public void addDrilldownPie(@NotNull String chartId, @NotNull Callable<Map<String, Map<String, Integer>>> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.DRILLDOWN_PIE, supplier));
    }

    @Override
    public void addSingleLineChart(@NotNull String chartId, @NotNull Callable<Integer> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.SINGLE_LINE, supplier));
    }

    @Override
    public void addMultiLineChart(@NotNull String chartId, @NotNull Callable<Map<String, Integer>> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.MULTI_LINE, supplier));
    }

    @Override
    public void addSimpleBarChart(@NotNull String chartId, @NotNull Callable<Map<String, Integer>> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.SIMPLE_BAR, supplier));
    }

    @Override
    public void addAdvancedBarChart(@NotNull String chartId, @NotNull Callable<Map<String, int[]>> supplier) {
        charts.put(chartId, new ChartDefinition(ChartType.ADVANCED_BAR, supplier));
    }

    @Override
    public boolean removeChart(@NotNull String chartId) {
        return charts.remove(chartId) != null;
    }

    @Override
    public void submitData() {
        // Placeholder - actual implementation would submit to bStats
        if (!isEnabled()) {
            return;
        }

        // In a real implementation, this would:
        // 1. Collect data from all charts
        // 2. Format the data according to bStats API
        // 3. Submit to bStats servers
    }

    @Override
    public int getPluginId() {
        return pluginId;
    }

    @Override
    public void shutdown() {
        enabled = false;
        charts.clear();
    }

    /**
     * Returns all registered charts.
     *
     * @return the charts map
     */
    public Map<String, ChartDefinition> getCharts() {
        return charts;
    }

    /**
     * Chart types supported by bStats.
     */
    public enum ChartType {
        SIMPLE_PIE,
        ADVANCED_PIE,
        DRILLDOWN_PIE,
        SINGLE_LINE,
        MULTI_LINE,
        SIMPLE_BAR,
        ADVANCED_BAR
    }

    /**
     * A chart definition with type and data supplier.
     *
     * @param type     the chart type
     * @param supplier the data supplier
     */
    public record ChartDefinition(ChartType type, Callable<?> supplier) {}
}
