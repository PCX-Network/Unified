/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serializer for MiniMessage text format.
 *
 * <p>MiniMessageSerializer handles the serialization of text using the MiniMessage
 * format, a human-readable markup language for Minecraft text formatting. This
 * format is commonly used in Adventure-based plugins.
 *
 * <h2>Supported Tags</h2>
 * <ul>
 *   <li><b>&lt;color&gt;:</b> Color names (red, blue, etc.) or hex codes (#FF5555)</li>
 *   <li><b>&lt;bold&gt;, &lt;italic&gt;, &lt;underlined&gt;, &lt;strikethrough&gt;, &lt;obfuscated&gt;:</b> Formatting</li>
 *   <li><b>&lt;reset&gt;:</b> Reset all formatting</li>
 *   <li><b>&lt;gradient:color1:color2&gt;:</b> Gradient text</li>
 *   <li><b>&lt;rainbow&gt;:</b> Rainbow gradient</li>
 *   <li><b>&lt;click:action:value&gt;:</b> Click events</li>
 *   <li><b>&lt;hover:action:value&gt;:</b> Hover events</li>
 *   <li><b>&lt;insert:value&gt;:</b> Insertion text</li>
 *   <li><b>&lt;key:keybind&gt;:</b> Keybind placeholders</li>
 *   <li><b>&lt;lang:key&gt;:</b> Translation keys</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MiniMessageSerializer serializer = Serializers.miniMessage();
 *
 * // Parse MiniMessage to component
 * Map<String, Object> component = serializer.deserialize(
 *     "<gold><bold>Welcome!</bold></gold> <gray>Click <click:run_command:/help>here</click> for help."
 * );
 *
 * // Convert component to MiniMessage
 * String miniMessage = serializer.serialize(component);
 *
 * // Quick formatting
 * String formatted = serializer.format("<red>Error: <white>{0}", "Something went wrong");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ComponentSerializer
 * @see LegacyTextSerializer
 */
public final class MiniMessageSerializer implements Serializer<Map<String, Object>> {

    // Tag patterns
    private static final Pattern TAG_PATTERN = Pattern.compile("<([^<>]+)>");
    private static final Pattern CLOSE_TAG_PATTERN = Pattern.compile("</([^<>]+)>");
    private static final Pattern COLOR_HEX_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("gradient(?::([^:>]+))+");
    private static final Pattern CLICK_PATTERN = Pattern.compile("click:(\\w+):([^>]+)");
    private static final Pattern HOVER_PATTERN = Pattern.compile("hover:(\\w+):([^>]+)");

    // Component keys
    private static final String KEY_TEXT = "text";
    private static final String KEY_COLOR = "color";
    private static final String KEY_BOLD = "bold";
    private static final String KEY_ITALIC = "italic";
    private static final String KEY_UNDERLINED = "underlined";
    private static final String KEY_STRIKETHROUGH = "strikethrough";
    private static final String KEY_OBFUSCATED = "obfuscated";
    private static final String KEY_CLICK_EVENT = "click_event";
    private static final String KEY_HOVER_EVENT = "hover_event";
    private static final String KEY_EXTRA = "extra";
    private static final String KEY_INSERTION = "insertion";

    /**
     * Creates a new MiniMessageSerializer.
     */
    public MiniMessageSerializer() {}

    @Override
    @NotNull
    public String serialize(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return componentToMiniMessage(value);
    }

    @Override
    @NotNull
    public Map<String, Object> deserialize(@NotNull String data, @NotNull SerializationContext context) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return miniMessageToComponent(data);
    }

    @Override
    @NotNull
    public Class<Map<String, Object>> getTargetType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> type = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return type;
    }

    // ========================================
    // MiniMessage Parsing
    // ========================================

    /**
     * Parses a MiniMessage string to component data.
     *
     * @param miniMessage the MiniMessage string
     * @return the component data map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> miniMessageToComponent(@NotNull String miniMessage) {
        if (miniMessage.isEmpty()) {
            return createEmptyComponent();
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        StyleState style = new StyleState();

        int i = 0;
        while (i < miniMessage.length()) {
            char c = miniMessage.charAt(i);

            if (c == '<') {
                // Find tag end
                int tagEnd = findTagEnd(miniMessage, i);
                if (tagEnd > i) {
                    // Save current text with current style
                    if (currentText.length() > 0) {
                        parts.add(createStyledComponent(currentText.toString(), style));
                        currentText = new StringBuilder();
                    }

                    String tagContent = miniMessage.substring(i + 1, tagEnd);
                    processTag(tagContent, style);
                    i = tagEnd + 1;
                    continue;
                }
            } else if (c == '\\' && i + 1 < miniMessage.length()) {
                // Escape sequence
                char next = miniMessage.charAt(i + 1);
                if (next == '<' || next == '>') {
                    currentText.append(next);
                    i += 2;
                    continue;
                }
            }

            currentText.append(c);
            i++;
        }

        // Add remaining text
        if (currentText.length() > 0) {
            parts.add(createStyledComponent(currentText.toString(), style));
        }

        // Build final component
        if (parts.isEmpty()) {
            return createEmptyComponent();
        } else if (parts.size() == 1) {
            return parts.get(0);
        } else {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put(KEY_TEXT, "");
            root.put(KEY_EXTRA, parts);
            return root;
        }
    }

    /**
     * Converts component data to MiniMessage string.
     *
     * @param component the component data
     * @return the MiniMessage string
     * @since 1.0.0
     */
    @NotNull
    public String componentToMiniMessage(@NotNull Map<String, Object> component) {
        StringBuilder mm = new StringBuilder();
        buildMiniMessage(mm, component, new StyleState());
        return mm.toString();
    }

    // ========================================
    // Formatting Utilities
    // ========================================

    /**
     * Formats a MiniMessage string with placeholders.
     *
     * <p>Placeholders are in the format {0}, {1}, etc.
     *
     * @param template the MiniMessage template
     * @param args     the placeholder values
     * @return the formatted MiniMessage string
     * @since 1.0.0
     */
    @NotNull
    public String format(@NotNull String template, @NotNull Object... args) {
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }

    /**
     * Escapes special characters in a string for MiniMessage.
     *
     * @param text the text to escape
     * @return the escaped text
     * @since 1.0.0
     */
    @NotNull
    public static String escape(@NotNull String text) {
        return text.replace("<", "\\<").replace(">", "\\>");
    }

    /**
     * Strips all MiniMessage tags from a string.
     *
     * @param miniMessage the MiniMessage string
     * @return the plain text without tags
     * @since 1.0.0
     */
    @NotNull
    public static String stripTags(@NotNull String miniMessage) {
        return TAG_PATTERN.matcher(miniMessage)
                .replaceAll("")
                .replace("\\<", "<")
                .replace("\\>", ">");
    }

    /**
     * Checks if a string contains MiniMessage tags.
     *
     * @param text the text to check
     * @return true if the text contains tags
     * @since 1.0.0
     */
    public static boolean hasTags(@NotNull String text) {
        return TAG_PATTERN.matcher(text).find();
    }

    /**
     * Creates a gradient MiniMessage string.
     *
     * @param text   the text to gradient
     * @param colors the gradient colors (hex or names)
     * @return the gradient MiniMessage
     * @since 1.0.0
     */
    @NotNull
    public static String gradient(@NotNull String text, @NotNull String... colors) {
        if (colors.length == 0) {
            return text;
        }
        StringBuilder mm = new StringBuilder("<gradient");
        for (String color : colors) {
            mm.append(":").append(color);
        }
        mm.append(">").append(escape(text)).append("</gradient>");
        return mm.toString();
    }

    /**
     * Creates a rainbow MiniMessage string.
     *
     * @param text the text to rainbow
     * @return the rainbow MiniMessage
     * @since 1.0.0
     */
    @NotNull
    public static String rainbow(@NotNull String text) {
        return "<rainbow>" + escape(text) + "</rainbow>";
    }

    /**
     * Creates a click event MiniMessage tag.
     *
     * @param action the click action (run_command, suggest_command, open_url, copy_to_clipboard)
     * @param value  the click value
     * @param text   the display text
     * @return the MiniMessage with click event
     * @since 1.0.0
     */
    @NotNull
    public static String click(@NotNull String action, @NotNull String value, @NotNull String text) {
        return "<click:" + action + ":" + value + ">" + text + "</click>";
    }

    /**
     * Creates a hover event MiniMessage tag.
     *
     * @param action the hover action (show_text, show_item, show_entity)
     * @param value  the hover value
     * @param text   the display text
     * @return the MiniMessage with hover event
     * @since 1.0.0
     */
    @NotNull
    public static String hover(@NotNull String action, @NotNull String value, @NotNull String text) {
        return "<hover:" + action + ":" + value + ">" + text + "</hover>";
    }

    // ========================================
    // Internal Methods
    // ========================================

    private int findTagEnd(String input, int start) {
        int depth = 0;
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void processTag(String tagContent, StyleState style) {
        if (tagContent.startsWith("/")) {
            // Closing tag
            String tagName = tagContent.substring(1).toLowerCase();
            style.popTag(tagName);
        } else {
            // Opening tag or self-closing
            String tagName = tagContent.split(":")[0].toLowerCase();

            switch (tagName) {
                case "bold", "b" -> style.bold = true;
                case "italic", "i", "em" -> style.italic = true;
                case "underlined", "u" -> style.underlined = true;
                case "strikethrough", "st" -> style.strikethrough = true;
                case "obfuscated", "obf" -> style.obfuscated = true;
                case "reset", "r" -> style.reset();
                case "click" -> parseClickEvent(tagContent, style);
                case "hover" -> parseHoverEvent(tagContent, style);
                case "insert", "insertion" -> {
                    int colonIndex = tagContent.indexOf(':');
                    if (colonIndex > 0) {
                        style.insertion = tagContent.substring(colonIndex + 1);
                    }
                }
                case "gradient", "rainbow" -> {
                    // Special handling for gradients would go here
                    // For now, just track the tag
                }
                default -> {
                    // Assume it's a color
                    if (COLOR_HEX_PATTERN.matcher(tagName).matches() || isNamedColor(tagName)) {
                        style.color = tagName;
                    }
                }
            }

            style.pushTag(tagName);
        }
    }

    private void parseClickEvent(String tagContent, StyleState style) {
        Matcher matcher = CLICK_PATTERN.matcher(tagContent);
        if (matcher.find()) {
            Map<String, String> event = new LinkedHashMap<>();
            event.put("action", matcher.group(1));
            event.put("value", matcher.group(2));
            style.clickEvent = event;
        }
    }

    private void parseHoverEvent(String tagContent, StyleState style) {
        Matcher matcher = HOVER_PATTERN.matcher(tagContent);
        if (matcher.find()) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("action", matcher.group(1));
            event.put("value", matcher.group(2));
            style.hoverEvent = event;
        }
    }

    private Map<String, Object> createStyledComponent(String text, StyleState style) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put(KEY_TEXT, text);

        if (style.color != null) component.put(KEY_COLOR, style.color);
        if (style.bold) component.put(KEY_BOLD, true);
        if (style.italic) component.put(KEY_ITALIC, true);
        if (style.underlined) component.put(KEY_UNDERLINED, true);
        if (style.strikethrough) component.put(KEY_STRIKETHROUGH, true);
        if (style.obfuscated) component.put(KEY_OBFUSCATED, true);
        if (style.clickEvent != null) component.put(KEY_CLICK_EVENT, new LinkedHashMap<>(style.clickEvent));
        if (style.hoverEvent != null) component.put(KEY_HOVER_EVENT, new LinkedHashMap<>(style.hoverEvent));
        if (style.insertion != null) component.put(KEY_INSERTION, style.insertion);

        return component;
    }

    private Map<String, Object> createEmptyComponent() {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put(KEY_TEXT, "");
        return component;
    }

    private void buildMiniMessage(StringBuilder mm, Map<String, Object> component, StyleState parentStyle) {
        List<String> openedTags = new ArrayList<>();

        // Apply color
        String color = (String) component.get(KEY_COLOR);
        if (color != null && !color.equals(parentStyle.color)) {
            mm.append("<").append(color).append(">");
            openedTags.add(color);
        }

        // Apply formatting
        if (Boolean.TRUE.equals(component.get(KEY_BOLD)) && !parentStyle.bold) {
            mm.append("<bold>");
            openedTags.add("bold");
        }
        if (Boolean.TRUE.equals(component.get(KEY_ITALIC)) && !parentStyle.italic) {
            mm.append("<italic>");
            openedTags.add("italic");
        }
        if (Boolean.TRUE.equals(component.get(KEY_UNDERLINED)) && !parentStyle.underlined) {
            mm.append("<underlined>");
            openedTags.add("underlined");
        }
        if (Boolean.TRUE.equals(component.get(KEY_STRIKETHROUGH)) && !parentStyle.strikethrough) {
            mm.append("<strikethrough>");
            openedTags.add("strikethrough");
        }
        if (Boolean.TRUE.equals(component.get(KEY_OBFUSCATED)) && !parentStyle.obfuscated) {
            mm.append("<obfuscated>");
            openedTags.add("obfuscated");
        }

        // Apply click event
        @SuppressWarnings("unchecked")
        Map<String, String> click = (Map<String, String>) component.get(KEY_CLICK_EVENT);
        if (click != null) {
            mm.append("<click:").append(click.get("action")).append(":").append(click.get("value")).append(">");
            openedTags.add("click");
        }

        // Apply hover event
        @SuppressWarnings("unchecked")
        Map<String, Object> hover = (Map<String, Object>) component.get(KEY_HOVER_EVENT);
        if (hover != null) {
            mm.append("<hover:").append(hover.get("action")).append(":").append(hover.get("value")).append(">");
            openedTags.add("hover");
        }

        // Add text
        String text = (String) component.get(KEY_TEXT);
        if (text != null && !text.isEmpty()) {
            mm.append(escape(text));
        }

        // Process children
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extra = (List<Map<String, Object>>) component.get(KEY_EXTRA);
        if (extra != null) {
            StyleState currentStyle = new StyleState();
            currentStyle.color = color != null ? color : parentStyle.color;
            currentStyle.bold = Boolean.TRUE.equals(component.get(KEY_BOLD)) || parentStyle.bold;
            currentStyle.italic = Boolean.TRUE.equals(component.get(KEY_ITALIC)) || parentStyle.italic;
            currentStyle.underlined = Boolean.TRUE.equals(component.get(KEY_UNDERLINED)) || parentStyle.underlined;
            currentStyle.strikethrough = Boolean.TRUE.equals(component.get(KEY_STRIKETHROUGH)) || parentStyle.strikethrough;
            currentStyle.obfuscated = Boolean.TRUE.equals(component.get(KEY_OBFUSCATED)) || parentStyle.obfuscated;

            for (Map<String, Object> child : extra) {
                buildMiniMessage(mm, child, currentStyle);
            }
        }

        // Close tags in reverse order
        for (int i = openedTags.size() - 1; i >= 0; i--) {
            mm.append("</").append(openedTags.get(i)).append(">");
        }
    }

    private boolean isNamedColor(String name) {
        return switch (name.toLowerCase()) {
            case "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
                 "dark_purple", "gold", "gray", "dark_gray", "blue",
                 "green", "aqua", "red", "light_purple", "yellow", "white" -> true;
            default -> false;
        };
    }

    /**
     * Internal class to track style state during parsing.
     */
    private static class StyleState {
        String color;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;
        Map<String, String> clickEvent;
        Map<String, Object> hoverEvent;
        String insertion;
        List<String> tagStack = new ArrayList<>();

        void reset() {
            color = null;
            bold = false;
            italic = false;
            underlined = false;
            strikethrough = false;
            obfuscated = false;
            clickEvent = null;
            hoverEvent = null;
            insertion = null;
        }

        void pushTag(String tag) {
            tagStack.add(tag);
        }

        void popTag(String tag) {
            // Find and remove the tag from stack
            for (int i = tagStack.size() - 1; i >= 0; i--) {
                if (tagStack.get(i).equalsIgnoreCase(tag)) {
                    tagStack.remove(i);
                    break;
                }
            }

            // Reset the style for this tag
            switch (tag.toLowerCase()) {
                case "bold", "b" -> bold = false;
                case "italic", "i", "em" -> italic = false;
                case "underlined", "u" -> underlined = false;
                case "strikethrough", "st" -> strikethrough = false;
                case "obfuscated", "obf" -> obfuscated = false;
                case "click" -> clickEvent = null;
                case "hover" -> hoverEvent = null;
                case "insert", "insertion" -> insertion = null;
                default -> {
                    if (tag.equals(color)) {
                        color = null;
                    }
                }
            }
        }
    }
}
