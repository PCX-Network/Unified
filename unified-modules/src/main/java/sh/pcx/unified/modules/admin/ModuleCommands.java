/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.admin;

import sh.pcx.unified.modules.core.ModuleManager;
import sh.pcx.unified.modules.core.ModuleRegistry;
import sh.pcx.unified.modules.lifecycle.ModuleState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides admin commands for managing modules at runtime.
 *
 * <p>This class implements the logic for module administration commands:
 * <ul>
 *   <li>{@code /modules list} - List all modules and their status</li>
 *   <li>{@code /modules info <name>} - Show detailed module information</li>
 *   <li>{@code /modules enable <name>} - Enable a disabled module</li>
 *   <li>{@code /modules disable <name>} - Disable an enabled module</li>
 *   <li>{@code /modules reload <name>} - Reload a specific module</li>
 *   <li>{@code /modules reload} - Reload all modules</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>This class provides the command logic but does not handle the actual
 * command registration, which is platform-specific. Integrate with your
 * platform's command system by calling these methods from your command executor.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyCommandExecutor implements TabExecutor {
 *
 *     private final ModuleCommands commands;
 *
 *     public MyCommandExecutor(ModuleManager manager) {
 *         this.commands = new ModuleCommands(manager);
 *     }
 *
 *     @Override
 *     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
 *         if (args.length == 0) {
 *             return false;
 *         }
 *
 *         CommandResult result = switch (args[0].toLowerCase()) {
 *             case "list" -> commands.list();
 *             case "info" -> commands.info(args.length > 1 ? args[1] : null);
 *             case "enable" -> commands.enable(args.length > 1 ? args[1] : null);
 *             case "disable" -> commands.disable(args.length > 1 ? args[1] : null);
 *             case "reload" -> args.length > 1 ? commands.reload(args[1]) : commands.reloadAll();
 *             default -> CommandResult.error("Unknown subcommand");
 *         };
 *
 *         for (String line : result.getMessages()) {
 *             sender.sendMessage(line);
 *         }
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleManager
 * @see ModuleInfo
 */
public final class ModuleCommands {

    private final ModuleManager manager;

    /**
     * Creates a new ModuleCommands instance.
     *
     * @param manager the module manager
     */
    public ModuleCommands(@NotNull ModuleManager manager) {
        this.manager = Objects.requireNonNull(manager, "Manager cannot be null");
    }

    /**
     * Lists all registered modules and their status.
     *
     * @return the command result with module list
     */
    @NotNull
    public CommandResult list() {
        Collection<ModuleRegistry.ModuleEntry> entries = manager.getRegistry().getAllEntries();

        if (entries.isEmpty()) {
            return CommandResult.success("No modules registered.");
        }

        List<String> messages = new ArrayList<>();
        messages.add("=== Registered Modules (" + entries.size() + ") ===");

        // Group by state
        Map<ModuleState, List<ModuleRegistry.ModuleEntry>> byState = entries.stream()
                .collect(Collectors.groupingBy(ModuleRegistry.ModuleEntry::getState));

        // List enabled first
        List<ModuleRegistry.ModuleEntry> enabled = byState.getOrDefault(ModuleState.ENABLED, List.of());
        if (!enabled.isEmpty()) {
            messages.add("Enabled (" + enabled.size() + "):");
            for (ModuleRegistry.ModuleEntry entry : enabled) {
                messages.add("  + " + entry.getName() + " v" + getVersion(entry));
            }
        }

        // List disabled
        List<ModuleRegistry.ModuleEntry> disabled = byState.getOrDefault(ModuleState.DISABLED, List.of());
        if (!disabled.isEmpty()) {
            messages.add("Disabled (" + disabled.size() + "):");
            for (ModuleRegistry.ModuleEntry entry : disabled) {
                messages.add("  - " + entry.getName());
            }
        }

        // List failed
        List<ModuleRegistry.ModuleEntry> failed = byState.getOrDefault(ModuleState.FAILED, List.of());
        if (!failed.isEmpty()) {
            messages.add("Failed (" + failed.size() + "):");
            for (ModuleRegistry.ModuleEntry entry : failed) {
                String error = entry.getErrorMessage();
                messages.add("  ! " + entry.getName() + (error != null ? ": " + error : ""));
            }
        }

        // List loading
        List<ModuleRegistry.ModuleEntry> loading = byState.getOrDefault(ModuleState.LOADING, List.of());
        if (!loading.isEmpty()) {
            messages.add("Loading (" + loading.size() + "):");
            for (ModuleRegistry.ModuleEntry entry : loading) {
                messages.add("  ~ " + entry.getName());
            }
        }

        return CommandResult.success(messages);
    }

    /**
     * Shows detailed information about a specific module.
     *
     * @param name the module name
     * @return the command result with module info
     */
    @NotNull
    public CommandResult info(String name) {
        if (name == null || name.isEmpty()) {
            return CommandResult.error("Usage: /modules info <name>");
        }

        Optional<ModuleRegistry.ModuleEntry> entryOpt = manager.getRegistry().getEntry(name);
        if (entryOpt.isEmpty()) {
            return CommandResult.error("Module not found: " + name);
        }

        ModuleInfo info = ModuleInfo.from(entryOpt.get());
        return CommandResult.success(info.toDisplayString());
    }

