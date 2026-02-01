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
 * Specifies a default value for a configuration field.
 *
 * <p>This annotation provides an explicit default value as a string that
 * is parsed and converted to the field type. It is used when:</p>
 * <ul>
 *   <li>The field value in code is null</li>
 *   <li>The configuration file is missing the value</li>
 *   <li>Generating default configuration files</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class ServerConfig {
 *
 *     @ConfigDefault("localhost")
 *     private String host;
 *
 *     @ConfigDefault("25565")
 *     private int port;
 *
 *     @ConfigDefault("true")
 *     private boolean enabled;
 *
 *     @ConfigDefault("3.14")
 *     private double ratio;
 * }
 * }</pre>
 *
 * <h2>Complex Defaults</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class FeatureConfig {
 *
 *     // List default (JSON array format)
 *     @ConfigDefault("[\"world\", \"world_nether\", \"world_the_end\"]")
 *     private List<String> enabledWorlds;
 *
 *     // Map default (JSON object format)
 *     @ConfigDefault("{\"admin\": 100, \"mod\": 50, \"user\": 10}")
 *     private Map<String, Integer> rolePriorities;
 *
 *     // Enum default
 *     @ConfigDefault("SURVIVAL")
 *     private GameMode defaultGameMode;
 * }
 * }</pre>
 *
 * <h2>Environment Variable Override</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class DatabaseConfig {
 *
 *     // Can be overridden by DATABASE_HOST env var
 *     @ConfigDefault("${DATABASE_HOST:localhost}")
 *     private String host;
 *
 *     // With explicit default after colon
 *     @ConfigDefault("${DATABASE_PORT:3306}")
 *     private int port;
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigSerializable
 * @see ConfigPath
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigDefault {

    /**
     * The default value as a string.
     *
     * <p>The string is parsed and converted to the field type.
     * Supports environment variable substitution with the syntax
     * {@code ${ENV_VAR}} or {@code ${ENV_VAR:default}}.</p>
     *
     * @return the default value string
     * @since 1.0.0
     */
    String value();

    /**
     * Whether the default should be written to the config file if missing.
     *
     * <p>When {@code true} (default), missing values are populated with
     * the default and saved. When {@code false}, the default is only
     * used in memory but not written to the file.</p>
     *
     * @return true to persist the default value
     * @since 1.0.0
     */
    boolean persist() default true;
}
