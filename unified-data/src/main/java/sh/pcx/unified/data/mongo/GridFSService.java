/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.client.gridfs.model.GridFSDownloadOptions;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.reactivestreams.client.gridfs.GridFSBucket;
import com.mongodb.reactivestreams.client.gridfs.GridFSBuckets;
import com.mongodb.reactivestreams.client.gridfs.GridFSDownloadPublisher;
import com.mongodb.reactivestreams.client.gridfs.GridFSFindPublisher;
import com.mongodb.reactivestreams.client.gridfs.GridFSUploadPublisher;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static sh.pcx.unified.data.mongo.MongoConnection.toCompletableFuture;
import static sh.pcx.unified.data.mongo.MongoConnection.toCompletableFutureList;
import static sh.pcx.unified.data.mongo.MongoConnection.toCompletableFutureVoid;

/**
 * Service for storing and retrieving large files using MongoDB GridFS.
 *
 * <p>GridFS is a specification for storing and retrieving files that exceed
 * the BSON document size limit of 16MB. It divides files into chunks and
 * stores each chunk as a separate document.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the GridFS service
 * GridFSService gridFS = mongoService.getGridFS();
 *
 * // Upload a file from bytes
 * byte[] imageData = loadImageFromSomewhere();
 * gridFS.upload("player_skin.png", imageData)
 *     .thenAccept(fileId -> {
 *         logger.info("Uploaded file with ID: " + fileId);
 *     });
 *
 * // Upload with metadata
 * Document metadata = new Document("playerId", player.getUniqueId().toString())
 *     .append("uploadedBy", uploader.getName())
 *     .append("contentType", "image/png");
 *
 * gridFS.upload("player_skin.png", imageData, metadata)
 *     .thenAccept(fileId -> {
 *         // File uploaded with metadata
 *     });
 *
 * // Upload from a file path
 * gridFS.uploadFromPath("backup.zip", Path.of("/tmp/backup.zip"))
 *     .thenAccept(fileId -> logger.info("Backup uploaded"));
 *
 * // Download a file
 * gridFS.download(fileId)
 *     .thenAccept(bytes -> {
 *         // Use the file bytes
 *     });
 *
 * // Download to a file path
 * gridFS.downloadToPath(fileId, Path.of("/tmp/downloaded.png"))
 *     .thenAccept(path -> logger.info("Downloaded to: " + path));
 *
 * // Find files by metadata
 * gridFS.find(Filters.eq("metadata.playerId", playerId.toString()))
 *     .thenAccept(files -> {
 *         files.forEach(file -> {
 *             logger.info("File: " + file.getFilename() + " (" + file.getLength() + " bytes)");
 *         });
 *     });
 *
 * // Delete a file
 * gridFS.delete(fileId).thenRun(() -> logger.info("File deleted"));
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Player skins and capes</li>
 *   <li>World backups and snapshots</li>
 *   <li>Schematic files</li>
 *   <li>Custom resource packs</li>
 *   <li>Plugin data exports</li>
 *   <li>Log file archives</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations return CompletableFuture
 * and execute asynchronously.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoService#getGridFS()
 * @see MongoService#getGridFS(String)
 */
