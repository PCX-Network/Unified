/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.completion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Context object providing information for tab completion.
 *
 * <p>The {@code CompletionContext} contains all the information a
 * {@link CompletionProvider} needs to generate relevant suggestions,
 * including the sender, current input, and previous arguments.</p>
 *
 * <h2>Context Properties</h2>
 * <ul>
 *   <li><b>Sender</b> - The entity requesting completion</li>
 *   <li><b>Command</b> - The command being completed</li>
 *   <li><b>Args</b> - Previously completed arguments</li>
 *   <li><b>Current Input</b> - The partial text being typed</li>
 *   <li><b>Arg Index</b> - The index of the current argument</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Completion Providers</h3>
 * <pre>{@code
 * commandService.registerCompletions("@teamMembers", context -> {
 *     // Check if sender is a player
 *     if (!context.isPlayer()) {
 *         return Collections.emptyList();
 *     }
 *
 *     // Get team from sender
 *     Player player = (Player) context.getSender();
 *     Team team = teamManager.getTeam(player);
 *
 *     // Filter by current input
 *     String filter = context.getCurrentInput().toLowerCase();
 *     return team.getMembers().stream()
 *         .map(Player::getName)
 *         .filter(n -> n.toLowerCase().startsWith(filter))
 *         .collect(Collectors.toList());
 * });
 * }</pre>
 *
 * <h3>Using Previous Arguments</h3>
 * <pre>{@code
 * // For "/shop buy <category> <item>"
 * commandService.registerCompletions("@shopItems", context -> {
 *     // Category was the first argument
 *     String category = context.getArg(0, "general");
 *     return shopManager.getItemsInCategory(category);
 * });
 * }</pre>
 *
 * <h3>Building Context</h3>
 * <pre>{@code
 * CompletionContext context = CompletionContext.builder()
 *     .sender(player)
 *     .command("warp")
 *     .args(new String[]{"set"})
 *     .currentInput("h")
 *     .argIndex(1)
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CompletionProvider
 * @see TabCompleter
 */
public class CompletionContext {

    private final Object sender;
    private final String command;
    private final String[] args;
    private final String currentInput;
    private final int argIndex;
    private final Map<String, Object> data;

    private CompletionContext(Builder builder) {
        this.sender = Objects.requireNonNull(builder.sender, "sender cannot be null");
        this.command = Objects.requireNonNull(builder.command, "command cannot be null");
        this.args = builder.args != null ? builder.args.clone() : new String[0];
        this.currentInput = builder.currentInput != null ? builder.currentInput : "";
        this.argIndex = builder.argIndex;
        this.data = new HashMap<>(builder.data);
    }

    /**
     * Gets the command sender requesting completion.
     *
     * @return the sender
     */
    @NotNull
    public Object getSender() {
        return sender;
    }

    /**
     * Gets the sender cast to a specific type.
     *
     * @param type the type class
     * @param <T> the type
     * @return the sender as the type
     * @throws ClassCastException if the sender is not the expected type
     */
    @NotNull
    public <T> T getSender(@NotNull Class<T> type) {
        return type.cast(sender);
    }

    /**
     * Checks if the sender is a player.
     *
     * @return {@code true} if the sender is a player
     */
    public boolean isPlayer() {
        String className = sender.getClass().getName().toLowerCase();
        return className.contains("player");
    }

    /**
     * Gets the command name being completed.
     *
     * @return the command name
     */
    @NotNull
    public String getCommand() {
        return command;
    }

    /**
     * Gets all completed arguments.
     *
     * @return array of previous arguments
     */
    @NotNull
    public String[] getArgs() {
        return args.clone();
    }

    /**
     * Gets a specific completed argument.
     *
     * @param index the argument index
     * @return the argument, or empty string if not found
     */
    @NotNull
    public String getArg(int index) {
        return getArg(index, "");
    }

    /**
     * Gets a specific completed argument with a default.
     *
     * @param index the argument index
     * @param defaultValue the default if not found
     * @return the argument or default
     */
    @NotNull
    public String getArg(int index, @NotNull String defaultValue) {
        if (index >= 0 && index < args.length) {
            return args[index];
        }
        return defaultValue;
    }

    /**
     * Gets the number of completed arguments.
     *
     * @return the argument count
     */
    public int getArgCount() {
        return args.length;
    }

    /**
     * Gets the partial input currently being typed.
     *
     * <p>This is the text after the last space, which is what the user
     * is currently typing and suggestions should match.</p>
     *
     * @return the current partial input
     */
    @NotNull
    public String getCurrentInput() {
        return currentInput;
    }

    /**
     * Gets the index of the argument being completed.
     *
     * <p>Index 0 is the first argument after the command name.</p>
     *
     * @return the current argument index
     */
    public int getArgIndex() {
        return argIndex;
    }

    /**
     * Checks if the sender has a permission.
     *
     * @param permission the permission node
     * @return {@code true} if the sender has the permission
     */
    public boolean hasPermission(@NotNull String permission) {
        // Platform-specific implementation needed
        // This is a simplified check
        try {
            java.lang.reflect.Method method = sender.getClass().getMethod("hasPermission", String.class);
            return (boolean) method.invoke(sender, permission);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Stores custom data in the context.
     *
     * @param key the data key
     * @param value the value
     */
    public void put(@NotNull String key, @Nullable Object value) {
        if (value != null) {
            data.put(key, value);
        } else {
            data.remove(key);
        }
    }

    /**
     * Retrieves custom data from the context.
     *
     * @param key the data key
     * @param type the value type
     * @param <T> the type
     * @return the value, or {@code null} if not found
     */
    @Nullable
    public <T> T get(@NotNull String key, @NotNull Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CompletionContext.
     */
    public static class Builder {

        private Object sender;
        private String command = "";
        private String[] args;
        private String currentInput;
        private int argIndex;
        private final Map<String, Object> data = new HashMap<>();

        /**
         * Sets the command sender.
         *
         * @param sender the sender
         * @return this builder
         */
        @NotNull
        public Builder sender(@NotNull Object sender) {
            this.sender = sender;
            return this;
        }

        /**
         * Sets the command name.
         *
         * @param command the command
         * @return this builder
         */
        @NotNull
        public Builder command(@NotNull String command) {
            this.command = command;
            return this;
        }

        /**
         * Sets the completed arguments.
         *
         * @param args the arguments
         * @return this builder
         */
        @NotNull
        public Builder args(@NotNull String[] args) {
            this.args = args;
            return this;
        }

        /**
         * Sets the current partial input.
         *
         * @param currentInput the input
         * @return this builder
         */
        @NotNull
        public Builder currentInput(@NotNull String currentInput) {
            this.currentInput = currentInput;
            return this;
        }

        /**
         * Sets the current argument index.
         *
         * @param argIndex the index
         * @return this builder
         */
        @NotNull
        public Builder argIndex(int argIndex) {
            this.argIndex = argIndex;
            return this;
        }

        /**
         * Adds custom data.
         *
         * @param key the data key
         * @param value the value
         * @return this builder
         */
        @NotNull
        public Builder put(@NotNull String key, @Nullable Object value) {
            if (value != null) {
                this.data.put(key, value);
            }
            return this;
        }

        /**
         * Builds the context.
         *
         * @return the built context
         */
        @NotNull
        public CompletionContext build() {
            return new CompletionContext(this);
        }
    }
}
