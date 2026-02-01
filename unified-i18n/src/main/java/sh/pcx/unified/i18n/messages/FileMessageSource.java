/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.messages;

import sh.pcx.unified.i18n.core.Locale;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A MessageSource that loads messages from a file.
 *
 * <p>Supports YAML, HOCON, and JSON formats. The format is auto-detected
 * from the file extension or can be explicitly specified.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Load from YAML file
 * MessageSource source = FileMessageSource.yaml(
 *     Locale.US_ENGLISH,
 *     Path.of("lang/en_US.yml")
 * );
 *
 * // Load messages
 * Map<String, String> messages = source.load();
 *
 * // Auto-detect format
 * MessageSource auto = FileMessageSource.of(
 *     Locale.GERMAN,
 *     Path.of("lang/de_DE.conf")
 * );
 * }</pre>
 *
 * <h2>File Format</h2>
 * <pre>
 * # lang/en_US.yml
 * messages:
 *   welcome: "&lt;green&gt;Welcome to the server!&lt;/green&gt;"
 *   goodbye: "Goodbye, {player}!"
 *
 * items:
 *   one: "You have {count} item"
 *   other: "You have {count} items"
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessageSource
 */
public final class FileMessageSource implements MessageSource {

    /**
     * File format enumeration.
     */
    public enum Format {
        YAML(".yml", ".yaml"),
        HOCON(".conf", ".hocon"),
        JSON(".json");

        private final String[] extensions;

        Format(String... extensions) {
            this.extensions = extensions;
        }

        /**
         * Detects the format from a file path.
         *
         * @param path the file path
         * @return the detected format, or YAML as default
         */
        @NotNull
        public static Format fromPath(@NotNull Path path) {
            String fileName = path.getFileName().toString().toLowerCase();
            for (Format format : values()) {
                for (String ext : format.extensions) {
                    if (fileName.endsWith(ext)) {
                        return format;
                    }
                }
            }
            return YAML; // Default to YAML
        }
    }

    private final Locale locale;
    private final Path path;
    private final Format format;
    private final int priority;

    private FileMessageSource(@NotNull Locale locale, @NotNull Path path,
                               @NotNull Format format, int priority) {
        this.locale = Objects.requireNonNull(locale, "locale cannot be null");
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.format = Objects.requireNonNull(format, "format cannot be null");
        this.priority = priority;
    }

    /**
     * Creates a source for a YAML file.
     *
     * @param locale the locale
     * @param path   the file path
     * @return the message source
     * @since 1.0.0
     */
    @NotNull
    public static FileMessageSource yaml(@NotNull Locale locale, @NotNull Path path) {
        return new FileMessageSource(locale, path, Format.YAML, 0);
    }

    /**
     * Creates a source for a HOCON file.
     *
     * @param locale the locale
     * @param path   the file path
     * @return the message source
     * @since 1.0.0
     */
    @NotNull
    public static FileMessageSource hocon(@NotNull Locale locale, @NotNull Path path) {
        return new FileMessageSource(locale, path, Format.HOCON, 0);
    }

    /**
     * Creates a source with auto-detected format.
     *
     * @param locale the locale
     * @param path   the file path
     * @return the message source
     * @since 1.0.0
     */
    @NotNull
    public static FileMessageSource of(@NotNull Locale locale, @NotNull Path path) {
        return new FileMessageSource(locale, path, Format.fromPath(path), 0);
    }

    /**
     * Creates a source with auto-detected format and priority.
     *
     * @param locale   the locale
     * @param path     the file path
     * @param priority the merge priority
     * @return the message source
     * @since 1.0.0
     */
    @NotNull
    public static FileMessageSource of(@NotNull Locale locale, @NotNull Path path, int priority) {
        return new FileMessageSource(locale, path, Format.fromPath(path), priority);
    }

    @Override
    @NotNull
    public Map<String, String> load() throws IOException {
        if (!Files.exists(path)) {
            return Collections.emptyMap();
        }

        ConfigurationNode root = loadNode();
        Map<String, String> messages = new LinkedHashMap<>();
        flattenNode(root, "", messages);
        return Collections.unmodifiableMap(messages);
    }

    /**
     * Loads the configuration node from the file.
     */
    private ConfigurationNode loadNode() throws IOException {
        return switch (format) {
            case YAML -> YamlConfigurationLoader.builder()
                    .path(path)
                    .build()
                    .load();
            case HOCON -> HoconConfigurationLoader.builder()
                    .path(path)
                    .build()
                    .load();
            case JSON -> YamlConfigurationLoader.builder() // YAML can parse JSON
                    .path(path)
                    .build()
                    .load();
        };
    }

    /**
     * Flattens a hierarchical node into a flat map.
     */
    private void flattenNode(ConfigurationNode node, String prefix, Map<String, String> result) {
        if (node.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
                flattenNode(entry.getValue(), fullKey, result);
            }
        } else if (!node.virtual()) {
            // It's a leaf node
            Object value = node.raw();
            if (value != null) {
                result.put(prefix, value.toString());
            }
        }
    }

    @Override
    @NotNull
    public String getDescription() {
        return "File: " + path.toAbsolutePath();
    }

    @Override
    public boolean supportsHotReload() {
        return true;
    }

    @Override
    @NotNull
    public Locale getLocale() {
        return locale;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Returns the file path.
     *
     * @return the path
     * @since 1.0.0
     */
    @NotNull
    public Path getPath() {
        return path;
    }

    /**
     * Returns the file format.
     *
     * @return the format
     * @since 1.0.0
     */
    @NotNull
    public Format getFormat() {
        return format;
    }

    /**
     * Checks if the source file exists.
     *
     * @return true if the file exists
     * @since 1.0.0
     */
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public String toString() {
        return "FileMessageSource{locale=" + locale + ", path=" + path + ", format=" + format + "}";
    }
}
