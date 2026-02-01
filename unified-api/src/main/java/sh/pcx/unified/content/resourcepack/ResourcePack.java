/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a resource pack created through the API.
 *
 * <p>ResourcePack encapsulates all the content of a resource pack,
 * including models, textures, sounds, fonts, and metadata.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ResourcePackService
 */
public interface ResourcePack {

    /**
     * Returns the pack namespace.
     *
     * @return the namespace (e.g., "myplugin")
     * @since 1.0.0
     */
    @NotNull
    String getNamespace();

    /**
     * Returns the pack description.
     *
     * @return the description component
     * @since 1.0.0
     */
    @NotNull
    Component getDescription();

    /**
     * Returns the pack format version.
     *
     * @return the format version
     * @since 1.0.0
     */
    int getPackFormat();

    /**
     * Returns the pack version.
     *
     * @return the version number
     * @since 1.0.0
     */
    int getVersion();

    /**
     * Sets the pack version.
     *
     * @param version the new version
     * @since 1.0.0
     */
    void setVersion(int version);

    /**
     * Returns the SHA-1 hash of the pack.
     *
     * @return an Optional containing the hash if generated
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getHash();

    /**
     * Returns the pack file path.
     *
     * @return an Optional containing the path if generated
     * @since 1.0.0
     */
    @NotNull
    Optional<Path> getPath();

    /**
     * Returns all item model keys in this pack.
     *
     * @return an unmodifiable set of model keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getItemModels();

    /**
     * Returns all block model keys in this pack.
     *
     * @return an unmodifiable set of model keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getBlockModels();

    /**
     * Returns all texture keys in this pack.
     *
     * @return an unmodifiable set of texture keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getTextures();

    /**
     * Returns all sound keys in this pack.
     *
     * @return an unmodifiable set of sound keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getSounds();

    /**
     * Returns all font keys in this pack.
     *
     * @return an unmodifiable set of font keys
     * @since 1.0.0
     */
    @NotNull
    Set<String> getFonts();

    /**
     * Checks if the pack has been generated.
     *
     * @return true if the pack file exists
     * @since 1.0.0
     */
    boolean isGenerated();

    /**
     * Returns the size of the pack in bytes.
     *
     * @return the pack size, or 0 if not generated
     * @since 1.0.0
     */
    long getSize();
}
