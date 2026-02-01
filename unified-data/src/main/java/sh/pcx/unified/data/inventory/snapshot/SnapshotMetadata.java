/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.snapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record containing metadata about an inventory snapshot.
 *
 * <p>SnapshotMetadata provides information about a snapshot without including
 * the actual inventory contents. This is useful for listing snapshots, displaying
 * information to users, or filtering snapshots by criteria.
 *
 * <h2>Metadata Fields</h2>
 * <ul>
 *   <li><b>snapshotId</b>: Unique identifier for the snapshot</li>
 *   <li><b>playerId</b>: UUID of the player who owns the snapshot</li>
 *   <li><b>name</b>: Optional friendly name (e.g., "death_backup")</li>
 *   <li><b>createdAt</b>: When the snapshot was created</li>
 *   <li><b>version</b>: Incremental version number</li>
 *   <li><b>itemCount</b>: Total number of items in the snapshot</li>
 *   <li><b>reason</b>: Why the snapshot was created (e.g., "death", "manual")</li>
 *   <li><b>serverName</b>: Server where the snapshot was created</li>
 *   <li><b>worldName</b>: World where the snapshot was created</li>
 *   <li><b>tags</b>: Optional key-value tags for categorization</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create metadata
 * SnapshotMetadata metadata = new SnapshotMetadata(
 *     UUID.randomUUID().toString(),
 *     playerId,
 *     "death_backup",
 *     Instant.now(),
 *     1,
 *     45,
 *     "death",
 *     "survival",
 *     "world",
 *     Map.of("cause", "lava")
 * );
 *
 * // Access metadata
 * String id = metadata.snapshotId();
 * int items = metadata.itemCount();
 * String reason = metadata.reason();
 * }</pre>
 *
 * @param snapshotId the unique snapshot identifier
 * @param playerId   the player's UUID
 * @param name       optional friendly name
 * @param createdAt  creation timestamp
 * @param version    version number
 * @param itemCount  total items in snapshot
 * @param reason     reason for snapshot creation
 * @param serverName server where created
 * @param worldName  world where created
 * @param tags       optional metadata tags
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SnapshotManager
 */
