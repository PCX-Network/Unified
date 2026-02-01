/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages MongoDB change stream subscriptions for real-time updates.
 *
 * <p>This manager provides a simplified interface for subscribing to MongoDB
 * change streams, which allow applications to receive real-time notifications
 * when data changes in the database.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the change stream manager
 * ChangeStreamManager manager = mongoService.getChangeStreamManager();
 *
 * // Subscribe to all changes on a collection
 * long subscriptionId = manager.subscribe("players", Document.class, event -> {
 *     switch (event.getOperationType()) {
 *         case INSERT -> logger.info("Player joined: " + event.getDocument().getString("name"));
 *         case UPDATE -> logger.info("Player updated: " + event.getDocumentKey());
 *         case DELETE -> logger.info("Player left: " + event.getDocumentKey());
 *     }
 * });
 *
 * // Subscribe with a filter
 * long subscriptionId = manager.subscribe("players", Document.class,
 *     ChangeStreamOptions.builder()
 *         .filter(Filters.eq("operationType", "insert"))
 *         .fullDocument(FullDocument.UPDATE_LOOKUP)
 *         .build(),
 *     listener);
 *
 * // Subscribe to specific document changes
 * long subscriptionId = manager.subscribeToDocument("players", playerId, listener);
 *
 * // Unsubscribe when done
 * manager.unsubscribe(subscriptionId);
 *
 * // Close all subscriptions on shutdown
 * manager.close();
 * }</pre>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>MongoDB must be running as a replica set (even single node)</li>
 *   <li>The database must use WiredTiger storage engine</li>
 *   <li>User must have read permissions on the collection</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Subscriptions can be added and removed
 * from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ChangeStreamListener
 * @see MongoService#getChangeStreamManager()
 */
