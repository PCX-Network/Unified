/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.input;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for input cancellation behavior.
 *
 * <p>Cancellation can occur when:
 * <ul>
 *   <li>The player presses Escape (for anvil/sign inputs)</li>
 *   <li>The player closes the inventory</li>
 *   <li>The player types a cancel keyword (for chat inputs)</li>
 *   <li>The player disconnects</li>
 *   <li>The input times out</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Default cancellation behavior
 * InputCancellation cancel = InputCancellation.defaults();
 *
 * // Custom cancel keyword for chat input
 * InputCancellation chatCancel = InputCancellation.builder()
 *     .cancelKeyword("cancel")
 *     .cancelMessage(Component.text("Input cancelled."))
 *     .returnToPreviousGui(true)
 *     .build();
 *
 * // Cancellation with custom behavior
 * InputCancellation custom = InputCancellation.builder()
 *     .allowEscape(false) // Don't allow escape key
 *     .onCancel(() -> player.sendMessage("You must complete the input!"))
 *     .build();
 *
 * // Use with chat input
 * ChatInput.builder()
 *     .prompt(Component.text("Enter name (type 'cancel' to cancel):"))
 *     .cancellation(chatCancel)
 *     .onComplete(result -> {
 *         if (result.isCancelled()) {
 *             // Handle cancellation
 *         }
 *     })
 *     .open(player);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InputTimeout
 * @see ChatInput
 * @see AnvilInput
 */
public final class InputCancellation {

    /**
     * Default cancellation with "cancel" keyword.
     */
    private static final InputCancellation DEFAULTS = builder().build();

    /**
     * Cancellation that doesn't allow cancelling.
     */
    private static final InputCancellation NONE = builder()
            .allowEscape(false)
            .cancelKeyword(null)
            .build();

    private final boolean allowEscape;
    private final boolean allowClose;
    private final String cancelKeyword;
    private final Component cancelMessage;
    private final boolean returnToPreviousGui;
    private final Runnable onCancel;

    /**
     * Private constructor for InputCancellation.
     *
     * @param allowEscape         whether escape key cancels the input
     * @param allowClose          whether closing cancels the input
     * @param cancelKeyword       the keyword to cancel in chat inputs
     * @param cancelMessage       the message to show on cancel
     * @param returnToPreviousGui whether to return to previous GUI
     * @param onCancel            callback when cancelled
     */
    private InputCancellation(
            boolean allowEscape,
            boolean allowClose,
            @Nullable String cancelKeyword,
            @Nullable Component cancelMessage,
            boolean returnToPreviousGui,
            @Nullable Runnable onCancel
    ) {
        this.allowEscape = allowEscape;
        this.allowClose = allowClose;
        this.cancelKeyword = cancelKeyword;
        this.cancelMessage = cancelMessage;
        this.returnToPreviousGui = returnToPreviousGui;
        this.onCancel = onCancel;
    }

    /**
     * Returns the default cancellation configuration.
     *
     * <p>Default settings:
     * <ul>
     *   <li>Allow escape: true</li>
     *   <li>Allow close: true</li>
     *   <li>Cancel keyword: "cancel"</li>
     *   <li>Return to previous GUI: true</li>
     * </ul>
     *
     * @return the default cancellation configuration
     * @since 1.0.0
     */
    @NotNull
    public static InputCancellation defaults() {
        return DEFAULTS;
    }

    /**
     * Returns a cancellation configuration that doesn't allow cancelling.
     *
     * <p>The player must complete the input to proceed.
     *
     * @return a non-cancellable configuration
     * @since 1.0.0
     */
    @NotNull
    public static InputCancellation none() {
        return NONE;
    }

    /**
     * Creates a new builder for InputCancellation.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the escape key cancels the input.
     *
     * @return true if escape cancels
     * @since 1.0.0
     */
    public boolean allowsEscape() {
        return allowEscape;
    }

    /**
     * Checks if closing the inventory cancels the input.
     *
     * @return true if closing cancels
     * @since 1.0.0
     */
    public boolean allowsClose() {
        return allowClose;
    }

    /**
     * Returns the cancel keyword for chat inputs.
     *
     * @return the cancel keyword, or empty if none
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getCancelKeyword() {
        return Optional.ofNullable(cancelKeyword);
    }

    /**
     * Checks if the given message matches the cancel keyword.
     *
     * @param message the message to check
     * @return true if the message is a cancel keyword
     * @since 1.0.0
     */
    public boolean isCancelKeyword(@NotNull String message) {
        return cancelKeyword != null && cancelKeyword.equalsIgnoreCase(message.trim());
    }

