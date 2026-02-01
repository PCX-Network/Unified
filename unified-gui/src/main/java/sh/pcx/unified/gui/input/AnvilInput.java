/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.input;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Anvil-based text input dialog.
 *
 * <p>AnvilInput uses the Minecraft anvil GUI to capture text input from players.
 * The player types in the anvil's rename field and confirms by clicking the
 * output slot. This provides a clean, familiar interface for text input.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple text input
 * AnvilInput.builder()
 *     .title(Component.text("Enter Item Name"))
 *     .defaultText("My Item")
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             String name = result.getText().orElse("");
 *             player.sendMessage(Component.text("Named: " + name));
 *         }
 *     })
 *     .open(player);
 *
 * // With validation
 * AnvilInput.builder()
 *     .title(Component.text("Enter Amount"))
 *     .defaultText("1")
 *     .validator(text -> {
 *         try {
 *             int amount = Integer.parseInt(text);
 *             return amount > 0 && amount <= 64;
 *         } catch (NumberFormatException e) {
 *             return false;
 *         }
 *     })
 *     .validationFailedMessage(Component.text("Enter 1-64"))
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             int amount = Integer.parseInt(result.getTextOrDefault("1"));
 *             giveItems(player, amount);
 *         }
 *     })
 *     .open(player);
 *
 * // Return to previous GUI
 * AnvilInput.builder()
 *     .title(Component.text("Search"))
 *     .previousGui(() -> createSearchResultsGui(player))
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             performSearch(result.getText().orElse(""));
 *         }
 *         // Will return to previous GUI automatically
 *     })
 *     .open(player);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent builder API via {@link #builder()}</li>
 *   <li>Input validation with custom validators</li>
 *   <li>Customizable left and output items</li>
 *   <li>Dynamic output item based on input text</li>
 *   <li>Cancel handling with optional return to previous GUI</li>
 *   <li>Text change callbacks for real-time updates</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>AnvilInput instances are immutable after creation. The open() method
 * and callbacks execute on the main server thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AnvilInputBuilder
 * @see AnvilInputResult
 * @see SignInput
 * @see ChatInput
 */
public final class AnvilInput {

    private final Component title;
    private final String defaultText;
    private final UnifiedItemStack leftItem;
    private final Function<String, UnifiedItemStack> outputItemGenerator;
    private final InputValidator<String> validator;
    private final Component validationFailedMessage;
    private final InputCancellation cancellation;
    private final InputCallback<AnvilInputResult> onComplete;
    private final Consumer<String> onTextChange;
    private final Supplier<Object> previousGui;
    private final boolean closeOnComplete;
    private final boolean preventClose;

    /**
     * Package-private constructor.
     * Use {@link #builder()} to create instances.
     *
     * @param title                   the anvil title
     * @param defaultText             the default text
     * @param leftItem                the left slot item
     * @param outputItemGenerator     the output item generator
     * @param validator               the input validator
     * @param validationFailedMessage the validation failed message
     * @param cancellation            the cancellation config
     * @param onComplete              the completion callback
     * @param onTextChange            the text change callback
     * @param previousGui             the previous GUI supplier
     * @param closeOnComplete         whether to close on complete
     * @param preventClose            whether to prevent closing
     */
    AnvilInput(
            @NotNull Component title,
            @NotNull String defaultText,
            @Nullable UnifiedItemStack leftItem,
            @Nullable Function<String, UnifiedItemStack> outputItemGenerator,
            @NotNull InputValidator<String> validator,
            @Nullable Component validationFailedMessage,
            @NotNull InputCancellation cancellation,
            @NotNull InputCallback<AnvilInputResult> onComplete,
            @Nullable Consumer<String> onTextChange,
            @Nullable Supplier<Object> previousGui,
            boolean closeOnComplete,
            boolean preventClose
    ) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.defaultText = Objects.requireNonNull(defaultText, "defaultText cannot be null");
        this.leftItem = leftItem;
        this.outputItemGenerator = outputItemGenerator;
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
        this.validationFailedMessage = validationFailedMessage;
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation cannot be null");
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete cannot be null");
        this.onTextChange = onTextChange;
        this.previousGui = previousGui;
        this.closeOnComplete = closeOnComplete;
        this.preventClose = preventClose;
    }

    /**
     * Creates a new builder for AnvilInput.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static AnvilInputBuilder builder() {
        return new AnvilInputBuilder();
    }

    /**
     * Opens this anvil input for a player.
     *
     * <p>This method is delegated to the platform-specific implementation
     * which handles the actual GUI creation and event handling.
     *
     * @param player the player to open for
     * @since 1.0.0
     */
    public void open(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        AnvilInputProvider.INSTANCE.open(this, player);
    }

    /**
     * Returns the anvil title.
     *
     * @return the title
     * @since 1.0.0
     */
    @NotNull
    public Component getTitle() {
        return title;
    }

    /**
     * Returns the default text.
     *
     * @return the default text
     * @since 1.0.0
     */
    @NotNull
    public String getDefaultText() {
        return defaultText;
    }

    /**
     * Returns the left slot item.
     *
     * @return the left item, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedItemStack> getLeftItem() {
        return Optional.ofNullable(leftItem);
    }

    /**
     * Returns the output item generator.
     *
     * @return the generator, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Function<String, UnifiedItemStack>> getOutputItemGenerator() {
        return Optional.ofNullable(outputItemGenerator);
    }

    /**
     * Generates an output item for the given text.
     *
     * @param text the current text
     * @return the output item, or empty if no generator
     * @since 1.0.0
     */
    @NotNull
    public Optional<UnifiedItemStack> generateOutputItem(@NotNull String text) {
        return outputItemGenerator != null
                ? Optional.ofNullable(outputItemGenerator.apply(text))
                : Optional.empty();
    }

    /**
     * Returns the input validator.
     *
     * @return the validator
     * @since 1.0.0
     */
    @NotNull
    public InputValidator<String> getValidator() {
        return validator;
    }

    /**
     * Validates the given text.
     *
     * @param text the text to validate
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    public InputValidator.ValidationResult validate(@NotNull String text) {
        return validator.validate(text);
    }

    /**
     * Returns the validation failed message.
     *
     * @return the message, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Component> getValidationFailedMessage() {
        return Optional.ofNullable(validationFailedMessage);
    }

    /**
     * Returns the cancellation configuration.
     *
     * @return the cancellation config
     * @since 1.0.0
     */
    @NotNull
    public InputCancellation getCancellation() {
        return cancellation;
    }

    /**
     * Returns the completion callback.
     *
     * @return the callback
     * @since 1.0.0
     */
    @NotNull
    public InputCallback<AnvilInputResult> getOnComplete() {
        return onComplete;
    }

    /**
     * Returns the text change callback.
     *
     * @return the callback, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Consumer<String>> getOnTextChange() {
        return Optional.ofNullable(onTextChange);
    }

    /**
     * Notifies the text change callback if present.
     *
     * @param text the new text
     * @since 1.0.0
     */
    public void notifyTextChange(@NotNull String text) {
        if (onTextChange != null) {
            onTextChange.accept(text);
        }
    }

    /**
     * Returns the previous GUI supplier.
     *
     * @return the supplier, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Supplier<Object>> getPreviousGui() {
        return Optional.ofNullable(previousGui);
    }

    /**
     * Checks if the anvil should close when input is completed.
     *
     * @return true if closing on complete
     * @since 1.0.0
     */
    public boolean shouldCloseOnComplete() {
        return closeOnComplete;
    }

    /**
     * Checks if the anvil should prevent closing.
     *
     * @return true if preventing close
     * @since 1.0.0
     */
    public boolean shouldPreventClose() {
        return preventClose;
    }

    /**
     * Creates a new builder with this input's settings.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder toBuilder() {
        return new AnvilInputBuilder()
                .title(title)
                .defaultText(defaultText)
                .leftItem(leftItem)
                .outputItem(outputItemGenerator)
                .validator(validator)
                .validationFailedMessage(validationFailedMessage)
                .cancellation(cancellation)
                .onComplete(onComplete)
                .onTextChange(onTextChange)
                .previousGui(previousGui)
                .closeOnComplete(closeOnComplete)
                .preventClose(preventClose);
    }

    @Override
    public String toString() {
        return "AnvilInput{" +
                "title=" + title +
                ", defaultText='" + defaultText + '\'' +
                ", closeOnComplete=" + closeOnComplete +
                ", preventClose=" + preventClose +
                '}';
    }
}

/**
 * Internal provider for creating anvil input dialogs.
 *
 * <p>This is used internally by the API to delegate to platform-specific
 * implementations. Plugin developers should use {@link AnvilInput#builder()}.
 *
 * @since 1.0.0
 */
interface AnvilInputProvider {

    /**
     * The registered provider instance.
     */
    AnvilInputProvider INSTANCE = new AnvilInputProvider() {
        @Override
        public void open(@NotNull AnvilInput input, @NotNull UnifiedPlayer player) {
            throw new IllegalStateException(
                    "AnvilInputProvider has not been initialized. " +
                    "Ensure the GUI module is loaded before using AnvilInput."
            );
        }
    };

    /**
     * Opens an anvil input for a player.
     *
     * @param input  the anvil input configuration
     * @param player the player to open for
     */
    void open(@NotNull AnvilInput input, @NotNull UnifiedPlayer player);
}
