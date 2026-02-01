/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for creating resource packs.
 *
 * @since 1.0.0
 */
public interface ResourcePackCreator {

    /**
     * Sets the pack description.
     *
     * @param description the description
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackCreator description(@NotNull Component description);

    /**
     * Sets the pack format version.
     *
     * @param format the format version (e.g., 22 for 1.20.5+)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackCreator packFormat(int format);

    /**
     * Sets supported pack formats range.
     *
     * @param min the minimum supported format
     * @param max the maximum supported format
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackCreator supportedFormats(int min, int max);

    /**
     * Builds the resource pack.
     *
     * @return the created ResourcePack
     * @since 1.0.0
     */
    @NotNull
    ResourcePack build();
}
