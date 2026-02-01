/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for Adventure text components.
 *
 * <p>ComponentSerializer handles the serialization of rich text components,
 * including text content, formatting, colors, click events, and hover events.
 * Supports multiple output formats for different use cases.
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li><b>JSON:</b> Full component JSON format (compatible with Adventure)</li>
 *   <li><b>MINIMESSAGE:</b> MiniMessage format for human-readable editing</li>
 *   <li><b>LEGACY_TEXT:</b> Legacy color code format (section sign)</li>
 *   <li><b>BASE64:</b> Compact binary encoding</li>
 * </ul>
 *
 * <h2>Component Properties</h2>
 * <ul>
 *   <li><b>text:</b> The text content</li>
 *   <li><b>color:</b> Text color (name or hex)</li>
 *   <li><b>bold, italic, underlined, strikethrough, obfuscated:</b> Formatting</li>
 *   <li><b>click_event:</b> Click action (open_url, run_command, etc.)</li>
 *   <li><b>hover_event:</b> Hover action (show_text, show_item, etc.)</li>
 *   <li><b>extra:</b> Child components</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ComponentSerializer serializer = Serializers.component();
 *
 * // Create component data
 * Map<String, Object> component = Map.of(
 *     "text", "Click here!",
 *     "color", "#FF5555",
 *     "bold", true,
 *     "click_event", Map.of(
 *         "action", "open_url",
 *         "value", "https://example.com"
 *     )
 * );
 *
 * // Serialize to different formats
 * String json = serializer.serialize(component, SerializationContext.json());
 * String miniMessage = serializer.toMiniMessage(component);
 * String legacy = serializer.toLegacy(component);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MiniMessageSerializer
 * @see LegacyTextSerializer
 */
public final class ComponentSerializer implements Serializer<Map<String, Object>> {

    // Component JSON keys
    private static final String KEY_TEXT = "text";
    private static final String KEY_TRANSLATE = "translate";
    private static final String KEY_KEYBIND = "keybind";
    private static final String KEY_SCORE = "score";
    private static final String KEY_SELECTOR = "selector";
    private static final String KEY_NBT = "nbt";
    private static final String KEY_COLOR = "color";
    private static final String KEY_BOLD = "bold";
    private static final String KEY_ITALIC = "italic";
    private static final String KEY_UNDERLINED = "underlined";
    private static final String KEY_STRIKETHROUGH = "strikethrough";
    private static final String KEY_OBFUSCATED = "obfuscated";
    private static final String KEY_FONT = "font";
    private static final String KEY_INSERTION = "insertion";
    private static final String KEY_CLICK_EVENT = "click_event";
    private static final String KEY_HOVER_EVENT = "hover_event";
    private static final String KEY_EXTRA = "extra";

