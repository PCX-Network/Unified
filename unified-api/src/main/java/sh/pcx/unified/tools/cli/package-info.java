/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * CLI tools and code generation utilities.
 *
 * <p>This package provides programmatic access to the CLI scaffolding tools:
 * <ul>
 *   <li>{@link sh.pcx.unified.tools.cli.ProjectGenerator} - Generate new projects and components</li>
 *   <li>{@link sh.pcx.unified.tools.cli.ProjectConfig} - Project configuration</li>
 *   <li>{@link sh.pcx.unified.tools.cli.ProjectTemplate} - Project templates</li>
 * </ul>
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
 * // Generate components
 * generator.generateModule(projectPath, "MyModule");
 * generator.generateCommand(projectPath, "MyCommand");
 * generator.generateGui(projectPath, "MyGui");
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.tools.cli.ProjectGenerator
 */
package sh.pcx.unified.tools.cli;
