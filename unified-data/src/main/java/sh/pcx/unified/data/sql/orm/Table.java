/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.orm;

import java.lang.annotation.*;

/**
 * Marks a class as a database table entity.
 *
 * <p>This annotation is used by the ORM-lite system to map Java classes to
 * database tables. The table name can be explicitly specified or derived
 * from the class name.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Explicit table name
 * @Table("player_data")
 * public class PlayerData {
 *     @Id
 *     @Column("uuid")
 *     private UUID uuid;
 *
 *     @Column("balance")
 *     private double balance;
 *
 *     @Column("last_login")
 *     private Instant lastLogin;
 * }
 *
 * // Table name derived from class (PlayerStats -> player_stats)
 * @Table
 * public class PlayerStats {
 *     // ...
 * }
 *
 * // With schema specification
 * @Table(value = "players", schema = "game_data")
 * public class Player {
 *     // ...
 * }
 * }</pre>
 *
 * <h2>Naming Conventions</h2>
 * <p>When the table name is not explicitly specified:
 * <ul>
 *   <li>{@code PlayerData} -> {@code player_data}</li>
 *   <li>{@code HTTPServer} -> {@code http_server}</li>
 *   <li>{@code User} -> {@code user}</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Column
 * @see Id
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {

    /**
     * The name of the database table.
     *
     * <p>If empty, the table name is derived from the class name using
     * snake_case conversion.
     *
     * @return the table name, or empty for auto-derived name
     * @since 1.0.0
     */
    String value() default "";

    /**
     * The database schema for this table.
     *
     * <p>If specified, the table will be qualified as {@code schema.table}.
     *
     * @return the schema name, or empty for default schema
     * @since 1.0.0
     */
    String schema() default "";

    /**
     * The database catalog for this table.
     *
     * <p>If specified, the table will be qualified as {@code catalog.schema.table}.
     *
     * @return the catalog name, or empty for default catalog
     * @since 1.0.0
     */
    String catalog() default "";

    /**
     * Whether to cache the table metadata.
     *
     * <p>When true (default), the entity mapper caches reflection data
     * for better performance.
     *
     * @return true to enable caching
     * @since 1.0.0
     */
    boolean cacheable() default true;
}