public record SnapshotMetadata(
    @NotNull String snapshotId,
    @NotNull UUID playerId,
    @Nullable String name,
    @NotNull Instant createdAt,
    int version,
    int itemCount,
    @Nullable String reason,
    @Nullable String serverName,
    @Nullable String worldName,
    @NotNull Map<String, String> tags
) {

    /**
     * Reason constant for snapshots created on player death.
     */
    public static final String REASON_DEATH = "death";

    /**
     * Reason constant for snapshots created manually by command.
     */
    public static final String REASON_MANUAL = "manual";

    /**
     * Reason constant for snapshots created on server shutdown.
     */
    public static final String REASON_SHUTDOWN = "shutdown";

    /**
     * Reason constant for snapshots created before world change.
     */
    public static final String REASON_WORLD_CHANGE = "world_change";

    /**
     * Reason constant for snapshots created before server transfer.
     */
    public static final String REASON_TRANSFER = "transfer";

    /**
     * Reason constant for automatic periodic backups.
     */
    public static final String REASON_AUTO = "auto";

    /**
     * Reason constant for snapshots created by plugins.
     */
    public static final String REASON_PLUGIN = "plugin";

    /**
     * Creates a SnapshotMetadata with validation.
     */
    public SnapshotMetadata {
        Objects.requireNonNull(snapshotId, "Snapshot ID cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(tags, "Tags cannot be null");

        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative: " + version);
        }
        if (itemCount < 0) {
            throw new IllegalArgumentException("Item count cannot be negative: " + itemCount);
        }

        // Make tags immutable
        tags = Map.copyOf(tags);
    }

    /**
     * Creates minimal metadata with required fields only.
     *
     * @param snapshotId the snapshot ID
     * @param playerId   the player's UUID
     * @param createdAt  the creation timestamp
     * @return new SnapshotMetadata instance
     * @since 1.0.0
     */
    @NotNull
    public static SnapshotMetadata minimal(
        @NotNull String snapshotId,
        @NotNull UUID playerId,
        @NotNull Instant createdAt
    ) {
        return new SnapshotMetadata(
            snapshotId, playerId, null, createdAt,
            1, 0, null, null, null, Map.of()
        );
    }

    /**
     * Creates a new builder for SnapshotMetadata.
     *
     * @param snapshotId the snapshot ID
     * @param playerId   the player's UUID
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull String snapshotId, @NotNull UUID playerId) {
        return new Builder(snapshotId, playerId);
    }

    /**
     * Checks if this snapshot has a name.
     *
     * @return true if name is set
     * @since 1.0.0
     */
    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    /**
     * Gets the display name (name or snapshot ID if no name).
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return hasName() ? name : snapshotId;
    }

    /**
     * Checks if this snapshot has a specific tag.
     *
     * @param key the tag key
     * @return true if the tag exists
     * @since 1.0.0
     */
    public boolean hasTag(@NotNull String key) {
        return tags.containsKey(key);
    }

    /**
     * Gets a tag value.
     *
     * @param key the tag key
     * @return the tag value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    public String getTag(@NotNull String key) {
        return tags.get(key);
    }

    /**
     * Gets a tag value with a default.
     *
     * @param key          the tag key
     * @param defaultValue the default value
     * @return the tag value, or defaultValue if not present
     * @since 1.0.0
     */
    @NotNull
    public String getTag(@NotNull String key, @NotNull String defaultValue) {
        return tags.getOrDefault(key, defaultValue);
    }

    /**
     * Creates a copy with a new name.
     *
     * @param newName the new name
     * @return a new SnapshotMetadata with the updated name
     * @since 1.0.0
     */
    @NotNull
    public SnapshotMetadata withName(@Nullable String newName) {
        return new SnapshotMetadata(
            snapshotId, playerId, newName, createdAt,
            version, itemCount, reason, serverName, worldName, tags
        );
    }

    /**
     * Creates a copy with an additional tag.
     *
     * @param key   the tag key
     * @param value the tag value
     * @return a new SnapshotMetadata with the added tag
     * @since 1.0.0
     */
    @NotNull
    public SnapshotMetadata withTag(@NotNull String key, @NotNull String value) {
        var newTags = new java.util.HashMap<>(tags);
        newTags.put(key, value);
        return new SnapshotMetadata(
            snapshotId, playerId, name, createdAt,
            version, itemCount, reason, serverName, worldName, newTags
        );
    }

    /**
     * Returns age of this snapshot.
     *
     * @return duration since creation
     * @since 1.0.0
     */
    @NotNull
    public java.time.Duration getAge() {
        return java.time.Duration.between(createdAt, Instant.now());
    }

    /**
     * Builder for SnapshotMetadata.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final String snapshotId;
        private final UUID playerId;
        private String name;
        private Instant createdAt = Instant.now();
        private int version = 1;
        private int itemCount = 0;
        private String reason;
        private String serverName;
        private String worldName;
        private Map<String, String> tags = Map.of();

        private Builder(@NotNull String snapshotId, @NotNull UUID playerId) {
            this.snapshotId = Objects.requireNonNull(snapshotId);
            this.playerId = Objects.requireNonNull(playerId);
        }

        /**
         * Sets the snapshot name.
         *
         * @param name the name
         * @return this builder
         */
        @NotNull
        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the timestamp
         * @return this builder
         */
        @NotNull
        public Builder createdAt(@NotNull Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the version number.
         *
         * @param version the version
         * @return this builder
         */
        @NotNull
        public Builder version(int version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the item count.
         *
         * @param itemCount the item count
         * @return this builder
         */
        @NotNull
        public Builder itemCount(int itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        /**
         * Sets the creation reason.
         *
         * @param reason the reason
         * @return this builder
         */
        @NotNull
        public Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * Sets the server name.
         *
         * @param serverName the server name
         * @return this builder
         */
        @NotNull
        public Builder serverName(@Nullable String serverName) {
            this.serverName = serverName;
            return this;
        }

        /**
         * Sets the world name.
         *
         * @param worldName the world name
         * @return this builder
         */
        @NotNull
        public Builder worldName(@Nullable String worldName) {
            this.worldName = worldName;
            return this;
        }

        /**
         * Sets the tags.
         *
         * @param tags the tags
         * @return this builder
         */
        @NotNull
        public Builder tags(@NotNull Map<String, String> tags) {
            this.tags = Map.copyOf(tags);
            return this;
        }

        /**
         * Adds a single tag.
         *
         * @param key   the tag key
         * @param value the tag value
         * @return this builder
         */
        @NotNull
        public Builder tag(@NotNull String key, @NotNull String value) {
            var newTags = new java.util.HashMap<>(this.tags);
            newTags.put(key, value);
            this.tags = newTags;
            return this;
        }

        /**
         * Builds the SnapshotMetadata.
         *
         * @return the new SnapshotMetadata
         */
        @NotNull
        public SnapshotMetadata build() {
            return new SnapshotMetadata(
                snapshotId, playerId, name, createdAt,
                version, itemCount, reason, serverName, worldName, tags
            );
        }
    }
}
