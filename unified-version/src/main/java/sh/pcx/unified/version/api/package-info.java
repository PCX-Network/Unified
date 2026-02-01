/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Version compatibility API interfaces for NMS abstraction.
 *
 * <p>This package contains the public interfaces for version-safe access to
 * Minecraft internals. These interfaces hide version-specific implementation
 * details behind stable APIs.
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.version.api.VersionProvider} - Version detection and comparison</li>
 *   <li>{@link sh.pcx.unified.version.api.NMSBridge} - NMS operations (packets, entities, chunks)</li>
 *   <li>{@link sh.pcx.unified.version.api.NBTService} - NBT data manipulation</li>
 *   <li>{@link sh.pcx.unified.version.api.ComponentBridge} - Adventure/Vanilla component conversion</li>
 *   <li>{@link sh.pcx.unified.version.api.GameRuleService} - Version-safe gamerule access</li>
 *   <li>{@link sh.pcx.unified.version.api.Feature} - Version-gated feature detection</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyPlugin {
 *     @Inject private VersionProvider version;
 *     @Inject private NMSBridge nms;
 *     @Inject private NBTService nbt;
 *     @Inject private ComponentBridge components;
 *     @Inject private GameRuleService gameRules;
 *
 *     public void onEnable() {
 *         // Check version
 *         if (version.isAtLeast(MinecraftVersion.V1_21)) {
 *             // Use 1.21+ features
 *         }
 *
 *         // Check feature support
 *         if (version.supports(Feature.COMPONENT_ITEMS)) {
 *             // Use data components
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Supported Versions</h2>
 * <p>The version compatibility layer supports Minecraft 1.20.5 through 1.21.11+:
 * <ul>
 *   <li>1.20.5-1.20.6 (v1_20_R4)</li>
 *   <li>1.21-1.21.1 (v1_21_R1)</li>
 *   <li>1.21.2-1.21.3 (v1_21_R2)</li>
 *   <li>1.21.4-1.21.10 (v1_21_R3)</li>
 *   <li>1.21.11+ (v1_21_R4)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@NullMarked
package sh.pcx.unified.version.api;

import org.jspecify.annotations.NullMarked;
