/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Input dialog framework for capturing user input through various methods.
 *
 * <p>This package provides several input mechanisms:
 * <ul>
 *   <li><strong>Anvil Input</strong> - Text input via anvil rename interface</li>
 *   <li><strong>Sign Input</strong> - Text input via sign editing (4 lines)</li>
 *   <li><strong>Chat Input</strong> - Text input via chat messages</li>
 *   <li><strong>Number Input</strong> - Numeric input with validation</li>
 *   <li><strong>Selection Input</strong> - Select from predefined options</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Anvil input for naming an item
 * AnvilInput.builder()
 *     .title(Component.text("Enter Item Name"))
 *     .defaultText("My Item")
 *     .validator(text -> text.length() >= 3)
 *     .onComplete(result -> {
 *         if (result.isConfirmed()) {
 *             player.sendMessage("You named it: " + result.getText());
 *         }
 *     })
 *     .open(player);
 *
 * // Sign input for multi-line text
 * SignInput.builder()
 *     .lines("Line 1", "", "", "")
 *     .onComplete(result -> {
 *         for (String line : result.getLines()) {
 *             player.sendMessage(Component.text("Line: " + line));
 *         }
 *     })
 *     .open(player);
 *
 * // Chat input with timeout
 * ChatInput.builder()
 *     .prompt(Component.text("Enter a message:"))
 *     .timeout(Duration.ofSeconds(30))
 *     .onComplete(result -> {
 *         if (!result.isTimedOut()) {
 *             player.sendMessage("You said: " + result.getMessage());
 *         }
 *     })
 *     .open(player);
 *
 * // Number input with range validation
 * NumberInput.builder()
 *     .prompt(Component.text("Enter amount (1-64):"))
 *     .range(1, 64)
 *     .onComplete(result -> {
 *         player.sendMessage("Amount: " + result.getValue());
 *     })
 *     .open(player);
 *
 * // Selection from options
 * SelectionInput.<String>builder()
 *     .title(Component.text("Select a color"))
 *     .option("Red", "red")
 *     .option("Green", "green")
 *     .option("Blue", "blue")
 *     .onSelect(color -> player.sendMessage("Selected: " + color))
 *     .open(player);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent builder API for all input types</li>
 *   <li>Input validation before accepting</li>
 *   <li>Timeout support with configurable duration</li>
 *   <li>Cancel handling with optional return to previous GUI</li>
 *   <li>Callback-based result handling</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.gui.input;