public class ChangeStreamManager implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeStreamManager.class);

    private final MongoConnection connection;
    private final Map<Long, ManagedSubscription<?>> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong subscriptionIdGenerator = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new change stream manager.
     *
     * @param connection the MongoDB connection
     * @since 1.0.0
     */
    public ChangeStreamManager(@NotNull MongoConnection connection) {
        this.connection = Objects.requireNonNull(connection, "Connection cannot be null");
    }

    // ===========================================
    // Collection-Level Subscriptions
    // ===========================================

    /**
     * Subscribes to changes on a collection.
     *
     * @param collectionName the collection name
     * @param documentClass  the document class
     * @param listener       the change listener
     * @param <T>            the document type
     * @return the subscription ID
     * @since 1.0.0
     */
    public <T> long subscribe(
            @NotNull String collectionName,
            @NotNull Class<T> documentClass,
            @NotNull ChangeStreamListener<T> listener
    ) {
        return subscribe(collectionName, documentClass, ChangeStreamOptions.defaults(), listener);
    }

    /**
     * Subscribes to changes on a collection with options.
     *
     * @param collectionName the collection name
     * @param documentClass  the document class
     * @param options        the subscription options
     * @param listener       the change listener
     * @param <T>            the document type
     * @return the subscription ID
     * @since 1.0.0
     */
    public <T> long subscribe(
            @NotNull String collectionName,
            @NotNull Class<T> documentClass,
            @NotNull ChangeStreamOptions options,
            @NotNull ChangeStreamListener<T> listener
    ) {
        ensureNotClosed();
        Objects.requireNonNull(collectionName, "Collection name cannot be null");
        Objects.requireNonNull(documentClass, "Document class cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");

        MongoCollection<T> collection = connection.getCollection(collectionName, documentClass);
        ChangeStreamPublisher<T> publisher;
        if (options.getPipeline().isEmpty()) {
            publisher = collection.watch(documentClass);
        } else {
            publisher = collection.watch(options.getPipeline(), documentClass);
        }
        publisher = createPublisher(publisher, options);

        return startSubscription(collectionName, documentClass, publisher, listener);
    }

    /**
     * Subscribes to changes on a specific document.
     *
     * @param collectionName the collection name
     * @param documentId     the document ID to watch
     * @param documentClass  the document class
     * @param listener       the change listener
     * @param <T>            the document type
     * @return the subscription ID
     * @since 1.0.0
     */
    public <T> long subscribeToDocument(
            @NotNull String collectionName,
            @NotNull Object documentId,
            @NotNull Class<T> documentClass,
            @NotNull ChangeStreamListener<T> listener
    ) {
        return subscribe(collectionName, documentClass,
                ChangeStreamOptions.builder()
                        .filter(Filters.eq("documentKey._id", documentId))
                        .fullDocument(FullDocument.UPDATE_LOOKUP)
                        .build(),
                listener);
    }

    // ===========================================
    // Database-Level Subscriptions
    // ===========================================

    /**
     * Subscribes to changes on all collections in the database.
     *
     * @param listener the change listener
     * @return the subscription ID
     * @since 1.0.0
     */
    public long subscribeToDatabase(@NotNull ChangeStreamListener<Document> listener) {
        return subscribeToDatabase(ChangeStreamOptions.defaults(), listener);
    }

    /**
     * Subscribes to changes on all collections in the database with options.
     *
     * @param options  the subscription options
     * @param listener the change listener
     * @return the subscription ID
     * @since 1.0.0
     */
    public long subscribeToDatabase(
            @NotNull ChangeStreamOptions options,
            @NotNull ChangeStreamListener<Document> listener
    ) {
        ensureNotClosed();
        Objects.requireNonNull(options, "Options cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");

        MongoDatabase database = connection.getDatabase();
        ChangeStreamPublisher<Document> publisher = createPublisher(database.watch(), options);

        return startSubscription("*", Document.class, publisher, listener);
    }

    // ===========================================
    // Subscription Management
    // ===========================================

    /**
     * Unsubscribes from a change stream.
     *
     * @param subscriptionId the subscription ID
     * @return true if the subscription was found and cancelled
     * @since 1.0.0
     */
    public boolean unsubscribe(long subscriptionId) {
        ManagedSubscription<?> subscription = subscriptions.remove(subscriptionId);
        if (subscription != null) {
            subscription.cancel();
            LOGGER.debug("Unsubscribed from change stream: {} on '{}'",
                    subscriptionId, subscription.collectionName);
            return true;
        }
        return false;
    }

    /**
     * Checks if a subscription is active.
     *
     * @param subscriptionId the subscription ID
     * @return true if the subscription is active
     * @since 1.0.0
     */
    public boolean isActive(long subscriptionId) {
        ManagedSubscription<?> subscription = subscriptions.get(subscriptionId);
        return subscription != null && subscription.isActive();
    }

    /**
     * Returns the number of active subscriptions.
     *
     * @return the subscription count
     * @since 1.0.0
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }

    /**
     * Returns all active subscription IDs.
     *
     * @return the subscription IDs
     * @since 1.0.0
     */
    @NotNull
    public List<Long> getSubscriptionIds() {
        return new ArrayList<>(subscriptions.keySet());
    }

    /**
     * Checks if the manager is closed.
     *
     * @return true if closed
     * @since 1.0.0
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes all subscriptions.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOGGER.info("Closing {} change stream subscription(s)", subscriptions.size());

            for (ManagedSubscription<?> subscription : subscriptions.values()) {
                try {
                    subscription.cancel();
                } catch (Exception e) {
                    LOGGER.error("Error cancelling subscription", e);
                }
            }

            subscriptions.clear();
            LOGGER.info("All change stream subscriptions closed");
        }
    }

    // ===========================================
    // Internal Methods
    // ===========================================

    private <T> ChangeStreamPublisher<T> createPublisher(
            ChangeStreamPublisher<T> publisher,
            ChangeStreamOptions options
    ) {
        if (options.getFullDocument() != null) {
            publisher = publisher.fullDocument(options.getFullDocument());
        }

        // Note: Pipeline must be specified when calling watch(), not after
        // The publisher already has the pipeline if it was provided

        if (options.getResumeToken() != null) {
            publisher = publisher.resumeAfter(options.getResumeToken());
        }

        if (options.getBatchSize() > 0) {
            publisher = publisher.batchSize(options.getBatchSize());
        }

        return publisher;
    }

    private <T> long startSubscription(
            String collectionName,
            Class<T> documentClass,
            ChangeStreamPublisher<T> publisher,
            ChangeStreamListener<T> listener
    ) {
        long subscriptionId = subscriptionIdGenerator.incrementAndGet();

        ManagedSubscription<T> managedSubscription = new ManagedSubscription<>(
                subscriptionId,
                collectionName,
                documentClass,
                listener
        );

        subscriptions.put(subscriptionId, managedSubscription);

        publisher.subscribe(managedSubscription);

        LOGGER.debug("Started change stream subscription: {} on '{}'", subscriptionId, collectionName);
        return subscriptionId;
    }

    private void ensureNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("ChangeStreamManager is closed");
        }
    }

    // ===========================================
    // Internal Classes
    // ===========================================

    /**
     * Internal class to manage a single subscription.
     */
    private class ManagedSubscription<T> implements Subscriber<ChangeStreamDocument<T>> {
        private final long subscriptionId;
        private final String collectionName;
        private final Class<T> documentClass;
        private final ChangeStreamListener<T> listener;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private volatile Subscription subscription;

        ManagedSubscription(
                long subscriptionId,
                String collectionName,
                Class<T> documentClass,
                ChangeStreamListener<T> listener
        ) {
            this.subscriptionId = subscriptionId;
            this.collectionName = collectionName;
            this.documentClass = documentClass;
            this.listener = listener;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ChangeStreamDocument<T> document) {
            if (!active.get()) return;

            try {
                ChangeStreamEventImpl<T> event = new ChangeStreamEventImpl<>(document);

                switch (event.getOperationType()) {
                    case INSERT -> listener.onInsert(event);
                    case UPDATE -> listener.onUpdate(event);
                    case REPLACE -> listener.onReplace(event);
                    case DELETE -> listener.onDelete(event);
                    case INVALIDATE -> listener.onInvalidate(event);
                    case DROP -> listener.onDrop(event);
                    case RENAME -> listener.onRename(event);
                    default -> { /* Ignore unknown operations */ }
                }
            } catch (Exception e) {
                LOGGER.error("Error processing change stream event", e);
                listener.onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (active.get()) {
                LOGGER.error("Change stream error on '{}'", collectionName, t);
                listener.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (active.compareAndSet(true, false)) {
                LOGGER.debug("Change stream completed: {} on '{}'", subscriptionId, collectionName);
                listener.onClose();
                subscriptions.remove(subscriptionId);
            }
        }

        void cancel() {
            if (active.compareAndSet(true, false)) {
                if (subscription != null) {
                    subscription.cancel();
                }
                listener.onClose();
            }
        }

        boolean isActive() {
            return active.get();
        }
    }

    /**
     * Implementation of ChangeEvent.
     */
    private static class ChangeStreamEventImpl<T> implements ChangeStreamListener.ChangeEvent<T> {
        private final ChangeStreamDocument<T> document;
        private final ChangeStreamListener.OperationType operationType;

        ChangeStreamEventImpl(ChangeStreamDocument<T> document) {
            this.document = document;
            this.operationType = mapOperationType(document.getOperationTypeString());
        }

        @Override
        @NotNull
        public ChangeStreamListener.OperationType getOperationType() {
            return operationType;
        }

        @Override
        @NotNull
        public Document getResumeToken() {
            BsonDocument token = document.getResumeToken();
            return token != null ? Document.parse(token.toJson()) : new Document();
        }

        @Override
        @NotNull
        public Document getDocumentKey() {
            BsonDocument key = document.getDocumentKey();
            return key != null ? Document.parse(key.toJson()) : new Document();
        }

        @Override
        public T getDocument() {
            return document.getFullDocument();
        }

        @Override
        public Document getUpdateDescription() {
            var desc = document.getUpdateDescription();
            if (desc == null) return null;

            Document result = new Document();
            if (desc.getUpdatedFields() != null) {
                result.append("updatedFields", Document.parse(desc.getUpdatedFields().toJson()));
            }
            if (desc.getRemovedFields() != null) {
                result.append("removedFields", desc.getRemovedFields());
            }
            return result;
        }

        @Override
        @NotNull
        public String getNamespace() {
            var ns = document.getNamespace();
            return ns != null ? ns.getFullName() : "";
        }

        @Override
        @NotNull
        public String getDatabaseName() {
            var ns = document.getNamespace();
            return ns != null ? ns.getDatabaseName() : "";
        }

        @Override
        @NotNull
        public String getCollectionName() {
            var ns = document.getNamespace();
            return ns != null ? ns.getCollectionName() : "";
        }

        @Override
        public Document getClusterTime() {
            var ct = document.getClusterTime();
            return ct != null ? new Document("clusterTime", ct) : null;
        }

        @Override
        public long getWallTime() {
            var wt = document.getWallTime();
            return wt != null ? wt.getValue() : 0;
        }

        private static ChangeStreamListener.OperationType mapOperationType(String type) {
            if (type == null) return ChangeStreamListener.OperationType.UNKNOWN;
            return switch (type.toLowerCase()) {
                case "insert" -> ChangeStreamListener.OperationType.INSERT;
                case "update" -> ChangeStreamListener.OperationType.UPDATE;
                case "replace" -> ChangeStreamListener.OperationType.REPLACE;
                case "delete" -> ChangeStreamListener.OperationType.DELETE;
                case "invalidate" -> ChangeStreamListener.OperationType.INVALIDATE;
                case "drop" -> ChangeStreamListener.OperationType.DROP;
                case "rename" -> ChangeStreamListener.OperationType.RENAME;
                case "dropdatabase" -> ChangeStreamListener.OperationType.DROP_DATABASE;
                default -> ChangeStreamListener.OperationType.UNKNOWN;
            };
        }
    }

    /**
     * Options for change stream subscriptions.
     *
     * @since 1.0.0
     */
    public static class ChangeStreamOptions {
        private final FullDocument fullDocument;
        private final List<Bson> pipeline;
        private final BsonDocument resumeToken;
        private final int batchSize;

        private ChangeStreamOptions(Builder builder) {
            this.fullDocument = builder.fullDocument;
            this.pipeline = new ArrayList<>(builder.pipeline);
            this.resumeToken = builder.resumeToken;
            this.batchSize = builder.batchSize;
        }

        /**
         * Returns the default options.
         *
         * @return default options with UPDATE_LOOKUP full document
         */
        @NotNull
        public static ChangeStreamOptions defaults() {
            return builder()
                    .fullDocument(FullDocument.UPDATE_LOOKUP)
                    .build();
        }

        /**
         * Creates a new options builder.
         *
         * @return a new builder
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        @Nullable
        public FullDocument getFullDocument() {
            return fullDocument;
        }

        @NotNull
        public List<Bson> getPipeline() {
            return pipeline;
        }

        @Nullable
        public BsonDocument getResumeToken() {
            return resumeToken;
        }

        public int getBatchSize() {
            return batchSize;
        }

        /**
         * Builder for ChangeStreamOptions.
         */
        public static class Builder {
            private FullDocument fullDocument = FullDocument.UPDATE_LOOKUP;
            private final List<Bson> pipeline = new ArrayList<>();
            private BsonDocument resumeToken;
            private int batchSize = 0;

            private Builder() {}

            /**
             * Sets the full document option.
             *
             * @param fullDocument the full document option
             * @return this builder
             */
            @NotNull
            public Builder fullDocument(@NotNull FullDocument fullDocument) {
                this.fullDocument = fullDocument;
                return this;
            }

            /**
             * Adds a filter to the pipeline.
             *
             * @param filter the filter
             * @return this builder
             */
            @NotNull
            public Builder filter(@NotNull Bson filter) {
                this.pipeline.add(Aggregates.match(filter));
                return this;
            }

            /**
             * Adds a projection to the pipeline.
             *
             * @param projection the projection
             * @return this builder
             */
            @NotNull
            public Builder project(@NotNull Bson projection) {
                this.pipeline.add(Aggregates.project(projection));
                return this;
            }

            /**
             * Adds pipeline stages.
             *
             * @param stages the pipeline stages
             * @return this builder
             */
            @NotNull
            public Builder pipeline(@NotNull Bson... stages) {
                this.pipeline.addAll(Arrays.asList(stages));
                return this;
            }

            /**
             * Sets the resume token for resuming a change stream.
             *
             * @param resumeToken the resume token
             * @return this builder
             */
            @NotNull
            public Builder resumeAfter(@NotNull BsonDocument resumeToken) {
                this.resumeToken = resumeToken;
                return this;
            }

            /**
             * Sets the batch size.
             *
             * @param batchSize the batch size
             * @return this builder
             */
            @NotNull
            public Builder batchSize(int batchSize) {
                this.batchSize = batchSize;
                return this;
            }

            /**
             * Builds the options.
             *
             * @return the options
             */
            @NotNull
            public ChangeStreamOptions build() {
                return new ChangeStreamOptions(this);
            }
        }
    }
}
