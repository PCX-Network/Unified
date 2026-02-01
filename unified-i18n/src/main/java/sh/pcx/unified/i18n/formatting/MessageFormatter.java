/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.formatting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import sh.pcx.unified.i18n.core.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats messages using MiniMessage with placeholder replacement support.
 *
 * <p>MessageFormatter handles the translation of raw message strings with
 * placeholders into Adventure Components. It supports MiniMessage formatting,
 * legacy color codes, and custom placeholder syntax.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MessageFormatter formatter = MessageFormatter.builder()
 *     .miniMessage(true)
 *     .legacyColors(true)
 *     .build();
 *
 * // Format a message
 * Component result = formatter.format(
 *     "<green>Welcome {player}!</green>",
 *     locale,
 *     Replacement.of("player", playerName)
 * );
 *
 * // Get plain text
 * String plain = formatter.formatPlain(
 *     "Welcome {player}!",
 *     Replacement.of("player", playerName)
 * );
 * }</pre>
 *
 * <h2>Placeholder Syntax</h2>
 * <p>Placeholders use curly braces: {@code {placeholder_name}}
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Replacement
 * @see MiniMessage
 */
public final class MessageFormatter {

    /**
     * Pattern for matching placeholders in format {name}.
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    /**
     * Pattern for matching legacy color codes.
     */
    private static final Pattern LEGACY_PATTERN = Pattern.compile("[&\u00A7]([0-9a-fk-orA-FK-OR])");

    private final MiniMessage miniMessage;
    private final boolean miniMessageEnabled;
    private final boolean legacyColorsEnabled;
    private final LegacyComponentSerializer legacySerializer;
    private final PlainTextComponentSerializer plainSerializer;

    private MessageFormatter(Builder builder) {
        this.miniMessageEnabled = builder.miniMessageEnabled;
        this.legacyColorsEnabled = builder.legacyColorsEnabled;

        // Build MiniMessage with custom settings
        MiniMessage.Builder mmBuilder = MiniMessage.builder();
        if (builder.strict) {
            mmBuilder.strict(true);
        }
        this.miniMessage = mmBuilder.build();

        this.legacySerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();

        this.plainSerializer = PlainTextComponentSerializer.plainText();
    }

    /**
     * Creates a new MessageFormatter with default settings.
     *
     * @return a new formatter with MiniMessage enabled
     * @since 1.0.0
     */
    @NotNull
    public static MessageFormatter create() {
        return builder().build();
    }

    /**
     * Creates a new builder for configuring a MessageFormatter.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Formats a message with the given replacements.
     *
     * @param message      the raw message string
     * @param locale       the locale for formatting (may be null)
     * @param replacements the placeholder replacements
     * @return the formatted Component
     * @since 1.0.0
     */
    @NotNull
    public Component format(@NotNull String message, @Nullable Locale locale,
                            @NotNull Replacement... replacements) {
        Objects.requireNonNull(message, "message cannot be null");

        // Convert legacy colors first if enabled
        String processed = message;
        if (legacyColorsEnabled) {
            processed = convertLegacyColors(processed);
        }

        // Replace placeholders with values
        processed = replacePlaceholders(processed, locale, replacements);

        // Parse with MiniMessage
        if (miniMessageEnabled) {
            // Also pass replacements as tag resolvers for MiniMessage tags
            TagResolver[] resolvers = createTagResolvers(locale, replacements);
            return miniMessage.deserialize(processed, TagResolver.resolver(resolvers));
        }

        return Component.text(processed);
    }

    /**
     * Formats a message and returns plain text.
     *
     * @param message      the raw message string
     * @param replacements the placeholder replacements
     * @return the plain text string
     * @since 1.0.0
     */
    @NotNull
    public String formatPlain(@NotNull String message, @NotNull Replacement... replacements) {
        return formatPlain(message, null, replacements);
    }

