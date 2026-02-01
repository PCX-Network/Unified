/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.v1_21_R4;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import sh.pcx.unified.version.api.ComponentBridge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Component bridge implementation for Minecraft 1.21.11+ (v1_21_R4).
 *
 * <p>This implementation handles conversion between Adventure components and
 * vanilla Minecraft components using Mojang mappings.
 *
 * <h2>CRITICAL Version Notes</h2>
 * <ul>
 *   <li><strong>Mojang Mappings:</strong> Component class names changed!
 *       <ul>
 *         <li>net.minecraft.network.chat.Component (was IChatBaseComponent)</li>
 *         <li>Component.Serializer (was IChatBaseComponent.ChatSerializer)</li>
 *         <li>MutableComponent (was IChatMutableComponent)</li>
 *       </ul>
 *   </li>
 *   <li>Paper provides PaperAdventure for conversion</li>
 * </ul>
 *
 * <h2>Class Mappings</h2>
 * <table>
 *   <tr><th>Spigot Mapping</th><th>Mojang Mapping</th></tr>
 *   <tr><td>IChatBaseComponent</td><td>Component</td></tr>
 *   <tr><td>IChatMutableComponent</td><td>MutableComponent</td></tr>
 *   <tr><td>ChatSerializer</td><td>Component.Serializer</td></tr>
 *   <tr><td>ChatComponentText</td><td>TextComponent / Component.literal()</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class ComponentBridgeImpl implements ComponentBridge {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    // Mojang-mapped component class
    private Class<?> vanillaComponentClass;

    /**
     * Creates a new component bridge for v1_21_R4 (Mojang mappings).
     */
    public ComponentBridgeImpl() {
        // TODO: Initialize Mojang-mapped component class
        // Note: In 1.21.11+ with Mojang mappings, the class is:
        // net.minecraft.network.chat.Component (interface)
        try {
            vanillaComponentClass = Class.forName("net.minecraft.network.chat.Component");
        } catch (ClassNotFoundException e) {
            // Fallback - should not happen in v1_21_R4
        }
    }

    @Override
    @NotNull
    public Object toVanilla(@NotNull Component component) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // Options:
        // 1. Use Paper's PaperAdventure:
        //    io.papermc.paper.adventure.PaperAdventure.asVanilla(component)
        //
        // 2. Use JSON serialization:
        //    String json = GSON.serialize(component);
        //    net.minecraft.network.chat.Component.Serializer.fromJson(json, registryAccess);
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public Component fromVanilla(@NotNull Object vanillaComponent) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // Options:
        // 1. Use Paper's PaperAdventure:
        //    io.papermc.paper.adventure.PaperAdventure.asAdventure(
        //        (net.minecraft.network.chat.Component) vanillaComponent)
        //
        // 2. Use JSON serialization:
        //    String json = net.minecraft.network.chat.Component.Serializer.toJson(
        //        (net.minecraft.network.chat.Component) vanillaComponent, registryAccess);
        //    return GSON.deserialize(json);
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public String toJson(@NotNull Component component) {
        return GSON.serialize(component);
    }

    @Override
    @NotNull
    public Component fromJson(@NotNull String json) {
        return GSON.deserialize(json);
    }

    @Override
    @NotNull
    public Component fromLegacy(@NotNull String legacyText) {
        String withSectionSymbols = legacyText.replace('&', '\u00A7');
        return LEGACY_SECTION.deserialize(withSectionSymbols);
    }

    @Override
    @NotNull
    public String toLegacy(@NotNull Component component) {
        return LEGACY_SECTION.serialize(component);
    }

    @Override
    @NotNull
    public Component fromMiniMessage(@NotNull String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    @Override
    @NotNull
    public String toMiniMessage(@NotNull Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    @Override
    @NotNull
    public String toPlainText(@NotNull Component component) {
        return PLAIN_TEXT.serialize(component);
    }

    @Override
    @NotNull
    public Component join(@NotNull Component separator, @NotNull Component... components) {
        if (components.length == 0) return Component.empty();
        if (components.length == 1) return components[0];
        Component result = components[0];
        for (int i = 1; i < components.length; i++) {
            result = result.append(separator).append(components[i]);
        }
        return result;
    }

    @Override
    @NotNull
    public Component join(@NotNull Component separator, @NotNull Iterable<Component> components) {
        List<Component> list = new ArrayList<>();
        components.forEach(list::add);
        return join(separator, list.toArray(new Component[0]));
    }

    @Override
    public boolean isEmpty(@NotNull Component component) {
        return toPlainText(component).isBlank();
    }

    @Override
    @NotNull
    public Class<?> getVanillaComponentClass() {
        if (vanillaComponentClass == null) {
            try {
                // In Mojang mappings, it's just Component
                vanillaComponentClass = Class.forName("net.minecraft.network.chat.Component");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Could not find vanilla Component class. " +
                        "This should not happen in v1_21_R4 (Mojang mappings).");
            }
        }
        return vanillaComponentClass;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
