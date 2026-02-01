/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Task for generating plugin.yml from extension configuration.
 *
 * @since 1.0.0
 */
public abstract class GeneratePluginYmlTask extends DefaultTask {

    @Nested
    public abstract Property<UnifiedPluginExtension> getExtension();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    public GeneratePluginYmlTask() {
        getOutputDirectory().convention(
                getProject().getLayout().getBuildDirectory().dir("generated/resources/main")
        );
    }

    @TaskAction
    public void generate() throws IOException {
        UnifiedPluginExtension ext = getExtension().get();

        StringBuilder yaml = new StringBuilder();

        // Required fields
        appendRequired(yaml, "name", ext.getName().getOrNull());
        appendRequired(yaml, "version", ext.getVersion().getOrNull());

        // Main class
        if (ext.getMain().isPresent()) {
            yaml.append("main: ").append(ext.getMain().get()).append("\n");
        }

        // API version
        yaml.append("api-version: '").append(ext.getMinecraftVersion().get()).append("'\n");

        // Optional fields
        appendOptional(yaml, "description", ext.getDescription().getOrNull());
        appendOptional(yaml, "author", ext.getAuthor().getOrNull());
        appendOptional(yaml, "website", ext.getWebsite().getOrNull());
        appendOptional(yaml, "prefix", ext.getPrefix().getOrNull());

        // Authors list
        if (ext.getAuthors().isPresent() && !ext.getAuthors().get().isEmpty()) {
            yaml.append("authors:\n");
            for (String author : ext.getAuthors().get()) {
                yaml.append("  - ").append(author).append("\n");
            }
        }

        // Dependencies
        appendList(yaml, "depend", ext.getDepend().getOrElse(List.of()));
        appendList(yaml, "softdepend", ext.getSoftDepend().getOrElse(List.of()));
        appendList(yaml, "loadbefore", ext.getLoadBefore().getOrElse(List.of()));

        // Write file
        Path outputDir = getOutputDirectory().get().getAsFile().toPath();
        Files.createDirectories(outputDir);
        Path pluginYml = outputDir.resolve("plugin.yml");
        Files.writeString(pluginYml, yaml.toString());

        getLogger().lifecycle("Generated plugin.yml at {}", pluginYml);
    }

    private void appendRequired(StringBuilder yaml, String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(key + " is required in unified extension");
        }
        yaml.append(key).append(": ").append(value).append("\n");
    }

    private void appendOptional(StringBuilder yaml, String key, String value) {
        if (value != null && !value.isEmpty()) {
            yaml.append(key).append(": ").append(value).append("\n");
        }
    }

    private void appendList(StringBuilder yaml, String key, List<String> values) {
        if (!values.isEmpty()) {
            yaml.append(key).append(":\n");
            for (String value : values) {
                yaml.append("  - ").append(value).append("\n");
            }
        }
    }
}
