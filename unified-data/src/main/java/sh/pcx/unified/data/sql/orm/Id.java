/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.orm;

import java.lang.annotation.*;

/**
 * Marks a field as the primary key of an entity.
 *
 * <p>This annotation is used by the ORM-lite system to identify the primary
 * key column of a database table. Each entity must have exactly one field
 * annotated with {@code @Id}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // UUID primary key (common for Minecraft)
 * @Table("players")
 * public class PlayerData {
 *     @Id
 *     @Column("uuid")
 *     private UUID uuid;
 * }
 *
 * // Auto-increment integer primary key
 * @Table("transactions")
 * public class Transaction {
 *     @Id(autoGenerate = true)
 *     @Column("id")
 *     private long id;
 *
 *     @Column("amount")
 *     private double amount;
 * }
 *
 * // String primary key
 * @Table("config")
 * public class ConfigEntry {
 *     @Id
 *     @Column("key")
 *     private String key;
 *
 *     @Column("value")
 *     private String value;
 * }
 * }</pre>
 *
 * <h2>Supported ID Types</h2>
 * <ul>
 *   <li>{@code UUID} - Recommended for player-related entities</li>
 *   <li>{@code Long/long} - Suitable for auto-increment IDs</li>
 *   <li>{@code Integer/int} - Suitable for smaller auto-increment IDs</li>
 *   <li>{@code String} - Suitable for natural keys</li>
 * </ul>
 *
 * <h2>Auto-Generation</h2>
 * <p>When {@link #autoGenerate()} is true:
 * <ul>
 *   <li>For numeric types: The database will auto-generate values (AUTO_INCREMENT)</li>
 *   <li>For UUID: A random UUID will be generated before insert if the value is null</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Table
 * @see Column
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Id {

    /**
     * Whether the ID should be auto-generated.
     *
     * <p>When true:
     * <ul>
     *   <li>For {@code Long/Integer}: Uses database AUTO_INCREMENT</li>
     *   <li>For {@code UUID}: Generates a random UUID if null on insert</li>
     * </ul>
     *
     * @return true if the ID is auto-generated
     * @since 1.0.0
     */
    boolean autoGenerate() default false;

    /**
     * The generation strategy for auto-generated IDs.
     *
     * @return the generation strategy
     * @since 1.0.0
     */
    GenerationType strategy() default GenerationType.AUTO;

    /**
     * ID generation strategies.
     *
     * @since 1.0.0
     */
    enum GenerationType {
        /**
         * Automatic strategy selection based on ID type.
         *
         * <ul>
         *   <li>Numeric types: Uses IDENTITY (auto-increment)</li>
         *   <li>UUID: Uses UUID generation</li>
         * </ul>
         */
        AUTO,

        /**
         * Database-managed identity column (AUTO_INCREMENT).
         *
         * <p>The database generates the ID value on insert.
         */
        IDENTITY,

        /**
         * UUID generation.
         *
         * <p>A random UUID is generated before insert if the value is null.
         */
        UUID,

        /**
         * Sequence-based generation (PostgreSQL).
         *
         * <p>Uses a database sequence to generate values.
         */
        SEQUENCE,

        /**
         * Application-provided ID.
         *
         * <p>The application is responsible for setting the ID before insert.
         */
        ASSIGNED
    }

    /**
     * The sequence name for SEQUENCE generation strategy.
     *
     * <p>Only applicable when {@link #strategy()} is {@link GenerationType#SEQUENCE}.
     *
     * @return the sequence name
     * @since 1.0.0
     */
    String sequenceName() default "";
}
