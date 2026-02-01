/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.orm;

import java.lang.annotation.*;

/**
 * Maps a field to a database column.
 *
 * <p>This annotation is used by the ORM-lite system to map Java fields to
 * database columns. The column name can be explicitly specified or derived
 * from the field name.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Table("players")
 * public class PlayerData {
 *     // Explicit column name
 *     @Id
 *     @Column("player_uuid")
 *     private UUID uuid;
 *
 *     // Column name derived from field (balance -> balance)
 *     @Column
 *     private double balance;
 *
 *     // With additional options
 *     @Column(value = "last_login", nullable = false)
 *     private Instant lastLogin;
 *
 *     // JSON serialized column
 *     @Column(value = "metadata", serialized = true)
 *     private Map<String, Object> metadata;
 *
 *     // Transient field (not mapped)
 *     private transient int cachedValue;
 * }
 * }</pre>
 *
 * <h2>Type Mappings</h2>
 * <p>The ORM supports the following Java to SQL type mappings:
 * <table border="1">
 *   <tr><th>Java Type</th><th>SQL Type</th></tr>
 *   <tr><td>String</td><td>VARCHAR(255)</td></tr>
 *   <tr><td>int, Integer</td><td>INT</td></tr>
 *   <tr><td>long, Long</td><td>BIGINT</td></tr>
 *   <tr><td>double, Double</td><td>DOUBLE</td></tr>
 *   <tr><td>float, Float</td><td>FLOAT</td></tr>
 *   <tr><td>boolean, Boolean</td><td>BOOLEAN</td></tr>
 *   <tr><td>UUID</td><td>VARCHAR(36)</td></tr>
 *   <tr><td>Instant</td><td>TIMESTAMP</td></tr>
 *   <tr><td>LocalDateTime</td><td>DATETIME</td></tr>
 *   <tr><td>LocalDate</td><td>DATE</td></tr>
 *   <tr><td>byte[]</td><td>BLOB</td></tr>
 *   <tr><td>Serialized objects</td><td>TEXT (JSON)</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Table
 * @see Id
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {

    /**
     * The name of the database column.
     *
     * <p>If empty, the column name is derived from the field name using
     * snake_case conversion.
     *
     * @return the column name, or empty for auto-derived name
     * @since 1.0.0
     */
    String value() default "";

    /**
     * Whether this column can contain NULL values.
     *
     * @return true if the column is nullable (default: true)
     * @since 1.0.0
     */
    boolean nullable() default true;

    /**
     * Whether this column has a unique constraint.
     *
     * @return true if the column must be unique
     * @since 1.0.0
     */
    boolean unique() default false;

    /**
     * The maximum length for string columns.
     *
     * <p>Only applicable to String fields. Default is 255.
     *
     * @return the maximum length
     * @since 1.0.0
     */
    int length() default 255;

    /**
     * The precision for decimal columns.
     *
     * <p>Only applicable to BigDecimal fields.
     *
     * @return the precision (total digits)
     * @since 1.0.0
     */
    int precision() default 10;

    /**
     * The scale for decimal columns.
     *
     * <p>Only applicable to BigDecimal fields.
     *
     * @return the scale (digits after decimal point)
     * @since 1.0.0
     */
    int scale() default 2;

    /**
     * The default value for this column as a SQL expression.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * @Column(defaultValue = "0")
     * private int score;
     *
     * @Column(defaultValue = "CURRENT_TIMESTAMP")
     * private Instant createdAt;
     *
     * @Column(defaultValue = "'active'")
     * private String status;
     * }</pre>
     *
     * @return the default SQL expression, or empty for no default
     * @since 1.0.0
     */
    String defaultValue() default "";

    /**
     * Whether this column should be serialized as JSON.
     *
     * <p>When true, complex objects (Maps, Lists, custom types) are
     * serialized to JSON for storage and deserialized on read.
     *
     * @return true to enable JSON serialization
     * @since 1.0.0
     */
    boolean serialized() default false;

    /**
     * Whether this column should be updated when the entity is saved.
     *
     * <p>Set to false for columns that should only be set on insert
     * (e.g., created_at timestamps).
     *
     * @return true if the column is updatable (default: true)
     * @since 1.0.0
     */
    boolean updatable() default true;

    /**
     * Whether this column should be included in INSERT statements.
     *
     * <p>Set to false for auto-generated columns.
     *
     * @return true if the column is insertable (default: true)
     * @since 1.0.0
     */
    boolean insertable() default true;

    /**
     * Custom SQL column definition.
     *
     * <p>When specified, this overrides the auto-generated column type.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * @Column(columnDefinition = "MEDIUMTEXT")
     * private String largeContent;
     *
     * @Column(columnDefinition = "DECIMAL(18,8)")
     * private BigDecimal preciseCurrency;
     * }</pre>
     *
     * @return the custom column definition, or empty for auto-derived
     * @since 1.0.0
     */
    String columnDefinition() default "";
}
