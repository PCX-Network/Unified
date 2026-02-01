/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.messages;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.i18n.core.Locale;
import sh.pcx.unified.i18n.core.MessageKey;
import sh.pcx.unified.i18n.formatting.MessageFormatter;
import sh.pcx.unified.i18n.formatting.PluralCategory;
import sh.pcx.unified.i18n.formatting.PluralRules;
import sh.pcx.unified.i18n.formatting.Replacement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A collection of messages for a specific locale.
 *
 * <p>MessageBundle stores all translated messages for a single locale and
 * provides methods for retrieving and formatting them. It supports hierarchical
 * keys, pluralization, and message merging.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a bundle
 * MessageBundle bundle = MessageBundle.create(Locale.US_ENGLISH);
 *
 * // Add messages
 * bundle.put("welcome", "Welcome to the server!");
 * bundle.put("items.one", "You have {count} item");
 * bundle.put("items.other", "You have {count} items");
 *
 * // Get raw message
 * Optional<String> raw = bundle.get(MessageKey.of("welcome"));
 *
 * // Format with placeholders
 * Component message = bundle.format(
 *     MessageKey.of("items"),
 *     5,
 *     Replacement.count("count", 5)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessageSource
 * @see Locale
 */
public final class MessageBundle {

    private final Locale locale;
    private final Map<String, String> messages;
    private final PluralRules pluralRules;
    private MessageFormatter formatter;

    /**
     * Creates a new empty MessageBundle for the given locale.
     *
     * @param locale the locale for this bundle
     */
    private MessageBundle(@NotNull Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale cannot be null");
        this.messages = new ConcurrentHashMap<>();
        this.pluralRules = PluralRules.forLocale(locale);
        this.formatter = MessageFormatter.create();
    }

    /**
     * Creates a new empty MessageBundle for the given locale.
     *
     * @param locale the locale
     * @return a new empty bundle
     * @since 1.0.0
     */
    @NotNull
    public static MessageBundle create(@NotNull Locale locale) {
        return new MessageBundle(locale);
    }

    /**
     * Creates a MessageBundle from a map of messages.
     *
     * @param locale   the locale
     * @param messages the messages map
     * @return a new bundle with the messages
     * @since 1.0.0
     */
    @NotNull
    public static MessageBundle of(@NotNull Locale locale, @NotNull Map<String, String> messages) {
        MessageBundle bundle = new MessageBundle(locale);
        bundle.messages.putAll(messages);
        return bundle;
    }

    /**
     * Returns the locale of this bundle.
     *
     * @return the locale
     * @since 1.0.0
     */
    @NotNull
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the message formatter to use.
     *
     * @param formatter the formatter
     * @since 1.0.0
     */
    public void setFormatter(@NotNull MessageFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter cannot be null");
    }

    /**
     * Returns the message formatter.
     *
     * @return the formatter
     * @since 1.0.0
     */
    @NotNull
    public MessageFormatter getFormatter() {
        return formatter;
    }

    /**
     * Adds or updates a message.
     *
     * @param key   the message key
     * @param value the message value
     * @since 1.0.0
     */
    public void put(@NotNull String key, @NotNull String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        messages.put(key, value);
    }

    /**
     * Adds or updates a message using a MessageKey.
     *
     * @param key   the message key
     * @param value the message value
     * @since 1.0.0
     */
    public void put(@NotNull MessageKey key, @NotNull String value) {
        put(key.getKey(), value);
    }

    /**
     * Adds all messages from a map.
     *
     * @param messages the messages to add
     * @since 1.0.0
     */
    public void putAll(@NotNull Map<String, String> messages) {
        this.messages.putAll(messages);
    }

    /**
     * Merges messages from another bundle.
     *
     * <p>Messages from the other bundle override existing messages.
     *
     * @param other the bundle to merge from
     * @since 1.0.0
     */
    public void merge(@NotNull MessageBundle other) {
        this.messages.putAll(other.messages);
    }

    /**
     * Gets the raw message for a key.
     *
     * @param key the message key
     * @return the raw message, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> get(@NotNull MessageKey key) {
        return Optional.ofNullable(messages.get(key.getKey()));
    }

    /**
     * Gets the raw message for a string key.
     *
     * @param key the message key string
     * @return the raw message, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> get(@NotNull String key) {
        return Optional.ofNullable(messages.get(key));
    }

    /**
     * Gets a message with pluralization.
     *
     * <p>Looks for keys like "key.one", "key.other", etc. based on the count.
     *
     * @param key   the base message key
     * @param count the count for plural selection
     * @return the appropriate plural form, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getPlural(@NotNull MessageKey key, long count) {
        PluralCategory category = pluralRules.select(count);
        String baseKey = key.getKey();

        // Try exact plural form
        String pluralKey = baseKey + "." + category.getKey();
        String message = messages.get(pluralKey);
        if (message != null) {
            return Optional.of(message);
        }

        // Fallback to "other"
        if (category != PluralCategory.OTHER) {
            message = messages.get(baseKey + ".other");
            if (message != null) {
                return Optional.of(message);
            }
        }

        // Try base key (for non-pluralized messages)
        return Optional.ofNullable(messages.get(baseKey));
    }

    /**
     * Formats a message with replacements.
     *
     * @param key          the message key
     * @param replacements the placeholder replacements
     * @return the formatted Component, or Component.empty() if not found
     * @since 1.0.0
     */
    @NotNull
    public Component format(@NotNull MessageKey key, @NotNull Replacement... replacements) {
        return get(key)
                .map(msg -> formatter.format(msg, locale, replacements))
                .orElse(Component.empty());
    }

    /**
     * Formats a message with pluralization and replacements.
     *
     * @param key          the message key
     * @param count        the count for plural selection
     * @param replacements the placeholder replacements
     * @return the formatted Component
     * @since 1.0.0
     */
    @NotNull
    public Component formatPlural(@NotNull MessageKey key, long count, @NotNull Replacement... replacements) {
        return getPlural(key, count)
                .map(msg -> formatter.format(msg, locale, replacements))
                .orElse(Component.empty());
    }

    /**
     * Formats a message to plain text.
     *
     * @param key          the message key
     * @param replacements the placeholder replacements
     * @return the plain text string, or empty string if not found
     * @since 1.0.0
     */
    @NotNull
    public String formatPlain(@NotNull MessageKey key, @NotNull Replacement... replacements) {
        return get(key)
                .map(msg -> formatter.formatPlain(msg, locale, replacements))
                .orElse("");
    }

    /**
     * Checks if a message key exists.
     *
     * @param key the message key
     * @return true if the key exists
     * @since 1.0.0
     */
    public boolean contains(@NotNull MessageKey key) {
        return messages.containsKey(key.getKey());
    }

    /**
     * Checks if a string key exists.
     *
     * @param key the key string
     * @return true if the key exists
     * @since 1.0.0
     */
    public boolean contains(@NotNull String key) {
        return messages.containsKey(key);
    }

    /**
     * Checks if any plural form exists for a key.
     *
     * @param key the base key
     * @return true if any plural form exists
     * @since 1.0.0
     */
    public boolean containsPlural(@NotNull MessageKey key) {
        String baseKey = key.getKey();
        for (PluralCategory category : PluralCategory.values()) {
            if (messages.containsKey(baseKey + "." + category.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of messages in this bundle.
     *
     * @return the message count
     * @since 1.0.0
     */
    public int size() {
        return messages.size();
    }

    /**
     * Checks if this bundle is empty.
     *
     * @return true if no messages are present
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * Returns all message keys.
     *
     * @return an unmodifiable set of keys
     * @since 1.0.0
     */
    @NotNull
    public Set<String> keys() {
        return Collections.unmodifiableSet(messages.keySet());
    }

    /**
     * Returns all messages as an unmodifiable map.
     *
     * @return the messages map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(messages);
    }

    /**
     * Clears all messages.
     *
     * @since 1.0.0
     */
    public void clear() {
        messages.clear();
    }

    /**
     * Removes a message.
     *
     * @param key the message key
     * @return the removed message, or null if not found
     * @since 1.0.0
     */
    @Nullable
    public String remove(@NotNull String key) {
        return messages.remove(key);
    }

    /**
     * Returns all keys that start with a prefix.
     *
     * @param prefix the key prefix
     * @return a set of matching keys
     * @since 1.0.0
     */
    @NotNull
    public Set<String> keysWithPrefix(@NotNull String prefix) {
        Set<String> result = new HashSet<>();
        for (String key : messages.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Creates a sub-bundle containing only keys with a specific prefix.
     *
     * @param prefix the key prefix
     * @return a new bundle with the filtered messages
     * @since 1.0.0
     */
    @NotNull
    public MessageBundle subBundle(@NotNull String prefix) {
        MessageBundle sub = new MessageBundle(locale);
        sub.setFormatter(formatter);

        String prefixWithDot = prefix.endsWith(".") ? prefix : prefix + ".";
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefixWithDot)) {
                // Remove prefix from key
                String newKey = key.substring(prefixWithDot.length());
                sub.put(newKey, entry.getValue());
            }
        }

        return sub;
    }

    @Override
    public String toString() {
        return "MessageBundle{locale=" + locale + ", messages=" + messages.size() + "}";
    }
}
