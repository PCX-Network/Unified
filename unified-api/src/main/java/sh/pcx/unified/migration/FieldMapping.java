/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Configuration for mapping fields between source and target formats during import.
 *
 * <p>FieldMapping defines how fields from a source format should be transformed
 * and mapped to fields in the target format. It supports direct mapping,
 * transformations, default values, and ignoring fields.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * FieldMapping mapping = FieldMapping.builder()
 *     // Direct mapping
 *     .map("inventory", "main_inventory")
 *     .map("armour", "armor")
 *
 *     // Mapping with transformation
 *     .map("balance", "economy_balance", value -> ((Number) value).doubleValue())
 *
 *     // Default value if source is missing
 *     .mapWithDefault("level", "player_level", 1)
 *
 *     // Ignore certain fields
 *     .ignore("cached_data")
 *     .ignore("temp_*")  // Wildcard support
 *
 *     // Keep field name but transform value
 *     .transform("last_login", value -> Instant.parse((String) value))
 *
 *     // Combine multiple source fields
 *     .combine("full_location", "world", "x", "y", "z")
 *
 *     .build();
 *
 * // Apply mapping
 * Map<String, Object> mapped = mapping.apply(sourceData);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see ImportContext
 */
public final class FieldMapping {

    private final Map<String, String> directMappings;
    private final Map<String, Function<Object, Object>> transformers;
    private final Map<String, Object> defaultValues;
    private final Set<String> ignoredFields;
    private final Set<String> ignoredPatterns;
    private final List<CombinedField> combinedFields;
    private final boolean passUnmapped;

