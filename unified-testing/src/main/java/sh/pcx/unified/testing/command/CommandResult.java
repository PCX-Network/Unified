/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of executing a command in tests.
 *
 * <p>CommandResult captures the success/failure state, any output messages,
 * and exception information for command execution verification.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockPlayer player = server.addPlayer("Steve");
 * player.addPermission("myplugin.admin");
 *
 * CommandResult result = player.performCommandWithResult("myplugin reload");
 *
 * assertThat(result.isSuccess()).isTrue();
 * assertThat(result.getMessage()).contains("Configuration reloaded");
 *
 * // Test failure case
 * MockPlayer regular = server.addPlayer("Regular");
 * CommandResult failResult = regular.performCommandWithResult("myplugin reload");
 *
 * assertThat(failResult.isSuccess()).isFalse();
 * assertThat(failResult.getMessage()).contains("No permission");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class CommandResult {

    private final boolean success;
    private final String message;
    private final Throwable exception;
    private final ResultType type;

    /**
     * Result type enumeration.
     */
    public enum ResultType {
        /** Command executed successfully. */
        SUCCESS,
        /** Command failed due to user error (bad arguments, no permission). */
        FAILURE,
        /** Command threw an exception. */
        ERROR,
        /** Command was not found. */
        NOT_FOUND,
        /** Command was cancelled. */
        CANCELLED
    }

    private CommandResult(
        boolean success,
        @Nullable String message,
        @Nullable Throwable exception,
        @NotNull ResultType type
    ) {
        this.success = success;
        this.message = message;
        this.exception = exception;
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Creates a successful result.
     *
     * @return a success result
     */
    @NotNull
    public static CommandResult success() {
        return new CommandResult(true, null, null, ResultType.SUCCESS);
    }

    /**
     * Creates a successful result with a message.
     *
     * @param message the success message
     * @return a success result
     */
    @NotNull
    public static CommandResult success(@Nullable String message) {
        return new CommandResult(true, message, null, ResultType.SUCCESS);
    }

    /**
     * Creates a failure result.
     *
     * @param message the failure message
     * @return a failure result
     */
    @NotNull
    public static CommandResult failure(@NotNull String message) {
        return new CommandResult(false, message, null, ResultType.FAILURE);
    }

    /**
     * Creates an error result from an exception.
     *
     * @param exception the exception that occurred
     * @return an error result
     */
    @NotNull
    public static CommandResult error(@NotNull Throwable exception) {
        return new CommandResult(
            false,
            exception.getMessage(),
            exception,
            ResultType.ERROR
        );
    }

    /**
     * Creates a not found result.
     *
     * @param command the command that was not found
     * @return a not found result
     */
    @NotNull
    public static CommandResult notFound(@NotNull String command) {
        return new CommandResult(
            false,
            "Unknown command: " + command,
            null,
            ResultType.NOT_FOUND
        );
    }

    /**
     * Creates a cancelled result.
     *
     * @return a cancelled result
     */
    @NotNull
    public static CommandResult cancelled() {
        return new CommandResult(false, "Command cancelled", null, ResultType.CANCELLED);
    }

    /**
     * Creates a cancelled result with a message.
     *
     * @param message the cancellation message
     * @return a cancelled result
     */
    @NotNull
    public static CommandResult cancelled(@Nullable String message) {
        return new CommandResult(false, message, null, ResultType.CANCELLED);
    }

    /**
     * Returns whether the command executed successfully.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns whether the command failed.
     *
     * @return true if failed
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Returns the result message.
     *
     * @return the message, or null if none
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * Returns the result message as an Optional.
     *
     * @return an Optional containing the message
     */
    @NotNull
    public Optional<String> getMessageOptional() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns any exception that occurred.
     *
     * @return the exception, or null if none
     */
    @Nullable
    public Throwable getException() {
        return exception;
    }

    /**
     * Returns the exception as an Optional.
     *
     * @return an Optional containing the exception
     */
    @NotNull
    public Optional<Throwable> getExceptionOptional() {
        return Optional.ofNullable(exception);
    }

    /**
     * Returns the result type.
     *
     * @return the result type
     */
    @NotNull
    public ResultType getType() {
        return type;
    }

    /**
     * Checks if the result type is SUCCESS.
     *
     * @return true if type is SUCCESS
     */
    public boolean isSuccessType() {
        return type == ResultType.SUCCESS;
    }

    /**
     * Checks if the result type is FAILURE.
     *
     * @return true if type is FAILURE
     */
    public boolean isFailureType() {
        return type == ResultType.FAILURE;
    }

    /**
     * Checks if the result type is ERROR.
     *
     * @return true if type is ERROR
     */
    public boolean isError() {
        return type == ResultType.ERROR;
    }

    /**
     * Checks if the command was not found.
     *
     * @return true if command was not found
     */
    public boolean isNotFound() {
        return type == ResultType.NOT_FOUND;
    }

    /**
     * Checks if the command was cancelled.
     *
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return type == ResultType.CANCELLED;
    }

    /**
     * Asserts that the command was successful.
     *
     * @throws AssertionError if the command failed
     */
    public void assertSuccess() {
        if (!success) {
            throw new AssertionError(
                "Expected command to succeed but it failed: " + message
            );
        }
    }

    /**
     * Asserts that the command failed.
     *
     * @throws AssertionError if the command succeeded
     */
    public void assertFailure() {
        if (success) {
            throw new AssertionError(
                "Expected command to fail but it succeeded"
            );
        }
    }

    /**
     * Asserts that the message contains the given text.
     *
     * @param text the text to check for
     * @throws AssertionError if the message doesn't contain the text
     */
    public void assertMessageContains(@NotNull String text) {
        if (message == null || !message.contains(text)) {
            throw new AssertionError(
                "Expected message to contain '" + text + "' but was: " + message
            );
        }
    }

    /**
     * Rethrows the exception if one occurred.
     *
     * @throws RuntimeException if an exception occurred
     */
    public void rethrowIfError() {
        if (exception != null) {
            throw new RuntimeException("Command execution failed", exception);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandResult that = (CommandResult) o;
        return success == that.success &&
               Objects.equals(message, that.message) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, type);
    }

    @Override
    public String toString() {
        return "CommandResult{" +
            "success=" + success +
            ", type=" + type +
            ", message='" + message + '\'' +
            (exception != null ? ", exception=" + exception.getClass().getSimpleName() : "") +
            '}';
    }
}
