/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.input;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of an anvil input dialog.
 *
 * <p>This class encapsulates the result of an anvil input operation,
 * including the text entered, the completion state, and any validation
 * errors that occurred.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * AnvilInput.builder()
 *     .title(Component.text("Enter Name"))
 *     .defaultText("Default")
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             String text = result.getText();
 *             player.sendMessage("You entered: " + text);
 *         } else if (result.isCancelled()) {
 *             player.sendMessage("Input cancelled");
 *         }
 *     })
 *     .open(player);
 * }</pre>
 *
 * <h2>States</h2>
 * <p>An anvil input result can be in one of these states:
 * <ul>
 *   <li>{@link State#CONFIRMED} - Player clicked the output item to confirm</li>
 *   <li>{@link State#CANCELLED} - Player closed the anvil or pressed escape</li>
 *   <li>{@link State#VALIDATION_FAILED} - Input failed validation</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AnvilInput
 * @see AnvilInputBuilder
 */
public final class AnvilInputResult {

    private final String text;
    private final State state;
    private final InputValidator.ValidationResult validationResult;

    /**
     * Creates a new AnvilInputResult.
     *
     * @param text             the text entered
     * @param state            the result state
     * @param validationResult the validation result, if any
     */
    private AnvilInputResult(
            @Nullable String text,
            @NotNull State state,
            @Nullable InputValidator.ValidationResult validationResult
    ) {
        this.text = text;
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.validationResult = validationResult;
    }

    /**
     * Creates a confirmed result with the given text.
     *
     * @param text the confirmed text
     * @return a confirmed result
     * @since 1.0.0
     */
    @NotNull
    public static AnvilInputResult confirmed(@NotNull String text) {
        Objects.requireNonNull(text, "text cannot be null");
        return new AnvilInputResult(text, State.CONFIRMED, null);
    }

    /**
     * Creates a cancelled result.
     *
     * @return a cancelled result
     * @since 1.0.0
     */
    @NotNull
    public static AnvilInputResult cancelled() {
        return new AnvilInputResult(null, State.CANCELLED, null);
    }

    /**
     * Creates a cancelled result with the last entered text.
     *
     * @param lastText the last text before cancellation
     * @return a cancelled result with text
     * @since 1.0.0
     */
    @NotNull
    public static AnvilInputResult cancelled(@Nullable String lastText) {
        return new AnvilInputResult(lastText, State.CANCELLED, null);
    }

    /**
     * Creates a validation failed result.
     *
     * @param text             the text that failed validation
     * @param validationResult the validation result with error
     * @return a validation failed result
     * @since 1.0.0
     */
    @NotNull
    public static AnvilInputResult validationFailed(
            @NotNull String text,
            @NotNull InputValidator.ValidationResult validationResult
    ) {
        Objects.requireNonNull(text, "text cannot be null");
        Objects.requireNonNull(validationResult, "validationResult cannot be null");
        return new AnvilInputResult(text, State.VALIDATION_FAILED, validationResult);
    }

    /**
     * Returns the text entered in the anvil.
     *
     * @return the entered text, or empty if cancelled without text
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getText() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the text entered, or a default value if not present.
     *
     * @param defaultValue the default value to return
     * @return the entered text or the default
     * @since 1.0.0
     */
    @NotNull
    public String getTextOrDefault(@NotNull String defaultValue) {
        return text != null ? text : defaultValue;
    }

    /**
     * Returns the text entered, or an empty string if not present.
     *
     * @return the entered text or empty string
     * @since 1.0.0
     */
    @NotNull
    public String getTextOrEmpty() {
        return text != null ? text : "";
    }

    /**
     * Returns the result state.
     *
     * @return the state
     * @since 1.0.0
     */
    @NotNull
    public State getState() {
        return state;
    }

    /**
     * Checks if the input was confirmed.
     *
     * @return true if the player confirmed the input
     * @since 1.0.0
     */
    public boolean isConfirmed() {
        return state == State.CONFIRMED;
    }

    /**
     * Checks if the input was cancelled.
     *
     * @return true if the player cancelled the input
     * @since 1.0.0
     */
    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    /**
     * Checks if validation failed.
     *
     * @return true if the input failed validation
     * @since 1.0.0
     */
    public boolean isValidationFailed() {
        return state == State.VALIDATION_FAILED;
    }

    /**
     * Returns the validation result if validation was performed.
     *
     * @return the validation result, or empty if not applicable
     * @since 1.0.0
     */
    @NotNull
    public Optional<InputValidator.ValidationResult> getValidationResult() {
        return Optional.ofNullable(validationResult);
    }

    /**
     * Checks if the result has text (regardless of state).
     *
     * @return true if text is present
     * @since 1.0.0
     */
    public boolean hasText() {
        return text != null && !text.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnvilInputResult that = (AnvilInputResult) o;
        return Objects.equals(text, that.text) &&
                state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, state);
    }

    @Override
    public String toString() {
        return "AnvilInputResult{" +
                "text='" + text + '\'' +
                ", state=" + state +
                '}';
    }

    /**
     * Possible states for anvil input results.
     *
     * @since 1.0.0
     */
    public enum State {
        /**
         * The player confirmed the input by clicking the output item.
         */
        CONFIRMED,

        /**
         * The player cancelled the input by closing or pressing escape.
         */
        CANCELLED,

        /**
         * The input failed validation and was not accepted.
         */
        VALIDATION_FAILED
    }
}
