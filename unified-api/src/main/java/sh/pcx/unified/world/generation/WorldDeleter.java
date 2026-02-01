/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Fluent builder for safely deleting worlds.
 *
 * <p>WorldDeleter provides options for handling the deletion process,
 * including confirmation requirements and cleanup callbacks.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Delete with confirmation
 * WorldDeleter deleter = worlds.delete("temp_world")
 *     .requireConfirmation(true);
 *
 * // First call - returns false, marks for confirmation
 * boolean ready = deleter.delete(); // false
 *
 * // Second call - actually deletes
 * boolean deleted = deleter.delete(); // true
 *
 * // Delete without confirmation
 * worlds.delete("old_world")
 *     .requireConfirmation(false)
 *     .onComplete(() -> log.info("World deleted"))
 *     .delete();
 *
 * // Async deletion
 * worlds.delete("arena-1")
 *     .deleteAsync()
 *     .thenAccept(success -> {
 *         if (success) {
 *             log.info("World deleted successfully");
 *         }
 *     });
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 */
public interface WorldDeleter {

    /**
     * Sets whether confirmation is required before deletion.
     *
     * <p>When enabled, the first call to {@link #delete()} will mark the
     * world for deletion and return false. The second call will actually
     * delete the world.
     *
     * @param require true to require confirmation
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldDeleter requireConfirmation(boolean require);

    /**
     * Sets a callback to run when deletion is complete.
     *
     * @param callback the completion callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldDeleter onComplete(@NotNull Runnable callback);

    /**
     * Sets a callback to run if deletion fails.
     *
     * @param callback the failure callback
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldDeleter onFailure(@NotNull Consumer<Throwable> callback);

    /**
     * Unloads the world before deletion if loaded.
     *
     * <p>This is enabled by default.
     *
     * @param unload true to unload before deletion
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    WorldDeleter unloadFirst(boolean unload);

    /**
     * Deletes the world synchronously.
     *
     * <p>If confirmation is required and this is the first call, this method
     * returns false and marks the world for deletion. A subsequent call will
     * actually delete the world.
     *
     * @return true if the world was deleted, false if confirmation is pending
     * @since 1.0.0
     */
    boolean delete();

    /**
     * Deletes the world asynchronously.
     *
     * @return a future that completes with true if successful
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deleteAsync();

    /**
     * Confirms the deletion if confirmation was required.
     *
     * <p>This is equivalent to calling {@link #delete()} again after the
     * initial call with confirmation required.
     *
     * @return true if the world was deleted
     * @since 1.0.0
     */
    boolean confirm();

    /**
     * Cancels a pending deletion.
     *
     * @return true if a pending deletion was cancelled
     * @since 1.0.0
     */
    boolean cancel();

    /**
     * Checks if deletion is pending confirmation.
     *
     * @return true if waiting for confirmation
     * @since 1.0.0
     */
    boolean isPendingConfirmation();

    /**
     * Gets the world name being deleted.
     *
     * @return the world name
     * @since 1.0.0
     */
    @NotNull
    String getWorldName();
}
