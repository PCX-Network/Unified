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
 * Marks a class as serializable to/from configuration files.
 *
 * <p>Classes annotated with {@code @ConfigSerializable} can be automatically
 * mapped to configuration nodes using Sponge Configurate's object mapper.
 * The annotation works with the standard {@code @ConfigSerializable} annotation
 * from Configurate but provides additional features.</p>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>Class must have a no-arg constructor (can be private)</li>
 *   <li>Fields to serialize must be non-static and non-transient</li>
 *   <li>Field types must be serializable (primitives, strings, lists, maps, or other @ConfigSerializable types)</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class DatabaseConfig {
 *
 *     @ConfigComment("The database host address")
 *     private String host = "localhost";
 *
 *     @ConfigComment("The database port")
 *     @Range(min = 1, max = 65535)
 *     private int port = 3306;
 *
 *     @ConfigComment("Database credentials")
 *     private String username = "root";
 *
 *     @ConfigPath("password")
 *     @ConfigDefault("")
 *     private String databasePassword = "";
 *
 *     // Getters and setters...
 * }
 * }</pre>
 *
 * <h2>Nested Configuration</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class PluginConfig {
 *
 *     @ConfigComment("Database settings")
 *     private DatabaseConfig database = new DatabaseConfig();
 *
 *     @ConfigComment("Cache settings")
 *     private CacheConfig cache = new CacheConfig();
 *
 *     @ConfigComment("List of enabled features")
 *     private List<String> enabledFeatures = List.of("feature1", "feature2");
 * }
 * }</pre>
 *
 * <h2>Generated YAML</h2>
 * <pre>{@code
 * # Database settings
 * database:
 *   # The database host address
 *   host: "localhost"
 *   # The database port
 *   port: 3306
 *   # Database credentials
 *   username: "root"
 *   password: ""
 *
 * # Cache settings
 * cache:
 *   enabled: true
 *   size: 1000
 *
 * # List of enabled features
 * enabledFeatures:
 *   - "feature1"
 *   - "feature2"
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigPath
 * @see ConfigDefault
 * @see ConfigComment
 * @see ConfigValidate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@org.spongepowered.configurate.objectmapping.ConfigSerializable
public @interface ConfigSerializable {

    /**
     * Whether to include fields from superclasses.
     *
     * <p>When {@code true}, fields from parent classes are also serialized.
     * Parent classes do not need to be annotated with @ConfigSerializable.</p>
     *
     * @return true to include superclass fields
     * @since 1.0.0
     */
    boolean includeSuper() default true;

    /**
     * Whether to fail on unknown fields during deserialization.
     *
     * <p>When {@code true}, loading will fail if the configuration file
     * contains keys that don't map to any field. This is useful for
     * catching typos in configuration files.</p>
     *
     * @return true to fail on unknown fields
     * @since 1.0.0
     */
    boolean strict() default false;
}
