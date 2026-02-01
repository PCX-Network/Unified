/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Handles migration of serialized data between schema versions.
 *
 * <p>SchemaMigration provides a framework for upgrading serialized data from
 * older versions to newer versions. Migrations are registered as steps that
 * transform data from one version to another.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a migration registry
 * SchemaMigration<Map<String, Object>> migration = SchemaMigration.create();
 *
 * // Register migration from v1 to v2
 * migration.register(SchemaVersion.of(1), SchemaVersion.of(2), data -> {
 *     // Rename "old_field" to "new_field"
 *     Object value = data.remove("old_field");
 *     if (value != null) {
 *         data.put("new_field", value);
 *     }
 *     return data;
 * });
 *
 * // Register migration from v2 to v3
 * migration.register(SchemaVersion.of(2), SchemaVersion.of(3), data -> {
 *     // Add default value for new required field
 *     data.putIfAbsent("required_field", "default");
 *     return data;
 * });
 *
 * // Migrate data from v1 to v3
 * Map<String, Object> oldData = loadData(); // version 1 data
 * Map<String, Object> newData = migration.migrate(oldData, SchemaVersion.of(1), SchemaVersion.of(3));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>SchemaMigration instances are thread-safe.
 *
 * @param <T> the type of data being migrated
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchemaVersion
 * @see VersionedData
 */
public final class SchemaMigration<T> {

    private final Map<MigrationKey, MigrationStep<T>> migrations = new ConcurrentHashMap<>();
    private final Map<SchemaVersion, List<SchemaVersion>> migrationPaths = new ConcurrentHashMap<>();

    private SchemaMigration() {}

    /**
     * Creates a new SchemaMigration instance.
     *
     * @param <T> the type of data being migrated
     * @return a new SchemaMigration
     * @since 1.0.0
     */
    @NotNull
    public static <T> SchemaMigration<T> create() {
        return new SchemaMigration<>();
    }

    /**
     * Registers a migration step from one version to another.
     *
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param migrator    the migration function
     * @return this SchemaMigration for chaining
     * @throws IllegalArgumentException if toVersion is not newer than fromVersion
     * @since 1.0.0
     */
    @NotNull
    public SchemaMigration<T> register(@NotNull SchemaVersion fromVersion,
                                        @NotNull SchemaVersion toVersion,
                                        @NotNull Function<T, T> migrator) {
        Objects.requireNonNull(fromVersion, "fromVersion cannot be null");
        Objects.requireNonNull(toVersion, "toVersion cannot be null");
        Objects.requireNonNull(migrator, "migrator cannot be null");

        if (!toVersion.isNewerThan(fromVersion)) {
            throw new IllegalArgumentException(
                    "Target version must be newer than source version: " +
                    fromVersion + " -> " + toVersion);
        }

        MigrationKey key = new MigrationKey(fromVersion, toVersion);
        migrations.put(key, new MigrationStep<>(fromVersion, toVersion, migrator));
        migrationPaths.clear(); // Clear cached paths
        return this;
    }

    /**
     * Registers a migration step using a builder pattern.
     *
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @return a MigrationStepBuilder for configuring the migration
     * @since 1.0.0
     */
    @NotNull
    public MigrationStepBuilder from(@NotNull SchemaVersion fromVersion, @NotNull SchemaVersion toVersion) {
        return new MigrationStepBuilder(fromVersion, toVersion);
    }

    /**
     * Migrates data from one version to another.
     *
     * <p>If there is no direct migration, the system will attempt to find
     * a path of intermediate migrations.
     *
     * @param data        the data to migrate
     * @param fromVersion the current version of the data
     * @param toVersion   the target version
     * @return the migrated data
     * @throws SerializationException if no migration path is found
     * @since 1.0.0
     */
    @NotNull
    public T migrate(@NotNull T data, @NotNull SchemaVersion fromVersion,
                     @NotNull SchemaVersion toVersion) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(fromVersion, "fromVersion cannot be null");
        Objects.requireNonNull(toVersion, "toVersion cannot be null");

        if (fromVersion.equals(toVersion)) {
            return data;
        }

        List<MigrationStep<T>> path = findMigrationPath(fromVersion, toVersion);
        if (path.isEmpty()) {
            throw new SerializationException(
                    "No migration path found from " + fromVersion + " to " + toVersion);
        }

