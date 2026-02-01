/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builder for merging resource packs.
 *
 * @since 1.0.0
 */
public interface ResourcePackMergeBuilder {

    /**
     * Adds a pack with priority.
     *
     * @param pack     the pack to add
     * @param priority the merge priority
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackMergeBuilder add(@NotNull ResourcePack pack, @NotNull MergePriority priority);

    /**
     * Sets a conflict resolution handler.
     *
     * @param handler the conflict handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackMergeBuilder onConflict(@NotNull ConflictHandler handler);

    /**
     * Builds the merged pack.
     *
     * @return the merged ResourcePack
     * @since 1.0.0
     */
    @NotNull
    ResourcePack build();

    /**
     * Conflict handler for merge conflicts.
     */
    @FunctionalInterface
    interface ConflictHandler {
        /**
         * Resolves a conflict between sources.
         *
         * @param path    the conflicting path
         * @param sources the source packs with this path
         * @return the pack to use
         */
        @NotNull
        ResourcePack resolve(@NotNull String path, @NotNull List<ResourcePack> sources);
    }
}
