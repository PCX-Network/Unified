/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the difference between two snapshots.
 *
 * <p>SnapshotDiff computes and stores the changes between a before and after
 * snapshot, including added, removed, and modified fields.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Snapshot before = Snapshot.of(Map.of("balance", 100.0, "name", "Steve"));
 * Snapshot after = Snapshot.of(Map.of("balance", 250.0, "name", "Steve", "level", 10));
 *
 * SnapshotDiff diff = SnapshotDiff.compute(before, after);
 *
 * // Check what changed
 * Set<String> modified = diff.getModifiedFields();  // ["balance"]
 * Set<String> added = diff.getAddedFields();        // ["level"]
 * Set<String> removed = diff.getRemovedFields();    // []
 *
 * // Get specific changes
 * SnapshotDiff.Change change = diff.getChange("balance");
 * Object oldValue = change.oldValue();  // 100.0
 * Object newValue = change.newValue();  // 250.0
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and therefore thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Snapshot
 * @see AuditEntry
 */
public final class SnapshotDiff {

    private final Set<String> addedFields;
    private final Set<String> removedFields;
    private final Set<String> modifiedFields;
    private final Map<String, Change> changes;

    /**
     * Creates a new SnapshotDiff.
     */
    private SnapshotDiff(
            @NotNull Set<String> addedFields,
            @NotNull Set<String> removedFields,
            @NotNull Set<String> modifiedFields,
            @NotNull Map<String, Change> changes) {
        this.addedFields = Collections.unmodifiableSet(new LinkedHashSet<>(addedFields));
        this.removedFields = Collections.unmodifiableSet(new LinkedHashSet<>(removedFields));
        this.modifiedFields = Collections.unmodifiableSet(new LinkedHashSet<>(modifiedFields));
        this.changes = Collections.unmodifiableMap(new LinkedHashMap<>(changes));
    }

    /**
     * Computes the difference between two snapshots.
     *
     * @param before the before snapshot (can be null for creation)
     * @param after  the after snapshot (can be null for deletion)
     * @return the computed difference
     * @since 1.0.0
     */
    @NotNull
    public static SnapshotDiff compute(@Nullable Snapshot before, @Nullable Snapshot after) {
        Set<String> added = new LinkedHashSet<>();
        Set<String> removed = new LinkedHashSet<>();
        Set<String> modified = new LinkedHashSet<>();
        Map<String, Change> changes = new LinkedHashMap<>();

        Map<String, Object> beforeData = before != null ? before.getData() : Map.of();
        Map<String, Object> afterData = after != null ? after.getData() : Map.of();

        // Find removed and modified fields
        for (Map.Entry<String, Object> entry : beforeData.entrySet()) {
            String key = entry.getKey();
            Object oldValue = entry.getValue();

            if (!afterData.containsKey(key)) {
                removed.add(key);
                changes.put(key, new Change(oldValue, null, ChangeType.REMOVED));
            } else {
                Object newValue = afterData.get(key);
                if (!Objects.equals(oldValue, newValue)) {
                    modified.add(key);
                    changes.put(key, new Change(oldValue, newValue, ChangeType.MODIFIED));
                }
            }
        }

        // Find added fields
        for (Map.Entry<String, Object> entry : afterData.entrySet()) {
            String key = entry.getKey();
            if (!beforeData.containsKey(key)) {
                added.add(key);
                changes.put(key, new Change(null, entry.getValue(), ChangeType.ADDED));
            }
        }

        return new SnapshotDiff(added, removed, modified, changes);
    }

    /**
     * Gets all fields that were added.
     *
     * @return set of added field names
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getAddedFields() {
        return addedFields;
    }

    /**
     * Gets all fields that were removed.
     *
     * @return set of removed field names
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getRemovedFields() {
        return removedFields;
    }

    /**
     * Gets all fields that were modified.
     *
     * @return set of modified field names
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getModifiedFields() {
        return modifiedFields;
    }

    /**
     * Gets all changes.
     *
     * @return map of field name to change
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Change> getChanges() {
        return changes;
    }

    /**
     * Gets the change for a specific field.
     *
     * @param field the field name
     * @return the change, or null if no change
     * @since 1.0.0
     */
    @Nullable
    public Change getChange(@NotNull String field) {
        return changes.get(field);
    }

    /**
     * Checks if there are any changes.
     *
     * @return true if there are changes
     * @since 1.0.0
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Gets the total number of changes.
     *
     * @return the change count
     * @since 1.0.0
     */
    public int getChangeCount() {
        return changes.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotDiff that = (SnapshotDiff) o;
        return Objects.equals(changes, that.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changes);
    }

    @Override
    public String toString() {
        return "SnapshotDiff{added=" + addedFields.size() +
               ", removed=" + removedFields.size() +
               ", modified=" + modifiedFields.size() + "}";
    }

    /**
     * Type of change.
     *
     * @since 1.0.0
     */
    public enum ChangeType {
        /** Field was added. */
        ADDED,
        /** Field was removed. */
        REMOVED,
        /** Field was modified. */
        MODIFIED
    }

    /**
     * Represents a single field change.
     *
     * @param oldValue the old value (null for additions)
     * @param newValue the new value (null for removals)
     * @param type     the type of change
     * @since 1.0.0
     */
    public record Change(
            @Nullable Object oldValue,
            @Nullable Object newValue,
            @NotNull ChangeType type
    ) {
        /**
         * Creates a new change record.
         */
        public Change {
            Objects.requireNonNull(type, "type cannot be null");
        }
    }
}