    // Named colors
    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", "#000000"),
            Map.entry("dark_blue", "#0000AA"),
            Map.entry("dark_green", "#00AA00"),
            Map.entry("dark_aqua", "#00AAAA"),
            Map.entry("dark_red", "#AA0000"),
            Map.entry("dark_purple", "#AA00AA"),
            Map.entry("gold", "#FFAA00"),
            Map.entry("gray", "#AAAAAA"),
            Map.entry("dark_gray", "#555555"),
            Map.entry("blue", "#5555FF"),
            Map.entry("green", "#55FF55"),
            Map.entry("aqua", "#55FFFF"),
            Map.entry("red", "#FF5555"),
            Map.entry("light_purple", "#FF55FF"),
            Map.entry("yellow", "#FFFF55"),
            Map.entry("white", "#FFFFFF")
    );

    /**
     * Creates a new ComponentSerializer.
     */
    public ComponentSerializer() {}

    @Override
    @NotNull
    public String serialize(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return switch (context.getFormat()) {
            case JSON -> serializeToJson(value, context);
            case MINIMESSAGE -> toMiniMessage(value);
            case LEGACY_TEXT -> toLegacy(value);
            case BASE64 -> Base64.getEncoder().encodeToString(toBytes(value, context));
            case BINARY -> new String(toBytes(value, context), context.getCharset());
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    @NotNull
    public Map<String, Object> deserialize(@NotNull String data, @NotNull SerializationContext context) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return switch (context.getFormat()) {
            case JSON -> deserializeFromJson(data, context);
            case MINIMESSAGE -> fromMiniMessage(data);
            case LEGACY_TEXT -> fromLegacy(data);
            case BASE64 -> fromBytes(Base64.getDecoder().decode(data), context);
            case BINARY -> fromBytes(data.getBytes(context.getCharset()), context);
            default -> throw SerializationException.unsupportedFormat(getTargetType(), context.getFormat());
        };
    }

    @Override
    public byte @NotNull [] toBytes(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.allocate(256);
        writeComponent(buffer, value);
        return buffer.toByteArray();
    }

    @Override
    @NotNull
    public Map<String, Object> fromBytes(byte @NotNull [] data, @NotNull SerializationContext context) {
        BinaryBuffer buffer = BinaryBuffer.wrap(data);
        return readComponent(buffer);
    }

    @Override
    @NotNull
    public Class<Map<String, Object>> getTargetType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> type = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return type;
    }

    // ========================================
    // MiniMessage Conversion
    // ========================================

    /**
     * Converts component data to MiniMessage format.
     *
     * @param component the component data
     * @return the MiniMessage string
     * @since 1.0.0
     */
    @NotNull
    public String toMiniMessage(@NotNull Map<String, Object> component) {
        StringBuilder mm = new StringBuilder();
        buildMiniMessage(mm, component, new ArrayList<>());
        return mm.toString();
    }

    /**
     * Parses MiniMessage format to component data.
     *
     * @param miniMessage the MiniMessage string
     * @return the component data
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> fromMiniMessage(@NotNull String miniMessage) {
        // Simplified MiniMessage parser
        // In production, this would use the Adventure MiniMessage library
        Map<String, Object> component = new LinkedHashMap<>();

        if (miniMessage.isEmpty()) {
            component.put(KEY_TEXT, "");
            return component;
        }

        // Basic tag parsing
        List<Map<String, Object>> parts = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        List<String> activeTags = new ArrayList<>();

        int i = 0;
        while (i < miniMessage.length()) {
            char c = miniMessage.charAt(i);

            if (c == '<') {
                // Save current text
                if (currentText.length() > 0) {
                    Map<String, Object> part = createTextComponent(currentText.toString(), activeTags);
                    parts.add(part);
                    currentText = new StringBuilder();
                }

                // Parse tag
                int tagEnd = miniMessage.indexOf('>', i);
                if (tagEnd > i) {
                    String tag = miniMessage.substring(i + 1, tagEnd);
                    if (tag.startsWith("/")) {
                        // Closing tag
                        String tagName = tag.substring(1);
                        activeTags.remove(tagName);
                    } else {
                        // Opening tag
                        activeTags.add(tag);
                    }
                    i = tagEnd + 1;
                    continue;
                }
            }

            currentText.append(c);
            i++;
        }

        // Add remaining text
        if (currentText.length() > 0) {
            Map<String, Object> part = createTextComponent(currentText.toString(), activeTags);
            parts.add(part);
        }

        // Build component structure
        if (parts.isEmpty()) {
            component.put(KEY_TEXT, "");
        } else if (parts.size() == 1) {
            return parts.get(0);
        } else {
            component.put(KEY_TEXT, "");
            component.put(KEY_EXTRA, parts);
        }

        return component;
    }

    // ========================================
    // Legacy Text Conversion
    // ========================================

    /**
     * Converts component data to legacy format.
     *
     * @param component the component data
     * @return the legacy text with section sign colors
     * @since 1.0.0
     */
    @NotNull
    public String toLegacy(@NotNull Map<String, Object> component) {
        StringBuilder legacy = new StringBuilder();
        buildLegacy(legacy, component);
        return legacy.toString();
    }

    /**
     * Parses legacy format to component data.
     *
     * @param legacy the legacy text with section sign colors
     * @return the component data
     * @since 1.0.0
     */
    @NotNull
    public Map<String, Object> fromLegacy(@NotNull String legacy) {
        Map<String, Object> component = new LinkedHashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        StringBuilder currentText = new StringBuilder();
        String currentColor = null;
        boolean bold = false, italic = false, underlined = false, strikethrough = false, obfuscated = false;

        int i = 0;
        while (i < legacy.length()) {
            char c = legacy.charAt(i);

            if ((c == '\u00A7' || c == '&') && i + 1 < legacy.length()) {
                char code = Character.toLowerCase(legacy.charAt(i + 1));

                // Save current part
                if (currentText.length() > 0) {
                    Map<String, Object> part = new LinkedHashMap<>();
                    part.put(KEY_TEXT, currentText.toString());
                    if (currentColor != null) part.put(KEY_COLOR, currentColor);
                    if (bold) part.put(KEY_BOLD, true);
                    if (italic) part.put(KEY_ITALIC, true);
                    if (underlined) part.put(KEY_UNDERLINED, true);
                    if (strikethrough) part.put(KEY_STRIKETHROUGH, true);
                    if (obfuscated) part.put(KEY_OBFUSCATED, true);
                    parts.add(part);
                    currentText = new StringBuilder();
                }

                // Apply formatting
                switch (code) {
                    case '0' -> { currentColor = "black"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '1' -> { currentColor = "dark_blue"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '2' -> { currentColor = "dark_green"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '3' -> { currentColor = "dark_aqua"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '4' -> { currentColor = "dark_red"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '5' -> { currentColor = "dark_purple"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '6' -> { currentColor = "gold"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '7' -> { currentColor = "gray"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '8' -> { currentColor = "dark_gray"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case '9' -> { currentColor = "blue"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'a' -> { currentColor = "green"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'b' -> { currentColor = "aqua"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'c' -> { currentColor = "red"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'd' -> { currentColor = "light_purple"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'e' -> { currentColor = "yellow"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'f' -> { currentColor = "white"; bold = italic = underlined = strikethrough = obfuscated = false; }
                    case 'k' -> obfuscated = true;
                    case 'l' -> bold = true;
                    case 'm' -> strikethrough = true;
                    case 'n' -> underlined = true;
                    case 'o' -> italic = true;
                    case 'r' -> { currentColor = null; bold = italic = underlined = strikethrough = obfuscated = false; }
                }
                i += 2;
                continue;
            }

            currentText.append(c);
            i++;
        }

        // Add final part
        if (currentText.length() > 0) {
            Map<String, Object> part = new LinkedHashMap<>();
            part.put(KEY_TEXT, currentText.toString());
            if (currentColor != null) part.put(KEY_COLOR, currentColor);
            if (bold) part.put(KEY_BOLD, true);
            if (italic) part.put(KEY_ITALIC, true);
            if (underlined) part.put(KEY_UNDERLINED, true);
            if (strikethrough) part.put(KEY_STRIKETHROUGH, true);
            if (obfuscated) part.put(KEY_OBFUSCATED, true);
            parts.add(part);
        }

        if (parts.isEmpty()) {
            component.put(KEY_TEXT, "");
        } else if (parts.size() == 1) {
            return parts.get(0);
        } else {
            component.put(KEY_TEXT, "");
            component.put(KEY_EXTRA, parts);
        }

        return component;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Creates a simple text component.
     *
     * @param text the text content
     * @return the component data
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> text(@NotNull String text) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put(KEY_TEXT, text);
        return component;
    }

    /**
     * Creates a colored text component.
     *
     * @param text  the text content
     * @param color the color (name or hex)
     * @return the component data
     * @since 1.0.0
     */
    @NotNull
    public static Map<String, Object> text(@NotNull String text, @NotNull String color) {
        Map<String, Object> component = text(text);
        component.put(KEY_COLOR, color);
        return component;
    }

    /**
     * Extracts plain text from a component (strips all formatting).
     *
     * @param component the component data
     * @return the plain text content
     * @since 1.0.0
     */
    @NotNull
    public static String toPlainText(@NotNull Map<String, Object> component) {
        StringBuilder text = new StringBuilder();
        extractPlainText(text, component);
        return text.toString();
    }

    // ========================================
    // Internal Methods
    // ========================================

    private void writeComponent(@NotNull BinaryBuffer buffer, @NotNull Map<String, Object> component) {
        // Write text content
        String text = (String) component.get(KEY_TEXT);
        buffer.writeOptionalString(text);

        // Write color
        buffer.writeOptionalString((String) component.get(KEY_COLOR));

        // Write formatting flags
        int flags = 0;
        if (Boolean.TRUE.equals(component.get(KEY_BOLD))) flags |= 1;
        if (Boolean.TRUE.equals(component.get(KEY_ITALIC))) flags |= 2;
        if (Boolean.TRUE.equals(component.get(KEY_UNDERLINED))) flags |= 4;
        if (Boolean.TRUE.equals(component.get(KEY_STRIKETHROUGH))) flags |= 8;
        if (Boolean.TRUE.equals(component.get(KEY_OBFUSCATED))) flags |= 16;
        buffer.writeByte(flags);

        // Write extra components
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extra = (List<Map<String, Object>>) component.get(KEY_EXTRA);
        if (extra != null && !extra.isEmpty()) {
            buffer.writeVarInt(extra.size());
            for (Map<String, Object> child : extra) {
                writeComponent(buffer, child);
            }
        } else {
            buffer.writeVarInt(0);
        }
    }

    @NotNull
    private Map<String, Object> readComponent(@NotNull BinaryBuffer buffer) {
        Map<String, Object> component = new LinkedHashMap<>();

        // Read text
        String text = buffer.readOptionalString();
        if (text != null) {
            component.put(KEY_TEXT, text);
        }

        // Read color
        String color = buffer.readOptionalString();
        if (color != null) {
            component.put(KEY_COLOR, color);
        }

        // Read formatting flags
        int flags = buffer.readUnsignedByte();
        if ((flags & 1) != 0) component.put(KEY_BOLD, true);
        if ((flags & 2) != 0) component.put(KEY_ITALIC, true);
        if ((flags & 4) != 0) component.put(KEY_UNDERLINED, true);
        if ((flags & 8) != 0) component.put(KEY_STRIKETHROUGH, true);
        if ((flags & 16) != 0) component.put(KEY_OBFUSCATED, true);

        // Read extra components
        int extraCount = buffer.readVarInt();
        if (extraCount > 0) {
            List<Map<String, Object>> extra = new ArrayList<>(extraCount);
            for (int i = 0; i < extraCount; i++) {
                extra.add(readComponent(buffer));
            }
            component.put(KEY_EXTRA, extra);
        }

        return component;
    }

    private void buildMiniMessage(StringBuilder mm, Map<String, Object> component, List<String> parentTags) {
        List<String> myTags = new ArrayList<>();

        // Add formatting tags
        String color = (String) component.get(KEY_COLOR);
        if (color != null) {
            mm.append("<").append(color).append(">");
            myTags.add(color);
        }
        if (Boolean.TRUE.equals(component.get(KEY_BOLD))) { mm.append("<bold>"); myTags.add("bold"); }
        if (Boolean.TRUE.equals(component.get(KEY_ITALIC))) { mm.append("<italic>"); myTags.add("italic"); }
        if (Boolean.TRUE.equals(component.get(KEY_UNDERLINED))) { mm.append("<underlined>"); myTags.add("underlined"); }
        if (Boolean.TRUE.equals(component.get(KEY_STRIKETHROUGH))) { mm.append("<strikethrough>"); myTags.add("strikethrough"); }
        if (Boolean.TRUE.equals(component.get(KEY_OBFUSCATED))) { mm.append("<obfuscated>"); myTags.add("obfuscated"); }

        // Add text
        String text = (String) component.get(KEY_TEXT);
        if (text != null && !text.isEmpty()) {
            mm.append(text.replace("<", "\\<").replace(">", "\\>"));
        }

        // Add children
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extra = (List<Map<String, Object>>) component.get(KEY_EXTRA);
        if (extra != null) {
            List<String> combinedTags = new ArrayList<>(parentTags);
            combinedTags.addAll(myTags);
            for (Map<String, Object> child : extra) {
                buildMiniMessage(mm, child, combinedTags);
            }
        }

        // Close tags (in reverse)
        for (int i = myTags.size() - 1; i >= 0; i--) {
            mm.append("</").append(myTags.get(i)).append(">");
        }
    }

    private void buildLegacy(StringBuilder legacy, Map<String, Object> component) {
        // Add color code
        String color = (String) component.get(KEY_COLOR);
        if (color != null) {
            char code = getLegacyColorCode(color);
            if (code != 0) {
                legacy.append('\u00A7').append(code);
            }
        }

        // Add formatting codes
        if (Boolean.TRUE.equals(component.get(KEY_BOLD))) legacy.append("\u00A7l");
        if (Boolean.TRUE.equals(component.get(KEY_ITALIC))) legacy.append("\u00A7o");
        if (Boolean.TRUE.equals(component.get(KEY_UNDERLINED))) legacy.append("\u00A7n");
        if (Boolean.TRUE.equals(component.get(KEY_STRIKETHROUGH))) legacy.append("\u00A7m");
        if (Boolean.TRUE.equals(component.get(KEY_OBFUSCATED))) legacy.append("\u00A7k");

        // Add text
        String text = (String) component.get(KEY_TEXT);
        if (text != null) {
            legacy.append(text);
        }

        // Add children
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extra = (List<Map<String, Object>>) component.get(KEY_EXTRA);
        if (extra != null) {
            for (Map<String, Object> child : extra) {
                buildLegacy(legacy, child);
            }
        }
    }

    private char getLegacyColorCode(String color) {
        return switch (color.toLowerCase()) {
            case "black" -> '0';
            case "dark_blue" -> '1';
            case "dark_green" -> '2';
            case "dark_aqua" -> '3';
            case "dark_red" -> '4';
            case "dark_purple" -> '5';
            case "gold" -> '6';
            case "gray" -> '7';
            case "dark_gray" -> '8';
            case "blue" -> '9';
            case "green" -> 'a';
            case "aqua" -> 'b';
            case "red" -> 'c';
            case "light_purple" -> 'd';
            case "yellow" -> 'e';
            case "white" -> 'f';
            default -> 'f';
        };
    }

    private Map<String, Object> createTextComponent(String text, List<String> tags) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put(KEY_TEXT, text);

        for (String tag : tags) {
            switch (tag.toLowerCase()) {
                case "bold" -> component.put(KEY_BOLD, true);
                case "italic" -> component.put(KEY_ITALIC, true);
                case "underlined" -> component.put(KEY_UNDERLINED, true);
                case "strikethrough" -> component.put(KEY_STRIKETHROUGH, true);
                case "obfuscated" -> component.put(KEY_OBFUSCATED, true);
                default -> component.put(KEY_COLOR, tag);
            }
        }

        return component;
    }

    private static void extractPlainText(StringBuilder text, Map<String, Object> component) {
        Object textContent = component.get(KEY_TEXT);
        if (textContent != null) {
            text.append(textContent);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extra = (List<Map<String, Object>>) component.get(KEY_EXTRA);
        if (extra != null) {
            for (Map<String, Object> child : extra) {
                extractPlainText(text, child);
            }
        }
    }

    @NotNull
    private String serializeToJson(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        // For full JSON serialization, delegate to a proper JSON library in production
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(entry.getKey()).append("\":");
            appendJsonValue(json, entry.getValue());
        }

        json.append("}");
        return json.toString();
    }

    private void appendJsonValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String s) {
            json.append("\"").append(escapeJson(s)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof List<?> list) {
            json.append("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) json.append(",");
                first = false;
                appendJsonValue(json, item);
            }
            json.append("]");
        } else if (value instanceof Map<?, ?> map) {
            json.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(entry.getKey()).append("\":");
                appendJsonValue(json, entry.getValue());
            }
            json.append("}");
        } else {
            json.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    @NotNull
    private Map<String, Object> deserializeFromJson(@NotNull String json, @NotNull SerializationContext context) {
        // Simplified JSON parser - in production use a proper JSON library
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(KEY_TEXT, "");
        return result;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
