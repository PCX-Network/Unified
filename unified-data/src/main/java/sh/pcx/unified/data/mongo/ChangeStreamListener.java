/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

/**
 * Listener interface for MongoDB change stream events.
 *
 * <p>Implementations of this interface receive notifications when documents
 * in a watched collection are inserted, updated, replaced, or deleted.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a change stream listener
 * ChangeStreamListener<PlayerData> listener = new ChangeStreamListener<>() {
 *     @Override
 *     public void onInsert(ChangeEvent<PlayerData> event) {
 *         PlayerData player = event.getDocument();
 *         logger.info("New player joined: " + player.getName());
 *         // Notify online players, update leaderboard, etc.
 *     }
 *
 *     @Override
 *     public void onUpdate(ChangeEvent<PlayerData> event) {
 *         Document updateDescription = event.getUpdateDescription();
 *         Set<String> updatedFields = updateDescription.get("updatedFields", Document.class).keySet();
 *
 *         if (updatedFields.contains("balance")) {
 *             // Balance was updated, sync with economy plugin
 *         }
 *     }
 *
 *     @Override
 *     public void onDelete(ChangeEvent<PlayerData> event) {
 *         String playerId = event.getDocumentKey().getString("_id");
 *         logger.info("Player data deleted: " + playerId);
 *     }
 *
 *     @Override
 *     public void onError(Throwable error) {
 *         logger.error("Change stream error", error);
 *     }
 * };
 *
 * // Subscribe to changes
 * changeStreamManager.subscribe("players", PlayerData.class, listener);
 * }</pre>
 *
 * <h2>Operation Types</h2>
 * <ul>
 *   <li>{@link #onInsert(ChangeEvent)} - Called when a document is inserted</li>
 *   <li>{@link #onUpdate(ChangeEvent)} - Called when a document is updated (partial)</li>
 *   <li>{@link #onReplace(ChangeEvent)} - Called when a document is replaced (full)</li>
 *   <li>{@link #onDelete(ChangeEvent)} - Called when a document is deleted</li>
 *   <li>{@link #onInvalidate(ChangeEvent)} - Called when the change stream is invalidated</li>
 *   <li>{@link #onDrop(ChangeEvent)} - Called when the collection is dropped</li>
 *   <li>{@link #onRename(ChangeEvent)} - Called when the collection is renamed</li>
 *   <li>{@link #onError(Throwable)} - Called when an error occurs</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as events may be delivered from
 * multiple threads concurrently.
 *
 * @param <T> the document type
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ChangeStreamManager
 * @see ChangeEvent
 */
public interface ChangeStreamListener<T> {

    /**
     * Called when a new document is inserted.
     *
     * @param event the change event containing the inserted document
     * @since 1.0.0
     */
    default void onInsert(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when a document is updated (partial update).
     *
     * <p>Use {@link ChangeEvent#getUpdateDescription()} to get details about
     * which fields were updated.
     *
     * @param event the change event
     * @since 1.0.0
     */
    default void onUpdate(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when a document is replaced (full document replacement).
     *
     * @param event the change event containing the new document
     * @since 1.0.0
     */
    default void onReplace(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when a document is deleted.
     *
     * <p>Use {@link ChangeEvent#getDocumentKey()} to get the deleted document's ID.
     *
     * @param event the change event
     * @since 1.0.0
     */
    default void onDelete(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when the change stream is invalidated.
     *
     * <p>This typically happens when the collection is dropped, renamed, or
     * when the database is dropped. After invalidation, the change stream
     * will be closed.
     *
     * @param event the change event
     * @since 1.0.0
     */
    default void onInvalidate(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when the collection is dropped.
     *
     * @param event the change event
     * @since 1.0.0
     */
    default void onDrop(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when the collection is renamed.
     *
     * @param event the change event
     * @since 1.0.0
     */
    default void onRename(@NotNull ChangeEvent<T> event) {
        // Default implementation does nothing
    }

    /**
     * Called when an error occurs in the change stream.
     *
     * @param error the error that occurred
     * @since 1.0.0
     */
    default void onError(@NotNull Throwable error) {
        // Default implementation does nothing
    }

    /**
     * Called when the change stream is closed.
     *
     * @since 1.0.0
     */
    default void onClose() {
        // Default implementation does nothing
    }

    /**
     * Represents a change stream event.
     *
     * @param <T> the document type
     * @since 1.0.0
     */
    interface ChangeEvent<T> {

        /**
         * Returns the operation type.
         *
         * @return the operation type
         * @since 1.0.0
         */
        @NotNull
        OperationType getOperationType();

        /**
         * Returns the resume token for this event.
         *
         * <p>Can be used to resume the change stream from this point.
         *
         * @return the resume token document
         * @since 1.0.0
         */
        @NotNull
        Document getResumeToken();

        /**
         * Returns the document key (typically contains _id).
         *
         * @return the document key
         * @since 1.0.0
         */
        @NotNull
        Document getDocumentKey();

        /**
         * Returns the full document for insert, replace, and update operations
         * (when fullDocument option is enabled).
         *
         * @return the document, or null if not available
         * @since 1.0.0
         */
        T getDocument();

        /**
         * Returns the update description for update operations.
         *
         * <p>Contains "updatedFields" and "removedFields" documents.
         *
         * @return the update description, or null if not an update
         * @since 1.0.0
         */
        Document getUpdateDescription();

        /**
         * Returns the namespace (database.collection).
         *
         * @return the namespace
         * @since 1.0.0
         */
        @NotNull
        String getNamespace();

        /**
         * Returns the database name.
         *
         * @return the database name
         * @since 1.0.0
         */
        @NotNull
        String getDatabaseName();

        /**
         * Returns the collection name.
         *
         * @return the collection name
         * @since 1.0.0
         */
        @NotNull
        String getCollectionName();

        /**
         * Returns the cluster time of the event.
         *
         * @return the cluster time document
         * @since 1.0.0
         */
        Document getClusterTime();

        /**
         * Returns the wall clock time of the event.
         *
         * @return the wall time in milliseconds
         * @since 1.0.0
         */
        long getWallTime();
    }

    /**
     * Types of change stream operations.
     *
     * @since 1.0.0
     */
    enum OperationType {
        /**
         * Document was inserted.
         */
        INSERT,

        /**
         * Document was updated (partial).
         */
        UPDATE,

        /**
         * Document was replaced (full).
         */
        REPLACE,

        /**
         * Document was deleted.
         */
        DELETE,

        /**
         * Change stream was invalidated.
         */
        INVALIDATE,

        /**
         * Collection was dropped.
         */
        DROP,

        /**
         * Collection was renamed.
         */
        RENAME,

        /**
         * Database was dropped.
         */
        DROP_DATABASE,

        /**
         * Unknown operation type.
         */
        UNKNOWN
    }
}