    /**
     * Enables a disabled module.
     *
     * @param name the module name
     * @return the command result
     */
    @NotNull
    public CommandResult enable(String name) {
        if (name == null || name.isEmpty()) {
            return CommandResult.error("Usage: /modules enable <name>");
        }

        if (!manager.getRegistry().contains(name)) {
            return CommandResult.error("Module not found: " + name);
        }

        ModuleState state = manager.getRegistry().getState(name);
        if (state == ModuleState.ENABLED) {
            return CommandResult.error("Module '" + name + "' is already enabled.");
        }

        if (!state.canEnable()) {
            return CommandResult.error("Cannot enable module from state: " + state);
        }

        boolean success = manager.enable(name);
        if (success) {
            return CommandResult.success("Module '" + name + "' has been enabled.");
        } else {
            String error = manager.getRegistry().getError(name);
            return CommandResult.error("Failed to enable module: " + (error != null ? error : "Unknown error"));
        }
    }

    /**
     * Disables an enabled module.
     *
     * @param name the module name
     * @return the command result
     */
    @NotNull
    public CommandResult disable(String name) {
        if (name == null || name.isEmpty()) {
            return CommandResult.error("Usage: /modules disable <name>");
        }

        if (!manager.getRegistry().contains(name)) {
            return CommandResult.error("Module not found: " + name);
        }

        ModuleState state = manager.getRegistry().getState(name);
        if (state == ModuleState.DISABLED) {
            return CommandResult.error("Module '" + name + "' is already disabled.");
        }

        if (!state.canDisable()) {
            return CommandResult.error("Cannot disable module from state: " + state);
        }

        boolean success = manager.disable(name);
        if (success) {
            return CommandResult.success("Module '" + name + "' has been disabled.");
        } else {
            return CommandResult.error("Failed to disable module.");
        }
    }

    /**
     * Reloads a specific module.
     *
     * @param name the module name
     * @return the command result
     */
    @NotNull
    public CommandResult reload(String name) {
        if (name == null || name.isEmpty()) {
            return CommandResult.error("Usage: /modules reload <name>");
        }

        if (!manager.getRegistry().contains(name)) {
            return CommandResult.error("Module not found: " + name);
        }

        ModuleState state = manager.getRegistry().getState(name);
        if (!state.canReload()) {
            return CommandResult.error("Cannot reload module from state: " + state);
        }

        boolean success = manager.reload(name);
        if (success) {
            return CommandResult.success("Module '" + name + "' has been reloaded.");
        } else {
            return CommandResult.error("Failed to reload module. Check console for details.");
        }
    }

    /**
     * Reloads all reloadable modules.
     *
     * @return the command result
     */
    @NotNull
    public CommandResult reloadAll() {
        int count = 0;
        List<String> failed = new ArrayList<>();

        for (String name : manager.getRegistry().getNames()) {
            if (manager.getRegistry().getState(name).canReload()) {
                if (manager.reload(name)) {
                    count++;
                } else {
                    failed.add(name);
                }
            }
        }

        List<String> messages = new ArrayList<>();
        messages.add("Reloaded " + count + " modules.");

        if (!failed.isEmpty()) {
            messages.add("Failed to reload: " + String.join(", ", failed));
        }

        return CommandResult.success(messages);
    }

    /**
     * Provides tab completions for module commands.
     *
     * @param args the current arguments
     * @return list of completions
     */
    @NotNull
    public List<String> tabComplete(@NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(
                    List.of("list", "info", "enable", "disable", "reload"),
                    args[0]
            );
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            Set<String> moduleNames = manager.getRegistry().getNames();

            return switch (subCommand) {
                case "info" -> filterStartsWith(new ArrayList<>(moduleNames), args[1]);
                case "enable" -> filterStartsWith(
                        manager.getRegistry().getEntriesByState(ModuleState.DISABLED).stream()
                                .map(ModuleRegistry.ModuleEntry::getName)
                                .collect(Collectors.toList()),
                        args[1]
                );
                case "disable" -> filterStartsWith(
                        manager.getRegistry().getEntriesByState(ModuleState.ENABLED).stream()
                                .map(ModuleRegistry.ModuleEntry::getName)
                                .collect(Collectors.toList()),
                        args[1]
                );
                case "reload" -> filterStartsWith(
                        manager.getRegistry().getAllEntries().stream()
                                .filter(e -> e.getState().canReload())
                                .map(ModuleRegistry.ModuleEntry::getName)
                                .collect(Collectors.toList()),
                        args[1]
                );
                default -> List.of();
            };
        }

        return List.of();
    }

    /**
     * Gets the version from a module entry.
     */
    private String getVersion(ModuleRegistry.ModuleEntry entry) {
        return entry.getAnnotation() != null ? entry.getAnnotation().version() : "1.0.0";
    }

    /**
     * Filters a list of strings by prefix.
     */
    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }

    /**
     * Result of a module command execution.
     */
    public static final class CommandResult {
        private final boolean success;
        private final List<String> messages;

        private CommandResult(boolean success, List<String> messages) {
            this.success = success;
            this.messages = List.copyOf(messages);
        }

        /**
         * Creates a successful result with a single message.
         *
         * @param message the message
         * @return the result
         */
        @NotNull
        public static CommandResult success(@NotNull String message) {
            return new CommandResult(true, List.of(message));
        }

        /**
         * Creates a successful result with multiple messages.
         *
         * @param messages the messages
         * @return the result
         */
        @NotNull
        public static CommandResult success(@NotNull List<String> messages) {
            return new CommandResult(true, messages);
        }

        /**
         * Creates an error result with a single message.
         *
         * @param message the error message
         * @return the result
         */
        @NotNull
        public static CommandResult error(@NotNull String message) {
            return new CommandResult(false, List.of(message));
        }

        /**
         * Returns whether the command succeeded.
         *
         * @return {@code true} if successful
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Returns the result messages.
         *
         * @return the messages
         */
        @NotNull
        public List<String> getMessages() {
            return messages;
        }

        /**
         * Returns the first message.
         *
         * @return the first message, or empty string
         */
        @NotNull
        public String getMessage() {
            return messages.isEmpty() ? "" : messages.get(0);
        }
    }
}
