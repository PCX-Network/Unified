/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.format;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Enumeration of supported configuration file formats.
 *
 * <p>Each format has specific characteristics and use cases:</p>
 * <ul>
 *   <li><b>YAML:</b> Human-readable, widely used, supports comments</li>
 *   <li><b>HOCON:</b> Advanced features like includes and substitutions</li>
 *   <li><b>JSON:</b> API compatibility, no comments, strict syntax</li>
 *   <li><b>TOML:</b> Clean syntax, good for simple configurations</li>
 * </ul>
 *
 * <h2>Format Detection</h2>
 * <pre>{@code
 * // Auto-detect from file path
 * ConfigFormat format = ConfigFormat.fromPath(path);
 *
 * // Get by extension
 * ConfigFormat format = ConfigFormat.fromExtension("yml").orElse(ConfigFormat.YAML);
 * }</pre>
 *
 * <h2>Format Capabilities</h2>
 * <pre>{@code
 * if (format.supportsComments()) {
 *     // Add comments to the config
 * }
 *
 * if (format.supportsIncludes()) {
 *     // Use include directives
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ConfigLoader
 * @see ConfigSaver
 */
public enum ConfigFormat {

    /**
     * YAML format (.yml, .yaml).
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Human-readable and writable</li>
     *   <li>Supports comments</li>
     *   <li>Indentation-based structure</li>
     *   <li>Good for plugin configurations</li>
     * </ul>
     */
    YAML("yaml", "YAML", true, false, true, "yml", "yaml"),

    /**
     * HOCON format (.conf, .hocon).
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Superset of JSON</li>
     *   <li>Supports comments</li>
     *   <li>Includes and substitutions</li>
     *   <li>Human-friendly syntax</li>
     *   <li>Good for complex configurations</li>
     * </ul>
     */
    HOCON("hocon", "HOCON", true, true, true, "conf", "hocon"),

    /**
     * JSON format (.json).
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Universal format</li>
     *   <li>Strict syntax</li>
     *   <li>No comments (by standard)</li>
     *   <li>Good for API/programmatic use</li>
     * </ul>
     */
    JSON("json", "JSON", false, false, false, "json"),

    /**
     * TOML format (.toml).
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Clean, minimal syntax</li>
     *   <li>Supports comments</li>
     *   <li>Explicit typing</li>
     *   <li>Good for simple configurations</li>
     * </ul>
     */
    TOML("toml", "TOML", true, false, true, "toml");

    private final String id;
    private final String displayName;
    private final boolean supportsComments;
    private final boolean supportsIncludes;
    private final boolean supportsMultilineStrings;
    private final List<String> extensions;

    ConfigFormat(
            String id,
            String displayName,
            boolean supportsComments,
            boolean supportsIncludes,
            boolean supportsMultilineStrings,
            String... extensions
    ) {
        this.id = id;
        this.displayName = displayName;
        this.supportsComments = supportsComments;
        this.supportsIncludes = supportsIncludes;
        this.supportsMultilineStrings = supportsMultilineStrings;
        this.extensions = Collections.unmodifiableList(Arrays.asList(extensions));
    }

    /**
     * Gets the format identifier.
     *
     * @return the format id
     * @since 1.0.0
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this format supports comments.
     *
     * @return true if comments are supported
     * @since 1.0.0
     */
    public boolean supportsComments() {
        return supportsComments;
    }

    /**
     * Checks if this format supports include directives.
     *
     * @return true if includes are supported
     * @since 1.0.0
     */
    public boolean supportsIncludes() {
        return supportsIncludes;
    }

    /**
     * Checks if this format supports multiline strings.
     *
     * @return true if multiline strings are supported
     * @since 1.0.0
     */
    public boolean supportsMultilineStrings() {
        return supportsMultilineStrings;
    }

    /**
     * Gets the file extensions associated with this format.
     *
     * @return list of extensions (without dot)
     * @since 1.0.0
     */
    @NotNull
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Gets the primary file extension.
     *
     * @return the primary extension
     * @since 1.0.0
     */
    @NotNull
    public String getPrimaryExtension() {
        return extensions.get(0);
    }

    /**
     * Gets the full filename for a given base name.
     *
     * @param baseName the base name without extension
     * @return the full filename with extension
     * @since 1.0.0
     */
    @NotNull
    public String getFilename(@NotNull String baseName) {
        return baseName + "." + getPrimaryExtension();
    }

    /**
     * Checks if a filename matches this format.
     *
     * @param filename the filename to check
     * @return true if the extension matches
     * @since 1.0.0
     */
    public boolean matches(@NotNull String filename) {
        String lower = filename.toLowerCase();
        return extensions.stream().anyMatch(ext -> lower.endsWith("." + ext));
    }

    /**
     * Detects the format from a file path.
     *
     * @param path the file path
     * @return the detected format
     * @throws IllegalArgumentException if the format cannot be detected
     * @since 1.0.0
     */
    @NotNull
    public static ConfigFormat fromPath(@NotNull Path path) {
        String filename = path.getFileName().toString();
        return fromFilename(filename)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot detect config format from path: " + path +
                                ". Supported extensions: .yml, .yaml, .conf, .hocon, .json, .toml"
                ));
    }

    /**
     * Detects the format from a filename.
     *
     * @param filename the filename
     * @return optional containing the format, empty if not detected
     * @since 1.0.0
     */
    @NotNull
    public static Optional<ConfigFormat> fromFilename(@NotNull String filename) {
        String lower = filename.toLowerCase();
        for (ConfigFormat format : values()) {
            if (format.matches(lower)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets a format by its extension.
     *
     * @param extension the file extension (with or without dot)
     * @return optional containing the format
     * @since 1.0.0
     */
    @NotNull
    public static Optional<ConfigFormat> fromExtension(@NotNull String extension) {
        String ext = extension.startsWith(".") ? extension.substring(1) : extension;
        String lower = ext.toLowerCase();
        for (ConfigFormat format : values()) {
            if (format.extensions.contains(lower)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets a format by its id.
     *
     * @param id the format id
     * @return optional containing the format
     * @since 1.0.0
     */
    @NotNull
    public static Optional<ConfigFormat> fromId(@Nullable String id) {
        if (id == null) {
            return Optional.empty();
        }
        String lower = id.toLowerCase();
        for (ConfigFormat format : values()) {
            if (format.id.equals(lower) || format.name().equalsIgnoreCase(lower)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the default configuration format.
     *
     * @return YAML as the default format
     * @since 1.0.0
     */
    @NotNull
    public static ConfigFormat getDefault() {
        return YAML;
    }
}
