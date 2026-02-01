/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.execution;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents the result of a command execution.
 *
 * <p>Command results provide information about whether a command succeeded
 * or failed, along with optional messages and return values. They can be
 * used for command chaining, logging, and error handling.</p>
 *
 * <h2>Result Types</h2>
 * <ul>
 *   <li><b>SUCCESS</b> - Command executed successfully</li>
 *   <li><b>FAILURE</b> - Command failed during execution</li>
 *   <li><b>NO_PERMISSION</b> - Sender lacks required permission</li>
 *   <li><b>INVALID_ARGS</b> - Arguments failed to parse or validate</li>
 *   <li><b>COOLDOWN</b> - Command is on cooldown</li>
 *   <li><b>NOT_FOUND</b> - Command or subcommand not found</li>
 *   <li><b>SENDER_MISMATCH</b> - Wrong sender type (e.g., console for player-only)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Checking Results</h3>
 * <pre>{@code
 * CommandResult result = executor.execute(player, "spawn");
 *
 * if (result.isSuccess()) {
 *     log.info("Spawn command executed successfully");
 * } else {
 *     log.warn("Spawn failed: " + result.getMessage());
 * }
 * }</pre>
 *
 * <h3>Handling Different Types</h3>
 * <pre>{@code
 * CommandResult result = executor.execute(player, "teleport", "Steve");
 *
 * switch (result.getType()) {
 *     case SUCCESS:
 *         log.info("Teleported successfully");
 *         break;
 *     case NO_PERMISSION:
 *         player.sendMessage("You need permission to teleport!");
 *         break;
 *     case INVALID_ARGS:
 *         player.sendMessage("Usage: /teleport <player>");
 *         break;
 *     case COOLDOWN:
 *         player.sendMessage("Wait before teleporting again!");
 *         break;
 * }
 * }</pre>
 *
 * <h3>With Return Values</h3>
 * <pre>{@code
 * // Command that returns a value
 * CommandResult result = executor.execute(console, "getbalance", "Steve");
 *
 * result.getValue(Double.class).ifPresent(balance -> {
 *     log.info("Steve's balance: " + balance);
 * });
 * }</pre>
 *
 * <h3>Creating Results in Commands</h3>
 * <pre>{@code
 * @Subcommand("transfer")
 * public CommandResult transfer(
 *     @Sender Player sender,
 *     @Arg("target") Player target,
 *     @Arg("amount") double amount
 * ) {
 *     if (amount <= 0) {
 *         return CommandResult.failure("Amount must be positive");
 *     }
 *
 *     if (!economy.has(sender, amount)) {
 *         return CommandResult.failure("Insufficient funds");
 *     }
 *
 *     economy.transfer(sender, target, amount);
 *     return CommandResult.success("Transferred $" + amount + " to " + target.getName());
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.commands.core.CommandExecutor
 */
public class CommandResult {

    private final Type type;
    private final String message;
    private final Object value;
    private final Throwable exception;
    private final long executionTime;

    private CommandResult(Type type, String message, Object value, Throwable exception, long executionTime) {
        this.type = type;
        this.message = message;
        this.value = value;
        this.exception = exception;
        this.executionTime = executionTime;
    }

    /**
     * Gets the result type.
     *
     * @return the result type
     */
    @NotNull
    public Type getType() {
        return type;
    }

    /**
     * Checks if the command succeeded.
     *
     * @return {@code true} if successful
     */
    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }

    /**
     * Checks if the command failed.
     *
     * @return {@code true} if failed
     */
    public boolean isFailure() {
        return type != Type.SUCCESS;
    }

    /**
     * Gets the result message.
     *
     * @return the message, or empty string if none
     */
    @NotNull
    public String getMessage() {
        return message != null ? message : "";
    }

    /**
     * Checks if a message is present.
     *
     * @return {@code true} if a message exists
     */
    public boolean hasMessage() {
        return message != null && !message.isEmpty();
    }

    /**
     * Gets an optional return value.
     *
     * @return the value, or empty if none
     */
    @NotNull
    public Optional<Object> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Gets the return value cast to a specific type.
     *
     * @param type the type class
     * @param <T> the type
     * @return the value, or empty if none or wrong type
     */
    @NotNull
    public <T> Optional<T> getValue(@NotNull Class<T> type) {
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Gets any exception that occurred.
     *
     * @return the exception, or empty if none
     */
    @NotNull
    public Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }

    /**
     * Gets the command execution time in milliseconds.
     *
     * @return execution time in ms
     */
    public long getExecutionTime() {
        return executionTime;
    }

    // Static factory methods

    /**
     * Creates a success result.
     *
     * @return a success result
     */
    @NotNull
    public static CommandResult success() {
        return new CommandResult(Type.SUCCESS, null, null, null, 0);
    }

    /**
     * Creates a success result with a message.
     *
     * @param message the success message
     * @return a success result
     */
    @NotNull
    public static CommandResult success(@NotNull String message) {
        return new CommandResult(Type.SUCCESS, message, null, null, 0);
    }

    /**
     * Creates a success result with a value.
     *
     * @param value the return value
     * @return a success result
     */
    @NotNull
    public static CommandResult success(@Nullable Object value) {
        return new CommandResult(Type.SUCCESS, null, value, null, 0);
    }

    /**
     * Creates a success result with message and value.
     *
     * @param message the success message
     * @param value the return value
     * @return a success result
     */
    @NotNull
    public static CommandResult success(@NotNull String message, @Nullable Object value) {
        return new CommandResult(Type.SUCCESS, message, value, null, 0);
    }

    /**
     * Creates a failure result.
     *
     * @param message the failure message
     * @return a failure result
     */
    @NotNull
    public static CommandResult failure(@NotNull String message) {
        return new CommandResult(Type.FAILURE, message, null, null, 0);
    }

    /**
     * Creates a failure result with an exception.
     *
     * @param message the failure message
     * @param exception the exception
     * @return a failure result
     */
    @NotNull
    public static CommandResult failure(@NotNull String message, @NotNull Throwable exception) {
        return new CommandResult(Type.FAILURE, message, null, exception, 0);
    }

    /**
     * Creates a no-permission result.
     *
     * @param permission the missing permission
     * @return a no-permission result
     */
    @NotNull
    public static CommandResult noPermission(@NotNull String permission) {
        return new CommandResult(Type.NO_PERMISSION, "Missing permission: " + permission, permission, null, 0);
    }

    /**
     * Creates an invalid-args result.
     *
     * @param message the error message
     * @return an invalid-args result
     */
    @NotNull
    public static CommandResult invalidArgs(@NotNull String message) {
        return new CommandResult(Type.INVALID_ARGS, message, null, null, 0);
    }

    /**
     * Creates a cooldown result.
     *
     * @param remainingMs remaining cooldown in milliseconds
     * @return a cooldown result
     */
    @NotNull
    public static CommandResult cooldown(long remainingMs) {
        String formatted = formatDuration(remainingMs);
        return new CommandResult(Type.COOLDOWN, "Please wait " + formatted, remainingMs, null, 0);
    }

    /**
     * Creates a not-found result.
     *
     * @param command the command that wasn't found
     * @return a not-found result
     */
    @NotNull
    public static CommandResult notFound(@NotNull String command) {
        return new CommandResult(Type.NOT_FOUND, "Unknown command: " + command, null, null, 0);
    }

    /**
     * Creates a sender-mismatch result.
     *
     * @param expectedType the expected sender type
     * @return a sender-mismatch result
     */
    @NotNull
    public static CommandResult senderMismatch(@NotNull String expectedType) {
        return new CommandResult(Type.SENDER_MISMATCH, "This command can only be run by: " + expectedType, null, null, 0);
    }

    /**
     * Creates a result with execution time.
     *
     * @param original the original result
     * @param executionTimeMs the execution time in milliseconds
     * @return a new result with execution time set
     */
    @NotNull
    public static CommandResult withExecutionTime(@NotNull CommandResult original, long executionTimeMs) {
        return new CommandResult(
                original.type,
                original.message,
                original.value,
                original.exception,
                executionTimeMs
        );
    }

    private static String formatDuration(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + " seconds";
        }
        long minutes = seconds / 60;
        seconds %= 60;
        if (minutes < 60) {
            return minutes + " minutes " + (seconds > 0 ? seconds + " seconds" : "");
        }
        long hours = minutes / 60;
        minutes %= 60;
        return hours + " hours " + (minutes > 0 ? minutes + " minutes" : "");
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", executionTime=" + executionTime + "ms" +
                '}';
    }

    /**
     * Result types.
     */
    public enum Type {
        /**
         * Command executed successfully.
         */
        SUCCESS,

        /**
         * Command failed during execution.
         */
        FAILURE,

        /**
         * Sender lacks required permission.
         */
        NO_PERMISSION,

        /**
         * Arguments failed to parse or validate.
         */
        INVALID_ARGS,

        /**
         * Command is on cooldown.
         */
        COOLDOWN,

        /**
         * Command or subcommand not found.
         */
        NOT_FOUND,

        /**
         * Wrong sender type.
         */
        SENDER_MISMATCH
    }
}
