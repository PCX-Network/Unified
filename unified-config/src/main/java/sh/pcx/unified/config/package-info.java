/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core configuration system using Sponge Configurate.
 *
 * <p>This package provides a unified configuration API for Minecraft plugins
 * supporting multiple formats (YAML, HOCON, JSON, TOML), type-safe mapping,
 * validation, hot-reload, and environment variable overrides.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.ConfigService} - Main configuration service interface</li>
 *   <li>{@link sh.pcx.unified.config.ConfigNode} - Type-safe wrapper around Configurate nodes</li>
 *   <li>{@link sh.pcx.unified.config.ConfigRoot} - Root configuration holder with save/reload</li>
 *   <li>{@link sh.pcx.unified.config.ConfigException} - Configuration error handling</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Define a configuration class
 * @ConfigSerializable
 * public class PluginConfig {
 *     @ConfigComment("Enable debug mode")
 *     private boolean debug = false;
 *
 *     @ConfigComment("Maximum players")
 *     @Range(min = 1, max = 100)
 *     private int maxPlayers = 16;
 * }
 *
 * // Load configuration
 * ConfigService configService = ...;
 * PluginConfig config = configService.load(PluginConfig.class, configPath);
 *
 * // Enable hot reload
 * configService.watch(PluginConfig.class, configPath, result -> {
 *     if (result.isSuccess()) {
 *         this.config = result.get();
 *     }
 * });
 * }</pre>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.format} - Format loaders and savers</li>
 *   <li>{@link sh.pcx.unified.config.annotation} - Configuration annotations</li>
 *   <li>{@link sh.pcx.unified.config.validation} - Validation framework</li>
 *   <li>{@link sh.pcx.unified.config.reload} - Hot reload functionality</li>
 *   <li>{@link sh.pcx.unified.config.advanced} - Advanced features</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config;
