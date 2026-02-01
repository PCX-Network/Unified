/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Type-safe representation of a message key used for translations.
 *
 * <p>MessageKey provides a type-safe way to reference translation keys,
 * avoiding string literal errors and providing IDE support for refactoring.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create message keys
 * MessageKey welcome = MessageKey.of("messages.welcome");
 * MessageKey error = MessageKey.of("errors", "not_found");
 *
 * // Use in translations
 * Component message = i18n.translate(locale, welcome,
 *     Replacement.of("player", playerName));
 *
 * // Keys with pluralization
 * MessageKey items = MessageKey.of("items.count");
 * Component plural = i18n.translate(locale, items,
 *     Replacement.of("count", itemCount));
 * }</pre>
 *
 * <h2>Key Format</h2>
 * <p>Keys use dot-notation to represent hierarchy:
 * <ul>
 *   <li>{@code messages.welcome} - A welcome message</li>
 *   <li>{@code errors.not_found} - An error message</li>
 *   <li>{@code items.one} / {@code items.other} - Plural forms</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.i18n.I18nService
 */
public final class MessageKey implements Comparable<MessageKey> {

    private final String key;
    private final String namespace;
    private final int hashCode;

    /**
     * Creates a new MessageKey with the specified key.
     *
     * @param key the full message key
     */
    private MessageKey(@NotNull String key, @Nullable String namespace) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.namespace = namespace;
        this.hashCode = Objects.hash(key, namespace);
    }

    /**
     * Creates a MessageKey from a string.
     *
     * @param key the message key string (e.g., "messages.welcome")
     * @return the MessageKey instance
     * @throws NullPointerException if key is null
     * @throws IllegalArgumentException if key is empty or blank
     * @since 1.0.0
     */
    @NotNull
    public static MessageKey of(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key cannot be empty or blank");
        }
        return new MessageKey(key, null);
    }

    /**
     * Creates a MessageKey from path components.
     *
     * <p>Components are joined with dots to form the full key.
     *
     * @param first the first path component
     * @param rest additional path components
     * @return the MessageKey instance
     * @since 1.0.0
     */
    @NotNull
    public static MessageKey of(@NotNull String first, @NotNull String... rest) {
        Objects.requireNonNull(first, "first cannot be null");
        if (rest.length == 0) {
            return of(first);
        }

        StringBuilder keyBuilder = new StringBuilder(first);
        for (String part : rest) {
            if (part != null && !part.isEmpty()) {
                keyBuilder.append('.').append(part);
            }
        }
        return of(keyBuilder.toString());
    }

    /**
     * Creates a namespaced MessageKey.
     *
     * <p>Namespaces are used to separate message keys between different plugins
     * or modules.
     *
     * @param namespace the namespace (e.g., plugin name)
     * @param key the message key
     * @return the namespaced MessageKey
     * @since 1.0.0
     */
    @NotNull
    public static MessageKey namespaced(@NotNull String namespace, @NotNull String key) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        if (namespace.isBlank()) {
            return of(key);
        }
        return new MessageKey(key, namespace);
    }

    /**
     * Returns the full key string.
     *
     * @return the key string
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Returns the namespace if set.
     *
     * @return the namespace, or null if not namespaced
     * @since 1.0.0
     */
    @Nullable
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the full qualified key including namespace.
     *
     * @return the qualified key (namespace:key or just key)
     * @since 1.0.0
     */
    @NotNull
    public String getQualifiedKey() {
        if (namespace != null && !namespace.isEmpty()) {
            return namespace + ":" + key;
        }
        return key;
    }

    /**
     * Checks if this key is namespaced.
     *
     * @return true if the key has a namespace
     * @since 1.0.0
     */
    public boolean isNamespaced() {
        return namespace != null && !namespace.isEmpty();
    }

    /**
     * Returns the parent key (everything before the last dot).
     *
     * <p>For example, "messages.errors.not_found" returns "messages.errors".
     *
     * @return the parent key, or null if no parent exists
     * @since 1.0.0
     */
    @Nullable
    public MessageKey getParent() {
        int lastDot = key.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        return new MessageKey(key.substring(0, lastDot), namespace);
    }

    /**
     * Returns the last segment of the key.
     *
     * <p>For example, "messages.errors.not_found" returns "not_found".
     *
     * @return the last segment
     * @since 1.0.0
     */
    @NotNull
    public String getLastSegment() {
        int lastDot = key.lastIndexOf('.');
        if (lastDot < 0) {
            return key;
        }
        return key.substring(lastDot + 1);
    }

    /**
     * Creates a child key by appending a segment.
     *
     * @param segment the segment to append
     * @return a new MessageKey with the appended segment
     * @since 1.0.0
     */
    @NotNull
    public MessageKey child(@NotNull String segment) {
        Objects.requireNonNull(segment, "segment cannot be null");
        return new MessageKey(key + "." + segment, namespace);
    }

    /**
     * Creates a sibling key by replacing the last segment.
     *
     * @param segment the new last segment
     * @return a new MessageKey with the replaced segment
     * @since 1.0.0
     */
    @NotNull
    public MessageKey sibling(@NotNull String segment) {
        MessageKey parent = getParent();
        if (parent == null) {
            return new MessageKey(segment, namespace);
        }
        return parent.child(segment);
    }

    /**
     * Returns this key with a specific namespace.
     *
     * @param namespace the namespace to set
     * @return a new MessageKey with the specified namespace
     * @since 1.0.0
     */
    @NotNull
    public MessageKey withNamespace(@NotNull String namespace) {
        return new MessageKey(key, namespace);
    }

    /**
     * Returns this key without a namespace.
     *
     * @return a new MessageKey without a namespace
     * @since 1.0.0
     */
    @NotNull
    public MessageKey withoutNamespace() {
        if (namespace == null) {
            return this;
        }
        return new MessageKey(key, null);
    }

    /**
     * Checks if this key starts with the given prefix.
     *
     * @param prefix the prefix to check
     * @return true if the key starts with the prefix
     * @since 1.0.0
     */
    public boolean startsWith(@NotNull String prefix) {
        return key.startsWith(prefix);
    }

    /**
     * Checks if this key starts with another key.
     *
     * @param other the key to check as prefix
     * @return true if this key starts with the other key
     * @since 1.0.0
     */
    public boolean startsWith(@NotNull MessageKey other) {
        return key.startsWith(other.key) && Objects.equals(namespace, other.namespace);
    }

    @Override
    public int compareTo(@NotNull MessageKey other) {
        int namespaceCompare = Objects.compare(namespace, other.namespace,
                (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b)));
        if (namespaceCompare != 0) {
            return namespaceCompare;
        }
        return key.compareTo(other.key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MessageKey other)) {
            return false;
        }
        return key.equals(other.key) && Objects.equals(namespace, other.namespace);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return getQualifiedKey();
    }
}
