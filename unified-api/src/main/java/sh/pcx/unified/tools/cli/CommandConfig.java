/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for generating a command class.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CommandConfig config = CommandConfig.builder()
 *     .description("Teleport to a player")
 *     .permission("myplugin.teleport")
 *     .aliases("tp", "tele")
 *     .playerOnly(true)
 *     .cooldown(5)
 *     .subcommand("here", "Teleport player to you")
 *     .subcommand("all", "Teleport all players")
 *     .build();
 *
 * generator.generateCommand(projectPath, "TeleportCommand", config);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ProjectGenerator
 */
public final class CommandConfig {

    private final String description;
    private final String permission;
    private final List<String> aliases;
    private final boolean playerOnly;
    private final int cooldown;
    private final List<SubcommandDef> subcommands;
    private final List<ArgumentDef> arguments;
    private final boolean async;

    private CommandConfig(Builder builder) {
        this.description = builder.description;
        this.permission = builder.permission;
        this.aliases = List.copyOf(builder.aliases);
        this.playerOnly = builder.playerOnly;
        this.cooldown = builder.cooldown;
        this.subcommands = List.copyOf(builder.subcommands);
        this.arguments = List.copyOf(builder.arguments);
        this.async = builder.async;
    }

    /**
     * Returns the command description.
     *
     * @return the description, or null
     * @since 1.0.0
     */
    @Nullable
    public String description() {
        return description;
    }

    /**
     * Returns the required permission.
     *
     * @return the permission, or null
     * @since 1.0.0
     */
    @Nullable
    public String permission() {
        return permission;
    }

    /**
     * Returns the command aliases.
     *
     * @return the aliases
     * @since 1.0.0
     */
    @NotNull
    public List<String> aliases() {
        return aliases;
    }

    /**
     * Returns whether the command is player-only.
     *
     * @return true if player-only
     * @since 1.0.0
     */
    public boolean playerOnly() {
        return playerOnly;
    }

    /**
     * Returns the cooldown in seconds.
     *
     * @return the cooldown (0 for none)
     * @since 1.0.0
     */
    public int cooldown() {
        return cooldown;
    }

    /**
     * Returns the subcommand definitions.
     *
     * @return the subcommands
     * @since 1.0.0
     */
    @NotNull
    public List<SubcommandDef> subcommands() {
        return subcommands;
    }

    /**
     * Returns the argument definitions.
     *
     * @return the arguments
     * @since 1.0.0
     */
    @NotNull
    public List<ArgumentDef> arguments() {
        return arguments;
    }

    /**
     * Returns whether the command runs asynchronously.
     *
     * @return true if async
     * @since 1.0.0
     */
    public boolean async() {
        return async;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default configuration.
     *
     * @return the default config
     * @since 1.0.0
     */
    @NotNull
    public static CommandConfig defaults() {
        return builder().build();
    }

    /**
     * Definition of a subcommand.
     *
     * @param name        the subcommand name
     * @param description the description
     * @param permission  the required permission
     * @since 1.0.0
     */
    public record SubcommandDef(
            @NotNull String name,
            @Nullable String description,
            @Nullable String permission
    ) {
        /**
         * Creates a subcommand definition.
         *
         * @param name        the name
         * @param description the description
         * @return the definition
         */
        public static SubcommandDef of(@NotNull String name, @Nullable String description) {
            return new SubcommandDef(name, description, null);
        }
    }

    /**
     * Definition of a command argument.
     *
     * @param name     the argument name
     * @param type     the argument type
     * @param required whether the argument is required
     * @param defaultValue the default value
     * @since 1.0.0
     */
    public record ArgumentDef(
            @NotNull String name,
            @NotNull String type,
            boolean required,
            @Nullable String defaultValue
    ) {
        /**
         * Creates a required argument definition.
         *
         * @param name the name
         * @param type the type
         * @return the definition
         */
        public static ArgumentDef required(@NotNull String name, @NotNull String type) {
            return new ArgumentDef(name, type, true, null);
        }

        /**
         * Creates an optional argument definition.
         *
         * @param name         the name
         * @param type         the type
         * @param defaultValue the default value
         * @return the definition
         */
        public static ArgumentDef optional(@NotNull String name, @NotNull String type, @Nullable String defaultValue) {
            return new ArgumentDef(name, type, false, defaultValue);
        }
    }

    /**
     * Builder for {@link CommandConfig}.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String description;
        private String permission;
        private final List<String> aliases = new ArrayList<>();
        private boolean playerOnly = false;
        private int cooldown = 0;
        private final List<SubcommandDef> subcommands = new ArrayList<>();
        private final List<ArgumentDef> arguments = new ArrayList<>();
        private boolean async = false;

        private Builder() {}

        /**
         * Sets the command description.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the required permission.
         *
         * @param permission the permission
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder permission(@Nullable String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Adds an alias.
         *
         * @param alias the alias
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder alias(@NotNull String alias) {
            this.aliases.add(alias);
            return this;
        }

        /**
         * Adds multiple aliases.
         *
         * @param aliases the aliases
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder aliases(@NotNull String... aliases) {
            this.aliases.addAll(List.of(aliases));
            return this;
        }

        /**
         * Sets whether the command is player-only.
         *
         * @param playerOnly true for player-only
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder playerOnly(boolean playerOnly) {
            this.playerOnly = playerOnly;
            return this;
        }

        /**
         * Sets the cooldown in seconds.
         *
         * @param seconds the cooldown
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder cooldown(int seconds) {
            this.cooldown = seconds;
            return this;
        }

        /**
         * Adds a subcommand.
         *
         * @param name        the subcommand name
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder subcommand(@NotNull String name, @Nullable String description) {
            this.subcommands.add(SubcommandDef.of(name, description));
            return this;
        }

        /**
         * Adds a subcommand with permission.
         *
         * @param name        the subcommand name
         * @param description the description
         * @param permission  the permission
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder subcommand(@NotNull String name, @Nullable String description, @Nullable String permission) {
            this.subcommands.add(new SubcommandDef(name, description, permission));
            return this;
        }

        /**
         * Adds a required argument.
         *
         * @param name the argument name
         * @param type the argument type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder argument(@NotNull String name, @NotNull String type) {
            this.arguments.add(ArgumentDef.required(name, type));
            return this;
        }

        /**
         * Adds an optional argument.
         *
         * @param name         the argument name
         * @param type         the argument type
         * @param defaultValue the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder optionalArgument(@NotNull String name, @NotNull String type, @Nullable String defaultValue) {
            this.arguments.add(ArgumentDef.optional(name, type, defaultValue));
            return this;
        }

        /**
         * Sets whether the command runs asynchronously.
         *
         * @param async true for async
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the command config
         * @since 1.0.0
         */
        @NotNull
        public CommandConfig build() {
            return new CommandConfig(this);
        }
    }
}
