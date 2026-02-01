/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A project template for scaffolding new plugins.
 *
 * <p>Templates define the structure and initial files for a new plugin project.
 * The framework provides several built-in templates, and custom templates
 * can be registered.
 *
 * <h2>Built-in Templates</h2>
 * <ul>
 *   <li>{@link #BASIC} - Simple plugin with basic structure</li>
 *   <li>{@link #ECONOMY} - Economy plugin with Vault integration</li>
 *   <li>{@link #MINIGAME} - Minigame plugin with arena support</li>
 *   <li>{@link #LIBRARY} - Library plugin for other plugins to depend on</li>
 *   <li>{@link #FULL} - Full-featured plugin with all components</li>
 * </ul>
 *
 * <h2>Custom Templates</h2>
 * <pre>{@code
 * ProjectTemplate custom = ProjectTemplate.custom("my-template")
 *     .description("My custom template")
 *     .file("src/main/java/{{PACKAGE}}/{{NAME}}Plugin.java", "main-class.java.template")
 *     .file("src/main/resources/plugin.yml", "plugin.yml.template")
 *     .variable("CUSTOM_VAR", "default-value")
 *     .build();
 *
 * generator.registerTemplate(custom);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ProjectGenerator
 */
public interface ProjectTemplate {

    /**
     * Basic plugin template with minimal structure.
     */
    ProjectTemplate BASIC = BuiltInTemplate.BASIC;

    /**
     * Economy plugin template with Vault integration.
     */
    ProjectTemplate ECONOMY = BuiltInTemplate.ECONOMY;

    /**
     * Minigame plugin template with arena support.
     */
    ProjectTemplate MINIGAME = BuiltInTemplate.MINIGAME;

    /**
     * Library plugin template for shared code.
     */
    ProjectTemplate LIBRARY = BuiltInTemplate.LIBRARY;

    /**
     * Full-featured plugin template with all components.
     */
    ProjectTemplate FULL = BuiltInTemplate.FULL;

    /**
     * Returns the template ID.
     *
     * @return the unique template ID
     * @since 1.0.0
     */
    @NotNull
    String id();

    /**
     * Returns the template display name.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    String displayName();

    /**
     * Returns the template description.
     *
     * @return the description, or null
     * @since 1.0.0
     */
    @Nullable
    String description();

    /**
     * Returns the files to generate.
     *
     * @return list of file definitions
     * @since 1.0.0
     */
    @NotNull
    List<FileDefinition> files();

    /**
     * Returns the default variable values.
     *
     * @return map of variable name to default value
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> defaultVariables();

    /**
     * Returns the dependencies to add.
     *
     * @return list of dependencies
     * @since 1.0.0
     */
    @NotNull
    List<String> dependencies();

    /**
     * Returns whether this is a built-in template.
     *
     * @return true if built-in
     * @since 1.0.0
     */
    boolean isBuiltIn();

    /**
     * Creates a custom template builder.
     *
     * @param id the template ID
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder custom(@NotNull String id) {
        return new Builder(id);
    }

    /**
     * A file to be generated from a template.
     *
     * @param outputPath     the output path (may contain variables)
     * @param templateSource the template source file or content
     * @param optional       whether the file is optional
     * @since 1.0.0
     */
    record FileDefinition(
            @NotNull String outputPath,
            @NotNull String templateSource,
            boolean optional
    ) {
        /**
         * Creates a required file definition.
         *
         * @param outputPath     the output path
         * @param templateSource the template source
         * @return the file definition
         */
        public static FileDefinition of(@NotNull String outputPath, @NotNull String templateSource) {
            return new FileDefinition(outputPath, templateSource, false);
        }

        /**
         * Creates an optional file definition.
         *
         * @param outputPath     the output path
         * @param templateSource the template source
         * @return the file definition
         */
        public static FileDefinition optional(@NotNull String outputPath, @NotNull String templateSource) {
            return new FileDefinition(outputPath, templateSource, true);
        }
    }

    /**
     * Builder for custom templates.
     *
     * @since 1.0.0
     */
    final class Builder {
        private final String id;
        private String name;
        private String description;
        private final java.util.List<FileDefinition> files = new java.util.ArrayList<>();
        private final java.util.Map<String, String> variables = new java.util.HashMap<>();
        private final java.util.List<String> dependencies = new java.util.ArrayList<>();

        private Builder(String id) {
            this.id = id;
            this.name = id;
        }

        /**
         * Sets the template name.
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
         * Sets the template description.
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
         * Adds a file to generate.
         *
         * @param outputPath     the output path
         * @param templateSource the template source
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder file(@NotNull String outputPath, @NotNull String templateSource) {
            this.files.add(FileDefinition.of(outputPath, templateSource));
            return this;
        }

        /**
         * Adds an optional file to generate.
         *
         * @param outputPath     the output path
         * @param templateSource the template source
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder optionalFile(@NotNull String outputPath, @NotNull String templateSource) {
            this.files.add(FileDefinition.optional(outputPath, templateSource));
            return this;
        }

        /**
         * Adds a default variable value.
         *
         * @param name  the variable name
         * @param value the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder variable(@NotNull String name, @NotNull String value) {
            this.variables.put(name, value);
            return this;
        }

        /**
         * Adds a dependency.
         *
         * @param dependency the dependency
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder dependency(@NotNull String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        /**
         * Builds the template.
         *
         * @return the custom template
         * @since 1.0.0
         */
        @NotNull
        public ProjectTemplate build() {
            return new ProjectTemplate() {
                @Override
                public @NotNull String id() { return id; }

                @Override
                public @NotNull String displayName() { return name; }

                @Override
                public @Nullable String description() { return description; }

                @Override
                public @NotNull List<FileDefinition> files() { return List.copyOf(files); }

                @Override
                public @NotNull Map<String, String> defaultVariables() { return Map.copyOf(variables); }

                @Override
                public @NotNull List<String> dependencies() { return List.copyOf(dependencies); }

                @Override
                public boolean isBuiltIn() { return false; }
            };
        }
    }

    /**
     * Built-in templates.
     */
    enum BuiltInTemplate implements ProjectTemplate {
        BASIC("basic", "Basic Plugin", "Simple plugin with basic structure"),
        ECONOMY("economy", "Economy Plugin", "Economy plugin with Vault integration"),
        MINIGAME("minigame", "Minigame Plugin", "Minigame plugin with arena support"),
        LIBRARY("library", "Library Plugin", "Library plugin for other plugins"),
        FULL("full", "Full Plugin", "Full-featured plugin with all components");

        private final String id;
        private final String displayName;
        private final String description;

        BuiltInTemplate(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public @NotNull String id() { return id; }

        @Override
        public @NotNull String displayName() { return displayName; }

        @Override
        public @Nullable String description() { return description; }

        @Override
        public @NotNull List<FileDefinition> files() { return List.of(); }

        @Override
        public @NotNull Map<String, String> defaultVariables() { return Map.of(); }

        @Override
        public @NotNull List<String> dependencies() { return List.of(); }

        @Override
        public boolean isBuiltIn() { return true; }
    }
}