    /**
     * Returns the message to show when input is cancelled.
     *
     * @return the cancel message, or empty if none
     * @since 1.0.0
     */
    @NotNull
    public Optional<Component> getCancelMessage() {
        return Optional.ofNullable(cancelMessage);
    }

    /**
     * Checks if the player should return to a previous GUI after cancellation.
     *
     * @return true if returning to previous GUI
     * @since 1.0.0
     */
    public boolean shouldReturnToPreviousGui() {
        return returnToPreviousGui;
    }

    /**
     * Returns the callback to execute on cancellation.
     *
     * @return the cancel callback, or empty if none
     * @since 1.0.0
     */
    @NotNull
    public Optional<Runnable> getOnCancel() {
        return Optional.ofNullable(onCancel);
    }

    /**
     * Executes the cancellation callback if present.
     *
     * @since 1.0.0
     */
    public void executeOnCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    /**
     * Checks if any form of cancellation is allowed.
     *
     * @return true if cancellation is possible
     * @since 1.0.0
     */
    public boolean isAllowed() {
        return allowEscape || allowClose || cancelKeyword != null;
    }

    /**
     * Creates a new builder from this configuration.
     *
     * @return a new builder with current settings
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .allowEscape(allowEscape)
                .allowClose(allowClose)
                .cancelKeyword(cancelKeyword)
                .cancelMessage(cancelMessage)
                .returnToPreviousGui(returnToPreviousGui)
                .onCancel(onCancel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputCancellation that = (InputCancellation) o;
        return allowEscape == that.allowEscape &&
                allowClose == that.allowClose &&
                returnToPreviousGui == that.returnToPreviousGui &&
                Objects.equals(cancelKeyword, that.cancelKeyword) &&
                Objects.equals(cancelMessage, that.cancelMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowEscape, allowClose, cancelKeyword, cancelMessage, returnToPreviousGui);
    }

    @Override
    public String toString() {
        return "InputCancellation{" +
                "allowEscape=" + allowEscape +
                ", allowClose=" + allowClose +
                ", cancelKeyword='" + cancelKeyword + '\'' +
                ", returnToPreviousGui=" + returnToPreviousGui +
                '}';
    }

    /**
     * Builder for creating {@link InputCancellation} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private boolean allowEscape = true;
        private boolean allowClose = true;
        private String cancelKeyword = "cancel";
        private Component cancelMessage = null;
        private boolean returnToPreviousGui = true;
        private Runnable onCancel = null;

        /**
         * Creates a new Builder with default settings.
         */
        private Builder() {
        }

        /**
         * Sets whether the escape key cancels the input.
         *
         * @param allowEscape true to allow escape
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder allowEscape(boolean allowEscape) {
            this.allowEscape = allowEscape;
            return this;
        }

        /**
         * Sets whether closing the inventory cancels the input.
         *
         * @param allowClose true to allow close
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder allowClose(boolean allowClose) {
            this.allowClose = allowClose;
            return this;
        }

        /**
         * Sets the cancel keyword for chat inputs.
         *
         * @param cancelKeyword the keyword, or null to disable
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder cancelKeyword(@Nullable String cancelKeyword) {
            this.cancelKeyword = cancelKeyword;
            return this;
        }

        /**
         * Sets the message to show when input is cancelled.
         *
         * @param cancelMessage the message, or null for none
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder cancelMessage(@Nullable Component cancelMessage) {
            this.cancelMessage = cancelMessage;
            return this;
        }

        /**
         * Sets the message to show when input is cancelled.
         *
         * @param cancelMessage the message string
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder cancelMessage(@NotNull String cancelMessage) {
            this.cancelMessage = Component.text(cancelMessage);
            return this;
        }

        /**
         * Sets whether to return to a previous GUI after cancellation.
         *
         * @param returnToPreviousGui true to return to previous GUI
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder returnToPreviousGui(boolean returnToPreviousGui) {
            this.returnToPreviousGui = returnToPreviousGui;
            return this;
        }

        /**
         * Sets a callback to execute when input is cancelled.
         *
         * @param onCancel the callback, or null for none
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder onCancel(@Nullable Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /**
         * Builds the InputCancellation.
         *
         * @return a new InputCancellation
         * @since 1.0.0
         */
        @NotNull
        public InputCancellation build() {
            return new InputCancellation(
                    allowEscape,
                    allowClose,
                    cancelKeyword,
                    cancelMessage,
                    returnToPreviousGui,
                    onCancel
            );
        }
    }
}
