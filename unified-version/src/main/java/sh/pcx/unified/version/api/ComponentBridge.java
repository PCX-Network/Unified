/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.api;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service interface for converting between Adventure components and vanilla Minecraft components.
 *
 * <p>This service provides bidirectional conversion between the Adventure text library used by
 * Paper/modern plugins and the vanilla Minecraft component system used by NMS code. It also
 * handles JSON serialization for network protocols and storage.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private ComponentBridge components;
 *
 * public void sendCustomTitle(Player player, Component title) {
 *     // Convert Adventure component to vanilla for NMS operations
 *     Object vanillaTitle = components.toVanilla(title);
 *
 *     // Use in NMS packet construction
 *     // ...
 * }
 *
 * public Component fromNmsMessage(Object vanillaComponent) {
 *     // Convert vanilla component to Adventure
 *     return components.fromVanilla(vanillaComponent);
 * }
 *
 * public void storeComponent(Component component) {
 *     // Serialize to JSON for storage
 *     String json = components.toJson(component);
 *     database.store("message", json);
 * }
 *
 * public Component loadComponent() {
 *     // Deserialize from JSON
 *     String json = database.load("message");
 *     return components.fromJson(json);
 * }
 * }</pre>
 *
 * <h2>Version Compatibility</h2>
 * <p>The vanilla component system changes between Minecraft versions:
 * <ul>
 *   <li>1.20.5+: Uses IChatBaseComponent with immutable design</li>
 *   <li>1.21+: Additional component types and styling options</li>
 *   <li>1.21.11+: Mojang mappings change class names</li>
 * </ul>
 *
 * <p>This service handles all these differences transparently.
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see NMSBridge
 */
public interface ComponentBridge extends Service {

    // ===== Adventure to Vanilla Conversion =====

    /**
     * Converts an Adventure component to a vanilla Minecraft component.
     *
     * <p>The returned object is an instance of the NMS IChatBaseComponent
     * (or Component in Mojang mappings).
     *
     * @param component the Adventure component
     * @return the vanilla component
     * @since 1.0.0
     */
    @NotNull
    Object toVanilla(@NotNull Component component);

    /**
     * Converts an Adventure component to vanilla, returning null for null input.
     *
     * @param component the Adventure component, may be null
     * @return the vanilla component, or null
     * @since 1.0.0
     */
    @Nullable
    default Object toVanillaNullable(@Nullable Component component) {
        return component == null ? null : toVanilla(component);
    }

    // ===== Vanilla to Adventure Conversion =====

    /**
     * Converts a vanilla Minecraft component to an Adventure component.
     *
     * @param vanillaComponent the vanilla component (IChatBaseComponent/Component)
     * @return the Adventure component
     * @throws IllegalArgumentException if the object is not a valid vanilla component
     * @since 1.0.0
     */
    @NotNull
    Component fromVanilla(@NotNull Object vanillaComponent);

    /**
     * Converts a vanilla component to Adventure, returning null for null input.
     *
     * @param vanillaComponent the vanilla component, may be null
     * @return the Adventure component, or null
     * @since 1.0.0
     */
    @Nullable
    default Component fromVanillaNullable(@Nullable Object vanillaComponent) {
        return vanillaComponent == null ? null : fromVanilla(vanillaComponent);
    }

    /**
     * Attempts to convert a vanilla component, returning empty on failure.
     *
     * @param vanillaComponent the vanilla component
     * @return the Adventure component, or empty component on failure
     * @since 1.0.0
     */
    @NotNull
    default Component fromVanillaSafe(@Nullable Object vanillaComponent) {
        if (vanillaComponent == null) {
            return Component.empty();
        }
        try {
            return fromVanilla(vanillaComponent);
        } catch (Exception e) {
            return Component.empty();
        }
    }

    // ===== JSON Serialization =====

    /**
     * Serializes an Adventure component to JSON format.
     *
     * <p>The JSON format is compatible with Minecraft's component JSON format
     * and can be used in commands, storage, or network protocols.
     *
     * @param component the Adventure component
     * @return the JSON string
     * @since 1.0.0
     */
    @NotNull
    String toJson(@NotNull Component component);

    /**
     * Deserializes a JSON string to an Adventure component.
     *
     * @param json the JSON string
     * @return the Adventure component
     * @throws IllegalArgumentException if the JSON is invalid
     * @since 1.0.0
     */
    @NotNull
    Component fromJson(@NotNull String json);

