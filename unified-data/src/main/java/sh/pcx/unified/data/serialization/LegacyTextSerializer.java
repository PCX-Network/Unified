/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serializer for legacy Minecraft text with section sign color codes.
 *
 * <p>LegacyTextSerializer handles the serialization of text using the legacy
 * formatting system with section sign color codes. This is used for
 * compatibility with older plugins and configurations.
 *
 * <h2>Supported Color Codes</h2>
 * <table>
 *   <tr><th>Code</th><th>Color</th><th>Code</th><th>Formatting</th></tr>
 *   <tr><td>0</td><td>Black</td><td>k</td><td>Obfuscated</td></tr>
 *   <tr><td>1</td><td>Dark Blue</td><td>l</td><td>Bold</td></tr>
 *   <tr><td>2</td><td>Dark Green</td><td>m</td><td>Strikethrough</td></tr>
 *   <tr><td>3</td><td>Dark Aqua</td><td>n</td><td>Underline</td></tr>
 *   <tr><td>4</td><td>Dark Red</td><td>o</td><td>Italic</td></tr>
 *   <tr><td>5</td><td>Dark Purple</td><td>r</td><td>Reset</td></tr>
 *   <tr><td>6</td><td>Gold</td></tr>
 *   <tr><td>7</td><td>Gray</td></tr>
 *   <tr><td>8</td><td>Dark Gray</td></tr>
 *   <tr><td>9</td><td>Blue</td></tr>
 *   <tr><td>a</td><td>Green</td></tr>
 *   <tr><td>b</td><td>Aqua</td></tr>
 *   <tr><td>c</td><td>Red</td></tr>
 *   <tr><td>d</td><td>Light Purple</td></tr>
 *   <tr><td>e</td><td>Yellow</td></tr>
 *   <tr><td>f</td><td>White</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LegacyTextSerializer serializer = Serializers.legacyText();
 *
 * // Parse legacy text to component
 * Map<String, Object> component = serializer.deserialize("&cRed &lBold &fWhite");
 *
 * // Convert component to legacy
 * String legacy = serializer.serialize(component);
 *
 * // Translate ampersand to section sign
 * String translated = LegacyTextSerializer.translateAmpersand("&cRed &lBold");
 *
 * // Strip all color codes
 * String plain = LegacyTextSerializer.stripColors(legacy);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This serializer is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ComponentSerializer
 * @see MiniMessageSerializer
 */
public final class LegacyTextSerializer implements Serializer<Map<String, Object>> {

    /**
     * The section sign character used in Minecraft text formatting.
     */
    public static final char SECTION_SIGN = '\u00A7';

    /**
     * Common alternate color code character.
     */
    public static final char AMPERSAND = '&';

    // Pattern for color codes
    private static final Pattern LEGACY_PATTERN = Pattern.compile("(?i)[\u00A7&][0-9A-FK-ORX]");
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[\u00A7&]x([\u00A7&][0-9A-F]){6}");

    // Component keys
    private static final String KEY_TEXT = "text";
    private static final String KEY_COLOR = "color";
    private static final String KEY_BOLD = "bold";
    private static final String KEY_ITALIC = "italic";
    private static final String KEY_UNDERLINED = "underlined";
    private static final String KEY_STRIKETHROUGH = "strikethrough";
    private static final String KEY_OBFUSCATED = "obfuscated";
    private static final String KEY_EXTRA = "extra";

    private final char colorChar;

    /**
     * Creates a new LegacyTextSerializer using the section sign.
     */
    public LegacyTextSerializer() {
        this(SECTION_SIGN);
    }

    /**
     * Creates a new LegacyTextSerializer with a custom color character.
     *
     * @param colorChar the character to use for color codes
     */
    public LegacyTextSerializer(char colorChar) {
        this.colorChar = colorChar;
    }

