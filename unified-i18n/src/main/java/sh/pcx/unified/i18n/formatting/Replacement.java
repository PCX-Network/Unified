/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.formatting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.Supplier;

/**
 * Represents a placeholder replacement for message translation.
 *
 * <p>Replacements are used to substitute placeholders in translated messages
 * with dynamic values. They support various value types and can be converted
 * to MiniMessage TagResolvers for Adventure integration.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple string replacement
 * Replacement playerName = Replacement.of("player", player.getName());
 *
 * // Numeric replacement (auto-formatted)
 * Replacement balance = Replacement.of("amount", 1234.56);
 *
 * // Component replacement
 * Replacement prefix = Replacement.component("prefix", player.getPrefix());
 *
 * // Use in translation
 * Component message = i18n.translate(locale, MessageKey.of("balance"),
 *     playerName, balance, prefix);
 * }</pre>
 *
 * <h2>Placeholder Format</h2>
 * <p>In message files, placeholders use curly braces: {@code {placeholder_name}}
 * <pre>
 * welcome: "Welcome {player} to the server!"
 * balance: "Your balance is {amount}"
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.i18n.I18nService
 */
public final class Replacement {

    private final String key;
    private final Object value;
    private final Type type;
    private final Supplier<?> supplier;

    private Replacement(@NotNull String key, @Nullable Object value,
                        @NotNull Type type, @Nullable Supplier<?> supplier) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.value = value;
        this.type = type;
        this.supplier = supplier;
    }

    /**
     * Creates a string replacement.
     *
     * @param key   the placeholder key (without braces)
     * @param value the replacement value
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement of(@NotNull String key, @Nullable String value) {
        return new Replacement(key, value != null ? value : "", Type.STRING, null);
    }

    /**
     * Creates a replacement from any object using its toString().
     *
     * @param key   the placeholder key
     * @param value the replacement value (toString() will be called)
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement of(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            return of(key, "");
        }
        if (value instanceof String s) {
            return of(key, s);
        }
        if (value instanceof Number n) {
            return number(key, n);
        }
        if (value instanceof Component c) {
            return component(key, c);
        }
        if (value instanceof ComponentLike cl) {
            return component(key, cl.asComponent());
        }
        if (value instanceof TemporalAccessor ta) {
            return date(key, ta, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return new Replacement(key, value.toString(), Type.STRING, null);
    }

    /**
     * Creates a numeric replacement with locale-aware formatting.
     *
     * @param key   the placeholder key
     * @param value the numeric value
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement number(@NotNull String key, @NotNull Number value) {
        return new Replacement(key, value, Type.NUMBER, null);
    }

    /**
     * Creates a numeric replacement for pluralization.
     *
     * <p>This value will be used both as the displayed value and for
     * selecting the appropriate plural form.
     *
     * @param key   the placeholder key (also used for plural selection)
     * @param value the count value
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement count(@NotNull String key, long value) {
        return new Replacement(key, value, Type.COUNT, null);
    }

    /**
     * Creates a component replacement for rich text.
     *
     * @param key       the placeholder key
     * @param component the component to insert
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement component(@NotNull String key, @NotNull Component component) {
        return new Replacement(key, component, Type.COMPONENT, null);
    }

    /**
     * Creates a date/time replacement with custom formatting.
     *
     * @param key       the placeholder key
     * @param temporal  the temporal value
     * @param formatter the date/time formatter
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement date(@NotNull String key, @NotNull TemporalAccessor temporal,
                                    @NotNull DateTimeFormatter formatter) {
        String formatted = formatter.format(temporal);
        return new Replacement(key, formatted, Type.STRING, null);
    }

    /**
     * Creates a lazy replacement that computes its value when needed.
     *
     * <p>Useful for expensive computations that may not be needed if
     * the placeholder isn't present in the message.
     *
     * @param key      the placeholder key
     * @param supplier the value supplier
     * @return the replacement instance
     * @since 1.0.0
     */
    @NotNull
    public static Replacement lazy(@NotNull String key, @NotNull Supplier<?> supplier) {
        return new Replacement(key, null, Type.LAZY, supplier);
    }

    /**
     * Creates multiple replacements from a map.
     *
     * @param map the map of key-value pairs
     * @return an array of replacements
     * @since 1.0.0
     */
    @NotNull
    public static Replacement[] fromMap(@NotNull Map<String, ?> map) {
        return map.entrySet().stream()
                .map(e -> of(e.getKey(), e.getValue()))
                .toArray(Replacement[]::new);
    }

    /**
     * Creates a builder for constructing multiple replacements.
     *
     * @return a new replacement builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the placeholder key.
     *
     * @return the key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Returns the raw value.
     *
     * @return the value, which may be null for lazy replacements
     * @since 1.0.0
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    /**
     * Returns the replacement type.
     *
     * @return the type
     * @since 1.0.0
     */
    @NotNull
    public Type getType() {
        return type;
    }

    /**
     * Checks if this replacement represents a count for pluralization.
     *
     * @return true if this is a count replacement
     * @since 1.0.0
     */
    public boolean isCount() {
        return type == Type.COUNT;
    }

    /**
     * Returns the count value if this is a count replacement.
     *
     * @return the count, or 1 if not a count replacement
     * @since 1.0.0
     */
    public long getCount() {
        if (type == Type.COUNT && value instanceof Number n) {
            return n.longValue();
        }
        return 1;
    }

    /**
     * Resolves the replacement value to a string.
     *
     * @param locale the locale for formatting (may be null)
     * @return the formatted string value
     * @since 1.0.0
     */
    @NotNull
    public String resolve(@Nullable java.util.Locale locale) {
        Object resolved = resolveValue();

        return switch (type) {
            case NUMBER, COUNT -> {
                if (resolved instanceof Number n) {
                    NumberFormat nf = locale != null
                            ? NumberFormat.getInstance(locale)
                            : NumberFormat.getInstance();
                    yield nf.format(n);
                }
                yield String.valueOf(resolved);
            }
            case COMPONENT -> {
                // Components are handled separately in MiniMessage
                yield resolved != null ? resolved.toString() : "";
            }
            default -> resolved != null ? resolved.toString() : "";
        };
    }

    /**
     * Resolves the value (including lazy evaluation).
     *
     * @return the resolved value
     */
    @Nullable
    private Object resolveValue() {
        if (type == Type.LAZY && supplier != null) {
            return supplier.get();
        }
        return value;
    }

    /**
     * Converts this replacement to a MiniMessage TagResolver.
     *
     * @return the tag resolver
     * @since 1.0.0
     */
    @NotNull
    public TagResolver toTagResolver() {
        Object resolved = resolveValue();

        if (type == Type.COMPONENT && resolved instanceof Component component) {
            return TagResolver.resolver(key, Tag.inserting(component));
        }

        String stringValue = resolve(null);
        return TagResolver.resolver(key, Tag.inserting(Component.text(stringValue)));
    }

    /**
     * Converts this replacement to a TagResolver with locale-aware formatting.
     *
     * @param locale the locale for formatting
     * @return the tag resolver
     * @since 1.0.0
     */
    @NotNull
    public TagResolver toTagResolver(@NotNull java.util.Locale locale) {
        Object resolved = resolveValue();

        if (type == Type.COMPONENT && resolved instanceof Component component) {
            return TagResolver.resolver(key, Tag.inserting(component));
        }

        String stringValue = resolve(locale);
        return TagResolver.resolver(key, Tag.inserting(Component.text(stringValue)));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Replacement other)) {
            return false;
        }
        return key.equals(other.key) && Objects.equals(value, other.value) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, type);
    }

    @Override
    public String toString() {
        return "Replacement{key='" + key + "', value=" + value + ", type=" + type + "}";
    }

    /**
     * The type of replacement value.
     *
     * @since 1.0.0
     */
    public enum Type {
        /**
         * Plain string replacement.
         */
        STRING,

        /**
         * Numeric replacement with locale-aware formatting.
         */
        NUMBER,

        /**
         * Count replacement for pluralization.
         */
        COUNT,

        /**
         * Rich text component replacement.
         */
        COMPONENT,

        /**
         * Lazy-evaluated replacement.
         */
        LAZY
    }

    /**
     * Builder for creating multiple replacements.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final List<Replacement> replacements = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a string replacement.
         *
         * @param key   the placeholder key
         * @param value the replacement value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder add(@NotNull String key, @Nullable String value) {
            replacements.add(Replacement.of(key, value));
            return this;
        }

        /**
         * Adds an object replacement.
         *
         * @param key   the placeholder key
         * @param value the replacement value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder add(@NotNull String key, @Nullable Object value) {
            replacements.add(Replacement.of(key, value));
            return this;
        }

        /**
         * Adds a numeric replacement.
         *
         * @param key   the placeholder key
         * @param value the numeric value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder number(@NotNull String key, @NotNull Number value) {
            replacements.add(Replacement.number(key, value));
            return this;
        }

        /**
         * Adds a count replacement for pluralization.
         *
         * @param key   the placeholder key
         * @param value the count value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder count(@NotNull String key, long value) {
            replacements.add(Replacement.count(key, value));
            return this;
        }

        /**
         * Adds a component replacement.
         *
         * @param key       the placeholder key
         * @param component the component
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder component(@NotNull String key, @NotNull Component component) {
            replacements.add(Replacement.component(key, component));
            return this;
        }

        /**
         * Adds all entries from a map.
         *
         * @param map the map of replacements
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder addAll(@NotNull Map<String, ?> map) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                replacements.add(Replacement.of(entry.getKey(), entry.getValue()));
            }
            return this;
        }

        /**
         * Builds the replacement array.
         *
         * @return an array of replacements
         * @since 1.0.0
         */
        @NotNull
        public Replacement[] build() {
            return replacements.toArray(Replacement[]::new);
        }

        /**
         * Builds and returns a list of replacements.
         *
         * @return a list of replacements
         * @since 1.0.0
         */
        @NotNull
        public List<Replacement> toList() {
            return new ArrayList<>(replacements);
        }
    }
}
