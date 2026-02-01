/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;
import sh.pcx.unified.content.resourcepack.ResourcePackModels.ArmorTexture;
import sh.pcx.unified.content.resourcepack.ResourcePackModels.BlockModel;
import sh.pcx.unified.content.resourcepack.ResourcePackModels.ItemModel;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Builder for adding content to a resource pack.
 *
 * @since 1.0.0
 */
public interface ResourcePackBuilder {

    /**
     * Adds an item model.
     *
     * @param key   the model key (e.g., "myplugin:ruby_sword")
     * @param model the item model
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder itemModel(@NotNull String key, @NotNull ItemModel model);

    /**
     * Adds a custom model data override.
     *
     * @param baseItem        the base item type
     * @param customModelData the custom model data value
     * @param model           the model to use
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder itemModelOverride(@NotNull String baseItem, int customModelData,
                                          @NotNull ItemModel model);

    /**
     * Adds a block model.
     *
     * @param key   the model key
     * @param model the block model
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder blockModel(@NotNull String key, @NotNull BlockModel model);

    /**
     * Adds a texture.
     *
     * @param key     the texture key
     * @param texture the texture data
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder texture(@NotNull String key, @NotNull InputStream texture);

    /**
     * Adds a texture from a path.
     *
     * @param key  the texture key
     * @param path the texture file path
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder texture(@NotNull String key, @NotNull Path path);

    /**
     * Adds a texture from bytes.
     *
     * @param key  the texture key
     * @param data the texture data
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder texture(@NotNull String key, @NotNull byte[] data);

    /**
     * Adds a sound definition.
     *
     * @param key        the sound key
     * @param definition the sound definition
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder sound(@NotNull String key, @NotNull SoundDefinition definition);

    /**
     * Adds a sound file.
     *
     * @param key   the sound file key
     * @param sound the sound data (.ogg)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder soundFile(@NotNull String key, @NotNull InputStream sound);

    /**
     * Adds a font definition.
     *
     * @param key        the font key
     * @param definition the font definition
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder font(@NotNull String key, @NotNull FontDefinition definition);

    /**
     * Adds an armor texture.
     *
     * @param material the armor material name
     * @param texture  the armor texture
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder armorTexture(@NotNull String material, @NotNull ArmorTexture texture);

    /**
     * Adds a language file.
     *
     * @param locale       the locale code (e.g., "en_us")
     * @param translations the translations map
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder language(@NotNull String locale,
                                 @NotNull Map<String, String> translations);

    /**
     * Generates the resource pack with all added content.
     *
     * @since 1.0.0
     */
    void generate();
}
