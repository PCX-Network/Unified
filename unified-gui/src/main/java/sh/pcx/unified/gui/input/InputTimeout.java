/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.input;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for input timeout behavior.
 *
 * <p>Timeouts can be configured for any input type to automatically close
 * the input dialog after a specified duration. When a timeout occurs,
 * an optional message can be displayed to the player.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple timeout of 30 seconds
 * InputTimeout timeout = InputTimeout.of(Duration.ofSeconds(30));
 *
 * // Timeout with message
 * InputTimeout timeoutWithMessage = InputTimeout.of(
 *     Duration.ofMinutes(1),
 *     Component.text("You took too long to respond!")
 * );
 *
 * // No timeout (wait indefinitely)
 * InputTimeout noTimeout = InputTimeout.none();
 *
 * // Use with chat input
 * ChatInput.builder()
 *     .prompt(Component.text("Enter a name:"))
 *     .timeout(InputTimeout.of(Duration.ofSeconds(30)))
 *     .onComplete(result -> {
 *         if (result.isTimedOut()) {
 *             player.sendMessage("Timed out!");
 *         } else {
 *             handleInput(result.getMessage());
 *         }
 *     })
 *     .open(player);
 * }</pre>
 *
 * <h2>Behavior</h2>
 * <p>When a timeout is triggered:
 * <ol>
 *   <li>The input dialog is closed</li>
 *   <li>The timeout message is sent to the player (if configured)</li>
 *   <li>The callback is invoked with a timed-out result</li>
 *   <li>If a previous GUI was open, the player returns to it (if configured)</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ChatInput
 * @see InputCancellation
 */
public final class InputTimeout {

    /**
     * Represents no timeout (wait indefinitely).
     */
    private static final InputTimeout NONE = new InputTimeout(null, null, false);

    /**
     * Default timeout of 60 seconds.
     */
    private static final InputTimeout DEFAULT = new InputTimeout(
            Duration.ofSeconds(60),
            null,
            true
    );

    private final Duration duration;
    private final Component message;
    private final boolean showMessage;

    /**
     * Private constructor for InputTimeout.
     *
     * @param duration    the timeout duration, or null for no timeout
     * @param message     the message to display on timeout
     * @param showMessage whether to show the timeout message
     */
    private InputTimeout(@Nullable Duration duration, @Nullable Component message, boolean showMessage) {
        this.duration = duration;
        this.message = message;
        this.showMessage = showMessage;
    }

    /**
     * Creates an InputTimeout with no timeout (wait indefinitely).
     *
     * @return an InputTimeout with no timeout
     * @since 1.0.0
     */
    @NotNull
    public static InputTimeout none() {
        return NONE;
    }

    /**
     * Creates a default timeout of 60 seconds.
     *
     * @return a default InputTimeout
     * @since 1.0.0
     */
    @NotNull
    public static InputTimeout defaultTimeout() {
        return DEFAULT;
    }

    /**
     * Creates an InputTimeout with the specified duration.
     *
     * @param duration the timeout duration
     * @return a new InputTimeout
     * @throws IllegalArgumentException if duration is negative
     * @since 1.0.0
     */
    @NotNull
    public static InputTimeout of(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        return new InputTimeout(duration, null, true);
    }

    /**
     * Creates an InputTimeout with the specified duration and message.
     *
     * @param duration the timeout duration
     * @param message  the message to display when timeout occurs
     * @return a new InputTimeout
     * @throws IllegalArgumentException if duration is negative
     * @since 1.0.0
     */
    @NotNull
    public static InputTimeout of(@NotNull Duration duration, @NotNull Component message) {
        Objects.requireNonNull(duration, "duration cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        return new InputTimeout(duration, message, true);
    }

    /**
     * Creates an InputTimeout from seconds.
     *
     * @param seconds the number of seconds
     * @return a new InputTimeout
     * @throws IllegalArgumentException if seconds is negative
     * @since 1.0.0
     */
    @NotNull
    public static InputTimeout ofSeconds(long seconds) {
        return of(Duration.ofSeconds(seconds));
    }

    /**
     * Creates an InputTimeout from minutes.
     *
     * @param minutes the number of minutes
     * @return a new InputTimeout
     * @throws IllegalArgumentException if minutes is negative
     * @since 1.0.0
     */
    @NotNull
    public static InputTimeout ofMinutes(long minutes) {
        return of(Duration.ofMinutes(minutes));
    }

    /**
     * Checks if this represents no timeout.
     *
     * @return true if there is no timeout configured
     * @since 1.0.0
     */
    public boolean isNone() {
        return duration == null;
    }

    /**
     * Checks if this represents a configured timeout.
     *
     * @return true if a timeout is configured
     * @since 1.0.0
     */
    public boolean hasTimeout() {
        return duration != null;
    }

    /**
     * Returns the timeout duration.
     *
     * @return the timeout duration, or null if no timeout
     * @since 1.0.0
     */
    @Nullable
    public Duration getDuration() {
        return duration;
    }

    /**
     * Returns the timeout duration in milliseconds.
     *
     * @return the timeout in milliseconds, or -1 if no timeout
     * @since 1.0.0
     */
    public long toMillis() {
        return duration != null ? duration.toMillis() : -1L;
    }

    /**
     * Returns the timeout duration in seconds.
     *
     * @return the timeout in seconds, or -1 if no timeout
     * @since 1.0.0
     */
    public long toSeconds() {
        return duration != null ? duration.toSeconds() : -1L;
    }

    /**
     * Returns the timeout duration in Minecraft ticks (20 ticks per second).
     *
     * @return the timeout in ticks, or -1 if no timeout
     * @since 1.0.0
     */
    public long toTicks() {
        return duration != null ? duration.toMillis() / 50 : -1L;
    }

    /**
     * Returns the message to display when timeout occurs.
     *
     * @return the timeout message, or null if no message
     * @since 1.0.0
     */
    @Nullable
    public Component getMessage() {
        return message;
    }

    /**
     * Checks if a message should be shown on timeout.
     *
     * @return true if a message should be shown
     * @since 1.0.0
     */
    public boolean shouldShowMessage() {
        return showMessage && message != null;
    }

    /**
     * Creates a new InputTimeout with the specified message.
     *
     * @param message the message to display on timeout
     * @return a new InputTimeout with the message
     * @since 1.0.0
     */
    @NotNull
    public InputTimeout withMessage(@NotNull Component message) {
        Objects.requireNonNull(message, "message cannot be null");
        return new InputTimeout(this.duration, message, true);
    }

    /**
     * Creates a new InputTimeout without showing a message.
     *
     * @return a new InputTimeout that doesn't show a message
     * @since 1.0.0
     */
    @NotNull
    public InputTimeout silent() {
        return new InputTimeout(this.duration, null, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputTimeout that = (InputTimeout) o;
        return showMessage == that.showMessage &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, message, showMessage);
    }

    @Override
    public String toString() {
        if (duration == null) {
            return "InputTimeout{none}";
        }
        return "InputTimeout{duration=" + duration + ", hasMessage=" + (message != null) + "}";
    }
}
