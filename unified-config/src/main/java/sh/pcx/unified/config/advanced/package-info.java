/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Advanced configuration features including profiles, merging, and environment overrides.
 *
 * <p>This package provides advanced functionality for complex configuration
 * scenarios including environment-specific profiles, configuration merging,
 * and environment variable overrides.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.advanced.EnvironmentOverrides} -
 *       Override config values with environment variables</li>
 *   <li>{@link sh.pcx.unified.config.advanced.ConfigProfile} -
 *       Environment-specific configuration profiles</li>
 *   <li>{@link sh.pcx.unified.config.advanced.ConfigMerger} -
 *       Merge multiple configuration sources</li>
 * </ul>
 *
 * <h2>Environment Overrides Example</h2>
 * <pre>{@code
 * // In config: host: "${DATABASE_HOST:localhost}"
 * EnvironmentOverrides overrides = new EnvironmentOverrides("MYAPP");
 * overrides.apply(config);
 * }</pre>
 *
 * <h2>Profiles Example</h2>
 * <pre>{@code
 * ConfigProfile profile = ConfigProfile.fromEnvironment();
 * PluginConfig config = profile.load(
 *     PluginConfig.class, configDir, "config", configLoader
 * );
 * // Loads: config.yml + config-{profile}.yml
 * }</pre>
 *
 * <h2>Merging Example</h2>
 * <pre>{@code
 * ConfigMerger merger = new ConfigMerger(configLoader);
 * PluginConfig config = merger.merge(PluginConfig.class,
 *     defaultsPath,
 *     configPath,
 *     overridesPath
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config.advanced;