    private FieldMapping(Builder builder) {
        this.directMappings = Map.copyOf(builder.directMappings);
        this.transformers = Map.copyOf(builder.transformers);
        this.defaultValues = Map.copyOf(builder.defaultValues);
        this.ignoredFields = Set.copyOf(builder.ignoredFields);
        this.ignoredPatterns = Set.copyOf(builder.ignoredPatterns);
        this.combinedFields = List.copyOf(builder.combinedFields);
        this.passUnmapped = builder.passUnmapped;
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Returns the target field name for a source field.
     *
     * @param sourceField the source field name
     * @return the target field name, or the source name if not mapped
     * @since 1.0.0
     */
    @NotNull
    public String getTargetField(@NotNull String sourceField) {
        return directMappings.getOrDefault(sourceField, sourceField);
    }

    /**
     * Checks if a source field has a direct mapping.
     *
     * @param sourceField the source field name
     * @return true if a mapping exists
     * @since 1.0.0
     */
    public boolean hasMapping(@NotNull String sourceField) {
        return directMappings.containsKey(sourceField);
    }

    /**
     * Checks if a source field should be ignored.
     *
     * @param sourceField the source field name
     * @return true if the field should be ignored
     * @since 1.0.0
     */
    public boolean isIgnored(@NotNull String sourceField) {
        if (ignoredFields.contains(sourceField)) {
            return true;
        }
        for (String pattern : ignoredPatterns) {
            if (matchesPattern(sourceField, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a field has a transformer.
     *
     * @param field the field name (source or target)
     * @return true if a transformer exists
     * @since 1.0.0
     */
    public boolean hasTransformer(@NotNull String field) {
        return transformers.containsKey(field);
    }

    /**
     * Returns the default value for a field.
     *
     * @param targetField the target field name
     * @return the default value, or null if none
     * @since 1.0.0
     */
    @Nullable
    public Object getDefaultValue(@NotNull String targetField) {
        return defaultValues.get(targetField);
    }

    /**
     * Checks if unmapped fields should be passed through.
     *
     * @return true if unmapped fields are included in output
     * @since 1.0.0
     */
    public boolean isPassUnmapped() {
        return passUnmapped;
    }

    /**
     * Returns all direct mappings.
     *
     * @return an unmodifiable map of source to target field names
     * @since 1.0.0
     */
    @NotNull
    public Map<String, String> getDirectMappings() {
        return directMappings;
    }

    /**
     * Returns all ignored fields.
     *
     * @return an unmodifiable set of ignored field names
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getIgnoredFields() {
        return ignoredFields;
    }

    /**
     * Returns all combined field definitions.
     *
     * @return an unmodifiable list of combined fields
     * @since 1.0.0
     */
    @NotNull
    public List<CombinedField> getCombinedFields() {
        return combinedFields;
    }

    // ========================================================================
    // Application
    // ========================================================================

    /**
     * Applies this mapping to source data.
     *
     * @param sourceData the source data map
     * @return the mapped data
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> apply(@NotNull Map<String, Object> sourceData) {
        Map<String, Object> result = new HashMap<>();

        // Process each source field
        for (Map.Entry<String, Object> entry : sourceData.entrySet()) {
            String sourceField = entry.getKey();
            Object value = entry.getValue();

            // Skip ignored fields
            if (isIgnored(sourceField)) {
                continue;
            }

            // Get target field name
            String targetField = getTargetField(sourceField);

            // Apply transformer if exists
            if (transformers.containsKey(sourceField)) {
                value = transformers.get(sourceField).apply(value);
            } else if (transformers.containsKey(targetField)) {
                value = transformers.get(targetField).apply(value);
            }

            // Only include if mapped or passUnmapped is true
            if (hasMapping(sourceField) || passUnmapped) {
                result.put(targetField, value);
            }
        }

        // Apply default values for missing fields
        for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
            result.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Process combined fields
        for (CombinedField combined : combinedFields) {
            Object combinedValue = combined.combine(sourceData);
            if (combinedValue != null) {
                result.put(combined.targetField(), combinedValue);
            }
        }

        return result;
    }

    /**
     * Transforms a single value.
     *
     * @param field the field name
     * @param value the value to transform
     * @return the transformed value
     * @since 1.0.0
     */
    @Nullable
    public Object transform(@NotNull String field, @Nullable Object value) {
        if (value == null) {
            return defaultValues.get(field);
        }
        Function<Object, Object> transformer = transformers.get(field);
        return transformer != null ? transformer.apply(value) : value;
    }

    private boolean matchesPattern(String field, String pattern) {
        // Simple wildcard matching
        if (pattern.endsWith("*")) {
            return field.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return field.endsWith(pattern.substring(1));
        }
        return field.equals(pattern);
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Creates an empty mapping that passes all fields unchanged.
     *
     * @return an identity mapping
     * @since 1.0.0
     */
    @NotNull
    public static FieldMapping identity() {
        return builder().passUnmapped(true).build();
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

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * Definition for combining multiple source fields into one target field.
     *
     * @param targetField   the target field name
     * @param sourceFields  the source field names to combine
     * @param combiner      the function to combine values
     * @since 1.0.0
     */
    public record CombinedField(
            @NotNull String targetField,
            @NotNull List<String> sourceFields,
            @NotNull Function<List<Object>, Object> combiner
    ) {
        public CombinedField {
            Objects.requireNonNull(targetField, "targetField cannot be null");
            Objects.requireNonNull(sourceFields, "sourceFields cannot be null");
            Objects.requireNonNull(combiner, "combiner cannot be null");
            sourceFields = List.copyOf(sourceFields);
        }

        /**
         * Combines values from source data.
         *
         * @param sourceData the source data map
         * @return the combined value
         */
        @Nullable
        Object combine(@NotNull Map<String, Object> sourceData) {
            List<Object> values = new ArrayList<>();
            for (String field : sourceFields) {
                values.add(sourceData.get(field));
            }
            return combiner.apply(values);
        }
    }

    /**
     * Builder for FieldMapping.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final Map<String, String> directMappings = new HashMap<>();
        private final Map<String, Function<Object, Object>> transformers = new HashMap<>();
        private final Map<String, Object> defaultValues = new HashMap<>();
        private final Set<String> ignoredFields = new HashSet<>();
        private final Set<String> ignoredPatterns = new HashSet<>();
        private final List<CombinedField> combinedFields = new ArrayList<>();
        private boolean passUnmapped = false;

        private Builder() {}

        /**
         * Maps a source field to a target field.
         *
         * @param sourceField the source field name
         * @param targetField the target field name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder map(@NotNull String sourceField, @NotNull String targetField) {
            directMappings.put(sourceField, targetField);
            return this;
        }

        /**
         * Maps a source field to a target field with transformation.
         *
         * @param sourceField the source field name
         * @param targetField the target field name
         * @param transformer the value transformer
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder map(@NotNull String sourceField, @NotNull String targetField,
                          @NotNull Function<Object, Object> transformer) {
            directMappings.put(sourceField, targetField);
            transformers.put(sourceField, transformer);
            return this;
        }

        /**
         * Maps a source field with a default value if missing.
         *
         * @param sourceField  the source field name
         * @param targetField  the target field name
         * @param defaultValue the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder mapWithDefault(@NotNull String sourceField, @NotNull String targetField,
                                      @NotNull Object defaultValue) {
            directMappings.put(sourceField, targetField);
            defaultValues.put(targetField, defaultValue);
            return this;
        }

        /**
         * Transforms a field value without renaming.
         *
         * @param field       the field name
         * @param transformer the value transformer
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder transform(@NotNull String field, @NotNull Function<Object, Object> transformer) {
            transformers.put(field, transformer);
            return this;
        }

        /**
         * Sets a default value for a target field.
         *
         * @param targetField  the target field name
         * @param defaultValue the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder defaultValue(@NotNull String targetField, @NotNull Object defaultValue) {
            defaultValues.put(targetField, defaultValue);
            return this;
        }

        /**
         * Ignores a source field during mapping.
         *
         * @param sourceField the source field name (supports * wildcard)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder ignore(@NotNull String sourceField) {
            if (sourceField.contains("*")) {
                ignoredPatterns.add(sourceField);
            } else {
                ignoredFields.add(sourceField);
            }
            return this;
        }

        /**
         * Ignores multiple source fields.
         *
         * @param sourceFields the source field names
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder ignoreAll(@NotNull String... sourceFields) {
            for (String field : sourceFields) {
                ignore(field);
            }
            return this;
        }

        /**
         * Combines multiple source fields into one target field.
         *
         * @param targetField  the target field name
         * @param combiner     the combining function
         * @param sourceFields the source field names
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder combine(@NotNull String targetField,
                              @NotNull Function<List<Object>, Object> combiner,
                              @NotNull String... sourceFields) {
            combinedFields.add(new CombinedField(targetField, List.of(sourceFields), combiner));
            return this;
        }

        /**
         * Combines multiple source fields using string concatenation.
         *
         * @param targetField  the target field name
         * @param separator    the separator string
         * @param sourceFields the source field names
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder combineAsString(@NotNull String targetField, @NotNull String separator,
                                       @NotNull String... sourceFields) {
            return combine(targetField, values -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) sb.append(separator);
                    sb.append(values.get(i));
                }
                return sb.toString();
            }, sourceFields);
        }

        /**
         * Sets whether unmapped fields should pass through.
         *
         * @param passUnmapped true to include unmapped fields
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder passUnmapped(boolean passUnmapped) {
            this.passUnmapped = passUnmapped;
            return this;
        }

        /**
         * Merges another mapping into this builder.
         *
         * @param other the mapping to merge
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder merge(@NotNull FieldMapping other) {
            directMappings.putAll(other.directMappings);
            transformers.putAll(other.transformers);
            defaultValues.putAll(other.defaultValues);
            ignoredFields.addAll(other.ignoredFields);
            ignoredPatterns.addAll(other.ignoredPatterns);
            combinedFields.addAll(other.combinedFields);
            return this;
        }

        /**
         * Builds the field mapping.
         *
         * @return a new FieldMapping instance
         * @since 1.0.0
         */
        @NotNull
        public FieldMapping build() {
            return new FieldMapping(this);
        }
    }
}
