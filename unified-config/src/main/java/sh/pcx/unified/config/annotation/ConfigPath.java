/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a custom path for a configuration field.
 *
 * <p>By default, fields are serialized using their Java field name. This
 * annotation allows specifying a different name or path in the configuration
 * file, which is useful for:</p>
 * <ul>
 *   <li>Using different naming conventions (e.g., snake_case in config)</li>
 *   <li>Renaming fields without breaking existing configs</li>
 *   <li>Mapping to nested paths</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class ServerConfig {
 *
 *     // Maps to "server-name" instead of "serverName"
 *     @ConfigPath("server-name")
 *     private String serverName = "My Server";
 *
 *     // Maps to "max_players" instead of "maxPlayers"
 *     @ConfigPath("max_players")
 *     private int maxPlayers = 100;
 *
 *     // Maps to nested path "connection.timeout"
 *     @ConfigPath("connection.timeout")
 *     private int connectionTimeout = 30;
 * }
 * }</pre>
 *
 * <h2>Generated YAML</h2>
 * <pre>{@code
 * server-name: "My Server"
 * max_players: 100
 * connection:
 *   timeout: 30
 * }</pre>
 *
 * <h2>Aliasing (Legacy Support)</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class MigrationConfig {
 *
 *     // Reads from "old-name" but writes to "newName"
 *     @ConfigPath(value = "newName", aliases = {"old-name", "legacy-name"})
 *     private String value = "default";
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigSerializable
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigPath {

    /**
     * The path to use in the configuration file.
     *
     * <p>Supports dot-notation for nested paths (e.g., "database.connection.host").</p>
     *
     * @return the configuration path
     * @since 1.0.0
     */
    String value();

    /**
     * Alternative paths to read from (for migration/legacy support).
     *
     * <p>When loading, if the primary path is not found, these aliases
     * are checked in order. When saving, only the primary path is used.</p>
     *
     * @return array of alias paths
     * @since 1.0.0
     */
    String[] aliases() default {};
}
