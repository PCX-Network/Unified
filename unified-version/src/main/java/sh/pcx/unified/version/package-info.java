/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Version compatibility layer for NMS abstraction (Minecraft 1.20.5 - 1.21.11+).
 *
 * <p>This module provides version-safe access to Minecraft internals, abstracting away
 * the differences between server versions behind stable interfaces.
 *
 * <h2>Module Structure</h2>
 * <pre>
 * unified-version/
 * ├── api/           - Public interfaces (VersionProvider, NMSBridge, NBTService, etc.)
 * ├── detection/     - Version detection and registry
 * ├── v1_20_R4/      - 1.20.5-1.20.6 implementation
 * ├── v1_21_R1/      - 1.21-1.21.1 implementation
 * ├── v1_21_R2/      - 1.21.2-1.21.3 implementation
 * ├── v1_21_R3/      - 1.21.4-1.21.10 implementation
 * └── v1_21_R4/      - 1.21.11+ implementation (Mojang mappings)
 * </pre>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Inject services
 * @Inject private VersionProvider version;
 * @Inject private NMSBridge nms;
 * @Inject private NBTService nbt;
 * @Inject private ComponentBridge components;
 * @Inject private GameRuleService gameRules;
 *
 * // Check version
 * if (version.isAtLeast(MinecraftVersion.V1_21)) {
 *     // Use 1.21+ features
 * }
 *
 * // Check features
 * if (version.supports(Feature.COMPONENT_ITEMS)) {
 *     // Use component-based items
 * }
 *
 * // Version-safe gamerule access
 * gameRules.set(world, GameRules.KEEP_INVENTORY, true);
 * }</pre>
 *
 * <h2>Version Support Matrix</h2>
 * <table>
 *   <caption>Supported Minecraft versions and NMS packages</caption>
 *   <tr><th>Minecraft</th><th>NMS Version</th><th>Key Features</th></tr>
 *   <tr><td>1.20.5-1.20.6</td><td>v1_20_R4</td><td>Data Components</td></tr>
 *   <tr><td>1.21-1.21.1</td><td>v1_21_R1</td><td>Tricky Trials, Breeze, Mace</td></tr>
 *   <tr><td>1.21.2-1.21.3</td><td>v1_21_R2</td><td>Bundles</td></tr>
 *   <tr><td>1.21.4-1.21.10</td><td>v1_21_R3</td><td>Pale Garden, Creaking</td></tr>
 *   <tr><td>1.21.11+</td><td>v1_21_R4</td><td>Mojang mappings, Registry gamerules</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.version.api.VersionProvider
 * @see sh.pcx.unified.version.api.NMSBridge
 * @see sh.pcx.unified.version.api.NBTService
 * @see sh.pcx.unified.version.api.ComponentBridge
 * @see sh.pcx.unified.version.api.GameRuleService
 */
@NullMarked
package sh.pcx.unified.version;

import org.jspecify.annotations.NullMarked;
