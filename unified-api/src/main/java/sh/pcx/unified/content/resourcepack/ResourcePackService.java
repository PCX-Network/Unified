/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for creating, hosting, and distributing resource packs.
 *
 * <p>ResourcePackService provides a fluent API for dynamically generating
 * resource packs at runtime, including custom models, textures, sounds,
 * and fonts. It also supports hosting packs via built-in HTTP server
 * and prompting players to download.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Dynamic Generation</b> - Build packs programmatically</li>
 *   <li><b>Custom Models</b> - Item and block models</li>
 *   <li><b>Textures</b> - Add/modify textures</li>
 *   <li><b>Sounds</b> - Custom sound effects and music</li>
 *   <li><b>Fonts</b> - Custom fonts and glyphs</li>
 *   <li><b>Auto-Hosting</b> - Built-in HTTP server</li>
 *   <li><b>Player Prompting</b> - Send pack requests</li>
 *   <li><b>Merging</b> - Combine multiple packs</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private ResourcePackService resourcePacks;
 *
 * // Create resource pack
 * ResourcePack pack = resourcePacks.create("myplugin")
 *     .description(Component.text("My Plugin Resources"))
 *     .packFormat(22)
 *     .build();
 *
 * // Add content
 * resourcePacks.builder(pack)
 *     .itemModel("myplugin:ruby_sword", ItemModel.builder()
 *         .parent("minecraft:item/handheld")
 *         .texture("layer0", "myplugin:item/ruby_sword")
 *         .build())
 *     .texture("myplugin:item/ruby_sword", getResource("textures/ruby_sword.png"))
 *     .sound("myplugin:boss_roar", SoundDefinition.builder()
 *         .sound("myplugin:sounds/boss_roar")
 *         .build())
 *     .generate();
 *
 * // Host and send to players
 * ResourcePackServer server = resourcePacks.host(pack).port(8080).start();
 * resourcePacks.send(player, server.getUrl(), server.getHash(), true,
 *     Component.text("Please accept our resource pack!"));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ResourcePack
 * @see ResourcePackBuilder
 */
public interface ResourcePackService extends Service {

    /**
     * Creates a new resource pack with the specified namespace.
     *
     * @param namespace the pack namespace (e.g., "myplugin")
     * @return a new ResourcePackCreator
     * @since 1.0.0
     */
    @NotNull
    ResourcePackCreator create(@NotNull String namespace);

    /**
     * Gets a registered resource pack by namespace.
     *
     * @param namespace the pack namespace
     * @return an Optional containing the pack if found
     * @since 1.0.0
     */
    @NotNull
    Optional<ResourcePack> get(@NotNull String namespace);

    /**
     * Returns all registered resource packs.
     *
     * @return an unmodifiable collection of packs
     * @since 1.0.0
     */
    @NotNull
    Collection<ResourcePack> getAll();

    /**
     * Creates a builder for modifying a resource pack.
     *
     * @param pack the pack to modify
     * @return a new ResourcePackBuilder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackBuilder builder(@NotNull ResourcePack pack);

    /**
     * Hosts a resource pack on the built-in HTTP server.
     *
     * @param pack the pack to host
     * @return a server builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackServerBuilder host(@NotNull ResourcePack pack);

    /**
     * Sends a resource pack to a player.
     *
     * @param player   the player
     * @param url      the pack URL
     * @param hash     the SHA-1 hash
     * @param required whether the pack is required
     * @param prompt   the prompt message (null for default)
     * @since 1.0.0
     */
    void send(@NotNull UnifiedPlayer player, @NotNull String url, @NotNull String hash,
              boolean required, @Nullable Component prompt);

    /**
     * Sends the default resource pack to a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void send(@NotNull UnifiedPlayer player);

    /**
     * Registers a handler for resource pack responses.
     *
     * @param handler the response handler
     * @since 1.0.0
     */
    void onResponse(@NotNull Consumer<ResourcePackResponseEvent> handler);

    /**
     * Merges multiple resource packs into one.
     *
     * @param name  the merged pack name
     * @param packs the packs to merge
     * @return the merged ResourcePack
     * @since 1.0.0
     */
    @NotNull
    ResourcePack merge(@NotNull String name, @NotNull ResourcePack... packs);

    /**
     * Creates a merge builder for combining packs with conflict resolution.
     *
     * @param name the merged pack name
     * @return a new ResourcePackMergeBuilder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackMergeBuilder merge(@NotNull String name);

    /**
     * Regenerates a resource pack after modifications.
     *
     * @param pack the pack to regenerate
     * @since 1.0.0
     */
    void regenerate(@NotNull ResourcePack pack);

    /**
     * Saves a resource pack to a file.
     *
     * @param pack the pack to save
     * @param path the output path
     * @since 1.0.0
     */
    void save(@NotNull ResourcePack pack, @NotNull Path path);

    /**
     * Loads a resource pack from a file.
     *
     * @param path the pack file path
     * @return the loaded ResourcePack
     * @since 1.0.0
     */
    @NotNull
    ResourcePack load(@NotNull Path path);
}
