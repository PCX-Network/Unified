/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link CommandContext.Builder}.
 *
 * <p>This builder creates immutable {@link CommandContext} instances
 * with all necessary execution information.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * CommandContext context = CommandContext.builder()
 *     .sender(player)
 *     .label("spawn")
 *     .args(new String[]{"home"})
 *     .command(registeredCommand)
 *     .put("source", "gui")
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandContext
 */
public class CommandContextBuilder implements CommandContext.Builder {

    private Object sender;
    private String label;
    private String[] args = new String[0];
    private CommandRegistry.RegisteredCommand command;
    private CommandRegistry.RegisteredSubcommand subcommand;
    private final Map<String, Object> parsedArgs = new HashMap<>();
    private final Map<String, Object> data = new HashMap<>();

    @Override
    public CommandContext.Builder sender(@NotNull Object sender) {
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        return this;
    }

    @Override
    public CommandContext.Builder label(@NotNull String label) {
        this.label = Objects.requireNonNull(label, "label cannot be null");
        return this;
    }

    @Override
    public CommandContext.Builder args(@NotNull String[] args) {
        this.args = Objects.requireNonNull(args, "args cannot be null");
        return this;
    }

    @Override
    public CommandContext.Builder command(@NotNull CommandRegistry.RegisteredCommand command) {
        this.command = Objects.requireNonNull(command, "command cannot be null");
        return this;
    }

    @Override
    public CommandContext.Builder subcommand(@Nullable CommandRegistry.RegisteredSubcommand subcommand) {
        this.subcommand = subcommand;
        return this;
    }

    @Override
    public CommandContext.Builder parsedArg(@NotNull String name, @Nullable Object value) {
        Objects.requireNonNull(name, "name cannot be null");
        if (value != null) {
            parsedArgs.put(name, value);
        }
        return this;
    }

    @Override
    public CommandContext.Builder put(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value != null) {
            data.put(key, value);
        }
        return this;
    }

    @Override
    @NotNull
    public CommandContext build() {
        Objects.requireNonNull(sender, "sender must be set");
        Objects.requireNonNull(label, "label must be set");
        return new SimpleCommandContext(this);
    }

    /**
     * Simple immutable implementation of CommandContext.
     */
    private static class SimpleCommandContext implements CommandContext {

        private final Object sender;
        private final String label;
        private final String[] args;
        private final CommandRegistry.RegisteredCommand command;
        private final CommandRegistry.RegisteredSubcommand subcommand;
        private final Map<String, Object> parsedArgs;
        private final Map<String, Object> data;

        SimpleCommandContext(CommandContextBuilder builder) {
            this.sender = builder.sender;
            this.label = builder.label;
            this.args = builder.args.clone();
            this.command = builder.command;
            this.subcommand = builder.subcommand;
            this.parsedArgs = new HashMap<>(builder.parsedArgs);
            this.data = new HashMap<>(builder.data);
        }

        @Override
        @NotNull
        public Object getSender() {
            return sender;
        }

        @Override
        @NotNull
        public <T> T getSender(@NotNull Class<T> type) {
            return type.cast(sender);
        }

        @Override
        @Nullable
        public Object getSenderAsPlayer() {
            // Platform-specific implementation would check sender type
            return isPlayer() ? sender : null;
        }

        @Override
        public boolean isPlayer() {
            // This is a simplified check - actual implementation would use platform APIs
            String className = sender.getClass().getName().toLowerCase();
            return className.contains("player");
        }

        @Override
        public boolean isConsole() {
            String className = sender.getClass().getName().toLowerCase();
            return className.contains("console");
        }

        @Override
        @NotNull
        public String getLabel() {
            return label;
        }

        @Override
        @NotNull
        public String[] getArgs() {
            return args.clone();
        }

        @Override
        public int getArgCount() {
            return args.length;
        }

        @Override
        @NotNull
        public String getArg(int index) {
            if (index < 0 || index >= args.length) {
                throw new IndexOutOfBoundsException("Argument index " + index + " out of bounds");
            }
            return args[index];
        }

        @Override
        @NotNull
        public String getArg(int index, @NotNull String defaultValue) {
            if (index < 0 || index >= args.length) {
                return defaultValue;
            }
            return args[index];
        }

        @Override
        public boolean hasArg(int index) {
            return index >= 0 && index < args.length;
        }

        @Override
        @NotNull
        public <T> Optional<T> getParsedArg(@NotNull String name, @NotNull Class<T> type) {
            Object value = parsedArgs.get(name);
            if (value != null && type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            return Optional.empty();
        }

        @Override
        @NotNull
        public Map<String, Object> getParsedArgs() {
            return new HashMap<>(parsedArgs);
        }

        @Override
        public void put(@NotNull String key, @Nullable Object value) {
            if (value != null) {
                data.put(key, value);
            } else {
                data.remove(key);
            }
        }

        @Override
        @NotNull
        public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            return Optional.empty();
        }

        @Override
        public boolean has(@NotNull String key) {
            return data.containsKey(key);
        }

        @Override
        public void sendMessage(@NotNull String message) {
            // Platform-specific implementation would send the message
            // This is a placeholder for the interface
            throw new UnsupportedOperationException(
                "sendMessage requires platform-specific implementation"
            );
        }

        @Override
        public void sendError(@NotNull String message) {
            // Platform-specific implementation
            sendMessage("<red>" + message);
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            // Platform-specific implementation would check permissions
            throw new UnsupportedOperationException(
                "hasPermission requires platform-specific implementation"
            );
        }

        @Override
        @NotNull
        public String getFullCommand() {
            StringBuilder sb = new StringBuilder("/").append(label);
            for (String arg : args) {
                sb.append(" ").append(arg);
            }
            return sb.toString();
        }

        @Override
        @NotNull
        public CommandRegistry.RegisteredCommand getCommand() {
            if (command == null) {
                throw new IllegalStateException("Command not set in context");
            }
            return command;
        }

        @Override
        @NotNull
        public Optional<CommandRegistry.RegisteredSubcommand> getSubcommand() {
            return Optional.ofNullable(subcommand);
        }
    }
}
