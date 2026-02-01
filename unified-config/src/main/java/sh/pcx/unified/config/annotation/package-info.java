/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Annotations for configuration class mapping and validation.
 *
 * <p>This package provides annotations for defining configuration
 * classes that can be automatically serialized to and from
 * configuration files.</p>
 *
 * <h2>Available Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.annotation.ConfigSerializable} - Marks a class as serializable</li>
 *   <li>{@link sh.pcx.unified.config.annotation.ConfigPath} - Custom path for a field</li>
 *   <li>{@link sh.pcx.unified.config.annotation.ConfigDefault} - Default value for a field</li>
 *   <li>{@link sh.pcx.unified.config.annotation.ConfigComment} - Comment in config file</li>
 *   <li>{@link sh.pcx.unified.config.annotation.ConfigValidate} - Apply validation constraint</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @ConfigSerializable
 * @ConfigComment("Database configuration settings")
 * public class DatabaseConfig {
 *
 *     @ConfigComment("Database host address")
 *     @ConfigDefault("localhost")
 *     private String host = "localhost";
 *
 *     @ConfigComment("Database port")
 *     @ConfigPath("connection.port")
 *     @Range(min = 1, max = 65535)
 *     private int port = 3306;
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config.annotation;
