/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.advanced;

import sh.pcx.unified.config.ConfigException;
import sh.pcx.unified.config.format.ConfigFormat;
import sh.pcx.unified.config.format.ConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Merges multiple configuration sources into a single configuration.
 *
 * <p>ConfigMerger combines configurations from multiple files or nodes,
 * with later sources overriding earlier ones. It supports different
 * merge strategies for collections and provides hooks for custom
 * merge logic.</p>
 *
 * <h2>Merge Order</h2>
 * <p>Configurations are merged in order, with later sources taking
 * precedence:</p>
 * <ol>
 *   <li>Defaults (lowest priority)</li>
 *   <li>Base configuration</li>
 *   <li>Profile-specific overrides</li>
 *   <li>Local overrides</li>
 *   <li>Environment variables (highest priority)</li>
 * </ol>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ConfigMerger merger = new ConfigMerger(configLoader);
 *
 * // Merge multiple files
 * PluginConfig config = merger.merge(PluginConfig.class,
 *     defaultsPath,    // First (lowest priority)
 *     configPath,      // Second
 *     overridesPath    // Third (highest priority)
 * );
 * }</pre>
 *
 * <h2>With Merge Strategy</h2>
 * <pre>{@code
 * ConfigMerger merger = new ConfigMerger(configLoader)
 *     .withListStrategy(ListMergeStrategy.APPEND)
 *     .withMapStrategy(MapMergeStrategy.DEEP_MERGE);
 *
 * ConfigurationNode merged = merger.mergeNodes(node1, node2, node3);
 * }</pre>
 *
 * <h2>Custom Merge Logic</h2>
 * <pre>{@code
 * merger.withMergeHandler("permissions", (base, override) -> {
 *     // Custom logic for merging permissions
 *     Set<String> combined = new HashSet<>();
 *     combined.addAll(base.getList(String.class));
 *     combined.addAll(override.getList(String.class));
 *     return node.set(new ArrayList<>(combined));
 * });
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class ConfigMerger {

    private final ConfigLoader loader;
    private ListMergeStrategy listStrategy;
    private MapMergeStrategy mapStrategy;
    private final java.util.Map<String, BiFunction<ConfigurationNode, ConfigurationNode, ConfigurationNode>> customHandlers;

    /**
     * Creates a new ConfigMerger.
     *
     * @param loader the config loader to use
     */
    public ConfigMerger(@NotNull ConfigLoader loader) {
        this.loader = loader;
        this.listStrategy = ListMergeStrategy.REPLACE;
        this.mapStrategy = MapMergeStrategy.DEEP_MERGE;
        this.customHandlers = new java.util.HashMap<>();
    }

    /**
     * Sets the strategy for merging lists.
     *
     * @param strategy the list merge strategy
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ConfigMerger withListStrategy(@NotNull ListMergeStrategy strategy) {
        this.listStrategy = strategy;
        return this;
    }

    /**
     * Sets the strategy for merging maps.
     *
     * @param strategy the map merge strategy
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ConfigMerger withMapStrategy(@NotNull MapMergeStrategy strategy) {
        this.mapStrategy = strategy;
        return this;
    }

    /**
     * Adds a custom merge handler for a specific path.
     *
     * @param path the configuration path
     * @param handler the merge handler
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public ConfigMerger withMergeHandler(
            @NotNull String path,
            @NotNull BiFunction<ConfigurationNode, ConfigurationNode, ConfigurationNode> handler
    ) {
        customHandlers.put(path, handler);
        return this;
    }

    /**
     * Merges multiple configuration files.
     *
     * @param type the configuration class type
     * @param paths the configuration file paths (in merge order)
     * @param <T> the configuration type
     * @return the merged configuration
     * @throws ConfigException if loading or merging fails
     * @since 1.0.0
     */
    @NotNull
    @SafeVarargs
    public final <T> T merge(@NotNull Class<T> type, @NotNull Path... paths) {
        if (paths.length == 0) {
            throw new ConfigException("At least one path is required for merging");
        }

        ConfigurationNode result = null;

        for (Path path : paths) {
            if (java.nio.file.Files.exists(path)) {
                ConfigurationNode node = loader.load(path);
                if (result == null) {
                    result = node.copy();
                } else {
                    mergeInto(result, node, "");
                }
            }
        }

        if (result == null) {
            throw ConfigException.fileNotFound(paths[0]);
        }

        try {
            T config = result.get(type);
            if (config == null) {
                throw new ConfigException("Failed to deserialize merged configuration");
            }
            return config;
        } catch (Exception e) {
            if (e instanceof ConfigException) {
                throw (ConfigException) e;
            }
            throw ConfigException.serializationError(paths[0], "", e);
        }
    }

    /**
     * Merges multiple configuration nodes.
     *
     * @param nodes the nodes to merge (in order)
     * @return the merged node
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode mergeNodes(@NotNull ConfigurationNode... nodes) {
        if (nodes.length == 0) {
            throw new ConfigException("At least one node is required for merging");
        }

        ConfigurationNode result = nodes[0].copy();

        for (int i = 1; i < nodes.length; i++) {
            mergeInto(result, nodes[i], "");
        }

        return result;
    }

    /**
     * Merges multiple configuration nodes from a list.
     *
     * @param nodes the nodes to merge (in order)
     * @return the merged node
     * @since 1.0.0
     */
    @NotNull
    public ConfigurationNode mergeNodes(@NotNull List<ConfigurationNode> nodes) {
        return mergeNodes(nodes.toArray(new ConfigurationNode[0]));
    }

    /**
     * Merges a source node into a target node.
     *
     * @param target the target node (modified in place)
     * @param source the source node
     * @since 1.0.0
     */
    public void mergeInto(@NotNull ConfigurationNode target, @NotNull ConfigurationNode source) {
        mergeInto(target, source, "");
    }

    /**
     * Recursively merges source into target.
     */
    private void mergeInto(
            @NotNull ConfigurationNode target,
            @NotNull ConfigurationNode source,
            @NotNull String currentPath
    ) {
        // Check for custom handler
        BiFunction<ConfigurationNode, ConfigurationNode, ConfigurationNode> handler = customHandlers.get(currentPath);
        if (handler != null) {
            ConfigurationNode result = handler.apply(target, source);
            try {
                target.set(result.raw());
            } catch (Exception ignored) {
            }
            return;
        }

        if (source.isMap() && target.isMap()) {
            // Deep merge maps
            if (mapStrategy == MapMergeStrategy.DEEP_MERGE) {
                for (var entry : source.childrenMap().entrySet()) {
                    Object key = entry.getKey();
                    ConfigurationNode sourceChild = entry.getValue();
                    ConfigurationNode targetChild = target.node(key);

                    String childPath = currentPath.isEmpty()
                            ? key.toString()
                            : currentPath + "." + key;

                    if (targetChild.virtual() || !targetChild.isMap() || !sourceChild.isMap()) {
                        // Replace
                        try {
                            targetChild.set(sourceChild.raw());
                        } catch (Exception ignored) {
                        }
                    } else {
                        // Recurse
                        mergeInto(targetChild, sourceChild, childPath);
                    }
                }
            } else {
                // Replace entire map
                try {
                    target.set(source.raw());
                } catch (Exception ignored) {
                }
            }
        } else if (source.isList() && target.isList()) {
            // Merge lists based on strategy
            mergeLists(target, source);
        } else if (!source.virtual()) {
            // Replace value
            try {
                target.set(source.raw());
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Merges lists based on the list merge strategy.
     */
    private void mergeLists(
            @NotNull ConfigurationNode target,
            @NotNull ConfigurationNode source
    ) {
        try {
            switch (listStrategy) {
                case REPLACE:
                    target.set(source.raw());
                    break;

                case APPEND:
                    List<Object> appendList = new ArrayList<>(target.getList(Object.class));
                    appendList.addAll(source.getList(Object.class));
                    target.set(appendList);
                    break;

                case PREPEND:
                    List<Object> prependList = new ArrayList<>(source.getList(Object.class));
                    prependList.addAll(target.getList(Object.class));
                    target.set(prependList);
                    break;

                case UNIQUE:
                    List<Object> uniqueList = new ArrayList<>(target.getList(Object.class));
                    for (Object item : source.getList(Object.class)) {
                        if (!uniqueList.contains(item)) {
                            uniqueList.add(item);
                        }
                    }
                    target.set(uniqueList);
                    break;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Strategy for merging list values.
     */
    public enum ListMergeStrategy {
        /**
         * Replace the entire list with the new value.
         */
        REPLACE,

        /**
         * Append new values to the existing list.
         */
        APPEND,

        /**
         * Prepend new values before the existing list.
         */
        PREPEND,

        /**
         * Append only unique values (no duplicates).
         */
        UNIQUE
    }

    /**
     * Strategy for merging map values.
     */
    public enum MapMergeStrategy {
        /**
         * Replace the entire map with the new value.
         */
        REPLACE,

        /**
         * Recursively merge nested maps.
         */
        DEEP_MERGE
    }

    /**
     * Builder for creating merged configurations.
     *
     * @param <T> the configuration type
     */
    public static class MergeBuilder<T> {

        private final ConfigMerger merger;
        private final Class<T> type;
        private final List<ConfigurationNode> nodes;

        /**
         * Creates a new merge builder.
         *
         * @param merger the config merger
         * @param type the configuration type
         */
        public MergeBuilder(@NotNull ConfigMerger merger, @NotNull Class<T> type) {
            this.merger = merger;
            this.type = type;
            this.nodes = new ArrayList<>();
        }

        /**
         * Adds a configuration file to merge.
         *
         * @param path the file path
         * @return this for chaining
         */
        @NotNull
        public MergeBuilder<T> add(@NotNull Path path) {
            if (java.nio.file.Files.exists(path)) {
                nodes.add(merger.loader.load(path));
            }
            return this;
        }

        /**
         * Adds a configuration node to merge.
         *
         * @param node the node
         * @return this for chaining
         */
        @NotNull
        public MergeBuilder<T> add(@NotNull ConfigurationNode node) {
            nodes.add(node);
            return this;
        }

        /**
         * Adds a configuration file if it exists.
         *
         * @param path the file path
         * @return this for chaining
         */
        @NotNull
        public MergeBuilder<T> addIfExists(@NotNull Path path) {
            if (java.nio.file.Files.exists(path)) {
                nodes.add(merger.loader.load(path));
            }
            return this;
        }

        /**
         * Builds and returns the merged configuration.
         *
         * @return the merged configuration
         * @throws ConfigException if merging fails
         */
        @NotNull
        public T build() {
            if (nodes.isEmpty()) {
                throw new ConfigException("No configuration sources provided");
            }

            ConfigurationNode merged = merger.mergeNodes(nodes);

            try {
                T result = merged.get(type);
                if (result == null) {
                    throw new ConfigException("Failed to deserialize merged configuration");
                }
                return result;
            } catch (Exception e) {
                if (e instanceof ConfigException) {
                    throw (ConfigException) e;
                }
                throw new ConfigException("Failed to merge configurations", e);
            }
        }
    }

    /**
     * Creates a merge builder.
     *
     * @param type the configuration type
     * @param <T> the type parameter
     * @return the merge builder
     * @since 1.0.0
     */
    @NotNull
    public <T> MergeBuilder<T> builder(@NotNull Class<T> type) {
        return new MergeBuilder<>(this, type);
    }
}
