/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.v1_21_R2;

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
 * Component bridge implementation for Minecraft 1.21.2-1.21.3 (v1_21_R2).
 *
 * <p>This implementation handles conversion between Adventure components and
 * vanilla Minecraft components for the Bundles update versions.
 *
 * <h2>Version Notes</h2>
 * <ul>
 *   <li>Same component structure as 1.21.x</li>
 *   <li>Uses Spigot mappings</li>
 * </ul>
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

    private Class<?> vanillaComponentClass;

    /**
     * Creates a new component bridge for v1_21_R2.
     */
    public ComponentBridgeImpl() {
        // TODO: Initialize NMS class reference
    }

    @Override
    @NotNull
    public Object toVanilla(@NotNull Component component) {
        // TODO: Implement for v1_21_R2
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R2");
    }

    @Override
    @NotNull
    public Component fromVanilla(@NotNull Object vanillaComponent) {
        // TODO: Implement for v1_21_R2
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R2");
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
                vanillaComponentClass = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
            } catch (ClassNotFoundException e) {
                try {
                    vanillaComponentClass = Class.forName("net.minecraft.network.chat.Component");
                } catch (ClassNotFoundException e2) {
                    throw new IllegalStateException("Could not find vanilla component class for v1_21_R2");
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
