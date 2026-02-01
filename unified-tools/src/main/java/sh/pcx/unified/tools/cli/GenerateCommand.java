/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import sh.pcx.unified.tools.cli.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Command for generating plugin components.
 *
 * <p>Examples:
 * <pre>
 * unified generate module MyModule
 * unified generate command MyCommand --permission=my.command
 * unified generate gui ShopGui --rows=6 --title="Shop"
 * unified generate service DataService
 * unified generate crud Player --fields="name:String,kills:int"
 * </pre>
 *
 * @since 1.0.0
 */
@Command(
        name = "generate",
        aliases = {"g", "gen"},
        description = "Generate plugin components",
        subcommands = {
                GenerateCommand.ModuleSubcommand.class,
                GenerateCommand.CommandSubcommand.class,
                GenerateCommand.GuiSubcommand.class,
                GenerateCommand.ServiceSubcommand.class,
                GenerateCommand.ListenerSubcommand.class,
                GenerateCommand.ConfigSubcommand.class,
                GenerateCommand.CrudSubcommand.class
        }
)
public final class GenerateCommand implements Callable<Integer> {

    @ParentCommand
    private UnifiedCli parent;

    @Option(names = {"--project", "-p"}, description = "Project directory")
    private Path projectDir = Path.of(".");

    @Override
    public Integer call() {
        // Interactive mode
        System.out.println("What would you like to generate?");
        System.out.println("  1. Module");
        System.out.println("  2. Command");
        System.out.println("  3. GUI");
        System.out.println("  4. Service");
        System.out.println("  5. Listener");
        System.out.println("  6. Config");
        System.out.println("  7. CRUD Entity");

        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter choice (1-7): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter name: ");
            String name = scanner.nextLine().trim();

            if (name.isEmpty()) {
                System.err.println("Error: Name is required");
                return 1;
            }

            DefaultProjectGenerator generator = new DefaultProjectGenerator();

            var result = switch (choice) {
                case 1 -> generator.generateModule(projectDir, name).join();
                case 2 -> generator.generateCommand(projectDir, name).join();
                case 3 -> generator.generateGui(projectDir, name).join();
                case 4 -> generator.generateService(projectDir, name).join();
                case 5 -> generator.generateListener(projectDir, name).join();
                case 6 -> generator.generateConfig(projectDir, name).join();
                case 7 -> {
                    System.out.print("Enter fields (name:Type,name:Type): ");
                    String fieldsStr = scanner.nextLine().trim();
                    Map<String, String> fields = parseFields(fieldsStr);
                    yield generator.generateCrud(projectDir, name, fields).join();
                }
                default -> {
                    System.err.println("Invalid choice");
                    yield null;
                }
            };

            if (result != null && result.isSuccess()) {
                System.out.println("\nGenerated successfully!");
                for (Path file : result.generatedFiles()) {
                    System.out.println("  " + file);
                }
                return 0;
            } else {
                return 1;
            }

        } catch (NumberFormatException e) {
            System.err.println("Invalid input");
            return 1;
        }
    }

    private Map<String, String> parseFields(String fieldsStr) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (!fieldsStr.isEmpty()) {
            for (String field : fieldsStr.split(",")) {
                String[] parts = field.trim().split(":");
                if (parts.length == 2) {
                    fields.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return fields;
    }

    /**
     * Module generation subcommand.
     */
    @Command(name = "module", description = "Generate a module class")
    public static class ModuleSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "Module name")
        private String name;

        @Option(names = {"--description", "-d"}, description = "Module description")
        private String description;

        @Option(names = {"--enabled"}, description = "Enabled by default", defaultValue = "true")
        private boolean enabled;

        @Option(names = {"--configurable"}, description = "Has configuration")
        private boolean configurable;

        @Override
        public Integer call() {
            ModuleConfig config = ModuleConfig.builder()
                    .description(description)
                    .enabledByDefault(enabled)
                    .configurable(configurable)
                    .build();

            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateModule(parent.projectDir, name, config).join();

            return handleResult(result);
        }
    }

    /**
     * Command generation subcommand.
     */
    @Command(name = "command", aliases = {"cmd"}, description = "Generate a command class")
    public static class CommandSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "Command name")
        private String name;

        @Option(names = {"--permission", "-perm"}, description = "Required permission")
        private String permission;

        @Option(names = {"--aliases", "-a"}, description = "Command aliases", split = ",")
        private List<String> aliases = new ArrayList<>();

        @Option(names = {"--player-only"}, description = "Player only command")
        private boolean playerOnly;

        @Option(names = {"--async"}, description = "Run asynchronously")
        private boolean async;

        @Override
        public Integer call() {
            CommandConfig.Builder builder = CommandConfig.builder()
                    .permission(permission)
                    .playerOnly(playerOnly)
                    .async(async);

            for (String alias : aliases) {
                builder.alias(alias);
            }

            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateCommand(parent.projectDir, name, builder.build()).join();

            return handleResult(result);
        }
    }

    /**
     * GUI generation subcommand.
     */
    @Command(name = "gui", description = "Generate a GUI class")
    public static class GuiSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "GUI name")
        private String name;

        @Option(names = {"--title", "-t"}, description = "GUI title")
        private String title;

        @Option(names = {"--rows", "-r"}, description = "Number of rows (1-6)")
        private int rows = 3;

        @Option(names = {"--pagination"}, description = "Include pagination")
        private boolean pagination;

        @Option(names = {"--border"}, description = "Fill border")
        private boolean border;

        @Override
        public Integer call() {
            GuiConfig config = GuiConfig.builder()
                    .title(title != null ? title : name)
                    .rows(rows)
                    .pagination(pagination)
                    .fillBorder(border)
                    .build();

            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateGui(parent.projectDir, name, config).join();

            return handleResult(result);
        }
    }

    /**
     * Service generation subcommand.
     */
    @Command(name = "service", description = "Generate a service class")
    public static class ServiceSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "Service name")
        private String name;

        @Override
        public Integer call() {
            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateService(parent.projectDir, name).join();

            return handleResult(result);
        }
    }

    /**
     * Listener generation subcommand.
     */
    @Command(name = "listener", description = "Generate a listener class")
    public static class ListenerSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "Listener name")
        private String name;

        @Override
        public Integer call() {
            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateListener(parent.projectDir, name).join();

            return handleResult(result);
        }
    }

    /**
     * Config generation subcommand.
     */
    @Command(name = "config", description = "Generate a config class")
    public static class ConfigSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "Config name")
        private String name;

        @Override
        public Integer call() {
            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateConfig(parent.projectDir, name).join();

            return handleResult(result);
        }
    }

    /**
     * CRUD generation subcommand.
     */
    @Command(name = "crud", description = "Generate CRUD operations for an entity")
    public static class CrudSubcommand implements Callable<Integer> {
        @ParentCommand
        private GenerateCommand parent;

        @Parameters(index = "0", description = "Entity name")
        private String name;

        @Option(names = {"--fields", "-f"}, description = "Entity fields (name:Type,name:Type)")
        private String fields = "";

        @Override
        public Integer call() {
            Map<String, String> fieldMap = new LinkedHashMap<>();
            if (!fields.isEmpty()) {
                for (String field : fields.split(",")) {
                    String[] parts = field.trim().split(":");
                    if (parts.length == 2) {
                        fieldMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }

            DefaultProjectGenerator generator = new DefaultProjectGenerator();
            var result = generator.generateCrud(parent.projectDir, name, fieldMap).join();

            return handleResult(result);
        }
    }

    private static int handleResult(ProjectGenerator.GenerationResult result) {
        if (result.isSuccess()) {
            System.out.println("Generated successfully!");
            for (Path file : result.generatedFiles()) {
                System.out.println("  " + file);
            }
            return 0;
        } else {
            System.err.println("Generation failed: " +
                    (result.error() != null ? result.error().getMessage() : "Unknown error"));
            return 1;
        }
    }
}
