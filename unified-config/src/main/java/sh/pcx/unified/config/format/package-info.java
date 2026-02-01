/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Configuration format support for YAML, HOCON, JSON, and TOML.
 *
 * <p>This package provides format-agnostic loading and saving of
 * configuration files using Sponge Configurate loaders.</p>
 *
 * <h2>Supported Formats</h2>
 * <table>
 *   <tr><th>Format</th><th>Extensions</th><th>Features</th></tr>
 *   <tr><td>YAML</td><td>.yml, .yaml</td><td>Comments, human-readable</td></tr>
 *   <tr><td>HOCON</td><td>.conf, .hocon</td><td>Comments, includes, substitutions</td></tr>
 *   <tr><td>JSON</td><td>.json</td><td>API compatible, strict syntax</td></tr>
 *   <tr><td>TOML</td><td>.toml</td><td>Comments, clean syntax</td></tr>
 * </table>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.format.ConfigFormat} - Format enumeration</li>
 *   <li>{@link sh.pcx.unified.config.format.ConfigLoader} - Loads configuration files</li>
 *   <li>{@link sh.pcx.unified.config.format.ConfigSaver} - Saves configuration files</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config.format;
