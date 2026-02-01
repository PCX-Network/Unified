/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for generating a module class.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ModuleConfig config = ModuleConfig.builder()
 *     .description("Handles player data management")
 *     .enabledByDefault(true)
 *     .configurable(true)
 *     .dependency("DatabaseModule")
 *     .build();
 *
 * generator.generateModule(projectPath, "PlayerDataModule", config);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ProjectGenerator
 */
public final class ModuleConfig {

    private final String description;
    private final boolean enabledByDefault;
    private final boolean configurable;
    private final List<String> dependencies;
    private final List<String> softDependencies;

    private ModuleConfig(Builder builder) {
        this.description = builder.description;
        this.enabledByDefault = builder.enabledByDefault;
        this.configurable = builder.configurable;
        this.dependencies = List.copyOf(builder.dependencies);
        this.softDependencies = List.copyOf(builder.softDependencies);
    }

    /**
     * Returns the module description.
     *
     * @return the description, or null
     * @since 1.0.0
     */
    @Nullable
    public String description() {
        return description;
    }

    /**
     * Returns whether the module is enabled by default.
     *
     * @return true if enabled by default
     * @since 1.0.0
     */
    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    /**
     * Returns whether the module is configurable.
     *
     * @return true if configurable
     * @since 1.0.0
     */
    public boolean configurable() {
        return configurable;
    }

    /**
     * Returns the module dependencies.
     *
     * @return the dependencies
     * @since 1.0.0
     */
    @NotNull
    public List<String> dependencies() {
        return dependencies;
    }

    /**
     * Returns the soft dependencies.
     *
     * @return the soft dependencies
     * @since 1.0.0
     */
    @NotNull
    public List<String> softDependencies() {
        return softDependencies;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default configuration.
     *
     * @return the default config
     * @since 1.0.0
     */
    @NotNull
    public static ModuleConfig defaults() {
        return builder().build();
    }

    /**
     * Builder for {@link ModuleConfig}.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String description;
        private boolean enabledByDefault = true;
        private boolean configurable = false;
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> softDependencies = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the module description.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets whether the module is enabled by default.
         *
         * @param enabled true to enable by default
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder enabledByDefault(boolean enabled) {
            this.enabledByDefault = enabled;
            return this;
        }

        /**
         * Sets whether the module is configurable.
         *
         * @param configurable true to make configurable
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder configurable(boolean configurable) {
            this.configurable = configurable;
            return this;
        }

        /**
         * Adds a hard dependency on another module.
         *
         * @param moduleName the module name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dependency(@NotNull String moduleName) {
            this.dependencies.add(moduleName);
            return this;
        }

        /**
         * Adds a soft dependency on another module.
         *
         * @param moduleName the module name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder softDependency(@NotNull String moduleName) {
            this.softDependencies.add(moduleName);
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the module config
         * @since 1.0.0
         */
        @NotNull
        public ModuleConfig build() {
            return new ModuleConfig(this);
        }
    }
}
