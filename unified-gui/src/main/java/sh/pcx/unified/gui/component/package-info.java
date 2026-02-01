/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Reusable GUI components for building interactive inventory interfaces.
 *
 * <p>This package provides a comprehensive set of pre-built components for
 * creating user-friendly inventory GUIs. Components are designed to be
 * composable, reusable, and easily customizable.
 *
 * <h2>Component Categories</h2>
 * <ul>
 *   <li><b>Buttons</b>: Interactive clickable elements
 *     <ul>
 *       <li>{@link sh.pcx.unified.gui.component.Button} - Base button with click handling</li>
 *       <li>{@link sh.pcx.unified.gui.component.ToggleButton} - On/off toggle functionality</li>
 *       <li>{@link sh.pcx.unified.gui.component.CycleButton} - Cycles through options</li>
 *     </ul>
 *   </li>
 *   <li><b>Navigation</b>: GUI navigation controls
 *     <ul>
 *       <li>{@link sh.pcx.unified.gui.component.BackButton} - Returns to previous GUI</li>
 *       <li>{@link sh.pcx.unified.gui.component.CloseButton} - Closes the GUI</li>
 *       <li>{@link sh.pcx.unified.gui.component.PageButton} - Page navigation</li>
 *       <li>{@link sh.pcx.unified.gui.component.RefreshButton} - Refreshes content</li>
 *     </ul>
 *   </li>
 *   <li><b>Decorative</b>: Visual elements
 *     <ul>
 *       <li>{@link sh.pcx.unified.gui.component.Border} - Border patterns</li>
 *       <li>{@link sh.pcx.unified.gui.component.Filler} - Empty slot filler</li>
 *       <li>{@link sh.pcx.unified.gui.component.Divider} - Section dividers</li>
 *     </ul>
 *   </li>
 *   <li><b>Interactive</b>: Value input/display components
 *     <ul>
 *       <li>{@link sh.pcx.unified.gui.component.ProgressBar} - Visual progress indicator</li>
 *       <li>{@link sh.pcx.unified.gui.component.Slider} - Numeric value slider</li>
 *       <li>{@link sh.pcx.unified.gui.component.NumberInput} - Increment/decrement input</li>
 *     </ul>
 *   </li>
 *   <li><b>Dialogs</b>: Modal dialog components
 *     <ul>
 *       <li>{@link sh.pcx.unified.gui.component.ConfirmationDialog} - Yes/No confirmation</li>
 *       <li>{@link sh.pcx.unified.gui.component.InfoDialog} - Information display</li>
 *       <li>{@link sh.pcx.unified.gui.component.SelectionDialog} - Option selection</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Using the factory for common components
 * Button closeBtn = GUIComponents.closeButton();
 * Button backBtn = GUIComponents.backButton();
 *
 * // Creating custom buttons with builders
 * Button customBtn = Button.builder()
 *     .item(ItemBuilder.of("minecraft:diamond").name("Click Me!").build())
 *     .onClick(ctx -> ctx.player().sendMessage("Clicked!"))
 *     .build();
 *
 * // Toggle button
 * ToggleButton toggle = ToggleButton.builder()
 *     .enabledItem(greenWool)
 *     .disabledItem(redWool)
 *     .initialState(false)
 *     .onToggle((ctx, enabled) -> setFeature(enabled))
 *     .build();
 *
 * // Decorative borders
 * Border.full(glassPane).applyTo(gui);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.gui.component;
