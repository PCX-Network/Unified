/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing a single audit log entry.
 *
 * <p>An AuditEntry captures all the details of an auditable event, including
 * who performed the action (actor), what was affected (target), when it happened,
 * and what changed (before/after snapshots).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an audit entry for a player balance update
 * AuditEntry entry = AuditEntry.builder()
 *     .action(AuditAction.UPDATE)
 *     .actorId(adminId)
 *     .actorName("AdminPlayer")
 *     .targetType("Economy")
 *     .targetId(playerId.toString())
 *     .beforeSnapshot(Snapshot.of("balance", 100.0))
 *     .afterSnapshot(Snapshot.of("balance", 250.0))
 *     .metadata("reason", "Admin adjustment")
 *     .metadata("ip", "192.168.1.1")
 *     .build();
 *
 * // Query fields
 * String actor = entry.actorName();
 * AuditAction action = entry.action();
 * Map<String, Object> before = entry.beforeSnapshot().getData();
 * }</pre>
 *
 * <h2>Entry Components</h2>
 * <ul>
 *   <li><b>Identity</b> - Unique ID and timestamp</li>
 *   <li><b>Actor</b> - Who performed the action (player, console, system)</li>
 *   <li><b>Action</b> - What type of action was performed</li>
 *   <li><b>Target</b> - What was affected (type and identifier)</li>
 *   <li><b>Snapshots</b> - Before and after state for change tracking</li>
 *   <li><b>Metadata</b> - Additional context (IP, reason, etc.)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This record is immutable and therefore thread-safe. All collections
 * are defensively copied on creation.
 *
 * @param id             the unique entry identifier
 * @param timestamp      when the event occurred
 * @param action         the type of action performed
 * @param actorId        the actor's unique identifier (null for system)
 * @param actorName      the actor's display name
 * @param actorType      the type of actor (PLAYER, CONSOLE, SYSTEM, PLUGIN)
 * @param targetType     the type of target resource
 * @param targetId       the target's unique identifier
 * @param beforeSnapshot the state before the action (null for CREATE)
 * @param afterSnapshot  the state after the action (null for DELETE)
 * @param metadata       additional context information
 * @since 1.0.0
 * @author Supatuck
 * @see AuditAction
 * @see Snapshot
 * @see AuditContext
 */
