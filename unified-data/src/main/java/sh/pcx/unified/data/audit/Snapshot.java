/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a snapshot of data state for audit purposes.
 *
 * <p>A Snapshot captures the state of a data object at a specific point in time,
 * allowing comparison between before and after states to determine what changed.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a snapshot
 * Snapshot before = Snapshot.of("balance", 100.0);
 * Snapshot after = Snapshot.of("balance", 250.0);
 *
 * // Create from a map
 * Map<String, Object> data = Map.of(
 *     "name", "Steve",
 *     "level", 10,
 *     "health", 20.0
 * );
 * Snapshot snapshot = Snapshot.of(data);
 *
 * // Access data
 * Object balance = snapshot.get("balance");
 * Map<String, Object> allData = snapshot.getData();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and therefore thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SnapshotDiff
 * @see AuditEntry
 */
public final class Snapshot {

    private final Map<String, Object> data;

    /**
     * Creates a snapshot with the given data.
     *
     * @param data the snapshot data
     */
    private Snapshot(@NotNull Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    /**
     * Creates an empty snapshot.
     *
     * @return an empty snapshot
     * @since 1.0.0
     */
    @NotNull
    public static Snapshot empty() {
        return new Snapshot(Map.of());
    }

    /**
     * Creates a snapshot with a single key-value pair.
     *
     * @param key   the key
     * @param value the value
     * @return a new snapshot
     * @since 1.0.0
     */
    @NotNull
    public static Snapshot of(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(key, value);
        return new Snapshot(data);
    }

    /**
     * Creates a snapshot from a map of values.
     *
     * @param data the data map
     * @return a new snapshot
     * @since 1.0.0
     */
    @NotNull
    public static Snapshot of(@NotNull Map<String, Object> data) {
        Objects.requireNonNull(data, "data cannot be null");
        return new Snapshot(data);
    }

    /**
     * Gets the value for a specific key.
     *
     * @param key the key
     * @return the value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    public Object get(@NotNull String key) {
        return data.get(key);
    }

    /**
     * Checks if this snapshot contains a key.
     *
     * @param key the key to check
     * @return true if the key is present
     * @since 1.0.0
     */
    public boolean containsKey(@NotNull String key) {
        return data.containsKey(key);
    }

    /**
     * Gets all data in this snapshot.
     *
     * @return an unmodifiable map of the data
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Checks if this snapshot is empty.
     *
     * @return true if empty
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Gets the number of entries in this snapshot.
     *
     * @return the size
     * @since 1.0.0
     */
    public int size() {
        return data.size();
    }

    /**
     * Creates a new builder for constructing snapshots.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snapshot snapshot = (Snapshot) o;
        return Objects.equals(data, snapshot.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "Snapshot{data=" + data + "}";
    }

    /**
     * Builder for creating Snapshot instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final Map<String, Object> data = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Adds a key-value pair to the snapshot.
         *
         * @param key   the key
         * @param value the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder put(@NotNull String key, @Nullable Object value) {
            data.put(key, value);
            return this;
        }

        /**
         * Adds all entries from a map.
         *
         * @param entries the entries to add
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder putAll(@NotNull Map<String, Object> entries) {
            data.putAll(entries);
            return this;
        }

        /**
         * Builds the snapshot.
         *
         * @return a new Snapshot
         * @since 1.0.0
         */
        @NotNull
        public Snapshot build() {
            return new Snapshot(data);
        }
    }
}