        T result = data;
        for (MigrationStep<T> step : path) {
            result = step.apply(result);
        }
        return result;
    }

    /**
     * Attempts to migrate data, returning empty if no path is found.
     *
     * @param data        the data to migrate
     * @param fromVersion the current version of the data
     * @param toVersion   the target version
     * @return an Optional containing the migrated data, or empty if migration failed
     * @since 1.0.0
     */
    @NotNull
    public Optional<T> tryMigrate(@NotNull T data, @NotNull SchemaVersion fromVersion,
                                   @NotNull SchemaVersion toVersion) {
        try {
            return Optional.of(migrate(data, fromVersion, toVersion));
        } catch (SerializationException e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if a migration path exists between two versions.
     *
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @return true if a migration path exists
     * @since 1.0.0
     */
    public boolean canMigrate(@NotNull SchemaVersion fromVersion, @NotNull SchemaVersion toVersion) {
        if (fromVersion.equals(toVersion)) {
            return true;
        }
        return !findMigrationPath(fromVersion, toVersion).isEmpty();
    }

    /**
     * Returns the migration path between two versions.
     *
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @return the list of versions in the migration path (empty if no path exists)
     * @since 1.0.0
     */
    @NotNull
    public List<SchemaVersion> getMigrationPath(@NotNull SchemaVersion fromVersion,
                                                  @NotNull SchemaVersion toVersion) {
        if (fromVersion.equals(toVersion)) {
            return Collections.singletonList(fromVersion);
        }

        List<MigrationStep<T>> steps = findMigrationPath(fromVersion, toVersion);
        if (steps.isEmpty()) {
            return Collections.emptyList();
        }

        List<SchemaVersion> path = new ArrayList<>();
        path.add(fromVersion);
        for (MigrationStep<T> step : steps) {
            path.add(step.toVersion());
        }
        return Collections.unmodifiableList(path);
    }

    /**
     * Returns all registered migrations.
     *
     * @return an unmodifiable map of migration keys to steps
     * @since 1.0.0
     */
    @NotNull
    public Map<MigrationKey, MigrationStep<T>> getMigrations() {
        return Collections.unmodifiableMap(migrations);
    }

    /**
     * Returns the latest version that can be migrated to from a given version.
     *
     * @param fromVersion the source version
     * @return the latest reachable version
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion getLatestMigratableVersion(@NotNull SchemaVersion fromVersion) {
        SchemaVersion latest = fromVersion;

        for (MigrationKey key : migrations.keySet()) {
            if (key.fromVersion().equals(latest)) {
                SchemaVersion candidate = getLatestMigratableVersion(key.toVersion());
                if (candidate.isNewerThan(latest)) {
                    latest = candidate;
                }
            }
        }

        return latest;
    }

    private List<MigrationStep<T>> findMigrationPath(SchemaVersion from, SchemaVersion to) {
        // Check for direct migration first
        MigrationKey directKey = new MigrationKey(from, to);
        MigrationStep<T> direct = migrations.get(directKey);
        if (direct != null) {
            return Collections.singletonList(direct);
        }

        // BFS to find the shortest path
        Map<SchemaVersion, MigrationStep<T>> visited = new HashMap<>();
        List<SchemaVersion> queue = new ArrayList<>();
        queue.add(from);
        visited.put(from, null);

        while (!queue.isEmpty()) {
            SchemaVersion current = queue.remove(0);

            for (MigrationStep<T> step : migrations.values()) {
                if (step.fromVersion().equals(current) && !visited.containsKey(step.toVersion())) {
                    visited.put(step.toVersion(), step);

                    if (step.toVersion().equals(to)) {
                        // Reconstruct path
                        List<MigrationStep<T>> path = new ArrayList<>();
                        SchemaVersion v = to;
                        while (visited.get(v) != null) {
                            MigrationStep<T> s = visited.get(v);
                            path.add(0, s);
                            v = s.fromVersion();
                        }
                        return path;
                    }

                    queue.add(step.toVersion());
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Key for identifying a migration from one version to another.
     *
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @since 1.0.0
     */
    public record MigrationKey(
            @NotNull SchemaVersion fromVersion,
            @NotNull SchemaVersion toVersion
    ) implements Comparable<MigrationKey> {

        @Override
        public int compareTo(@NotNull MigrationKey other) {
            int result = this.fromVersion.compareTo(other.fromVersion);
            if (result != 0) return result;
            return this.toVersion.compareTo(other.toVersion);
        }
    }

    /**
     * A single migration step from one version to another.
     *
     * @param <T> the type of data being migrated
     * @since 1.0.0
     */
    public record MigrationStep<T>(
            @NotNull SchemaVersion fromVersion,
            @NotNull SchemaVersion toVersion,
            @NotNull Function<T, T> migrator
    ) {

        /**
         * Applies this migration to the given data.
         *
         * @param data the data to migrate
         * @return the migrated data
         * @since 1.0.0
         */
        @NotNull
        public T apply(@NotNull T data) {
            try {
                return migrator.apply(data);
            } catch (Exception e) {
                throw new SerializationException(
                        "Migration from " + fromVersion + " to " + toVersion + " failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Builder for configuring a migration step.
     *
     * @since 1.0.0
     */
    public final class MigrationStepBuilder {
        private final SchemaVersion fromVersion;
        private final SchemaVersion toVersion;
        private String description;

        private MigrationStepBuilder(SchemaVersion fromVersion, SchemaVersion toVersion) {
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }

        /**
         * Sets a description for this migration step.
         *
         * @param description the description
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public MigrationStepBuilder description(@NotNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Registers the migration with the given migrator function.
         *
         * @param migrator the migration function
         * @return the parent SchemaMigration for chaining
         * @since 1.0.0
         */
        @NotNull
        public SchemaMigration<T> with(@NotNull Function<T, T> migrator) {
            return SchemaMigration.this.register(fromVersion, toVersion, migrator);
        }
    }
}
