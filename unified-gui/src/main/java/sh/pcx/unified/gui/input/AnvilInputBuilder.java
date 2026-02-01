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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fluent builder for creating {@link AnvilInput} instances.
 *
 * <p>This builder provides a clean, chainable API for configuring anvil-based
 * text input dialogs with validation, callbacks, and custom appearance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple anvil input
 * AnvilInput.builder()
 *     .title(Component.text("Enter Name"))
 *     .defaultText("Default Name")
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             handleInput(result.getText());
 *         }
 *     })
 *     .open(player);
 *
 * // With validation
 * AnvilInput.builder()
 *     .title(Component.text("Set Price"))
 *     .defaultText("100")
 *     .validator(InputValidator.numeric())
 *     .validationFailedMessage(Component.text("Please enter a number"))
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             int price = Integer.parseInt(result.getTextOrDefault("0"));
 *             setPrice(price);
 *         }
 *     })
 *     .open(player);
 *
 * // With custom items
 * AnvilInput.builder()
 *     .title(Component.text("Rename Item"))
 *     .leftItem(ItemBuilder.of("minecraft:name_tag").build())
 *     .outputItem(item -> ItemBuilder.of("minecraft:name_tag")
 *         .name(Component.text(item))
 *         .build())
 *     .defaultText(currentName)
 *     .onComplete(result -> renameItem(result))
 *     .open(player);
 * }</pre>
 *
 * <h2>Configuration Options</h2>
 * <ul>
 *   <li><strong>title</strong> - The title shown in the anvil GUI</li>
 *   <li><strong>defaultText</strong> - Initial text in the input field</li>
 *   <li><strong>leftItem</strong> - Item shown in the left slot</li>
 *   <li><strong>outputItem</strong> - Dynamic item generator for the output slot</li>
 *   <li><strong>validator</strong> - Validates input before accepting</li>
 *   <li><strong>cancellation</strong> - Configure cancel behavior</li>
 *   <li><strong>previousGui</strong> - GUI to return to on cancel/complete</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AnvilInput
 * @see AnvilInputResult
 */
public final class AnvilInputBuilder {

    private Component title = Component.text("Enter Text");
    private String defaultText = "";
    private UnifiedItemStack leftItem = null;
    private java.util.function.Function<String, UnifiedItemStack> outputItemGenerator = null;
    private InputValidator<String> validator = InputValidator.alwaysValid();
    private Component validationFailedMessage = null;
    private InputCancellation cancellation = InputCancellation.defaults();
    private InputCallback<AnvilInputResult> onComplete = InputCallback.empty();
    private Consumer<String> onTextChange = null;
    private Supplier<Object> previousGui = null;
    private boolean closeOnComplete = true;
    private boolean preventClose = false;

    /**
     * Package-private constructor.
     * Use {@link AnvilInput#builder()} to create instances.
     */
    AnvilInputBuilder() {
    }

