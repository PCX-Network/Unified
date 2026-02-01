/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core GUI framework for creating inventory-based user interfaces.
 *
 * <p>This package provides an enterprise-grade GUI framework with support for:
 * <ul>
 *     <li>Abstract base classes for building custom GUIs</li>
 *     <li>Centralized GUI lifecycle management</li>
 *     <li>Type-safe click handling</li>
 *     <li>State management for dynamic GUIs</li>
 *     <li>Navigation stack with back button support</li>
 *     <li>Slot patterns for declarative layouts</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create a simple GUI
 * public class MainMenuGUI extends AbstractGUI {
 *
 *     public MainMenuGUI(GUIContext context) {
 *         super(context, Layout.CHEST_27);
 *         setTitle(Component.text("Main Menu"));
 *     }
 *
 *     @Override
 *     protected void setup() {
 *         // Set up border
 *         fillBorder(borderItem());
 *
 *         // Add a button
 *         setSlot(13, Slot.of(settingsItem())
 *             .onClick(click -> {
 *                 navigateTo(new SettingsGUI(click.context()));
 *                 return ClickResult.DENY;
 *             }));
 *     }
 * }
 *
 * // Open the GUI
 * guiManager.open(player, new MainMenuGUI(GUIContext.of(player)));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.gui.AbstractGUI
 * @see sh.pcx.unified.gui.GUIManager
 */
package sh.pcx.unified.gui;