public class GridFSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridFSService.class);

    /**
     * Default bucket name used by GridFS.
     */
    public static final String DEFAULT_BUCKET = "fs";

    /**
     * Default chunk size (255KB).
     */
    public static final int DEFAULT_CHUNK_SIZE = 261120;

    private final GridFSBucket bucket;
    private final String bucketName;

    /**
     * Creates a new GridFS service with the default bucket.
     *
     * @param connection the MongoDB connection
     * @since 1.0.0
     */
    public GridFSService(@NotNull MongoConnection connection) {
        this(connection, DEFAULT_BUCKET);
    }

    /**
     * Creates a new GridFS service with a custom bucket name.
     *
     * @param connection the MongoDB connection
     * @param bucketName the bucket name
     * @since 1.0.0
     */
    public GridFSService(@NotNull MongoConnection connection, @NotNull String bucketName) {
        Objects.requireNonNull(connection, "Connection cannot be null");
        Objects.requireNonNull(bucketName, "Bucket name cannot be null");

        this.bucketName = bucketName;
        this.bucket = GridFSBuckets.create(connection.getDatabase(), bucketName);
    }

    /**
     * Returns the bucket name.
     *
     * @return the bucket name
     * @since 1.0.0
     */
    @NotNull
    public String getBucketName() {
        return bucketName;
    }

    // ===========================================
    // Upload Operations
    // ===========================================

    /**
     * Uploads a file from bytes.
     *
     * @param filename the filename
     * @param data     the file data
     * @return a future completing with the file ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ObjectId> upload(@NotNull String filename, byte @NotNull [] data) {
        return upload(filename, data, null);
    }

    /**
     * Uploads a file from bytes with metadata.
     *
     * @param filename the filename
     * @param data     the file data
     * @param metadata the file metadata (optional)
     * @return a future completing with the file ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ObjectId> upload(
            @NotNull String filename,
            byte @NotNull [] data,
            @Nullable Document metadata
    ) {
        Objects.requireNonNull(filename, "Filename cannot be null");
        Objects.requireNonNull(data, "Data cannot be null");

        GridFSUploadOptions options = new GridFSUploadOptions();
        if (metadata != null) {
            options.metadata(metadata);
        }

        Publisher<ByteBuffer> source = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                private boolean done = false;

                @Override
                public void request(long n) {
                    if (!done && n > 0) {
                        done = true;
                        subscriber.onNext(ByteBuffer.wrap(data));
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        };

        GridFSUploadPublisher<ObjectId> publisher = bucket.uploadFromPublisher(filename, source, options);
        return toCompletableFuture(publisher);
    }

    /**
     * Uploads a file from an input stream.
     *
     * @param filename    the filename
     * @param inputStream the input stream
     * @return a future completing with the file ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ObjectId> upload(
            @NotNull String filename,
            @NotNull InputStream inputStream
    ) {
        return upload(filename, inputStream, null);
    }

    /**
     * Uploads a file from an input stream with metadata.
     *
     * @param filename    the filename
     * @param inputStream the input stream
     * @param metadata    the file metadata (optional)
     * @return a future completing with the file ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ObjectId> upload(
            @NotNull String filename,
            @NotNull InputStream inputStream,
            @Nullable Document metadata
    ) {
        Objects.requireNonNull(inputStream, "Input stream cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            try {
                return inputStream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read input stream", e);
            }
        }).thenCompose(data -> upload(filename, data, metadata));
    }

    /**
     * Uploads a file from a path.
     *
     * @param filename the filename (can be different from the source file name)
     * @param path     the source file path
     * @return a future completing with the file ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ObjectId> uploadFromPath(
            @NotNull String filename,
            @NotNull Path path
    ) {
        return uploadFromPath(filename, path, null);
    }

    /**
     * Uploads a file from a path with metadata.
     *
     * @param filename the filename
     * @param path     the source file path
     * @param metadata the file metadata (optional)
     * @return a future completing with the file ID
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<ObjectId> uploadFromPath(
            @NotNull String filename,
            @NotNull Path path,
            @Nullable Document metadata
    ) {
        Objects.requireNonNull(path, "Path cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + path, e);
            }
        }).thenCompose(data -> upload(filename, data, metadata));
    }

    // ===========================================
    // Download Operations
    // ===========================================

    /**
     * Downloads a file by ID.
     *
     * @param fileId the file ID
     * @return a future completing with the file data
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<byte[]> download(@NotNull ObjectId fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");

        GridFSDownloadPublisher publisher = bucket.downloadToPublisher(fileId);
        return downloadToBytes(publisher);
    }

    /**
     * Downloads a file by ID.
     *
     * @param fileId the file ID as BsonValue
     * @return a future completing with the file data
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<byte[]> download(@NotNull BsonValue fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");

        GridFSDownloadPublisher publisher = bucket.downloadToPublisher(fileId);
        return downloadToBytes(publisher);
    }

    /**
     * Downloads a file by filename.
     *
     * @param filename the filename
     * @return a future completing with the file data
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<byte[]> downloadByFilename(@NotNull String filename) {
        Objects.requireNonNull(filename, "Filename cannot be null");

        GridFSDownloadPublisher publisher = bucket.downloadToPublisher(filename);
        return downloadToBytes(publisher);
    }

    /**
     * Downloads a file by filename with specific revision.
     *
     * @param filename the filename
     * @param revision the revision (-1 for most recent, 0 for original, etc.)
     * @return a future completing with the file data
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<byte[]> downloadByFilename(
            @NotNull String filename,
            int revision
    ) {
        Objects.requireNonNull(filename, "Filename cannot be null");

        GridFSDownloadOptions options = new GridFSDownloadOptions().revision(revision);
        GridFSDownloadPublisher publisher = bucket.downloadToPublisher(filename, options);
        return downloadToBytes(publisher);
    }

    /**
     * Downloads a file to a path.
     *
     * @param fileId the file ID
     * @param path   the destination path
     * @return a future completing with the destination path
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Path> downloadToPath(
            @NotNull ObjectId fileId,
            @NotNull Path path
    ) {
        Objects.requireNonNull(path, "Path cannot be null");

        return download(fileId).thenApply(data -> {
            try {
                Files.write(path, data);
                return path;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + path, e);
            }
        });
    }

    /**
     * Downloads a file to an output stream.
     *
     * @param fileId       the file ID
     * @param outputStream the output stream
     * @return a future completing when the download is done
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> downloadToStream(
            @NotNull ObjectId fileId,
            @NotNull OutputStream outputStream
    ) {
        Objects.requireNonNull(outputStream, "Output stream cannot be null");

        return download(fileId).thenAccept(data -> {
            try {
                outputStream.write(data);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to stream", e);
            }
        });
    }

    private CompletableFuture<byte[]> downloadToBytes(GridFSDownloadPublisher publisher) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                future.complete(outputStream.toByteArray());
            }
        });

        return future;
    }

    // ===========================================
    // Find Operations
    // ===========================================

    /**
     * Finds all files in the bucket.
     *
     * @return a future completing with the list of files
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<GridFSFile>> findAll() {
        return toCompletableFutureList(bucket.find());
    }

    /**
     * Finds files matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with matching files
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<GridFSFile>> find(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return toCompletableFutureList(bucket.find(filter));
    }

    /**
     * Finds a file by ID.
     *
     * @param fileId the file ID
     * @return a future completing with the file if found
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Optional<GridFSFile>> findById(@NotNull ObjectId fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        return find(new Document("_id", fileId))
                .thenApply(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
    }

    /**
     * Finds files by filename.
     *
     * @param filename the filename
     * @return a future completing with matching files
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<GridFSFile>> findByFilename(@NotNull String filename) {
        Objects.requireNonNull(filename, "Filename cannot be null");
        return find(new Document("filename", filename));
    }

    /**
     * Checks if a file exists.
     *
     * @param fileId the file ID
     * @return a future completing with true if the file exists
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> exists(@NotNull ObjectId fileId) {
        return findById(fileId).thenApply(Optional::isPresent);
    }

    // ===========================================
    // Delete Operations
    // ===========================================

    /**
     * Deletes a file by ID.
     *
     * @param fileId the file ID
     * @return a future completing when the file is deleted
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> delete(@NotNull ObjectId fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        return toCompletableFutureVoid(bucket.delete(fileId));
    }

    /**
     * Deletes a file by ID.
     *
     * @param fileId the file ID as BsonValue
     * @return a future completing when the file is deleted
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> delete(@NotNull BsonValue fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        return toCompletableFutureVoid(bucket.delete(fileId));
    }

    /**
     * Deletes all files matching a filter.
     *
     * @param filter the filter to apply
     * @return a future completing with the number of files deleted
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> deleteMany(@NotNull Bson filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");

        return find(filter).thenCompose(files -> {
            List<CompletableFuture<Void>> deleteFutures = files.stream()
                    .map(file -> delete(file.getObjectId()))
                    .toList();

            return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> (long) files.size());
        });
    }

    /**
     * Deletes all files by filename.
     *
     * @param filename the filename
     * @return a future completing with the number of files deleted
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Long> deleteByFilename(@NotNull String filename) {
        return deleteMany(new Document("filename", filename));
    }

    // ===========================================
    // Rename Operations
    // ===========================================

    /**
     * Renames a file.
     *
     * @param fileId      the file ID
     * @param newFilename the new filename
     * @return a future completing when the file is renamed
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> rename(@NotNull ObjectId fileId, @NotNull String newFilename) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        Objects.requireNonNull(newFilename, "New filename cannot be null");
        return toCompletableFutureVoid(bucket.rename(fileId, newFilename));
    }

    /**
     * Renames a file.
     *
     * @param fileId      the file ID as BsonValue
     * @param newFilename the new filename
     * @return a future completing when the file is renamed
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> rename(@NotNull BsonValue fileId, @NotNull String newFilename) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        Objects.requireNonNull(newFilename, "New filename cannot be null");
        return toCompletableFutureVoid(bucket.rename(fileId, newFilename));
    }

    // ===========================================
    // Bucket Operations
    // ===========================================

    /**
     * Drops the entire bucket (deletes all files).
     *
     * @return a future completing when the bucket is dropped
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> drop() {
        LOGGER.warn("Dropping GridFS bucket: {}", bucketName);
        return toCompletableFutureVoid(bucket.drop());
    }
}
