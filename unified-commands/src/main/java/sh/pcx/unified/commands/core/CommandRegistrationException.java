/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when command registration fails.
 *
 * <p>This exception is thrown when a command cannot be registered with the
 * command system. Common causes include:</p>
 * <ul>
 *   <li>Missing {@code @Command} annotation on the class</li>
 *   <li>Duplicate command names</li>
 *   <li>Invalid method signatures for handlers</li>
 *   <li>Missing required dependencies</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Handling Registration Errors</h3>
 * <pre>{@code
 * try {
 *     commandService.register(new MyCommand());
 * } catch (CommandRegistrationException e) {
 *     logger.error("Failed to register command: " + e.getMessage());
 *     if (e.getCommandClass() != null) {
 *         logger.error("Command class: " + e.getCommandClass().getName());
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see CommandService
 */
public class CommandRegistrationException extends RuntimeException {

    private final Class<?> commandClass;
    private final String commandName;

    /**
     * Creates a new registration exception.
     *
     * @param message the error message
     */
    public CommandRegistrationException(@NotNull String message) {
        super(message);
        this.commandClass = null;
        this.commandName = null;
    }

    /**
     * Creates a new registration exception with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CommandRegistrationException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.commandClass = null;
        this.commandName = null;
    }

    /**
     * Creates a new registration exception for a specific command class.
     *
     * @param message the error message
     * @param commandClass the command class that failed to register
     */
    public CommandRegistrationException(@NotNull String message, @NotNull Class<?> commandClass) {
        super(message + " (class: " + commandClass.getName() + ")");
        this.commandClass = commandClass;
        this.commandName = null;
    }

    /**
     * Creates a new registration exception for a specific command.
     *
     * @param message the error message
     * @param commandClass the command class
     * @param commandName the command name
     */
    public CommandRegistrationException(
            @NotNull String message,
            @Nullable Class<?> commandClass,
            @Nullable String commandName
    ) {
        super(buildMessage(message, commandClass, commandName));
        this.commandClass = commandClass;
        this.commandName = commandName;
    }

    /**
     * Creates a new registration exception with full details.
     *
     * @param message the error message
     * @param commandClass the command class
     * @param commandName the command name
     * @param cause the underlying cause
     */
    public CommandRegistrationException(
            @NotNull String message,
            @Nullable Class<?> commandClass,
            @Nullable String commandName,
            @NotNull Throwable cause
    ) {
        super(buildMessage(message, commandClass, commandName), cause);
        this.commandClass = commandClass;
        this.commandName = commandName;
    }

    private static String buildMessage(String message, Class<?> commandClass, String commandName) {
        StringBuilder sb = new StringBuilder(message);
        if (commandName != null) {
            sb.append(" (command: ").append(commandName).append(")");
        }
        if (commandClass != null) {
            sb.append(" [class: ").append(commandClass.getName()).append("]");
        }
        return sb.toString();
    }

    /**
     * Gets the command class that failed to register.
     *
     * @return the command class, or {@code null} if not specified
     */
    @Nullable
    public Class<?> getCommandClass() {
        return commandClass;
    }

    /**
     * Gets the command name that failed to register.
     *
     * @return the command name, or {@code null} if not specified
     */
    @Nullable
    public String getCommandName() {
        return commandName;
    }

    /**
     * Creates an exception for a missing annotation.
     *
     * @param commandClass the class missing the annotation
     * @return a new exception
     */
    @NotNull
    public static CommandRegistrationException missingAnnotation(@NotNull Class<?> commandClass) {
        return new CommandRegistrationException(
                "Class is not annotated with @Command",
                commandClass
        );
    }

    /**
     * Creates an exception for a duplicate command name.
     *
     * @param commandName the duplicate name
     * @param existingClass the class that already registered the name
     * @param newClass the class attempting to register
     * @return a new exception
     */
    @NotNull
    public static CommandRegistrationException duplicate(
            @NotNull String commandName,
            @NotNull Class<?> existingClass,
            @NotNull Class<?> newClass
    ) {
        return new CommandRegistrationException(
                "Command '" + commandName + "' is already registered by " + existingClass.getName(),
                newClass,
                commandName
        );
    }

    /**
     * Creates an exception for an invalid handler method.
     *
     * @param commandClass the command class
     * @param methodName the invalid method name
     * @param reason the reason it's invalid
     * @return a new exception
     */
    @NotNull
    public static CommandRegistrationException invalidHandler(
            @NotNull Class<?> commandClass,
            @NotNull String methodName,
            @NotNull String reason
    ) {
        return new CommandRegistrationException(
                "Invalid handler method '" + methodName + "': " + reason,
                commandClass,
                null
        );
    }
}
