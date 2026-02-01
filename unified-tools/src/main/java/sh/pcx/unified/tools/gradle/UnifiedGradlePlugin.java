/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin for building plugins with the UnifiedPlugin API.
 *
 * <p>Usage in build.gradle.kts:
 * <pre>{@code
 * plugins {
 *     id("sh.pcx.unified") version "1.0.0"
 * }
 *
 * unified {
 *     name = "MyPlugin"
 *     version = "1.0.0"
 *     author = "YourName"
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
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class UnifiedGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Apply Java plugin if not already applied
        project.getPlugins().apply(JavaPlugin.class);

        // Create extension
        UnifiedPluginExtension extension = project.getExtensions()
                .create("unified", UnifiedPluginExtension.class, project);

        // Register tasks
        registerTasks(project, extension);

        // Configure project after evaluation
        project.afterEvaluate(p -> configureProject(p, extension));
    }

    private void registerTasks(Project project, UnifiedPluginExtension extension) {
        // Generate plugin.yml task
        TaskProvider<GeneratePluginYmlTask> generatePluginYml = project.getTasks()
                .register("generatePluginYml", GeneratePluginYmlTask.class, task -> {
                    task.setGroup("unified");
                    task.setDescription("Generates plugin.yml from extension configuration");
                    task.getExtension().set(extension);
                });

        // Validate plugin task
        project.getTasks().register("validatePlugin", task -> {
            task.setGroup("unified");
            task.setDescription("Validates plugin configuration");
            task.doLast(t -> {
                if (extension.getName().getOrNull() == null) {
                    throw new IllegalStateException("Plugin name is required");
                }
                project.getLogger().lifecycle("Plugin configuration is valid");
            });
        });

        // Make processResources depend on generatePluginYml
        project.getTasks().named("processResources", task -> {
            task.dependsOn(generatePluginYml);
        });
    }

    private void configureProject(Project project, UnifiedPluginExtension extension) {
        // Add UnifiedPlugin API dependency
        project.getDependencies().add("implementation",
                "sh.pcx:unified-api:" + extension.getApiVersion().getOrElse("1.0.0-SNAPSHOT"));

        // Configure Java toolchain
        project.getExtensions().configure("java", java -> {
            // Java 21 by default
        });
    }
}
