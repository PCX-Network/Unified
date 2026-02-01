/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension for configuring UnifiedPlugin projects.
 *
 * <p>Example usage:
 * <pre>{@code
 * unified {
 *     name = "MyPlugin"
 *     version = "1.0.0"
 *     description = "My awesome plugin"
 *     author = "YourName"
 *     website = "https://example.com"
 *
 *     apiVersion = "1.21"
 *
 *     depend = listOf("Vault")
 *     softDepend = listOf("LuckPerms")
 *
 *     modules {
 *         enabled = true
 *         autoDiscovery = true
 *     }
 *
 *     metrics {
 *         bstats {
 *             enabled = true
 *             pluginId = 12345
 *         }
 *         prometheus {
 *             enabled = true
 *             port = 9090
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public abstract class UnifiedPluginExtension {

    private final Project project;
    private final ModulesExtension modules;
    private final MetricsExtension metrics;

    @Inject
    public UnifiedPluginExtension(Project project) {
        this.project = project;
        this.modules = project.getObjects().newInstance(ModulesExtension.class);
        this.metrics = project.getObjects().newInstance(MetricsExtension.class, project);

        // Set defaults
        getVersion().convention(project.getVersion().toString());
        getApiVersion().convention("1.0.0-SNAPSHOT");
        getMinecraftVersion().convention("1.21");
    }

    /**
     * The plugin name.
     */
    public abstract Property<String> getName();

    /**
     * The plugin version.
     */
    public abstract Property<String> getVersion();

    /**
     * The plugin description.
     */
    public abstract Property<String> getDescription();

    /**
     * The plugin author.
     */
    public abstract Property<String> getAuthor();

    /**
     * List of plugin authors.
     */
    public abstract ListProperty<String> getAuthors();

    /**
     * The plugin website.
     */
    public abstract Property<String> getWebsite();

    /**
     * The main class (auto-detected if not specified).
     */
    public abstract Property<String> getMain();

    /**
     * The UnifiedPlugin API version to use.
     */
    public abstract Property<String> getApiVersion();

    /**
     * The target Minecraft version.
     */
    public abstract Property<String> getMinecraftVersion();

    /**
     * Hard dependencies.
     */
    public abstract ListProperty<String> getDepend();

    /**
     * Soft dependencies.
     */
    public abstract ListProperty<String> getSoftDepend();

    /**
     * Plugins to load before this one.
     */
    public abstract ListProperty<String> getLoadBefore();

    /**
     * The plugin prefix for logging.
     */
    public abstract Property<String> getPrefix();

    /**
     * Configure modules settings.
     */
    public void modules(Action<ModulesExtension> action) {
        action.execute(modules);
    }

    /**
     * Get modules extension.
     */
    public ModulesExtension getModules() {
        return modules;
    }

    /**
     * Configure metrics settings.
     */
    public void metrics(Action<MetricsExtension> action) {
        action.execute(metrics);
    }

    /**
     * Get metrics extension.
     */
    public MetricsExtension getMetrics() {
        return metrics;
    }

    /**
     * Modules configuration.
     */
    public static abstract class ModulesExtension {

        /**
         * Whether modules are enabled.
         */
        public abstract Property<Boolean> getEnabled();

        /**
         * Whether to auto-discover modules.
         */
        public abstract Property<Boolean> getAutoDiscovery();

        /**
         * Package to scan for modules.
         */
        public abstract Property<String> getScanPackage();
    }

    /**
     * Metrics configuration.
     */
    public static abstract class MetricsExtension {

        private final BStatsExtension bstats;
        private final PrometheusExtension prometheus;

        @Inject
        public MetricsExtension(Project project) {
            this.bstats = project.getObjects().newInstance(BStatsExtension.class);
            this.prometheus = project.getObjects().newInstance(PrometheusExtension.class);
        }

        /**
         * Configure bStats.
         */
        public void bstats(Action<BStatsExtension> action) {
            action.execute(bstats);
        }

        public BStatsExtension getBstats() {
            return bstats;
        }

        /**
         * Configure Prometheus.
         */
        public void prometheus(Action<PrometheusExtension> action) {
            action.execute(prometheus);
        }

        public PrometheusExtension getPrometheus() {
            return prometheus;
        }
    }

    /**
     * bStats configuration.
     */
    public static abstract class BStatsExtension {

        /**
         * Whether bStats is enabled.
         */
        public abstract Property<Boolean> getEnabled();

        /**
         * The bStats plugin ID.
         */
        public abstract Property<Integer> getPluginId();
    }

    /**
     * Prometheus configuration.
     */
    public static abstract class PrometheusExtension {

        public PrometheusExtension() {
            getPort().convention(9090);
            getPath().convention("/metrics");
        }

        /**
         * Whether Prometheus export is enabled.
         */
        public abstract Property<Boolean> getEnabled();

        /**
         * The port to expose metrics on.
         */
        public abstract Property<Integer> getPort();

        /**
         * The path to expose metrics on.
         */
        public abstract Property<String> getPath();
    }
}
