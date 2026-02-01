/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.orm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sh.pcx.unified.data.sql.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps entities to and from database ResultSets.
 *
 * <p>This class provides automatic mapping between Java entities annotated
 * with {@code @Table}, {@code @Column}, and {@code @Id} annotations and
 * database rows.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create mapper for an entity class
 * EntityMapper<PlayerData> mapper = EntityMapper.forClass(PlayerData.class);
 *
 * // Map a ResultSet row to an entity
 * try (ResultSet rs = statement.executeQuery()) {
 *     while (rs.next()) {
 *         PlayerData player = mapper.mapRow(rs);
 *         // Use player...
 *     }
 * }
 *
 * // Get table information
 * String tableName = mapper.getTableName();      // "player_data"
 * String idColumn = mapper.getIdColumnName();    // "uuid"
 * List<String> columns = mapper.getColumnNames();
 *
 * // Get column values for INSERT/UPDATE
 * Map<String, Object> values = mapper.getColumnValues(playerData);
 * Object idValue = mapper.getIdValue(playerData);
 * }</pre>
 *
 * <h2>Supported Types</h2>
 * <p>The mapper supports automatic conversion for:
 * <ul>
 *   <li>Primitives and their wrappers (int, long, double, boolean, etc.)</li>
 *   <li>String</li>
 *   <li>UUID (stored as VARCHAR(36))</li>
 *   <li>Temporal types (Instant, LocalDateTime, LocalDate, LocalTime)</li>
 *   <li>byte[] (BLOB)</li>
 *   <li>Enums (stored as String)</li>
 *   <li>Serialized objects (stored as JSON)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>EntityMapper instances are thread-safe and can be cached and reused.
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author Supatuck
 * @see Table
 * @see Column
 * @see Id
 */
public class EntityMapper<T> {

    private static final Map<Class<?>, EntityMapper<?>> CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private final Class<T> entityClass;
    private final String tableName;
    private final String schema;
    private final String catalog;
    private final Field idField;
    private final String idColumnName;
    private final Id idAnnotation;
    private final List<FieldMapping> fieldMappings;
    private final Constructor<T> constructor;

    /**
     * Mapping information for a single field.
     */
    private record FieldMapping(
            Field field,
            String columnName,
            Column annotation,
            boolean isId
    ) {}

    /**
     * Creates an EntityMapper for the specified class.
     *
     * @param entityClass the entity class
     * @throws IllegalArgumentException if the class is not a valid entity
     * @since 1.0.0
     */
    private EntityMapper(@NotNull Class<T> entityClass) {
        this.entityClass = entityClass;

        // Parse @Table annotation
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new IllegalArgumentException(
                    "Class " + entityClass.getName() + " is not annotated with @Table"
            );
        }

        this.tableName = tableAnnotation.value().isEmpty()
                ? toSnakeCase(entityClass.getSimpleName())
                : tableAnnotation.value();
        this.schema = tableAnnotation.schema().isEmpty() ? null : tableAnnotation.schema();
        this.catalog = tableAnnotation.catalog().isEmpty() ? null : tableAnnotation.catalog();

