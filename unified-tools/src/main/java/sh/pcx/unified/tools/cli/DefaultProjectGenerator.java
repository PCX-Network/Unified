/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import sh.pcx.unified.tools.cli.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link ProjectGenerator}.
 *
 * @since 1.0.0
 */
public final class DefaultProjectGenerator implements ProjectGenerator {

    private final List<ProjectTemplate> templates = new ArrayList<>(List.of(
            ProjectTemplate.BASIC,
            ProjectTemplate.ECONOMY,
            ProjectTemplate.MINIGAME,
            ProjectTemplate.LIBRARY,
            ProjectTemplate.FULL
    ));

    @Override
    public @NotNull CompletableFuture<GenerationResult> createProject(
            @NotNull Path targetDir,
            @NotNull ProjectConfig config
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Path> generatedFiles = new ArrayList<>();
                List<String> messages = new ArrayList<>();

                // Create directories
                Files.createDirectories(targetDir);

                String packagePath = config.packageName().replace('.', '/');
                Path srcMainJava = targetDir.resolve("src/main/java").resolve(packagePath);
                Path srcMainResources = targetDir.resolve("src/main/resources");
                Path srcTestJava = targetDir.resolve("src/test/java").resolve(packagePath);

                Files.createDirectories(srcMainJava);
                Files.createDirectories(srcMainResources);
                if (config.includeTesting()) {
                    Files.createDirectories(srcTestJava);
                }

                // Generate build.gradle.kts
                Path buildGradle = targetDir.resolve("build.gradle.kts");
                Files.writeString(buildGradle, generateBuildGradle(config));
                generatedFiles.add(buildGradle);

                // Generate settings.gradle.kts
                Path settingsGradle = targetDir.resolve("settings.gradle.kts");
                Files.writeString(settingsGradle, generateSettingsGradle(config));
                generatedFiles.add(settingsGradle);

                // Generate gradle.properties
                Path gradleProps = targetDir.resolve("gradle.properties");
                Files.writeString(gradleProps, generateGradleProperties(config));
                generatedFiles.add(gradleProps);

                // Generate main plugin class
                String className = toPascalCase(config.name()) + "Plugin";
                Path mainClass = srcMainJava.resolve(className + ".java");
                Files.writeString(mainClass, generateMainClass(config, className));
                generatedFiles.add(mainClass);

                // Generate plugin.yml
                Path pluginYml = srcMainResources.resolve("plugin.yml");
                Files.writeString(pluginYml, generatePluginYml(config, className));
                generatedFiles.add(pluginYml);

                // Generate config.yml
                Path configYml = srcMainResources.resolve("config.yml");
                Files.writeString(configYml, generateConfigYml(config));
                generatedFiles.add(configYml);

                // Generate .gitignore
                Path gitignore = targetDir.resolve(".gitignore");
                Files.writeString(gitignore, generateGitignore());
                generatedFiles.add(gitignore);

                // Generate test class if testing enabled
                if (config.includeTesting()) {
                    Path testClass = srcTestJava.resolve(className + "Test.java");
                    Files.writeString(testClass, generateTestClass(config, className));
                    generatedFiles.add(testClass);
                }

                messages.add("Project created successfully");

                return new DefaultGenerationResult(true, generatedFiles, null, messages);

            } catch (IOException e) {
                return new DefaultGenerationResult(false, List.of(), e, List.of(e.getMessage()));
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateModule(
            @NotNull Path projectDir,
            @NotNull String name
    ) {
        return generateModule(projectDir, name, ModuleConfig.defaults());
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateModule(
            @NotNull Path projectDir,
            @NotNull String name,
            @NotNull ModuleConfig config
    ) {
        return generateComponent(projectDir, name, "modules", generateModuleClass(name, config));
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateCommand(
            @NotNull Path projectDir,
            @NotNull String name
    ) {
        return generateCommand(projectDir, name, CommandConfig.defaults());
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateCommand(
            @NotNull Path projectDir,
            @NotNull String name,
            @NotNull CommandConfig config
    ) {
        return generateComponent(projectDir, name, "commands", generateCommandClass(name, config));
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateGui(
            @NotNull Path projectDir,
            @NotNull String name
    ) {
        return generateGui(projectDir, name, GuiConfig.defaults());
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateGui(
            @NotNull Path projectDir,
            @NotNull String name,
            @NotNull GuiConfig config
    ) {
        return generateComponent(projectDir, name, "gui", generateGuiClass(name, config));
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateService(
            @NotNull Path projectDir,
            @NotNull String name
    ) {
        return generateComponent(projectDir, name, "services", generateServiceClass(name));
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateListener(
            @NotNull Path projectDir,
            @NotNull String name
    ) {
        return generateComponent(projectDir, name, "listeners", generateListenerClass(name));
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateConfig(
            @NotNull Path projectDir,
            @NotNull String name
    ) {
        return generateComponent(projectDir, name, "config", generateConfigClass(name));
    }

    @Override
    public @NotNull CompletableFuture<GenerationResult> generateCrud(
            @NotNull Path projectDir,
            @NotNull String entityName,
            @NotNull Map<String, String> fields
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Path> generatedFiles = new ArrayList<>();
                String packageName = detectPackageName(projectDir);

                // Generate entity class
                Path entityDir = findSourceDir(projectDir).resolve(packageName.replace('.', '/')).resolve("entities");
                Files.createDirectories(entityDir);
                Path entityFile = entityDir.resolve(entityName + ".java");
                Files.writeString(entityFile, generateEntityClass(packageName, entityName, fields));
                generatedFiles.add(entityFile);

                // Generate repository
                Path repoDir = findSourceDir(projectDir).resolve(packageName.replace('.', '/')).resolve("repositories");
                Files.createDirectories(repoDir);
                Path repoFile = repoDir.resolve(entityName + "Repository.java");
                Files.writeString(repoFile, generateRepositoryClass(packageName, entityName, fields));
                generatedFiles.add(repoFile);

                // Generate service
                Path serviceDir = findSourceDir(projectDir).resolve(packageName.replace('.', '/')).resolve("services");
                Files.createDirectories(serviceDir);
                Path serviceFile = serviceDir.resolve(entityName + "Service.java");
                Files.writeString(serviceFile, generateCrudServiceClass(packageName, entityName));
                generatedFiles.add(serviceFile);

                return new DefaultGenerationResult(true, generatedFiles, null, List.of("CRUD generated successfully"));

            } catch (IOException e) {
                return new DefaultGenerationResult(false, List.of(), e, List.of(e.getMessage()));
            }
        });
    }

    @Override
    public @NotNull List<ProjectTemplate> availableTemplates() {
        return List.copyOf(templates);
    }

    @Override
    public void registerTemplate(@NotNull ProjectTemplate template) {
        templates.add(template);
    }

    private CompletableFuture<GenerationResult> generateComponent(
            Path projectDir,
            String name,
            String subpackage,
            String content
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String packageName = detectPackageName(projectDir);
                Path sourceDir = findSourceDir(projectDir);
                Path targetDir = sourceDir.resolve(packageName.replace('.', '/')).resolve(subpackage);
                Files.createDirectories(targetDir);

                String className = toPascalCase(name);
                if (!className.endsWith(capitalize(subpackage.substring(0, subpackage.length() - 1)))) {
                    // Don't double-suffix (e.g., MyModuleModule)
                }

                Path file = targetDir.resolve(className + ".java");
                String finalContent = content.replace("{{PACKAGE}}", packageName + "." + subpackage);
                Files.writeString(file, finalContent);

                return new DefaultGenerationResult(true, List.of(file), null, List.of("Generated " + className));

            } catch (IOException e) {
                return new DefaultGenerationResult(false, List.of(), e, List.of(e.getMessage()));
            }
        });
    }

    private String detectPackageName(Path projectDir) throws IOException {
        Path srcDir = findSourceDir(projectDir);
        if (Files.exists(srcDir)) {
            try (var stream = Files.walk(srcDir, 10)) {
                return stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .findFirst()
                        .map(p -> {
                            String relative = srcDir.relativize(p).toString();
                            String packagePath = relative.substring(0, relative.lastIndexOf('/'));
                            return packagePath.replace('/', '.').replace('\\', '.');
                        })
                        .orElse("com.example.plugin");
            }
        }
        return "com.example.plugin";
    }

    private Path findSourceDir(Path projectDir) {
        return projectDir.resolve("src/main/java");
    }

    // Template generation methods

    private String generateBuildGradle(ProjectConfig config) {
        return """
                plugins {
                    java
                    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14" apply false
                    id("com.gradleup.shadow") version "8.3.6"
                }

                group = "%s"
                version = "%s"

                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(21))
                    }
                }

                repositories {
                    mavenCentral()
                    maven("https://repo.papermc.io/repository/maven-public/")
                }

                dependencies {
                    compileOnly("io.papermc.paper:paper-api:%s-R0.1-SNAPSHOT")

                    // UnifiedPlugin API
                    implementation("sh.pcx:unified-api:1.0.0-SNAPSHOT")

                    // Testing
                    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
                }

                tasks.test {
                    useJUnitPlatform()
                }

                tasks.shadowJar {
                    archiveClassifier.set("")
                    relocate("sh.pcx.unified", "%s.libs.unified")
                }

                tasks.build {
                    dependsOn(tasks.shadowJar)
                }
                """.formatted(
                config.packageName(),
                config.version(),
                config.minecraftVersion(),
                config.packageName()
        );
    }

    private String generateSettingsGradle(ProjectConfig config) {
        return """
                rootProject.name = "%s"
                """.formatted(config.name());
    }

    private String generateGradleProperties(ProjectConfig config) {
        return """
                org.gradle.jvmargs=-Xmx2G
                org.gradle.parallel=true
                """;
    }

    private String generateMainClass(ProjectConfig config, String className) {
        return """
                package %s;

                import sh.pcx.unified.UnifiedPlugin;
                import sh.pcx.unified.PluginMeta;

                /**
                 * Main plugin class for %s.
                 */
                @PluginMeta(
                    name = "%s",
                    version = "%s"
                )
                public final class %s extends UnifiedPlugin {

                    @Override
                    public void onPluginEnable() {
                        getLogger().info("%s has been enabled!");
                    }

                    @Override
                    public void onPluginDisable() {
                        getLogger().info("%s has been disabled!");
                    }
                }
                """.formatted(
                config.packageName(),
                config.name(),
                config.name(),
                config.version(),
                className,
                config.name(),
                config.name()
        );
    }

    private String generatePluginYml(ProjectConfig config, String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: ").append(config.name()).append("\n");
        sb.append("version: '${version}'\n");
        sb.append("main: ").append(config.packageName()).append(".").append(className).append("\n");
        sb.append("api-version: '").append(config.minecraftVersion()).append("'\n");

        if (config.description() != null) {
            sb.append("description: ").append(config.description()).append("\n");
        }
        if (config.author() != null) {
            sb.append("author: ").append(config.author()).append("\n");
        }
        if (!config.dependencies().isEmpty()) {
            sb.append("depend:\n");
            for (String dep : config.dependencies()) {
                sb.append("  - ").append(dep).append("\n");
            }
        }
        if (!config.softDependencies().isEmpty()) {
            sb.append("softdepend:\n");
            for (String dep : config.softDependencies()) {
                sb.append("  - ").append(dep).append("\n");
            }
        }

        return sb.toString();
    }

    private String generateConfigYml(ProjectConfig config) {
        return """
                # %s Configuration

                # Enable debug mode for additional logging
                debug: false

                # Database settings
                database:
                  type: sqlite
                  file: data.db
                """.formatted(config.name());
    }

    private String generateGitignore() {
        return """
                # Gradle
                .gradle/
                build/

                # IDE
                .idea/
                *.iml
                .vscode/
                .project
                .classpath
                .settings/

                # OS
                .DS_Store
                Thumbs.db

                # Logs
                *.log
                logs/

                # Local config
                local.properties
                """;
    }

    private String generateTestClass(ProjectConfig config, String className) {
        return """
                package %s;

                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;

                class %sTest {

                    @Test
                    void pluginLoads() {
                        // Basic test to verify plugin structure
                        assertNotNull(%s.class);
                    }
                }
                """.formatted(config.packageName(), className, className);
    }

    private String generateModuleClass(String name, ModuleConfig config) {
        String className = toPascalCase(name);
        if (!className.endsWith("Module")) {
            className += "Module";
        }

        return """
                package {{PACKAGE}};

                import sh.pcx.unified.modules.annotation.Module;
                import sh.pcx.unified.inject.PostConstruct;
                import sh.pcx.unified.inject.PreDestroy;

                /**
                 * %s
                 */
                @Module(
                    id = "%s",
                    name = "%s",
                    enabled = %s
                )
                public final class %s {

                    @PostConstruct
                    public void onEnable() {
                        // Module initialization
                    }

                    @PreDestroy
                    public void onDisable() {
                        // Module cleanup
                    }
                }
                """.formatted(
                config.description() != null ? config.description() : className,
                name.toLowerCase(),
                name,
                config.enabledByDefault(),
                className
        );
    }

    private String generateCommandClass(String name, CommandConfig config) {
        String className = toPascalCase(name);
        if (!className.endsWith("Command")) {
            className += "Command";
        }

        String permission = config.permission() != null
                ? "permission = \"" + config.permission() + "\""
                : "";

        return """
                package {{PACKAGE}};

                import sh.pcx.unified.commands.annotation.Command;
                import sh.pcx.unified.commands.annotation.CommandExecutor;
                import sh.pcx.unified.player.UnifiedPlayer;

                /**
                 * %s command.
                 */
                @Command(
                    name = "%s"%s
                )
                public final class %s {

                    @CommandExecutor
                    public void execute(UnifiedPlayer player) {
                        player.sendMessage("Hello from %s!");
                    }
                }
                """.formatted(
                name,
                name.toLowerCase(),
                permission.isEmpty() ? "" : ", " + permission,
                className,
                name
        );
    }

    private String generateGuiClass(String name, GuiConfig config) {
        String className = toPascalCase(name);
        if (!className.endsWith("Gui") && !className.endsWith("GUI")) {
            className += "Gui";
        }

        return """
                package {{PACKAGE}};

                import sh.pcx.unified.gui.Gui;
                import sh.pcx.unified.gui.GuiBuilder;
                import sh.pcx.unified.player.UnifiedPlayer;

                /**
                 * %s GUI.
                 */
                public final class %s {

                    public Gui create(UnifiedPlayer player) {
                        return GuiBuilder.chest()
                            .title("%s")
                            .rows(%d)
                            .build();
                    }
                }
                """.formatted(
                name,
                className,
                config.title(),
                config.rows()
        );
    }

    private String generateServiceClass(String name) {
        String className = toPascalCase(name);
        if (!className.endsWith("Service")) {
            className += "Service";
        }

        return """
                package {{PACKAGE}};

                import sh.pcx.unified.service.Service;
                import sh.pcx.unified.inject.PostConstruct;
                import sh.pcx.unified.inject.PreDestroy;

                /**
                 * %s service.
                 */
                public final class %s implements Service {

                    @PostConstruct
                    public void initialize() {
                        // Service initialization
                    }

                    @PreDestroy
                    public void shutdown() {
                        // Service cleanup
                    }

                    @Override
                    public String getServiceName() {
                        return "%s";
                    }
                }
                """.formatted(name, className, className);
    }

    private String generateListenerClass(String name) {
        String className = toPascalCase(name);
        if (!className.endsWith("Listener")) {
            className += "Listener";
        }

        return """
                package {{PACKAGE}};

                import sh.pcx.unified.event.UnifiedEvent;
                import jakarta.inject.Inject;

                /**
                 * %s event listener.
                 */
                public final class %s {

                    @Inject
                    public %s() {
                        // Constructor
                    }

                    // Add event handler methods here
                    // @EventHandler
                    // public void onEvent(SomeEvent event) { }
                }
                """.formatted(name, className, className);
    }

    private String generateConfigClass(String name) {
        String className = toPascalCase(name);
        if (!className.endsWith("Config")) {
            className += "Config";
        }

        return """
                package {{PACKAGE}};

                import sh.pcx.unified.inject.Config;

                /**
                 * %s configuration.
                 */
                @Config(path = "%s.yml")
                public final class %s {

                    private boolean enabled = true;
                    private String message = "Hello, World!";

                    public boolean isEnabled() {
                        return enabled;
                    }

                    public String getMessage() {
                        return message;
                    }
                }
                """.formatted(name, name.toLowerCase(), className);
    }

    private String generateEntityClass(String packageName, String entityName, Map<String, String> fields) {
        StringBuilder fieldsCode = new StringBuilder();
        StringBuilder gettersSetters = new StringBuilder();

        for (Map.Entry<String, String> field : fields.entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();

            fieldsCode.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");

            // Getter
            gettersSetters.append("    public ").append(fieldType).append(" get")
                    .append(capitalize(fieldName)).append("() {\n");
            gettersSetters.append("        return ").append(fieldName).append(";\n");
            gettersSetters.append("    }\n\n");

            // Setter
            gettersSetters.append("    public void set").append(capitalize(fieldName))
                    .append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
            gettersSetters.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            gettersSetters.append("    }\n\n");
        }

        return """
                package %s.entities;

                import java.util.UUID;

                /**
                 * %s entity.
                 */
                public final class %s {

                    private UUID id;
                %s
                    public UUID getId() {
                        return id;
                    }

                    public void setId(UUID id) {
                        this.id = id;
                    }

                %s}
                """.formatted(packageName, entityName, entityName, fieldsCode, gettersSetters);
    }

    private String generateRepositoryClass(String packageName, String entityName, Map<String, String> fields) {
        return """
                package %s.repositories;

                import %s.entities.%s;
                import java.util.*;
                import java.util.concurrent.ConcurrentHashMap;

                /**
                 * Repository for %s entities.
                 */
                public final class %sRepository {

                    private final Map<UUID, %s> entities = new ConcurrentHashMap<>();

                    public Optional<%s> findById(UUID id) {
                        return Optional.ofNullable(entities.get(id));
                    }

                    public List<%s> findAll() {
                        return new ArrayList<>(entities.values());
                    }

                    public void save(%s entity) {
                        if (entity.getId() == null) {
                            entity.setId(UUID.randomUUID());
                        }
                        entities.put(entity.getId(), entity);
                    }

                    public void delete(UUID id) {
                        entities.remove(id);
                    }

                    public boolean exists(UUID id) {
                        return entities.containsKey(id);
                    }
                }
                """.formatted(
                packageName, packageName, entityName,
                entityName, entityName, entityName, entityName, entityName, entityName
        );
    }

    private String generateCrudServiceClass(String packageName, String entityName) {
        return """
                package %s.services;

                import %s.entities.%s;
                import %s.repositories.%sRepository;
                import sh.pcx.unified.service.Service;
                import jakarta.inject.Inject;
                import java.util.*;

                /**
                 * Service for %s CRUD operations.
                 */
                public final class %sService implements Service {

                    private final %sRepository repository;

                    @Inject
                    public %sService(%sRepository repository) {
                        this.repository = repository;
                    }

                    public Optional<%s> get(UUID id) {
                        return repository.findById(id);
                    }

                    public List<%s> getAll() {
                        return repository.findAll();
                    }

                    public %s create(%s entity) {
                        repository.save(entity);
                        return entity;
                    }

                    public %s update(%s entity) {
                        repository.save(entity);
                        return entity;
                    }

                    public void delete(UUID id) {
                        repository.delete(id);
                    }

                    @Override
                    public String getServiceName() {
                        return "%sService";
                    }
                }
                """.formatted(
                packageName, packageName, entityName, packageName, entityName,
                entityName, entityName, entityName, entityName, entityName,
                entityName, entityName, entityName, entityName, entityName, entityName, entityName
        );
    }

    // Utility methods

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (c == '-' || c == '_' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    /**
     * Default generation result implementation.
     */
    private record DefaultGenerationResult(
            boolean isSuccess,
            List<Path> generatedFiles,
            Throwable error,
            List<String> messages
    ) implements GenerationResult {

        @Override
        public boolean isSuccess() {
            return isSuccess;
        }

        @Override
        public @NotNull List<Path> generatedFiles() {
            return generatedFiles;
        }

        @Override
        public @Nullable Throwable error() {
            return error;
        }

        @Override
        public @NotNull List<String> messages() {
            return messages;
        }
    }
}
