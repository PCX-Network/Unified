/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Container for data with version metadata.
 *
 * <p>VersionedData wraps any serializable data with version information,
 * timestamps, and optional metadata. This enables:
 * <ul>
 *   <li>Version tracking for migration</li>
 *   <li>Audit trail with timestamps</li>
 *   <li>Type safety with embedded type information</li>
 *   <li>Checksum validation for data integrity</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create versioned data
 * VersionedData<PlayerStats> versioned = VersionedData.of(
 *     playerStats,
 *     SchemaVersion.of(1, 0)
 * );
 *
 * // Access metadata
 * SchemaVersion version = versioned.getVersion();
 * Instant created = versioned.getCreatedAt();
 *
 * // Serialize with version info
 * String json = serializer.serialize(versioned, context);
 *
 * // Deserialize and check version
 * VersionedData<PlayerStats> loaded = serializer.deserialize(json, context);
 * if (loaded.getVersion().isOlderThan(currentVersion)) {
 *     // Migration needed
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>VersionedData instances are immutable and therefore thread-safe,
 * assuming the wrapped data is also immutable or thread-safe.
 *
 * @param <T> the type of the wrapped data
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchemaVersion
 * @see SchemaMigration
 */
public final class VersionedData<T> {

    private final T data;
    private final SchemaVersion version;
    private final String typeName;
    private final Instant createdAt;
    private final Instant modifiedAt;
    private final String checksum;
    private final String sourceId;

    private VersionedData(Builder<T> builder) {
        this.data = Objects.requireNonNull(builder.data, "data cannot be null");
        this.version = Objects.requireNonNull(builder.version, "version cannot be null");
        this.typeName = builder.typeName != null ? builder.typeName : builder.data.getClass().getName();
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.modifiedAt = builder.modifiedAt != null ? builder.modifiedAt : this.createdAt;
        this.checksum = builder.checksum;
        this.sourceId = builder.sourceId;
    }

    /**
     * Creates versioned data with the current schema version.
     *
     * @param data the data to wrap
     * @param <T>  the data type
     * @return a new VersionedData instance
     * @since 1.0.0
     */
    @NotNull
    public static <T> VersionedData<T> of(@NotNull T data) {
        return new Builder<T>().data(data).version(SchemaVersion.current()).build();
    }

    /**
     * Creates versioned data with a specific version.
     *
     * @param data    the data to wrap
     * @param version the schema version
     * @param <T>     the data type
     * @return a new VersionedData instance
     * @since 1.0.0
     */
    @NotNull
    public static <T> VersionedData<T> of(@NotNull T data, @NotNull SchemaVersion version) {
        return new Builder<T>().data(data).version(version).build();
    }

    /**
     * Creates a new builder for VersionedData.
     *
     * @param <T> the data type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Returns the wrapped data.
     *
     * @return the data
     * @since 1.0.0
     */
    @NotNull
    public T getData() {
        return data;
    }

    /**
     * Returns the schema version.
     *
     * @return the version
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion getVersion() {
        return version;
    }

    /**
     * Returns the type name of the wrapped data.
     *
     * @return the type name
     * @since 1.0.0
     */
    @NotNull
    public String getTypeName() {
        return typeName;
    }

    /**
     * Returns when this data was created.
     *
     * @return the creation timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns when this data was last modified.
     *
     * @return the modification timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getModifiedAt() {
        return modifiedAt;
    }

    /**
     * Returns the data checksum for integrity validation.
     *
     * @return an Optional containing the checksum if present
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getChecksum() {
        return Optional.ofNullable(checksum);
    }

    /**
     * Returns the source identifier (e.g., server name, plugin name).
     *
     * @return an Optional containing the source ID if present
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getSourceId() {
        return Optional.ofNullable(sourceId);
    }

    /**
     * Checks if this data requires migration to the given version.
     *
     * @param targetVersion the target version
     * @return true if migration is required
     * @since 1.0.0
     */
    public boolean requiresMigrationTo(@NotNull SchemaVersion targetVersion) {
        return version.requiresMigrationFrom(targetVersion);
    }

    /**
     * Checks if this data is compatible with the given version.
     *
     * @param targetVersion the target version
     * @return true if compatible
     * @since 1.0.0
     */
    public boolean isCompatibleWith(@NotNull SchemaVersion targetVersion) {
        return targetVersion.isCompatibleWith(version);
    }

    /**
     * Creates a new VersionedData with updated data.
     *
     * @param newData the new data
     * @return a new VersionedData with updated data and modified timestamp
     * @since 1.0.0
     */
    @NotNull
    public VersionedData<T> withData(@NotNull T newData) {
        return new Builder<T>()
                .data(newData)
                .version(version)
                .typeName(typeName)
                .createdAt(createdAt)
                .modifiedAt(Instant.now())
                .sourceId(sourceId)
                .build();
    }

    /**
     * Creates a new VersionedData with updated version.
     *
     * @param newVersion the new version
     * @return a new VersionedData with updated version
     * @since 1.0.0
     */
    @NotNull
    public VersionedData<T> withVersion(@NotNull SchemaVersion newVersion) {
        return new Builder<T>()
                .data(data)
                .version(newVersion)
                .typeName(typeName)
                .createdAt(createdAt)
                .modifiedAt(Instant.now())
                .checksum(checksum)
                .sourceId(sourceId)
                .build();
    }

    /**
     * Transforms the wrapped data using the provided function.
     *
     * @param mapper the transformation function
     * @param <R>    the result type
     * @return a new VersionedData with transformed data
     * @since 1.0.0
     */
    @NotNull
    public <R> VersionedData<R> map(@NotNull java.util.function.Function<T, R> mapper) {
        R transformed = mapper.apply(data);
        return new Builder<R>()
                .data(transformed)
                .version(version)
                .createdAt(createdAt)
                .modifiedAt(Instant.now())
                .sourceId(sourceId)
                .build();
    }

    /**
     * Creates a builder pre-populated with this instance's values.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public Builder<T> toBuilder() {
        return new Builder<T>()
                .data(data)
                .version(version)
                .typeName(typeName)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .checksum(checksum)
                .sourceId(sourceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedData<?> that = (VersionedData<?>) o;
        return Objects.equals(data, that.data) &&
                Objects.equals(version, that.version) &&
                Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, version, typeName);
    }

    @Override
    public String toString() {
        return "VersionedData{" +
                "type=" + typeName +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", modifiedAt=" + modifiedAt +
                '}';
    }

    /**
     * Builder for creating {@link VersionedData} instances.
     *
     * @param <T> the data type
     * @since 1.0.0
     */
    public static final class Builder<T> {
        private T data;
        private SchemaVersion version;
        private String typeName;
        private Instant createdAt;
        private Instant modifiedAt;
        private String checksum;
        private String sourceId;

        private Builder() {}

        /**
         * Sets the data.
         *
         * @param data the data
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> data(@NotNull T data) {
            this.data = data;
            return this;
        }

        /**
         * Sets the schema version.
         *
         * @param version the version
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> version(@NotNull SchemaVersion version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the type name.
         *
         * @param typeName the type name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> typeName(@Nullable String typeName) {
            this.typeName = typeName;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> createdAt(@Nullable Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the modification timestamp.
         *
         * @param modifiedAt the modification timestamp
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> modifiedAt(@Nullable Instant modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        /**
         * Sets the data checksum.
         *
         * @param checksum the checksum
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> checksum(@Nullable String checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * Sets the source identifier.
         *
         * @param sourceId the source ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> sourceId(@Nullable String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        /**
         * Builds the VersionedData instance.
         *
         * @return a new VersionedData
         * @since 1.0.0
         */
        @NotNull
        public VersionedData<T> build() {
            return new VersionedData<>(this);
        }
    }
}
