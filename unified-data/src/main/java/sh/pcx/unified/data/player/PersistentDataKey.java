/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link DataKey} that persists to the database.
 *
 * <p>Persistent data keys are automatically saved when player data is saved and
 * loaded when player data is loaded from the database. Use this for data that
 * should survive server restarts and player disconnections.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Define persistent keys for your plugin
 * public final class MyPluginData {
 *     public static final PersistentDataKey<Integer> LEVEL =
 *         PersistentDataKey.create("myplugin:level", Integer.class, 1);
 *
 *     public static final PersistentDataKey<Long> EXPERIENCE =
 *         PersistentDataKey.create("myplugin:experience", Long.class, 0L);
 *
 *     public static final PersistentDataKey<String> RANK =
 *         PersistentDataKey.create("myplugin:rank", String.class, "default");
 * }
 *
 * // Usage
 * PlayerProfile profile = playerData.getProfile(player);
 * int level = profile.getData(MyPluginData.LEVEL);
 * profile.setData(MyPluginData.LEVEL, level + 1);
 * // Value will automatically be saved to database
 * }</pre>
 *
 * <h2>Database Storage</h2>
 * <p>Persistent keys are stored in the player data table with the following
 * considerations:
 * <ul>
 *   <li>Simple types (String, Integer, Long, Double, Boolean) are stored directly</li>
 *   <li>Complex objects are serialized to JSON</li>
 *   <li>Lists and Maps are serialized to JSON arrays/objects</li>
 *   <li>Custom types must implement proper serialization</li>
 * </ul>
 *
 * <h2>Sync Behavior</h2>
 * <p>When cross-server synchronization is enabled, changes to persistent keys
 * can be synchronized to other servers based on the configured {@link SyncStrategy}.
 *
 * @param <T> the type of value this key represents
 * @since 1.0.0
 * @author Supatuck
 * @see DataKey
 * @see TransientDataKey
 * @see CrossServerSync
 */
public final class PersistentDataKey<T> extends DataKey<T> {

    private final String tableName;
    private final String columnName;
    private final boolean indexed;
    private final boolean compressed;
    private final SyncStrategy syncStrategy;

    /**
     * Creates a new PersistentDataKey with full configuration.
     *
     * @param key          the unique identifier for this key
     * @param type         the class type of the value
     * @param defaultValue the default value when no value is set
     * @param tableName    the database table name, or null for default
     * @param columnName   the database column name, or null for default
     * @param indexed      whether to create a database index
     * @param compressed   whether to compress stored data
     * @param syncStrategy the cross-server sync strategy
     */
    private PersistentDataKey(@NotNull String key, @NotNull Class<T> type,
                               @Nullable T defaultValue, @Nullable String tableName,
                               @Nullable String columnName, boolean indexed,
                               boolean compressed, @NotNull SyncStrategy syncStrategy) {
        super(key, type, defaultValue, true);
        this.tableName = tableName;
        this.columnName = columnName;
        this.indexed = indexed;
        this.compressed = compressed;
        this.syncStrategy = Objects.requireNonNull(syncStrategy, "syncStrategy must not be null");
    }

    /**
     * Creates a new PersistentDataKey with default settings.
     *
     * <p>Uses the default table, derived column name, no index, no compression,
     * and LAZY sync strategy.
     *
     * @param key          the unique identifier for this key
     * @param type         the class type of the value
     * @param defaultValue the default value when no value is set
     * @param <T>          the type of value
     * @return a new PersistentDataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> PersistentDataKey<T> create(@NotNull String key, @NotNull Class<T> type,
                                                   @Nullable T defaultValue) {
        return new PersistentDataKey<>(key, type, defaultValue, null, null,
                false, false, SyncStrategy.LAZY);
    }

    /**
     * Creates a new PersistentDataKey without a default value.
     *
     * @param key  the unique identifier for this key
     * @param type the class type of the value
     * @param <T>  the type of value
     * @return a new PersistentDataKey instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> PersistentDataKey<T> create(@NotNull String key, @NotNull Class<T> type) {
        return create(key, type, null);
    }

    /**
     * Returns a builder for creating a PersistentDataKey with custom settings.
     *
     * @param key  the unique identifier for this key
     * @param type the class type of the value
     * @param <T>  the type of value
     * @return a new builder instance
     * @throws NullPointerException if key or type is null
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder(@NotNull String key, @NotNull Class<T> type) {
        return new Builder<>(key, type);
    }

    /**
     * Returns the database table name for this key.
     *
     * <p>If null, the default player data table is used.
     *
     * @return the table name, or null for default
     * @since 1.0.0
     */
    @Nullable
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the database column name for this key.
     *
     * <p>If null, the column name is derived from the key identifier.
     *
     * @return the column name, or null for derived
     * @since 1.0.0
     */
    @Nullable
    public String getColumnName() {
        return columnName;
    }

