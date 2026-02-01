/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Immutable metadata record for a unified plugin.
 *
 * <p>Contains all identifying information about a plugin including its name,
 * version, authors, and dependencies. This record is typically loaded from
 * the plugin's descriptor file (e.g., plugin.yml or unified-plugin.json).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create plugin metadata
 * PluginMeta meta = new PluginMeta(
 *     "MyPlugin",
 *     "1.0.0",
 *     "A cool plugin for your server",
 *     List.of("AuthorName", "ContributorName"),
 *     "https://example.com/myplugin",
 *     List.of("UnifiedPluginAPI"),
 *     List.of("Vault", "PlaceholderAPI"),
 *     "1.21"
 * );
 *
 * // Access metadata
 * String name = meta.name();
 * List<String> authors = meta.authors();
 * }</pre>
 *
 * @param name          the unique name of the plugin, must not be null or empty
 * @param version       the semantic version string of the plugin (e.g., "1.0.0")
 * @param description   a brief description of the plugin's functionality, may be null
 * @param authors       list of author names, may be empty but not null
 * @param website       the plugin's website URL, may be null
 * @param dependencies  list of required plugin dependencies, may be empty but not null
 * @param softDependencies list of optional plugin dependencies, may be empty but not null
 * @param apiVersion    the minimum Minecraft API version required (e.g., "1.21")
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record PluginMeta(
        @NotNull String name,
        @NotNull String version,
        @Nullable String description,
        @NotNull List<String> authors,
        @Nullable String website,
        @NotNull List<String> dependencies,
        @NotNull List<String> softDependencies,
        @NotNull String apiVersion
) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if name is null or empty
     * @throws IllegalArgumentException if version is null or empty
     * @throws IllegalArgumentException if apiVersion is null or empty
     * @throws NullPointerException if authors, dependencies, or softDependencies is null
     */
    public PluginMeta {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plugin name must not be null or empty");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Plugin version must not be null or empty");
        }
        if (apiVersion == null || apiVersion.isBlank()) {
            throw new IllegalArgumentException("API version must not be null or empty");
        }
        if (authors == null) {
            throw new NullPointerException("Authors list must not be null");
        }
        if (dependencies == null) {
            throw new NullPointerException("Dependencies list must not be null");
        }
        if (softDependencies == null) {
            throw new NullPointerException("Soft dependencies list must not be null");
        }

        // Create immutable copies
        authors = List.copyOf(authors);
        dependencies = List.copyOf(dependencies);
        softDependencies = List.copyOf(softDependencies);
    }

    /**
     * Returns the plugin description as an Optional.
     *
     * @return an Optional containing the description if present, or empty
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the plugin website as an Optional.
     *
     * @return an Optional containing the website URL if present, or empty
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getWebsite() {
        return Optional.ofNullable(website);
    }

    /**
     * Returns the primary author of the plugin.
     *
     * @return an Optional containing the first author if any authors exist, or empty
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getPrimaryAuthor() {
        return authors.isEmpty() ? Optional.empty() : Optional.of(authors.getFirst());
    }

    /**
     * Checks if this plugin has any hard dependencies.
     *
     * @return true if the plugin has at least one dependency
     * @since 1.0.0
     */
    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    /**
     * Checks if this plugin has any soft dependencies.
     *
     * @return true if the plugin has at least one soft dependency
     * @since 1.0.0
     */
    public boolean hasSoftDependencies() {
        return !softDependencies.isEmpty();
    }

    /**
     * Creates a builder for constructing PluginMeta instances.
     *
     * @param name the plugin name
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Builder class for creating {@link PluginMeta} instances.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * PluginMeta meta = PluginMeta.builder("MyPlugin")
     *     .version("1.0.0")
     *     .description("A cool plugin")
     *     .author("AuthorName")
     *     .website("https://example.com")
     *     .dependency("UnifiedPluginAPI")
     *     .apiVersion("1.21")
     *     .build();
     * }</pre>
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final String name;
        private String version = "1.0.0";
        private String description;
        private final java.util.ArrayList<String> authors = new java.util.ArrayList<>();
        private String website;
        private final java.util.ArrayList<String> dependencies = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> softDependencies = new java.util.ArrayList<>();
        private String apiVersion = "1.21";

        private Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Sets the plugin version.
         *
         * @param version the semantic version string
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder version(@NotNull String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the plugin description.
         *
         * @param description the plugin description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds an author to the plugin.
         *
         * @param author the author name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder author(@NotNull String author) {
            this.authors.add(author);
            return this;
        }

        /**
         * Sets all authors for the plugin.
         *
         * @param authors the list of author names
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder authors(@NotNull List<String> authors) {
            this.authors.clear();
            this.authors.addAll(authors);
            return this;
        }

        /**
         * Sets the plugin website URL.
         *
         * @param website the website URL
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder website(@Nullable String website) {
            this.website = website;
            return this;
        }

        /**
         * Adds a required dependency.
         *
         * @param dependency the plugin name this plugin depends on
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dependency(@NotNull String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        /**
         * Sets all required dependencies.
         *
         * @param dependencies the list of plugin names this plugin depends on
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dependencies(@NotNull List<String> dependencies) {
            this.dependencies.clear();
            this.dependencies.addAll(dependencies);
            return this;
        }

        /**
         * Adds a soft (optional) dependency.
         *
         * @param softDependency the plugin name this plugin optionally depends on
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder softDependency(@NotNull String softDependency) {
            this.softDependencies.add(softDependency);
            return this;
        }

        /**
         * Sets all soft (optional) dependencies.
         *
         * @param softDependencies the list of plugin names this plugin optionally depends on
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder softDependencies(@NotNull List<String> softDependencies) {
            this.softDependencies.clear();
            this.softDependencies.addAll(softDependencies);
            return this;
        }

        /**
         * Sets the minimum API version required.
         *
         * @param apiVersion the Minecraft API version (e.g., "1.21")
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder apiVersion(@NotNull String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        /**
         * Builds the PluginMeta instance.
         *
         * @return a new PluginMeta instance
         * @throws IllegalArgumentException if required fields are invalid
         * @since 1.0.0
         */
        @NotNull
        public PluginMeta build() {
            return new PluginMeta(
                    name,
                    version,
                    description,
                    authors,
                    website,
                    dependencies,
                    softDependencies,
                    apiVersion
            );
        }
    }
}