        // Get no-arg constructor
        try {
            this.constructor = entityClass.getDeclaredConstructor();
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Entity class " + entityClass.getName() + " must have a no-arg constructor"
            );
        }

        // Parse fields
        this.fieldMappings = new ArrayList<>();
        Field foundIdField = null;
        String foundIdColumnName = null;
        Id foundIdAnnotation = null;

        for (Field field : getAllFields(entityClass)) {
            // Skip transient and static fields
            if (Modifier.isTransient(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Column columnAnnotation = field.getAnnotation(Column.class);
            Id idAnnotation = field.getAnnotation(Id.class);

            // Skip fields without @Column (unless they have @Id)
            if (columnAnnotation == null && idAnnotation == null) {
                continue;
            }

            field.setAccessible(true);

            String columnName;
            if (columnAnnotation != null && !columnAnnotation.value().isEmpty()) {
                columnName = columnAnnotation.value();
            } else {
                columnName = toSnakeCase(field.getName());
            }

            boolean isId = idAnnotation != null;
            if (isId) {
                if (foundIdField != null) {
                    throw new IllegalArgumentException(
                            "Entity class " + entityClass.getName() + " has multiple @Id fields"
                    );
                }
                foundIdField = field;
                foundIdColumnName = columnName;
                foundIdAnnotation = idAnnotation;
            }

            fieldMappings.add(new FieldMapping(field, columnName, columnAnnotation, isId));
        }

        if (foundIdField == null) {
            throw new IllegalArgumentException(
                    "Entity class " + entityClass.getName() + " must have a field annotated with @Id"
            );
        }

        this.idField = foundIdField;
        this.idColumnName = foundIdColumnName;
        this.idAnnotation = foundIdAnnotation;
    }

    /**
     * Gets or creates an EntityMapper for the specified class.
     *
     * <p>EntityMappers are cached for performance.
     *
     * @param <T>         the entity type
     * @param entityClass the entity class
     * @return the entity mapper
     * @throws IllegalArgumentException if the class is not a valid entity
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> EntityMapper<T> forClass(@NotNull Class<T> entityClass) {
        return (EntityMapper<T>) CACHE.computeIfAbsent(entityClass, EntityMapper::new);
    }

    /**
     * Clears the mapper cache.
     *
     * <p>This is primarily useful for testing or when dynamically
     * reloading entity classes.
     *
     * @since 1.0.0
     */
    public static void clearCache() {
        CACHE.clear();
    }

    // ========================================================================
    // Table Information
    // ========================================================================

    /**
     * Returns the table name.
     *
     * @return the table name
     * @since 1.0.0
     */
    @NotNull
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the fully qualified table name (catalog.schema.table).
     *
     * @return the qualified table name
     * @since 1.0.0
     */
    @NotNull
    public String getQualifiedTableName() {
        StringBuilder sb = new StringBuilder();
        if (catalog != null) {
            sb.append(catalog).append(".");
        }
        if (schema != null) {
            sb.append(schema).append(".");
        }
        sb.append(tableName);
        return sb.toString();
    }

    /**
     * Returns the entity class.
     *
     * @return the entity class
     * @since 1.0.0
     */
    @NotNull
    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Returns the ID column name.
     *
     * @return the ID column name
     * @since 1.0.0
     */
    @NotNull
    public String getIdColumnName() {
        return idColumnName;
    }

    /**
     * Returns whether the ID is auto-generated.
     *
     * @return true if the ID is auto-generated
     * @since 1.0.0
     */
    public boolean isIdAutoGenerated() {
        return idAnnotation.autoGenerate();
    }

    /**
     * Returns all column names.
     *
     * @return the list of column names
     * @since 1.0.0
     */
    @NotNull
    public List<String> getColumnNames() {
        return fieldMappings.stream()
                .map(FieldMapping::columnName)
                .toList();
    }

    /**
     * Returns column names suitable for INSERT statements.
     *
     * <p>Excludes non-insertable columns and auto-generated IDs.
     *
     * @return the insertable column names
     * @since 1.0.0
     */
    @NotNull
    public List<String> getInsertableColumnNames() {
        return fieldMappings.stream()
                .filter(m -> {
                    if (m.isId() && isIdAutoGenerated()) {
                        return false; // Skip auto-generated ID
                    }
                    if (m.annotation() != null && !m.annotation().insertable()) {
                        return false;
                    }
                    return true;
                })
                .map(FieldMapping::columnName)
                .toList();
    }

    /**
     * Returns column names suitable for UPDATE statements.
     *
     * <p>Excludes the ID column and non-updatable columns.
     *
     * @return the updatable column names
     * @since 1.0.0
     */
    @NotNull
    public List<String> getUpdatableColumnNames() {
        return fieldMappings.stream()
                .filter(m -> {
                    if (m.isId()) {
                        return false; // Never update ID
                    }
                    if (m.annotation() != null && !m.annotation().updatable()) {
                        return false;
                    }
                    return true;
                })
                .map(FieldMapping::columnName)
                .toList();
    }

    // ========================================================================
    // Mapping Methods
    // ========================================================================

    /**
     * Maps a ResultSet row to an entity instance.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * try (ResultSet rs = statement.executeQuery()) {
     *     while (rs.next()) {
     *         PlayerData player = mapper.mapRow(rs);
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet positioned at the current row
     * @return the mapped entity
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    @NotNull
    public T mapRow(@NotNull ResultSet rs) throws SQLException {
        try {
            T entity = constructor.newInstance();

            for (FieldMapping mapping : fieldMappings) {
                Object value = getValueFromResultSet(rs, mapping);
                mapping.field().set(entity, value);
            }

            return entity;
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Failed to create entity instance", e);
        }
    }

    /**
     * Gets the ID value from an entity.
     *
     * @param entity the entity
     * @return the ID value
     * @since 1.0.0
     */
    @Nullable
    public Object getIdValue(@NotNull T entity) {
        try {
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get ID value", e);
        }
    }

    /**
     * Sets the ID value on an entity.
     *
     * @param entity the entity
     * @param value  the ID value
     * @since 1.0.0
     */
    public void setIdValue(@NotNull T entity, @Nullable Object value) {
        try {
            idField.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID value", e);
        }
    }

    /**
     * Gets all column values from an entity for INSERT.
     *
     * @param entity the entity
     * @return a map of column names to values
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> getInsertValues(@NotNull T entity) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (FieldMapping mapping : fieldMappings) {
            // Skip auto-generated ID
            if (mapping.isId() && isIdAutoGenerated()) {
                continue;
            }
            // Skip non-insertable columns
            if (mapping.annotation() != null && !mapping.annotation().insertable()) {
                continue;
            }

            try {
                Object value = mapping.field().get(entity);
                values.put(mapping.columnName(), convertToJdbcValue(value, mapping));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get field value", e);
            }
        }

        return values;
    }

    /**
     * Gets column values from an entity for UPDATE.
     *
     * @param entity the entity
     * @return a map of column names to values
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> getUpdateValues(@NotNull T entity) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (FieldMapping mapping : fieldMappings) {
            // Skip ID column
            if (mapping.isId()) {
                continue;
            }
            // Skip non-updatable columns
            if (mapping.annotation() != null && !mapping.annotation().updatable()) {
                continue;
            }

            try {
                Object value = mapping.field().get(entity);
                values.put(mapping.columnName(), convertToJdbcValue(value, mapping));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get field value", e);
            }
        }

        return values;
    }

    // ========================================================================
    // Schema Generation
    // ========================================================================

    /**
     * Generates a CREATE TABLE SQL statement for this entity.
     *
     * @param databaseType the target database type
     * @return the CREATE TABLE SQL
     * @since 1.0.0
     */
    @NotNull
    public String generateCreateTableSql(@NotNull DatabaseType databaseType) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

        List<String> columnDefs = new ArrayList<>();
        String primaryKeyDef = null;

        for (FieldMapping mapping : fieldMappings) {
            StringBuilder colDef = new StringBuilder();
            colDef.append(mapping.columnName()).append(" ");

            // Get SQL type
            if (mapping.annotation() != null && !mapping.annotation().columnDefinition().isEmpty()) {
                colDef.append(mapping.annotation().columnDefinition());
            } else {
                colDef.append(getSqlType(mapping.field().getType(), mapping.annotation(), databaseType));
            }

            // Handle ID column
            if (mapping.isId() && isIdAutoGenerated()) {
                // Use database-specific auto-increment syntax
                if (isNumericType(mapping.field().getType())) {
                    colDef = new StringBuilder();
                    colDef.append(mapping.columnName()).append(" ");
                    colDef.append(databaseType.getAutoIncrementSyntax());
                    primaryKeyDef = null; // Already includes PRIMARY KEY
                } else {
                    primaryKeyDef = "PRIMARY KEY (" + mapping.columnName() + ")";
                }
            } else if (mapping.isId()) {
                primaryKeyDef = "PRIMARY KEY (" + mapping.columnName() + ")";
            }

            // NOT NULL constraint
            if (mapping.annotation() != null && !mapping.annotation().nullable() && !mapping.isId()) {
                colDef.append(" NOT NULL");
            }

            // UNIQUE constraint
            if (mapping.annotation() != null && mapping.annotation().unique()) {
                colDef.append(" UNIQUE");
            }

            // Default value
            if (mapping.annotation() != null && !mapping.annotation().defaultValue().isEmpty()) {
                colDef.append(" DEFAULT ").append(mapping.annotation().defaultValue());
            }

            columnDefs.add(colDef.toString());
        }

        sql.append(String.join(", ", columnDefs));

        if (primaryKeyDef != null) {
            sql.append(", ").append(primaryKeyDef);
        }

        sql.append(")");
        return sql.toString();
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Gets a value from a ResultSet for a field.
     */
    @Nullable
    private Object getValueFromResultSet(@NotNull ResultSet rs, @NotNull FieldMapping mapping) throws SQLException {
        Class<?> fieldType = mapping.field().getType();
        String columnName = mapping.columnName();

        // Check if column exists in ResultSet
        try {
            rs.findColumn(columnName);
        } catch (SQLException e) {
            return null; // Column not in result
        }

        // Handle null
        Object rawValue = rs.getObject(columnName);
        if (rawValue == null || rs.wasNull()) {
            return getDefaultValue(fieldType);
        }

        // Handle serialized (JSON) columns
        if (mapping.annotation() != null && mapping.annotation().serialized()) {
            String json = rs.getString(columnName);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return GSON.fromJson(json, mapping.field().getGenericType());
        }

        // Type-specific conversions
        return convertFromJdbc(rawValue, fieldType);
    }

    /**
     * Converts a Java value to JDBC-compatible value.
     */
    @Nullable
    private Object convertToJdbcValue(@Nullable Object value, @NotNull FieldMapping mapping) {
        if (value == null) {
            return null;
        }

        // Handle serialized columns
        if (mapping.annotation() != null && mapping.annotation().serialized()) {
            return GSON.toJson(value);
        }

        // Handle specific types
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        if (value instanceof Instant instant) {
            return Timestamp.from(instant);
        }
        if (value instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt);
        }
        if (value instanceof LocalDate ld) {
            return java.sql.Date.valueOf(ld);
        }
        if (value instanceof LocalTime lt) {
            return java.sql.Time.valueOf(lt);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }

        return value;
    }

    /**
     * Converts a JDBC value to Java type.
     */
    @Nullable
    private Object convertFromJdbc(@NotNull Object value, @NotNull Class<?> targetType) {
        // UUID
        if (targetType == UUID.class) {
            return UUID.fromString(value.toString());
        }

        // Temporal types
        if (targetType == Instant.class) {
            if (value instanceof Timestamp ts) {
                return ts.toInstant();
            }
            return Instant.parse(value.toString());
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp ts) {
                return ts.toLocalDateTime();
            }
            return LocalDateTime.parse(value.toString());
        }
        if (targetType == LocalDate.class) {
            if (value instanceof java.sql.Date d) {
                return d.toLocalDate();
            }
            return LocalDate.parse(value.toString());
        }
        if (targetType == LocalTime.class) {
            if (value instanceof java.sql.Time t) {
                return t.toLocalTime();
            }
            return LocalTime.parse(value.toString());
        }

        // Enums
        if (targetType.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<Enum>) targetType, value.toString());
            return enumValue;
        }

        // Numeric conversions
        if (value instanceof Number num) {
            if (targetType == int.class || targetType == Integer.class) {
                return num.intValue();
            }
            if (targetType == long.class || targetType == Long.class) {
                return num.longValue();
            }
            if (targetType == double.class || targetType == Double.class) {
                return num.doubleValue();
            }
            if (targetType == float.class || targetType == Float.class) {
                return num.floatValue();
            }
            if (targetType == short.class || targetType == Short.class) {
                return num.shortValue();
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return num.byteValue();
            }
            if (targetType == BigDecimal.class) {
                return BigDecimal.valueOf(num.doubleValue());
            }
        }

        // Boolean
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof Number n) {
                return n.intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        }

        return value;
    }

    /**
     * Gets the SQL type for a Java type.
     */
    @NotNull
    private String getSqlType(@NotNull Class<?> type, @Nullable Column annotation, @NotNull DatabaseType dbType) {
        // String
        if (type == String.class) {
            int length = annotation != null ? annotation.length() : 255;
            return "VARCHAR(" + length + ")";
        }

        // Numeric types
        if (type == int.class || type == Integer.class) {
            return "INT";
        }
        if (type == long.class || type == Long.class) {
            return "BIGINT";
        }
        if (type == short.class || type == Short.class) {
            return "SMALLINT";
        }
        if (type == byte.class || type == Byte.class) {
            return "TINYINT";
        }
        if (type == double.class || type == Double.class) {
            return "DOUBLE";
        }
        if (type == float.class || type == Float.class) {
            return "FLOAT";
        }
        if (type == BigDecimal.class) {
            int precision = annotation != null ? annotation.precision() : 10;
            int scale = annotation != null ? annotation.scale() : 2;
            return "DECIMAL(" + precision + "," + scale + ")";
        }

        // Boolean
        if (type == boolean.class || type == Boolean.class) {
            return "BOOLEAN";
        }

        // UUID
        if (type == UUID.class) {
            return "VARCHAR(36)";
        }

        // Temporal types
        if (type == Instant.class || type == LocalDateTime.class) {
            return "TIMESTAMP";
        }
        if (type == LocalDate.class) {
            return "DATE";
        }
        if (type == LocalTime.class) {
            return "TIME";
        }

        // Binary
        if (type == byte[].class) {
            return "BLOB";
        }

        // Enum
        if (type.isEnum()) {
            return "VARCHAR(64)";
        }

        // Default to TEXT for complex/serialized types
        return "TEXT";
    }

    /**
     * Gets the default value for a primitive type.
     */
    @Nullable
    private Object getDefaultValue(@NotNull Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    /**
     * Checks if a type is numeric.
     */
    private boolean isNumericType(@NotNull Class<?> type) {
        return type == int.class || type == Integer.class ||
                type == long.class || type == Long.class ||
                type == short.class || type == Short.class ||
                type == byte.class || type == Byte.class;
    }

    /**
     * Gets all fields including inherited ones.
     */
    @NotNull
    private static List<Field> getAllFields(@NotNull Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Converts a camelCase string to snake_case.
     */
    @NotNull
    private static String toSnakeCase(@NotNull String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