    /**
     * Formats a message and returns plain text with locale.
     *
     * @param message      the raw message string
     * @param locale       the locale for formatting
     * @param replacements the placeholder replacements
     * @return the plain text string
     * @since 1.0.0
     */
    @NotNull
    public String formatPlain(@NotNull String message, @Nullable Locale locale,
                              @NotNull Replacement... replacements) {
        String processed = replacePlaceholders(message, locale, replacements);

        // Strip all formatting
        if (miniMessageEnabled) {
            Component component = miniMessage.deserialize(processed);
            return plainSerializer.serialize(component);
        }

        // Strip legacy codes
        return LEGACY_PATTERN.matcher(processed).replaceAll("");
    }

    /**
     * Replaces placeholder syntax with values.
     */
    private String replacePlaceholders(String message, @Nullable Locale locale,
                                        Replacement[] replacements) {
        if (replacements.length == 0) {
            return message;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);

        java.util.Locale javaLocale = locale != null ? locale.toJavaLocale() : java.util.Locale.getDefault();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = findReplacement(key, javaLocale, replacements);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Finds the replacement value for a placeholder key.
     */
    private String findReplacement(String key, java.util.Locale locale, Replacement[] replacements) {
        for (Replacement replacement : replacements) {
            if (replacement.getKey().equals(key)) {
                return replacement.resolve(locale);
            }
        }
        return "{" + key + "}"; // Keep original if not found
    }

    /**
     * Converts legacy color codes to MiniMessage format.
     */
    private String convertLegacyColors(String message) {
        // Convert & and section sign color codes to MiniMessage
        StringBuffer result = new StringBuffer();
        Matcher matcher = LEGACY_PATTERN.matcher(message);

        while (matcher.find()) {
            char code = matcher.group(1).toLowerCase().charAt(0);
            String replacement = convertLegacyCode(code);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Converts a single legacy color code to MiniMessage tag.
     */
    private String convertLegacyCode(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }

    /**
     * Creates TagResolvers from replacements for MiniMessage.
     */
    private TagResolver[] createTagResolvers(@Nullable Locale locale, Replacement[] replacements) {
        List<TagResolver> resolvers = new ArrayList<>();
        java.util.Locale javaLocale = locale != null ? locale.toJavaLocale() : null;

        for (Replacement replacement : replacements) {
            if (javaLocale != null) {
                resolvers.add(replacement.toTagResolver(javaLocale));
            } else {
                resolvers.add(replacement.toTagResolver());
            }
        }

        return resolvers.toArray(TagResolver[]::new);
    }

    /**
     * Returns the underlying MiniMessage instance.
     *
     * @return the MiniMessage instance
     * @since 1.0.0
     */
    @NotNull
    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    /**
     * Checks if MiniMessage formatting is enabled.
     *
     * @return true if MiniMessage is enabled
     * @since 1.0.0
     */
    public boolean isMiniMessageEnabled() {
        return miniMessageEnabled;
    }

    /**
     * Checks if legacy color code support is enabled.
     *
     * @return true if legacy colors are enabled
     * @since 1.0.0
     */
    public boolean isLegacyColorsEnabled() {
        return legacyColorsEnabled;
    }

    /**
     * Builder for creating MessageFormatter instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private boolean miniMessageEnabled = true;
        private boolean legacyColorsEnabled = true;
        private boolean strict = false;

        private Builder() {}

        /**
         * Enables or disables MiniMessage parsing.
         *
         * @param enabled true to enable MiniMessage
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder miniMessage(boolean enabled) {
            this.miniMessageEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables legacy color code support.
         *
         * @param enabled true to enable legacy colors
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder legacyColors(boolean enabled) {
            this.legacyColorsEnabled = enabled;
            return this;
        }

        /**
         * Enables strict MiniMessage parsing.
         *
         * @param strict true for strict parsing
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Builds the MessageFormatter.
         *
         * @return the configured formatter
         * @since 1.0.0
         */
        @NotNull
        public MessageFormatter build() {
            return new MessageFormatter(this);
        }
    }
}
