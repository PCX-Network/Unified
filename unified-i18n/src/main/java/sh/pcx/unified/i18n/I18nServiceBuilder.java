/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n;

import sh.pcx.unified.i18n.core.Locale;
import sh.pcx.unified.i18n.messages.MessageSource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder implementation for creating I18nService instances.
 *
 * <p>This builder allows configuring all aspects of the I18nService before
 * instantiation, including locales, message sources, formatting options,
 * and hot reload settings.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * I18nService i18n = I18nService.builder()
 *     .defaultLocale(Locale.US_ENGLISH)
 *     .fallbackLocale(Locale.US_ENGLISH)
 *     .directory(dataFolder.resolve("lang"))
 *     .hotReload(true)
 *     .miniMessage(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see I18nService
 */
public final class I18nServiceBuilder implements I18nService.Builder {

    private Locale defaultLocale = Locale.US_ENGLISH;
    private Locale fallbackLocale = Locale.US_ENGLISH;
    private final List<SourceEntry> sources = new ArrayList<>();
    private Path directory;
    private boolean hotReloadEnabled = false;
    private boolean miniMessageEnabled = true;
    private boolean legacyColorsEnabled = true;
    private I18nService.MissingKeyHandler missingKeyHandler;

    /**
     * Creates a new builder with default settings.
     */
    I18nServiceBuilder() {
        // Default missing key handler returns the key
        this.missingKeyHandler = (locale, key) -> "<" + key.getKey() + ">";
    }

    @Override
    @NotNull
    public I18nService.Builder defaultLocale(@NotNull Locale locale) {
        this.defaultLocale = Objects.requireNonNull(locale, "locale cannot be null");
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder fallbackLocale(@NotNull Locale locale) {
        this.fallbackLocale = Objects.requireNonNull(locale, "locale cannot be null");
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder addSource(@NotNull Locale locale, @NotNull MessageSource source) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(source, "source cannot be null");
        sources.add(new SourceEntry(locale, source));
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder directory(@NotNull Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory cannot be null");
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder hotReload(boolean enabled) {
        this.hotReloadEnabled = enabled;
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder miniMessage(boolean enabled) {
        this.miniMessageEnabled = enabled;
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder legacyColors(boolean enabled) {
        this.legacyColorsEnabled = enabled;
        return this;
    }

    @Override
    @NotNull
    public I18nService.Builder missingKeyHandler(@NotNull I18nService.MissingKeyHandler handler) {
        this.missingKeyHandler = Objects.requireNonNull(handler, "handler cannot be null");
        return this;
    }

    @Override
    @NotNull
    public I18nService build() {
        return new DefaultI18nService(this);
    }

    // Package-private getters for DefaultI18nService

    Locale getDefaultLocale() {
        return defaultLocale;
    }

    Locale getFallbackLocale() {
        return fallbackLocale;
    }

    List<SourceEntry> getSources() {
        return sources;
    }

    Path getDirectory() {
        return directory;
    }

    boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }

    boolean isMiniMessageEnabled() {
        return miniMessageEnabled;
    }

    boolean isLegacyColorsEnabled() {
        return legacyColorsEnabled;
    }

    I18nService.MissingKeyHandler getMissingKeyHandler() {
        return missingKeyHandler;
    }

    /**
     * Internal record for storing source entries.
     */
    record SourceEntry(Locale locale, MessageSource source) {}
}
