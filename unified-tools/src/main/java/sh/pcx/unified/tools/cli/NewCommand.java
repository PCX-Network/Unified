/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import sh.pcx.unified.tools.cli.ProjectConfig;
import sh.pcx.unified.tools.cli.ProjectTemplate;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command for creating new plugin projects.
 *
 * <p>Examples:
 * <pre>
 * unified new my-plugin
 * unified new my-plugin --template=economy
 * unified new my-plugin --package=com.example
 * </pre>
 *
 * @since 1.0.0
 */
@Command(
        name = "new",
        description = "Create a new plugin project"
)
public final class NewCommand implements Callable<Integer> {

    @ParentCommand
    private UnifiedCli parent;

    @Parameters(index = "0", description = "Project name")
    private String name;

    @Option(names = {"-t", "--template"}, description = "Project template (basic, economy, minigame, library, full)")
    private String template = "basic";

    @Option(names = {"-p", "--package"}, description = "Package name (default: derived from name)")
    private String packageName;

    @Option(names = {"-d", "--description"}, description = "Plugin description")
    private String description;

    @Option(names = {"-a", "--author"}, description = "Plugin author")
    private String author;

    @Option(names = {"--minecraft-version"}, description = "Target Minecraft version")
    private String minecraftVersion = "1.21";

    @Option(names = {"--kotlin"}, description = "Use Kotlin instead of Java")
    private boolean useKotlin = false;

    @Option(names = {"--no-tests"}, description = "Skip test setup")
    private boolean noTests = false;

    @Option(names = {"-o", "--output"}, description = "Output directory")
    private Path outputDir;

    @Override
    public Integer call() throws Exception {
        // Derive package name if not specified
        if (packageName == null) {
            packageName = "com.example." + sanitizePackageName(name);
        }

        // Resolve output directory
        Path targetDir = outputDir != null ? outputDir : Path.of(name);

        // Check if directory exists
        if (Files.exists(targetDir) && !isEmpty(targetDir)) {
            System.err.println("Error: Directory " + targetDir + " already exists and is not empty");
            return 1;
        }

        System.out.println("Creating new plugin project: " + name);
        System.out.println("  Template: " + template);
        System.out.println("  Package: " + packageName);
        System.out.println("  Output: " + targetDir.toAbsolutePath());

        // Build configuration
        ProjectConfig.Builder configBuilder = ProjectConfig.builder()
                .name(name)
                .packageName(packageName)
                .template(resolveTemplate(template))
                .minecraftVersion(minecraftVersion)
                .useKotlin(useKotlin)
                .includeTesting(!noTests);

        if (description != null) {
            configBuilder.description(description);
        }
        if (author != null) {
            configBuilder.author(author);
        }

        ProjectConfig config = configBuilder.build();

        // Generate project
        DefaultProjectGenerator generator = new DefaultProjectGenerator();
        var result = generator.createProject(targetDir, config).join();

        if (result.isSuccess()) {
            System.out.println("\nProject created successfully!");
            System.out.println("\nGenerated files:");
            for (Path file : result.generatedFiles()) {
                System.out.println("  " + file);
            }
            System.out.println("\nNext steps:");
            System.out.println("  cd " + name);
            System.out.println("  ./gradlew build");
            return 0;
        } else {
            System.err.println("Failed to create project: " + result.error());
            return 1;
        }
    }

    private ProjectTemplate resolveTemplate(String templateName) {
        return switch (templateName.toLowerCase()) {
            case "economy" -> ProjectTemplate.ECONOMY;
            case "minigame" -> ProjectTemplate.MINIGAME;
            case "library" -> ProjectTemplate.LIBRARY;
            case "full" -> ProjectTemplate.FULL;
            default -> ProjectTemplate.BASIC;
        };
    }

    private String sanitizePackageName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("^[0-9]+", "");
    }

    private boolean isEmpty(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return true;
        }
        try (var stream = Files.list(dir)) {
            return stream.findFirst().isEmpty();
        }
    }
}
