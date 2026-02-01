/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.input;

import org.jetbrains.annotations.NotNull;

/**
 * Callback interface for handling input completion events.
 *
 * <p>This functional interface is used to handle the result of input dialogs
 * such as anvil inputs, sign inputs, and chat inputs. The callback is invoked
 * when the player completes, cancels, or times out of the input.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple callback
 * InputCallback<AnvilInputResult> callback = result -> {
 *     if (result.isConfirmed()) {
 *         processInput(result.getText());
 *     } else if (result.isCancelled()) {
 *         player.sendMessage("Input cancelled");
 *     }
 * };
 *
 * // With timeout handling
 * InputCallback<ChatInputResult> chatCallback = result -> {
 *     if (result.isTimedOut()) {
 *         player.sendMessage("Input timed out");
 *         return;
 *     }
 *     handleMessage(result.getMessage());
 * };
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Callbacks are typically invoked on the main server thread. If you need
 * to perform async operations, use the scheduler service to dispatch work.
 *
 * @param <R> the result type (e.g., {@link AnvilInputResult}, {@link ChatInputResult})
 * @since 1.0.0
 * @author Supatuck
 * @see AnvilInputResult
 * @see SignInputResult
 * @see ChatInputResult
 */
@FunctionalInterface
public interface InputCallback<R> {

    /**
     * Called when input is completed.
     *
     * <p>This method is invoked regardless of how the input ended:
     * <ul>
     *   <li>Player confirmed the input</li>
     *   <li>Player cancelled the input</li>
     *   <li>Input timed out</li>
     * </ul>
     *
     * <p>Check the result object's state methods to determine how the input ended.
     *
     * @param result the input result containing the entered value and state
     * @since 1.0.0
     */
    void onComplete(@NotNull R result);

    /**
     * Creates a callback that does nothing.
     *
     * <p>Useful as a default or placeholder callback.
     *
     * @param <R> the result type
     * @return a no-op callback
     * @since 1.0.0
     */
    @NotNull
    static <R> InputCallback<R> empty() {
        return result -> {};
    }

    /**
     * Chains this callback with another callback.
     *
     * <p>Both callbacks will be invoked with the same result, in order.
     *
     * @param other the callback to chain after this one
     * @return a new callback that invokes both callbacks
     * @since 1.0.0
     */
    @NotNull
    default InputCallback<R> andThen(@NotNull InputCallback<R> other) {
        return result -> {
            this.onComplete(result);
            other.onComplete(result);
        };
    }
}