public record AuditEntry(
        @NotNull UUID id,
        @NotNull Instant timestamp,
        @NotNull AuditAction action,
        @Nullable UUID actorId,
        @NotNull String actorName,
        @NotNull ActorType actorType,
        @NotNull String targetType,
        @NotNull String targetId,
        @Nullable Snapshot beforeSnapshot,
        @Nullable Snapshot afterSnapshot,
        @NotNull Map<String, String> metadata
) {

    /**
     * Types of actors that can perform auditable actions.
     */
    public enum ActorType {
        /** A player performed the action. */
        PLAYER,
        /** The server console performed the action. */
        CONSOLE,
        /** An automated system performed the action. */
        SYSTEM,
        /** A plugin performed the action. */
        PLUGIN
    }

    /**
     * Compact constructor with validation and defensive copying.
     */
    public AuditEntry {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(actorName, "actorName cannot be null");
        Objects.requireNonNull(actorType, "actorType cannot be null");
        Objects.requireNonNull(targetType, "targetType cannot be null");
        Objects.requireNonNull(targetId, "targetId cannot be null");

        // Defensive copy of metadata
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();

        // Validate actor requirements
        if (actorName.isBlank()) {
            throw new IllegalArgumentException("actorName cannot be blank");
        }
        if (targetType.isBlank()) {
            throw new IllegalArgumentException("targetType cannot be blank");
        }
        if (targetId.isBlank()) {
            throw new IllegalArgumentException("targetId cannot be blank");
        }
    }

    /**
     * Checks if this entry has a before snapshot.
     *
     * @return true if a before snapshot exists
     * @since 1.0.0
     */
    public boolean hasBeforeSnapshot() {
        return beforeSnapshot != null;
    }

    /**
     * Checks if this entry has an after snapshot.
     *
     * @return true if an after snapshot exists
     * @since 1.0.0
     */
    public boolean hasAfterSnapshot() {
        return afterSnapshot != null;
    }

    /**
     * Checks if this entry has both before and after snapshots.
     *
     * @return true if both snapshots exist
     * @since 1.0.0
     */
    public boolean hasBothSnapshots() {
        return hasBeforeSnapshot() && hasAfterSnapshot();
    }

    /**
     * Computes the difference between before and after snapshots.
     *
     * @return the snapshot difference, or null if both snapshots are not present
     * @since 1.0.0
     */
    @Nullable
    public SnapshotDiff computeDiff() {
        if (!hasBothSnapshots()) {
            return null;
        }
        return SnapshotDiff.compute(beforeSnapshot, afterSnapshot);
    }

    /**
     * Returns a specific metadata value.
     *
     * @param key the metadata key
     * @return the value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    public String getMetadata(@NotNull String key) {
        return metadata.get(key);
    }

    /**
     * Checks if this entry was performed by a player.
     *
     * @return true if the actor is a player
     * @since 1.0.0
     */
    public boolean isPlayerAction() {
        return actorType == ActorType.PLAYER && actorId != null;
    }

    /**
     * Checks if this entry was performed by the console.
     *
     * @return true if the actor is the console
     * @since 1.0.0
     */
    public boolean isConsoleAction() {
        return actorType == ActorType.CONSOLE;
    }

    /**
     * Checks if this entry was performed by the system.
     *
     * @return true if the actor is the system
     * @since 1.0.0
     */
    public boolean isSystemAction() {
        return actorType == ActorType.SYSTEM;
    }

    /**
     * Generates a human-readable summary of this entry.
     *
     * @return a summary string
     * @since 1.0.0
     */
    @NotNull
    public String toSummary() {
        return action.formatMessage(actorName, targetType, targetId);
    }

    /**
     * Creates a new builder for constructing audit entries.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with this entry's values.
     *
     * @return a new builder with current values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .timestamp(timestamp)
                .action(action)
                .actorId(actorId)
                .actorName(actorName)
                .actorType(actorType)
                .targetType(targetType)
                .targetId(targetId)
                .beforeSnapshot(beforeSnapshot)
                .afterSnapshot(afterSnapshot)
                .metadata(new HashMap<>(metadata));
    }

    /**
     * Builder for creating {@link AuditEntry} instances.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * AuditEntry entry = AuditEntry.builder()
     *     .action(AuditAction.UPDATE)
     *     .actor(player)  // Sets actorId, actorName, actorType
     *     .target("Inventory", inventoryId)
     *     .beforeSnapshot(before)
     *     .afterSnapshot(after)
     *     .metadata("slot", "5")
     *     .build();
     * }</pre>
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private UUID id;
        private Instant timestamp;
        private AuditAction action;
        private UUID actorId;
        private String actorName;
        private ActorType actorType = ActorType.SYSTEM;
        private String targetType;
        private String targetId;
        private Snapshot beforeSnapshot;
        private Snapshot afterSnapshot;
        private Map<String, String> metadata = new HashMap<>();

        private Builder() {}

        /**
         * Sets the unique entry identifier.
         *
         * <p>If not set, a random UUID will be generated.
         *
         * @param id the entry ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder id(@NotNull UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the timestamp of the event.
         *
         * <p>If not set, the current time will be used.
         *
         * @param timestamp the event timestamp
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder timestamp(@NotNull Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the action type.
         *
         * @param action the action type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder action(@NotNull AuditAction action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the actor's unique identifier.
         *
         * @param actorId the actor ID (null for console/system)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder actorId(@Nullable UUID actorId) {
            this.actorId = actorId;
            return this;
        }

        /**
         * Sets the actor's display name.
         *
         * @param actorName the actor name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder actorName(@NotNull String actorName) {
            this.actorName = actorName;
            return this;
        }

        /**
         * Sets the actor type.
         *
         * @param actorType the actor type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder actorType(@NotNull ActorType actorType) {
            this.actorType = actorType;
            return this;
        }

        /**
         * Sets the actor from an AuditContext.
         *
         * @param context the audit context containing actor information
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder actor(@NotNull AuditContext context) {
            this.actorId = context.actorId();
            this.actorName = context.actorName();
            this.actorType = context.actorType();
            return this;
        }

        /**
         * Sets the target type.
         *
         * @param targetType the type of target resource
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder targetType(@NotNull String targetType) {
            this.targetType = targetType;
            return this;
        }

        /**
         * Sets the target identifier.
         *
         * @param targetId the target ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder targetId(@NotNull String targetId) {
            this.targetId = targetId;
            return this;
        }

        /**
         * Sets both target type and ID.
         *
         * @param type the target type
         * @param id   the target ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder target(@NotNull String type, @NotNull String id) {
            this.targetType = type;
            this.targetId = id;
            return this;
        }

        /**
         * Sets the before snapshot.
         *
         * @param snapshot the state before the action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder beforeSnapshot(@Nullable Snapshot snapshot) {
            this.beforeSnapshot = snapshot;
            return this;
        }

        /**
         * Sets the after snapshot.
         *
         * @param snapshot the state after the action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder afterSnapshot(@Nullable Snapshot snapshot) {
            this.afterSnapshot = snapshot;
            return this;
        }

        /**
         * Sets both before and after snapshots.
         *
         * @param before the state before the action
         * @param after  the state after the action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder snapshots(@Nullable Snapshot before, @Nullable Snapshot after) {
            this.beforeSnapshot = before;
            this.afterSnapshot = after;
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key   the metadata key
         * @param value the metadata value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder metadata(@NotNull String key, @NotNull String value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Sets all metadata entries.
         *
         * @param metadata the metadata map
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder metadata(@NotNull Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        /**
         * Builds the audit entry.
         *
         * @return a new AuditEntry instance
         * @throws NullPointerException     if required fields are null
         * @throws IllegalArgumentException if required fields are invalid
         * @since 1.0.0
         */
        @NotNull
        public AuditEntry build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            Objects.requireNonNull(action, "action is required");
            Objects.requireNonNull(actorName, "actorName is required");
            Objects.requireNonNull(targetType, "targetType is required");
            Objects.requireNonNull(targetId, "targetId is required");

            return new AuditEntry(
                    id, timestamp, action, actorId, actorName, actorType,
                    targetType, targetId, beforeSnapshot, afterSnapshot, metadata
            );
        }
    }
}