    /**
     * Returns the effective column name for database storage.
     *
     * <p>Returns the explicit column name if set, otherwise derives it from
     * the key name by replacing colons and dashes with underscores.
     *
     * @return the column name to use
     * @since 1.0.0
     */
    @NotNull
    public String getEffectiveColumnName() {
        if (columnName != null) {
            return columnName;
        }
        return getKey().replace(':', '_').replace('-', '_');
    }

    /**
     * Returns whether this key should have a database index.
     *
     * <p>Indexed keys can be queried efficiently but have higher storage
     * and write overhead. Enable indexing for keys used in queries.
     *
     * @return true if indexed
     * @since 1.0.0
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * Returns whether this key's data should be compressed.
     *
     * <p>Compression reduces storage size for large values (like JSON objects)
     * at the cost of CPU time for compression/decompression.
     *
     * @return true if compressed
     * @since 1.0.0
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * Returns the cross-server synchronization strategy for this key.
     *
     * @return the sync strategy
     * @since 1.0.0
     * @see SyncStrategy
     */
    @NotNull
    public SyncStrategy getSyncStrategy() {
        return syncStrategy;
    }

    @Override
    public String toString() {
        return "PersistentDataKey{" +
                "key='" + getKey() + '\'' +
                ", type=" + getType().getSimpleName() +
                ", indexed=" + indexed +
                ", compressed=" + compressed +
                ", syncStrategy=" + syncStrategy +
                '}';
    }

    /**
     * Builder for creating PersistentDataKey instances with custom configuration.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * PersistentDataKey<Integer> SCORE = PersistentDataKey.builder("game:score", Integer.class)
     *     .defaultValue(0)
     *     .indexed(true)
     *     .syncStrategy(SyncStrategy.EAGER)
     *     .build();
     * }</pre>
     *
     * @param <T> the type of value
     * @since 1.0.0
     */
    public static final class Builder<T> {
        private final String key;
        private final Class<T> type;
        private T defaultValue;
        private String tableName;
        private String columnName;
        private boolean indexed = false;
        private boolean compressed = false;
        private SyncStrategy syncStrategy = SyncStrategy.LAZY;

        private Builder(@NotNull String key, @NotNull Class<T> type) {
            this.key = Objects.requireNonNull(key, "key must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");
        }

        /**
         * Sets the default value for this key.
         *
         * @param defaultValue the default value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> defaultValue(@Nullable T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets the database table name for this key.
         *
         * @param tableName the table name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> tableName(@Nullable String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Sets the database column name for this key.
         *
         * @param columnName the column name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> columnName(@Nullable String columnName) {
            this.columnName = columnName;
            return this;
        }

        /**
         * Sets whether to create a database index for this key.
         *
         * @param indexed true to create an index
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> indexed(boolean indexed) {
            this.indexed = indexed;
            return this;
        }

        /**
         * Sets whether to compress stored data.
         *
         * @param compressed true to enable compression
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> compressed(boolean compressed) {
            this.compressed = compressed;
            return this;
        }

        /**
         * Sets the cross-server synchronization strategy.
         *
         * @param syncStrategy the sync strategy
         * @return this builder
         * @throws NullPointerException if syncStrategy is null
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> syncStrategy(@NotNull SyncStrategy syncStrategy) {
            this.syncStrategy = Objects.requireNonNull(syncStrategy, "syncStrategy must not be null");
            return this;
        }

        /**
         * Builds the PersistentDataKey with the configured settings.
         *
         * @return the new PersistentDataKey instance
         * @since 1.0.0
         */
        @NotNull
        public PersistentDataKey<T> build() {
            return new PersistentDataKey<>(key, type, defaultValue, tableName,
                    columnName, indexed, compressed, syncStrategy);
        }
    }
}
