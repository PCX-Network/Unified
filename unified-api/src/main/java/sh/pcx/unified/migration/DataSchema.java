/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Schema definition for data structures used in migration.
 *
 * <p>DataSchema describes the structure of data, including field names,
 * types, and constraints. It is used for validation and mapping suggestions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Define a schema
 * DataSchema playerSchema = DataSchema.builder()
 *     .name("player_data")
 *     .field("uuid", FieldType.STRING, true)
 *     .field("balance", FieldType.DOUBLE, false, 0.0)
 *     .field("inventory", FieldType.OBJECT)
 *     .field("last_login", FieldType.TIMESTAMP)
 *     .build();
 *
 * // Get field info
 * Optional<FieldDefinition> balanceField = schema.getField("balance");
 * if (balanceField.isPresent()) {
 *     Object defaultValue = balanceField.get().defaultValue();
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see FieldMapping
 */
public final class DataSchema {

    private final String name;
    private final String description;
    private final Map<String, FieldDefinition> fields;
    private final List<String> primaryKeys;

    private DataSchema(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.fields = Map.copyOf(builder.fields);
        this.primaryKeys = List.copyOf(builder.primaryKeys);
    }

    /**
     * Returns the schema name.
     *
     * @return the name
     * @since 1.0.0
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the schema description.
     *
     * @return the description, or null
     * @since 1.0.0
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Returns all field definitions.
     *
     * @return unmodifiable map of field name to definition
     * @since 1.0.0
     */
    @NotNull
    public Map<String, FieldDefinition> getFields() {
        return fields;
    }

    /**
     * Returns field names in order.
     *
     * @return list of field names
     * @since 1.0.0
     */
    @NotNull
    public List<String> getFieldNames() {
        return new ArrayList<>(fields.keySet());
    }

    /**
     * Returns a field definition by name.
     *
     * @param name the field name
     * @return the field definition, or empty
     * @since 1.0.0
     */
    @NotNull
    public Optional<FieldDefinition> getField(@NotNull String name) {
        return Optional.ofNullable(fields.get(name));
    }

    /**
     * Checks if a field exists.
     *
     * @param name the field name
     * @return true if the field exists
     * @since 1.0.0
     */
    public boolean hasField(@NotNull String name) {
        return fields.containsKey(name);
    }

    /**
     * Returns the number of fields.
     *
     * @return the field count
     * @since 1.0.0
     */
    public int getFieldCount() {
        return fields.size();
    }

    /**
     * Returns the primary key fields.
     *
     * @return list of primary key field names
     * @since 1.0.0
     */
    @NotNull
    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    /**
     * Returns only required fields.
     *
     * @return list of required field definitions
     * @since 1.0.0
     */
    @NotNull
    public List<FieldDefinition> getRequiredFields() {
        return fields.values().stream()
                .filter(FieldDefinition::required)
                .toList();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "DataSchema{" +
                "name='" + name + '\'' +
                ", fields=" + fields.size() +
                '}';
    }

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * Field type enumeration.
     *
     * @since 1.0.0
     */
    public enum FieldType {
        /** String/text field */
        STRING,
        /** Integer field */
        INTEGER,
        /** Long integer field */
        LONG,
        /** Double/floating point field */
        DOUBLE,
        /** Boolean field */
        BOOLEAN,
        /** Timestamp/date-time field */
        TIMESTAMP,
        /** UUID field */
        UUID,
        /** Binary/byte array field */
        BINARY,
        /** JSON/Map object field */
        OBJECT,
        /** List/array field */
        ARRAY,
        /** Unknown/any type */
        ANY
    }

    /**
     * Definition of a single field.
     *
     * @param name         the field name
     * @param type         the field type
     * @param required     whether the field is required
     * @param defaultValue the default value
     * @param description  field description
     * @param constraints  additional constraints
     * @since 1.0.0
     */
    public record FieldDefinition(
            @NotNull String name,
            @NotNull FieldType type,
            boolean required,
            @Nullable Object defaultValue,
            @Nullable String description,
            @NotNull Map<String, Object> constraints
    ) {
        public FieldDefinition {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
            constraints = constraints != null ? Map.copyOf(constraints) : Map.of();
        }

        /**
         * Creates a simple required field.
         *
         * @param name the field name
         * @param type the field type
         * @return the field definition
         */
        @NotNull
        public static FieldDefinition required(@NotNull String name, @NotNull FieldType type) {
            return new FieldDefinition(name, type, true, null, null, Map.of());
        }

        /**
         * Creates a simple optional field.
         *
         * @param name the field name
         * @param type the field type
         * @return the field definition
         */
        @NotNull
        public static FieldDefinition optional(@NotNull String name, @NotNull FieldType type) {
            return new FieldDefinition(name, type, false, null, null, Map.of());
        }

        /**
         * Creates an optional field with default value.
         *
         * @param name         the field name
         * @param type         the field type
         * @param defaultValue the default value
         * @return the field definition
         */
        @NotNull
        public static FieldDefinition optional(@NotNull String name, @NotNull FieldType type,
                                               @NotNull Object defaultValue) {
            return new FieldDefinition(name, type, false, defaultValue, null, Map.of());
        }
    }

    /**
     * Builder for DataSchema.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String name = "unknown";
        private String description;
        private final Map<String, FieldDefinition> fields = new LinkedHashMap<>();
        private final List<String> primaryKeys = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the schema name.
         *
         * @param name the name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the schema description.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds a field definition.
         *
         * @param definition the field definition
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder field(@NotNull FieldDefinition definition) {
            fields.put(definition.name(), definition);
            return this;
        }

        /**
         * Adds a required field.
         *
         * @param name the field name
         * @param type the field type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder field(@NotNull String name, @NotNull FieldType type) {
            return field(FieldDefinition.required(name, type));
        }

        /**
         * Adds a field with required flag.
         *
         * @param name     the field name
         * @param type     the field type
         * @param required whether required
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder field(@NotNull String name, @NotNull FieldType type, boolean required) {
            if (required) {
                return field(FieldDefinition.required(name, type));
            } else {
                return field(FieldDefinition.optional(name, type));
            }
        }

        /**
         * Adds a field with required flag and default value.
         *
         * @param name         the field name
         * @param type         the field type
         * @param required     whether required
         * @param defaultValue the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder field(@NotNull String name, @NotNull FieldType type,
                            boolean required, @Nullable Object defaultValue) {
            return field(new FieldDefinition(name, type, required, defaultValue, null, Map.of()));
        }

        /**
         * Adds a primary key field.
         *
         * @param fieldName the field name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder primaryKey(@NotNull String fieldName) {
            primaryKeys.add(fieldName);
            return this;
        }

        /**
         * Builds the schema.
         *
         * @return a new DataSchema
         * @since 1.0.0
         */
        @NotNull
        public DataSchema build() {
            return new DataSchema(this);
        }
    }
}
