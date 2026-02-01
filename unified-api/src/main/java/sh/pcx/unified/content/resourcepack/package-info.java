/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Resource pack generation system for the UnifiedPlugin framework.
 *
 * <p>This package provides a complete API for dynamically generating
 * resource packs at runtime, including models, textures, sounds, fonts,
 * and built-in hosting/distribution.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.content.resourcepack.ResourcePackService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.content.resourcepack.ResourcePack} - Pack representation</li>
 *   <li>{@link sh.pcx.unified.content.resourcepack.ResourcePackBuilder} - Content builder</li>
 *   <li>{@link sh.pcx.unified.content.resourcepack.ItemModel} - Item model definitions</li>
 *   <li>{@link sh.pcx.unified.content.resourcepack.SoundDefinition} - Sound definitions</li>
 *   <li>{@link sh.pcx.unified.content.resourcepack.FontDefinition} - Font definitions</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ResourcePack pack = resourcePacks.create("myplugin")
 *     .description(Component.text("My Plugin Resources"))
 *     .packFormat(22)
 *     .build();
 *
 * resourcePacks.builder(pack)
 *     .itemModel("myplugin:ruby_sword", ItemModel.builder()
 *         .parent("minecraft:item/handheld")
 *         .texture("layer0", "myplugin:item/ruby_sword")
 *         .build())
 *     .texture("myplugin:item/ruby_sword", rubyTexture)
 *     .generate();
 *
 * ResourcePackServer server = resourcePacks.host(pack).port(8080).start();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.content.resourcepack;
