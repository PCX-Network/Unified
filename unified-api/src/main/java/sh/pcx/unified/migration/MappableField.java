/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a field that can be mapped during import operations.
 *
 * <p>MappableField provides metadata about a source field that helps users
 * understand what data it contains and how it should be mapped.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get mappable fields from importer
 * List<MappableField> fields = importer.getMappableFields();
 *
 * for (MappableField field : fields) {
 *     System.out.println(field.name() + " (" + field.type() + ")");
 *     if (field.suggestedMapping() != null) {
 *         System.out.println("  -> " + field.suggestedMapping());
 *     }
 *     if (field.sampleValues() != null) {
 *         System.out.println("  Samples: " + field.sampleValues());
 *     }
 * }
 * }</pre>
 *
 * @param name             the field name in the source
 * @param type             the field type
 * @param description      human-readable description
 * @param required         whether the field is required
 * @param suggestedMapping suggested target field name
 * @param sampleValues     sample values from the source data
 * @param category         field category for grouping
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see FieldMapping
 */
public record MappableField(
        @NotNull String name,
        @NotNull DataSchema.FieldType type,
        @Nullable String description,
        boolean required,
        @Nullable String suggestedMapping,
        @Nullable List<String> sampleValues,
        @Nullable String category
) {

    /**
     * Compact constructor with validation.
     */
    public MappableField {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        if (sampleValues != null) {
            sampleValues = List.copyOf(sampleValues);
        }
    }

    /**
     * Creates a simple mappable field.
     *
     * @param name the field name
     * @param type the field type
     * @return a new mappable field
     * @since 1.0.0
     */
    @NotNull
    public static MappableField of(@NotNull String name, @NotNull DataSchema.FieldType type) {
        return new MappableField(name, type, null, false, null, null, null);
    }

    /**
     * Creates a mappable field with description.
     *
     * @param name        the field name
     * @param type        the field type
     * @param description the description
     * @return a new mappable field
     * @since 1.0.0
     */
    @NotNull
    public static MappableField of(@NotNull String name, @NotNull DataSchema.FieldType type,
                                   @NotNull String description) {
        return new MappableField(name, type, description, false, null, null, null);
    }

    /**
     * Creates a mappable field with suggested mapping.
     *
     * @param name             the field name
     * @param type             the field type
     * @param suggestedMapping the suggested target field
     * @return a new mappable field
     * @since 1.0.0
     */
    @NotNull
    public static MappableField withSuggestion(@NotNull String name, @NotNull DataSchema.FieldType type,
                                               @NotNull String suggestedMapping) {
        return new MappableField(name, type, null, false, suggestedMapping, null, null);
    }

    /**
     * Returns this field with a new suggested mapping.
     *
     * @param mapping the suggested mapping
     * @return a new instance with the suggested mapping
     * @since 1.0.0
     */
    @NotNull
    public MappableField withSuggestedMapping(@NotNull String mapping) {
        return new MappableField(name, type, description, required, mapping, sampleValues, category);
    }

    /**
     * Returns this field with sample values.
     *
     * @param samples the sample values
     * @return a new instance with samples
     * @since 1.0.0
     */
    @NotNull
    public MappableField withSamples(@NotNull List<String> samples) {
        return new MappableField(name, type, description, required, suggestedMapping, samples, category);
    }

    /**
     * Returns this field marked as required.
     *
     * @return a new instance marked as required
     * @since 1.0.0
     */
    @NotNull
    public MappableField asRequired() {
        return new MappableField(name, type, description, true, suggestedMapping, sampleValues, category);
    }

    /**
     * Returns this field with a category.
     *
     * @param category the category
     * @return a new instance with the category
     * @since 1.0.0
     */
    @NotNull
    public MappableField inCategory(@NotNull String category) {
        return new MappableField(name, type, description, required, suggestedMapping, sampleValues, category);
    }

    /**
     * Checks if this field has a suggested mapping.
     *
     * @return true if a suggestion exists
     * @since 1.0.0
     */
    public boolean hasSuggestion() {
        return suggestedMapping != null && !suggestedMapping.isBlank();
    }

    /**
     * Checks if this field has sample values.
     *
     * @return true if samples exist
     * @since 1.0.0
     */
    public boolean hasSamples() {
        return sampleValues != null && !sampleValues.isEmpty();
    }

    /**
     * Returns a display string for this field.
     *
     * @return the display string
     * @since 1.0.0
     */
    @NotNull
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(type.name().toLowerCase()).append(")");
        if (required) {
            sb.append(" *");
        }
        if (description != null) {
            sb.append(" - ").append(description);
        }
        return sb.toString();
    }

    /**
     * Builder for creating MappableField instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String name;
        private DataSchema.FieldType type = DataSchema.FieldType.STRING;
        private String description;
        private boolean required;
        private String suggestedMapping;
        private List<String> sampleValues;
        private String category;

        private Builder() {}

        /**
         * Creates a new builder.
         *
         * @return a new builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder create() {
            return new Builder();
        }

        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        @NotNull
        public Builder type(@NotNull DataSchema.FieldType type) {
            this.type = type;
            return this;
        }

        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NotNull
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        @NotNull
        public Builder suggestedMapping(@Nullable String suggestedMapping) {
            this.suggestedMapping = suggestedMapping;
            return this;
        }

        @NotNull
        public Builder sampleValues(@Nullable List<String> sampleValues) {
            this.sampleValues = sampleValues;
            return this;
        }

        @NotNull
        public Builder category(@Nullable String category) {
            this.category = category;
            return this;
        }

        @NotNull
        public MappableField build() {
            Objects.requireNonNull(name, "name is required");
            return new MappableField(name, type, description, required,
                    suggestedMapping, sampleValues, category);
        }
    }
}
