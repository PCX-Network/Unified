/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.v1_20_R4;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import sh.pcx.unified.version.api.ComponentBridge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Component bridge implementation for Minecraft 1.20.5-1.20.6 (v1_20_R4).
 *
 * <p>This implementation handles conversion between Adventure components and
 * vanilla Minecraft components for NMS operations.
 *
 * <h2>Version Notes</h2>
 * <ul>
 *   <li>Vanilla component class: net.minecraft.network.chat.Component</li>
 *   <li>Uses Spigot mappings (not Mojang mappings)</li>
 *   <li>Paper provides native Adventure support</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class ComponentBridgeImpl implements ComponentBridge {

    // Serializers
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    // NMS class reference
    private Class<?> vanillaComponentClass;

    /**
     * Creates a new component bridge for v1_20_R4.
     */
    public ComponentBridgeImpl() {
        // TODO: Initialize NMS class reference
        // try {
        //     vanillaComponentClass = Class.forName(
        //         "net.minecraft.network.chat.IChatBaseComponent");
        // } catch (ClassNotFoundException e) {
        //     // Try alternative class name
        // }
    }

    // ===== Adventure to Vanilla Conversion =====

    @Override
    @NotNull
    public Object toVanilla(@NotNull Component component) {
        // TODO: Implement for v1_20_R4
        // Paper provides a utility for this, or we can use JSON serialization
        // Example using Paper API:
        // return PaperAdventure.asVanilla(component);

        // Example using JSON:
        // String json = GSON.serialize(component);
        // return net.minecraft.network.chat.Component.Serializer.fromJson(json);

        throw new UnsupportedOperationException("Not yet implemented for v1_20_R4");
    }

    // ===== Vanilla to Adventure Conversion =====

    @Override
    @NotNull
    public Component fromVanilla(@NotNull Object vanillaComponent) {
        // TODO: Implement for v1_20_R4
        // Example using Paper API:
        // return PaperAdventure.asAdventure((net.minecraft.network.chat.Component) vanillaComponent);

        // Example using JSON:
        // String json = net.minecraft.network.chat.Component.Serializer.toJson(
        //     (net.minecraft.network.chat.Component) vanillaComponent);
        // return GSON.deserialize(json);

        throw new UnsupportedOperationException("Not yet implemented for v1_20_R4");
    }

    // ===== JSON Serialization =====

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

    // ===== Legacy Text Conversion =====

    @Override
    @NotNull
    public Component fromLegacy(@NotNull String legacyText) {
        // Handle both & and section symbol color codes
        String withSectionSymbols = legacyText.replace('&', '\u00A7');
        return LEGACY_SECTION.deserialize(withSectionSymbols);
    }

    @Override
    @NotNull
    public String toLegacy(@NotNull Component component) {
        return LEGACY_SECTION.serialize(component);
    }

    // ===== MiniMessage Support =====

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

    // ===== Plain Text =====

    @Override
    @NotNull
    public String toPlainText(@NotNull Component component) {
        return PLAIN_TEXT.serialize(component);
    }

    // ===== Utility Methods =====

    @Override
    @NotNull
    public Component join(@NotNull Component separator, @NotNull Component... components) {
        if (components.length == 0) {
            return Component.empty();
        }
        if (components.length == 1) {
            return components[0];
        }

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
        String plain = toPlainText(component);
        return plain.isBlank();
    }

    @Override
    @NotNull
    public Class<?> getVanillaComponentClass() {
        if (vanillaComponentClass == null) {
            try {
                // Try Spigot mapping name
                vanillaComponentClass = Class.forName(
                        "net.minecraft.network.chat.IChatBaseComponent");
            } catch (ClassNotFoundException e) {
                try {
                    // Try Mojang mapping name (shouldn't be used in v1_20_R4)
                    vanillaComponentClass = Class.forName(
                            "net.minecraft.network.chat.Component");
                } catch (ClassNotFoundException e2) {
                    throw new IllegalStateException(
                            "Could not find vanilla component class for v1_20_R4");
                }
            }
        }
        return vanillaComponentClass;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
