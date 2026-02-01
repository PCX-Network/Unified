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
 * Configuration for generating a new plugin project.
 *
 * <p>Use the {@link #builder()} method to create instances.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ProjectConfig config = ProjectConfig.builder()
 *     .name("my-awesome-plugin")
 *     .packageName("com.example.awesome")
 *     .description("An awesome plugin")
 *     .author("YourName")
 *     .version("1.0.0")
 *     .template(ProjectTemplate.BASIC)
 *     .minecraftVersion("1.21")
 *     .dependency("Vault")
 *     .dependency("LuckPerms")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ProjectGenerator
 */
public final class ProjectConfig {

    private final String name;
    private final String packageName;
    private final String description;
    private final String author;
    private final String version;
    private final ProjectTemplate template;
    private final String minecraftVersion;
    private final List<String> dependencies;
    private final List<String> softDependencies;
    private final boolean useKotlin;
    private final boolean includeTesting;
    private final String license;

    private ProjectConfig(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name is required");
        this.packageName = Objects.requireNonNull(builder.packageName, "packageName is required");
        this.description = builder.description;
        this.author = builder.author;
        this.version = builder.version;
        this.template = builder.template;
        this.minecraftVersion = builder.minecraftVersion;
        this.dependencies = List.copyOf(builder.dependencies);
        this.softDependencies = List.copyOf(builder.softDependencies);
        this.useKotlin = builder.useKotlin;
        this.includeTesting = builder.includeTesting;
        this.license = builder.license;
    }

    /**
     * Returns the plugin name.
     *
     * @return the name
     * @since 1.0.0
     */
    @NotNull
    public String name() {
        return name;
    }

    /**
     * Returns the package name.
     *
     * @return the package name
     * @since 1.0.0
     */
    @NotNull
    public String packageName() {
        return packageName;
    }

    /**
     * Returns the plugin description.
     *
     * @return the description, or null
     * @since 1.0.0
     */
    @Nullable
    public String description() {
        return description;
    }

    /**
     * Returns the plugin author.
     *
     * @return the author, or null
     * @since 1.0.0
     */
    @Nullable
    public String author() {
        return author;
    }

    /**
     * Returns the initial version.
     *
     * @return the version
     * @since 1.0.0
     */
    @NotNull
    public String version() {
        return version;
    }

    /**
     * Returns the project template.
     *
     * @return the template
     * @since 1.0.0
     */
    @NotNull
    public ProjectTemplate template() {
        return template;
    }

    /**
     * Returns the target Minecraft version.
     *
     * @return the Minecraft version
     * @since 1.0.0
     */
    @NotNull
    public String minecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Returns the list of hard dependencies.
     *
     * @return the dependencies
     * @since 1.0.0
     */
    @NotNull
    public List<String> dependencies() {
        return dependencies;
    }

    /**
     * Returns the list of soft dependencies.
     *
     * @return the soft dependencies
     * @since 1.0.0
     */
    @NotNull
    public List<String> softDependencies() {
        return softDependencies;
    }

    /**
     * Returns whether to use Kotlin.
     *
     * @return true if using Kotlin
     * @since 1.0.0
     */
    public boolean useKotlin() {
        return useKotlin;
    }

    /**
     * Returns whether to include testing setup.
     *
     * @return true if including tests
     * @since 1.0.0
     */
    public boolean includeTesting() {
        return includeTesting;
    }

    /**
     * Returns the license type.
     *
     * @return the license, or null
     * @since 1.0.0
     */
    @Nullable
    public String license() {
        return license;
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
     * Builder for {@link ProjectConfig}.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String name;
        private String packageName;
        private String description;
        private String author;
        private String version = "1.0.0-SNAPSHOT";
        private ProjectTemplate template = ProjectTemplate.BASIC;
        private String minecraftVersion = "1.21";
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> softDependencies = new ArrayList<>();
        private boolean useKotlin = false;
        private boolean includeTesting = true;
        private String license;

        private Builder() {}

        /**
         * Sets the plugin name.
         *
         * @param name the name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the package name.
         *
         * @param packageName the package name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder packageName(@NotNull String packageName) {
            this.packageName = packageName;
            return this;
        }

        /**
         * Sets the plugin description.
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
         * Sets the plugin author.
         *
         * @param author the author
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder author(@Nullable String author) {
            this.author = author;
            return this;
        }

        /**
         * Sets the initial version.
         *
         * @param version the version
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder version(@NotNull String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the project template.
         *
         * @param template the template
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder template(@NotNull ProjectTemplate template) {
            this.template = template;
            return this;
        }

        /**
         * Sets the target Minecraft version.
         *
         * @param version the Minecraft version
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder minecraftVersion(@NotNull String version) {
            this.minecraftVersion = version;
            return this;
        }

        /**
         * Adds a hard dependency.
         *
         * @param dependency the dependency name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dependency(@NotNull String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        /**
         * Adds a soft dependency.
         *
         * @param dependency the dependency name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder softDependency(@NotNull String dependency) {
            this.softDependencies.add(dependency);
            return this;
        }

        /**
         * Sets whether to use Kotlin.
         *
         * @param useKotlin true to use Kotlin
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder useKotlin(boolean useKotlin) {
            this.useKotlin = useKotlin;
            return this;
        }

        /**
         * Sets whether to include testing setup.
         *
         * @param includeTesting true to include tests
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder includeTesting(boolean includeTesting) {
            this.includeTesting = includeTesting;
            return this;
        }

        /**
         * Sets the license type.
         *
         * @param license the license (e.g., "MIT", "Apache-2.0")
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder license(@Nullable String license) {
            this.license = license;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the project config
         * @since 1.0.0
         */
        @NotNull
        public ProjectConfig build() {
            return new ProjectConfig(this);
        }
    }
}