    /**
     * Attempts to deserialize JSON, returning empty on failure.
     *
     * @param json the JSON string
     * @return the Adventure component, or empty component on failure
     * @since 1.0.0
     */
    @NotNull
    default Component fromJsonSafe(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return Component.empty();
        }
        try {
            return fromJson(json);
        } catch (Exception e) {
            return Component.empty();
        }
    }

    // ===== Legacy Text Conversion =====

    /**
     * Converts a legacy text string (with section symbols) to an Adventure component.
     *
     * <p>Supports both section symbol ({@code &#167;}) and ampersand ({@code &}) color codes.
     *
     * @param legacyText the legacy text string
     * @return the Adventure component
     * @since 1.0.0
     */
    @NotNull
    Component fromLegacy(@NotNull String legacyText);

    /**
     * Converts an Adventure component to a legacy text string.
     *
     * <p>Uses section symbols ({@code &#167;}) for color codes.
     *
     * @param component the Adventure component
     * @return the legacy text string
     * @since 1.0.0
     */
    @NotNull
    String toLegacy(@NotNull Component component);

    // ===== MiniMessage Support =====

    /**
     * Parses a MiniMessage formatted string to an Adventure component.
     *
     * <p>MiniMessage is Adventure's tag-based format:
     * {@code <red>Error: <bold>Something went wrong</bold></red>}
     *
     * @param miniMessage the MiniMessage string
     * @return the Adventure component
     * @since 1.0.0
     */
    @NotNull
    Component fromMiniMessage(@NotNull String miniMessage);

    /**
     * Serializes an Adventure component to MiniMessage format.
     *
     * @param component the Adventure component
     * @return the MiniMessage string
     * @since 1.0.0
     */
    @NotNull
    String toMiniMessage(@NotNull Component component);

    // ===== Plain Text =====

    /**
     * Extracts plain text content from a component.
     *
     * <p>Strips all formatting, colors, and styling.
     *
     * @param component the Adventure component
     * @return the plain text content
     * @since 1.0.0
     */
    @NotNull
    String toPlainText(@NotNull Component component);

    /**
     * Creates a plain text component.
     *
     * @param text the plain text
     * @return the Adventure component
     * @since 1.0.0
     */
    @NotNull
    default Component text(@NotNull String text) {
        return Component.text(text);
    }

    // ===== Utility Methods =====

    /**
     * Creates an empty component.
     *
     * @return an empty Adventure component
     * @since 1.0.0
     */
    @NotNull
    default Component empty() {
        return Component.empty();
    }

    /**
     * Creates a newline component.
     *
     * @return a newline component
     * @since 1.0.0
     */
    @NotNull
    default Component newline() {
        return Component.newline();
    }

    /**
     * Creates a space component.
     *
     * @return a space component
     * @since 1.0.0
     */
    @NotNull
    default Component space() {
        return Component.space();
    }

    /**
     * Joins multiple components with a separator.
     *
     * @param separator the separator component
     * @param components the components to join
     * @return the joined component
     * @since 1.0.0
     */
    @NotNull
    Component join(@NotNull Component separator, @NotNull Component... components);

    /**
     * Joins multiple components with a separator.
     *
     * @param separator the separator component
     * @param components the components to join
     * @return the joined component
     * @since 1.0.0
     */
    @NotNull
    Component join(@NotNull Component separator, @NotNull Iterable<Component> components);

    /**
     * Checks if a component is empty or contains only whitespace.
     *
     * @param component the component to check
     * @return true if empty or whitespace-only
     * @since 1.0.0
     */
    boolean isEmpty(@NotNull Component component);

    /**
     * Gets the class of the vanilla component type for this version.
     *
     * @return the vanilla component class
     * @since 1.0.0
     */
    @NotNull
    Class<?> getVanillaComponentClass();

    /**
     * Checks if an object is a valid vanilla component.
     *
     * @param object the object to check
     * @return true if it's a valid vanilla component
     * @since 1.0.0
     */
    default boolean isVanillaComponent(@Nullable Object object) {
        return object != null && getVanillaComponentClass().isInstance(object);
    }

    @Override
    default String getServiceName() {
        return "ComponentBridge";
    }
}
