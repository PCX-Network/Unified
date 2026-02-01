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
 * Adds a comment to a configuration field or class.
 *
 * <p>Comments are written to the configuration file when using formats
 * that support comments (YAML, HOCON, TOML). They help users understand
 * what each configuration option does.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * @ConfigComment("Main plugin configuration")
 * public class PluginConfig {
 *
 *     @ConfigComment("Whether debug mode is enabled")
 *     private boolean debug = false;
 *
 *     @ConfigComment("The server port to listen on")
 *     private int port = 8080;
 * }
 * }</pre>
 *
 * <h2>Multi-line Comments</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class DatabaseConfig {
 *
 *     @ConfigComment({
 *         "The database connection URL",
 *         "Format: jdbc:mysql://host:port/database",
 *         "Example: jdbc:mysql://localhost:3306/mydb"
 *     })
 *     private String url = "jdbc:mysql://localhost:3306/plugin";
 *
 *     @ConfigComment({
 *         "Connection pool size",
 *         "Recommended: 10-20 for small servers",
 *         "           : 50-100 for large servers"
 *     })
 *     @Range(min = 1, max = 200)
 *     private int poolSize = 10;
 * }
 * }</pre>
 *
 * <h2>Generated YAML</h2>
 * <pre>{@code
 * # The database connection URL
 * # Format: jdbc:mysql://host:port/database
 * # Example: jdbc:mysql://localhost:3306/mydb
 * url: "jdbc:mysql://localhost:3306/plugin"
 *
 * # Connection pool size
 * # Recommended: 10-20 for small servers
 * #            : 50-100 for large servers
 * poolSize: 10
 * }</pre>
 *
 * <h2>Section Headers</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class PluginConfig {
 *
 *     @ConfigComment(value = "=== Database Settings ===", header = true)
 *     private DatabaseConfig database = new DatabaseConfig();
 *
 *     @ConfigComment(value = "=== Cache Settings ===", header = true)
 *     private CacheConfig cache = new CacheConfig();
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigSerializable
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface ConfigComment {

    /**
     * The comment text.
     *
     * <p>Each element in the array becomes a separate line in the comment.</p>
     *
     * @return the comment lines
     * @since 1.0.0
     */
    String[] value();

    /**
     * Whether this is a section header.
     *
     * <p>Headers get extra spacing and formatting in the output.</p>
     *
     * @return true if this is a header comment
     * @since 1.0.0
     */
    boolean header() default false;

    /**
     * Whether to add a blank line before this comment.
     *
     * <p>Useful for visual separation between configuration sections.</p>
     *
     * @return true to add blank line before
     * @since 1.0.0
     */
    boolean blankBefore() default false;

    /**
     * Whether to add a blank line after this comment.
     *
     * @return true to add blank line after
     * @since 1.0.0
     */
    boolean blankAfter() default false;
}