    /**
     * Sets the title of the anvil GUI.
     *
     * @param title the title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    /**
     * Sets the title of the anvil GUI from a string.
     *
     * @param title the title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder title(@NotNull String title) {
        return title(Component.text(title));
    }

    /**
     * Sets the default text shown in the input field.
     *
     * @param defaultText the default text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder defaultText(@NotNull String defaultText) {
        this.defaultText = Objects.requireNonNull(defaultText, "defaultText cannot be null");
        return this;
    }

    /**
     * Sets the item shown in the left slot of the anvil.
     *
     * @param item the left slot item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder leftItem(@Nullable UnifiedItemStack item) {
        this.leftItem = item;
        return this;
    }

    /**
     * Sets a generator for the output item that updates as text changes.
     *
     * <p>The generator is called with the current text whenever it changes.
     *
     * @param generator the output item generator
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder outputItem(
            @NotNull java.util.function.Function<String, UnifiedItemStack> generator
    ) {
        this.outputItemGenerator = Objects.requireNonNull(generator, "generator cannot be null");
        return this;
    }

    /**
     * Sets a static output item.
     *
     * @param item the output item
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder outputItem(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        this.outputItemGenerator = text -> item;
        return this;
    }

    /**
     * Sets the validator for input text.
     *
     * @param validator the validator
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder validator(@NotNull InputValidator<String> validator) {
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
        return this;
    }

    /**
     * Sets the validator using a simple predicate.
     *
     * @param predicate the validation predicate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder validator(@NotNull java.util.function.Predicate<String> predicate) {
        return validator(InputValidator.of(predicate));
    }

    /**
     * Sets the message shown when validation fails.
     *
     * @param message the validation failed message
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder validationFailedMessage(@NotNull Component message) {
        this.validationFailedMessage = Objects.requireNonNull(message, "message cannot be null");
        return this;
    }

    /**
     * Sets the message shown when validation fails from a string.
     *
     * @param message the validation failed message
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder validationFailedMessage(@NotNull String message) {
        return validationFailedMessage(Component.text(message));
    }

    /**
     * Sets the cancellation configuration.
     *
     * @param cancellation the cancellation config
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder cancellation(@NotNull InputCancellation cancellation) {
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation cannot be null");
        return this;
    }

    /**
     * Sets whether the player can cancel the input.
     *
     * @param cancellable true to allow cancellation
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder cancellable(boolean cancellable) {
        this.cancellation = cancellable
                ? InputCancellation.defaults()
                : InputCancellation.none();
        return this;
    }

    /**
     * Sets the callback for when input is completed.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder onComplete(@NotNull InputCallback<AnvilInputResult> callback) {
        this.onComplete = Objects.requireNonNull(callback, "callback cannot be null");
        return this;
    }

    /**
     * Sets a callback that is invoked whenever the text changes.
     *
     * @param onTextChange the text change callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder onTextChange(@Nullable Consumer<String> onTextChange) {
        this.onTextChange = onTextChange;
        return this;
    }

    /**
     * Sets a supplier for the previous GUI to return to.
     *
     * <p>The supplier is called when returning to the previous GUI to get
     * a fresh instance. This is useful for GUIs that need updated state.
     *
     * @param previousGui the GUI supplier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder previousGui(@Nullable Supplier<Object> previousGui) {
        this.previousGui = previousGui;
        return this;
    }

    /**
     * Sets a static previous GUI to return to.
     *
     * @param previousGui the previous GUI
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder previousGui(@Nullable Object previousGui) {
        this.previousGui = previousGui != null ? () -> previousGui : null;
        return this;
    }

    /**
     * Sets whether the anvil should close when input is completed.
     *
     * @param closeOnComplete true to close on complete
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder closeOnComplete(boolean closeOnComplete) {
        this.closeOnComplete = closeOnComplete;
        return this;
    }

    /**
     * Sets whether to prevent the anvil from being closed.
     *
     * <p>If true, the player cannot close the anvil until they provide valid input.
     *
     * @param preventClose true to prevent closing
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder preventClose(boolean preventClose) {
        this.preventClose = preventClose;
        return this;
    }

    /**
     * Builds the AnvilInput.
     *
     * @return a new AnvilInput
     * @since 1.0.0
     */
    @NotNull
    public AnvilInput build() {
        return new AnvilInput(
                title,
                defaultText,
                leftItem,
                outputItemGenerator,
                validator,
                validationFailedMessage,
                cancellation,
                onComplete,
                onTextChange,
                previousGui,
                closeOnComplete,
                preventClose
        );
    }

    /**
     * Builds and immediately opens the anvil input for a player.
     *
     * @param player the player to open for
     * @since 1.0.0
     */
    public void open(@NotNull UnifiedPlayer player) {
        build().open(player);
    }

    /**
     * Conditionally applies a consumer to this builder.
     *
     * @param condition the condition
     * @param consumer  the consumer to apply if condition is true
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder applyIf(boolean condition, @NotNull Consumer<AnvilInputBuilder> consumer) {
        if (condition) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Applies a consumer to this builder.
     *
     * @param consumer the consumer to apply
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public AnvilInputBuilder apply(@NotNull Consumer<AnvilInputBuilder> consumer) {
        consumer.accept(this);
        return this;
    }
}
