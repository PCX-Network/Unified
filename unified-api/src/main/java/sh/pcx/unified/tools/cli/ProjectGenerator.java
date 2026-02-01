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
import java.util.concurrent.CompletableFuture;

/**
 * Generator for creating new plugin projects and components.
 *
 * <p>The ProjectGenerator provides programmatic access to the scaffolding
 * functionality available through the CLI tool.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ProjectGenerator generator = ProjectGenerator.create();
 *
 * // Create a new project
 * ProjectConfig config = ProjectConfig.builder()
 *     .name("my-plugin")
 *     .packageName("com.example.myplugin")
 *     .template(ProjectTemplate.BASIC)
 *     .build();
 *
 * generator.createProject(Path.of("./my-plugin"), config);
 *
 * // Generate a module
 * generator.generateModule(projectPath, "MyModule");
 *
 * // Generate a command
 * generator.generateCommand(projectPath, "MyCommand", CommandConfig.builder()
 *     .permission("myplugin.command")
 *     .aliases("mc", "myc")
 *     .build());
 *
 * // Generate a GUI
 * generator.generateGui(projectPath, "MyGui", GuiConfig.builder()
 *     .title("My GUI")
 *     .rows(6)
 *     .build());
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface ProjectGenerator {

    /**
     * Creates a new plugin project.
     *
     * @param targetDir the target directory
     * @param config    the project configuration
     * @return a future that completes when the project is created
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> createProject(
            @NotNull Path targetDir,
            @NotNull ProjectConfig config
    );

    /**
     * Generates a module class.
     *
     * @param projectDir the project directory
     * @param name       the module name
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateModule(
            @NotNull Path projectDir,
            @NotNull String name
    );

    /**
     * Generates a module class with configuration.
     *
     * @param projectDir the project directory
     * @param name       the module name
     * @param config     the module configuration
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateModule(
            @NotNull Path projectDir,
            @NotNull String name,
            @NotNull ModuleConfig config
    );

    /**
     * Generates a command class.
     *
     * @param projectDir the project directory
     * @param name       the command name
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateCommand(
            @NotNull Path projectDir,
            @NotNull String name
    );

    /**
     * Generates a command class with configuration.
     *
     * @param projectDir the project directory
     * @param name       the command name
     * @param config     the command configuration
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateCommand(
            @NotNull Path projectDir,
            @NotNull String name,
            @NotNull CommandConfig config
    );

    /**
     * Generates a GUI class.
     *
     * @param projectDir the project directory
     * @param name       the GUI name
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateGui(
            @NotNull Path projectDir,
            @NotNull String name
    );

    /**
     * Generates a GUI class with configuration.
     *
     * @param projectDir the project directory
     * @param name       the GUI name
     * @param config     the GUI configuration
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateGui(
            @NotNull Path projectDir,
            @NotNull String name,
            @NotNull GuiConfig config
    );

    /**
     * Generates a service class.
     *
     * @param projectDir the project directory
     * @param name       the service name
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateService(
            @NotNull Path projectDir,
            @NotNull String name
    );

    /**
     * Generates a listener class.
     *
     * @param projectDir the project directory
     * @param name       the listener name
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateListener(
            @NotNull Path projectDir,
            @NotNull String name
    );

    /**
     * Generates a configuration class.
     *
     * @param projectDir the project directory
     * @param name       the config name
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateConfig(
            @NotNull Path projectDir,
            @NotNull String name
    );

    /**
     * Generates CRUD operations for an entity.
     *
     * @param projectDir the project directory
     * @param entityName the entity name
     * @param fields     the entity fields (name to type mapping)
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<GenerationResult> generateCrud(
            @NotNull Path projectDir,
            @NotNull String entityName,
            @NotNull Map<String, String> fields
    );

    /**
     * Lists available templates.
     *
     * @return list of available templates
     * @since 1.0.0
     */
    @NotNull
    List<ProjectTemplate> availableTemplates();

    /**
     * Registers a custom template.
     *
     * @param template the template to register
     * @since 1.0.0
     */
    void registerTemplate(@NotNull ProjectTemplate template);

    /**
     * Creates a new ProjectGenerator instance.
     *
     * @return a new generator
     * @since 1.0.0
     */
    static ProjectGenerator create() {
        return new DefaultProjectGenerator();
    }

    /**
     * Result of a generation operation.
     *
     * @since 1.0.0
     */
    interface GenerationResult {

        /**
         * Returns whether the generation was successful.
         *
         * @return true if successful
         * @since 1.0.0
         */
        boolean isSuccess();

        /**
         * Returns the generated files.
         *
         * @return list of generated file paths
         * @since 1.0.0
         */
        @NotNull
        List<Path> generatedFiles();

        /**
         * Returns any error that occurred.
         *
         * @return the error, or null if successful
         * @since 1.0.0
         */
        @Nullable
        Throwable error();

        /**
         * Returns messages from the generation.
         *
         * @return list of messages
         * @since 1.0.0
         */
        @NotNull
        List<String> messages();
    }
}

/**
 * Default placeholder implementation.
 */
class DefaultProjectGenerator implements ProjectGenerator {
    @Override
    public @NotNull CompletableFuture<GenerationResult> createProject(@NotNull Path targetDir, @NotNull ProjectConfig config) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateModule(@NotNull Path projectDir, @NotNull String name) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateModule(@NotNull Path projectDir, @NotNull String name, @NotNull ModuleConfig config) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateCommand(@NotNull Path projectDir, @NotNull String name) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateCommand(@NotNull Path projectDir, @NotNull String name, @NotNull CommandConfig config) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateGui(@NotNull Path projectDir, @NotNull String name) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateGui(@NotNull Path projectDir, @NotNull String name, @NotNull GuiConfig config) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateService(@NotNull Path projectDir, @NotNull String name) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateListener(@NotNull Path projectDir, @NotNull String name) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateConfig(@NotNull Path projectDir, @NotNull String name) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateCrud(@NotNull Path projectDir, @NotNull String entityName, @NotNull Map<String, String> fields) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }

    @Override
    public @NotNull List<ProjectTemplate> availableTemplates() {
        return List.of();
    }

    @Override
    public void registerTemplate(@NotNull ProjectTemplate template) {
        throw new UnsupportedOperationException("Use unified-tools implementation");
    }
}