    @Override
    @NotNull
    public String serialize(@NotNull Map<String, Object> value, @NotNull SerializationContext context) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        StringBuilder legacy = new StringBuilder();
        buildLegacyString(legacy, value);
        return legacy.toString();
    }

    @Override
    @NotNull
    public Map<String, Object> deserialize(@NotNull String data, @NotNull SerializationContext context) {
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return parseLegacy(data);
    }

    @Override
    @NotNull
    public Class<Map<String, Object>> getTargetType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> type = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return type;
    }

    // ========================================
    // Static Utility Methods
    // ========================================

    /**
     * Translates ampersand color codes to section sign codes.
     *
     * @param input the input string with ampersand codes
     * @return the string with section sign codes
     * @since 1.0.0
     */
    @NotNull
    public static String translateAmpersand(@NotNull String input) {
        return translateColorCodes(AMPERSAND, SECTION_SIGN, input);
    }

    /**
     * Translates section sign color codes to ampersand codes.
     *
     * @param input the input string with section sign codes
     * @return the string with ampersand codes
     * @since 1.0.0
     */
    @NotNull
    public static String translateToAmpersand(@NotNull String input) {
        return translateColorCodes(SECTION_SIGN, AMPERSAND, input);
    }

    /**
     * Translates color codes from one character to another.
     *
     * @param from  the source color character
     * @param to    the target color character
     * @param input the input string
     * @return the translated string
     * @since 1.0.0
     */
    @NotNull
    public static String translateColorCodes(char from, char to, @NotNull String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == from && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = to;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /**
     * Strips all color and formatting codes from a string.
     *
     * @param input the input string with color codes
     * @return the plain text without codes
     * @since 1.0.0
     */
    @NotNull
    public static String stripColors(@NotNull String input) {
        return LEGACY_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Checks if a string contains any color codes.
     *
     * @param input the input string
     * @return true if color codes are present
     * @since 1.0.0
     */
    public static boolean hasColors(@NotNull String input) {
        return LEGACY_PATTERN.matcher(input).find();
    }

    /**
     * Gets the last color codes in a string.
     *
     * <p>Useful for continuing formatting after a break.
     *
     * @param input the input string
     * @return the accumulated color/format codes
     * @since 1.0.0
     */
    @NotNull
    public static String getLastColors(@NotNull String input) {
        StringBuilder result = new StringBuilder();
        int length = input.length();

        for (int index = length - 1; index > -1; index--) {
            char section = input.charAt(index);
            if (section == SECTION_SIGN || section == AMPERSAND) {
                if (index < length - 1) {
                    char c = input.charAt(index + 1);
                    if (isColorCode(c)) {
                        result.insert(0, String.valueOf(SECTION_SIGN) + c);
                        if (isColorCode(c) && !isFormatCode(c)) {
                            break;
                        }
                    }
                }
            }
        }

        return result.toString();
    }

    /**
     * Creates a colored string.
     *
     * @param color the color code character (0-9, a-f)
     * @param text  the text to color
     * @return the colored string
     * @since 1.0.0
     */
    @NotNull
    public static String color(char color, @NotNull String text) {
        return String.valueOf(SECTION_SIGN) + color + text;
    }

    /**
     * Creates a formatted string.
     *
     * @param format the format code (k, l, m, n, o)
     * @param text   the text to format
     * @return the formatted string
     * @since 1.0.0
     */
    @NotNull
    public static String format(char format, @NotNull String text) {
        return String.valueOf(SECTION_SIGN) + format + text;
    }

    /**
     * Converts a hex color to legacy format (for 1.16+).
     *
     * @param hex the hex color (e.g., "#FF5555" or "FF5555")
     * @return the legacy hex representation
     * @since 1.0.0
     */
    @NotNull
    public static String hexToLegacy(@NotNull String hex) {
        hex = hex.replace("#", "");
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SECTION_SIGN).append('x');
        for (char c : hex.toCharArray()) {
            sb.append(SECTION_SIGN).append(c);
        }
        return sb.toString();
    }

    /**
     * Gets the color name for a legacy color code.
     *
     * @param code the color code character
     * @return the color name
     * @since 1.0.0
     */
    @NotNull
    public static String getColorName(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            default -> "white";
        };
    }

    /**
     * Gets the color code for a color name.
     *
     * @param name the color name
     * @return the color code character
     * @since 1.0.0
     */
    public static char getColorCode(@NotNull String name) {
        return switch (name.toLowerCase()) {
            case "black" -> '0';
            case "dark_blue" -> '1';
            case "dark_green" -> '2';
            case "dark_aqua" -> '3';
            case "dark_red" -> '4';
            case "dark_purple" -> '5';
            case "gold" -> '6';
            case "gray", "grey" -> '7';
            case "dark_gray", "dark_grey" -> '8';
            case "blue" -> '9';
            case "green" -> 'a';
            case "aqua" -> 'b';
            case "red" -> 'c';
            case "light_purple", "pink" -> 'd';
            case "yellow" -> 'e';
            case "white" -> 'f';
            default -> 'f';
        };
    }

    // ========================================
    // Internal Methods
    // ========================================

    private void buildLegacyString(StringBuilder legacy, Map<String, Object> component) {
        // Apply color
        String color = (String) component.get(KEY_COLOR);
        if (color != null) {
            if (color.startsWith("#")) {
                legacy.append(hexToLegacy(color));
            } else {
                legacy.append(colorChar).append(getColorCode(color));
            }
        }

        // Apply formatting
        if (Boolean.TRUE.equals(component.get(KEY_OBFUSCATED))) {
            legacy.append(colorChar).append('k');
        }
        if (Boolean.TRUE.equals(component.get(KEY_BOLD))) {
            legacy.append(colorChar).append('l');
        }
        if (Boolean.TRUE.equals(component.get(KEY_STRIKETHROUGH))) {
            legacy.append(colorChar).append('m');
        }
        if (Boolean.TRUE.equals(component.get(KEY_UNDERLINED))) {
            legacy.append(colorChar).append('n');
        }
        if (Boolean.TRUE.equals(component.get(KEY_ITALIC))) {
            legacy.append(colorChar).append('o');
        }

        // Add text
        String text = (String) component.get(KEY_TEXT);
        if (text != null) {
            legacy.append(text);
        }

        // Process children
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extra = (List<Map<String, Object>>) component.get(KEY_EXTRA);
        if (extra != null) {
            for (Map<String, Object> child : extra) {
                buildLegacyString(legacy, child);
            }
        }
    }

    @NotNull
    private Map<String, Object> parseLegacy(@NotNull String legacy) {
        List<Map<String, Object>> parts = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        String currentColor = null;
        boolean bold = false, italic = false, underlined = false, strikethrough = false, obfuscated = false;

        int i = 0;
        while (i < legacy.length()) {
            char c = legacy.charAt(i);

            if ((c == SECTION_SIGN || c == AMPERSAND) && i + 1 < legacy.length()) {
                char code = Character.toLowerCase(legacy.charAt(i + 1));

                // Check for hex color
                if (code == 'x' && i + 13 < legacy.length()) {
                    StringBuilder hex = new StringBuilder("#");
                    boolean validHex = true;
                    for (int j = 0; j < 6; j++) {
                        int hexIndex = i + 2 + (j * 2) + 1;
                        if (hexIndex < legacy.length()) {
                            char hexChar = legacy.charAt(hexIndex);
                            if (Character.digit(hexChar, 16) != -1) {
                                hex.append(hexChar);
                            } else {
                                validHex = false;
                                break;
                            }
                        } else {
                            validHex = false;
                            break;
                        }
                    }

                    if (validHex && hex.length() == 7) {
                        // Save current part
                        if (currentText.length() > 0) {
                            parts.add(createPart(currentText.toString(), currentColor,
                                    bold, italic, underlined, strikethrough, obfuscated));
                            currentText = new StringBuilder();
                        }

                        currentColor = hex.toString();
                        bold = italic = underlined = strikethrough = obfuscated = false;
                        i += 14;
                        continue;
                    }
                }

                if (isColorCode(code)) {
                    // Save current part
                    if (currentText.length() > 0) {
                        parts.add(createPart(currentText.toString(), currentColor,
                                bold, italic, underlined, strikethrough, obfuscated));
                        currentText = new StringBuilder();
                    }

                    // Apply new formatting
                    if (isFormatCode(code)) {
                        switch (code) {
                            case 'k' -> obfuscated = true;
                            case 'l' -> bold = true;
                            case 'm' -> strikethrough = true;
                            case 'n' -> underlined = true;
                            case 'o' -> italic = true;
                            case 'r' -> {
                                currentColor = null;
                                bold = italic = underlined = strikethrough = obfuscated = false;
                            }
                        }
                    } else {
                        currentColor = getColorName(code);
                        bold = italic = underlined = strikethrough = obfuscated = false;
                    }

                    i += 2;
                    continue;
                }
            }

            currentText.append(c);
            i++;
        }

        // Add final part
        if (currentText.length() > 0) {
            parts.add(createPart(currentText.toString(), currentColor,
                    bold, italic, underlined, strikethrough, obfuscated));
        }

        // Build component
        if (parts.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put(KEY_TEXT, "");
            return empty;
        } else if (parts.size() == 1) {
            return parts.get(0);
        } else {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put(KEY_TEXT, "");
            root.put(KEY_EXTRA, parts);
            return root;
        }
    }

    private Map<String, Object> createPart(String text, String color,
                                            boolean bold, boolean italic, boolean underlined,
                                            boolean strikethrough, boolean obfuscated) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put(KEY_TEXT, text);
        if (color != null) part.put(KEY_COLOR, color);
        if (bold) part.put(KEY_BOLD, true);
        if (italic) part.put(KEY_ITALIC, true);
        if (underlined) part.put(KEY_UNDERLINED, true);
        if (strikethrough) part.put(KEY_STRIKETHROUGH, true);
        if (obfuscated) part.put(KEY_OBFUSCATED, true);
        return part;
    }

    private static boolean isColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) > -1;
    }

    private static boolean isFormatCode(char c) {
        return "KkLlMmNnOoRr".indexOf(c) > -1;
    }
}
