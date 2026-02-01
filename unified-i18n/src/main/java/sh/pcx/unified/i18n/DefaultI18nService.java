/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import sh.pcx.unified.i18n.core.Locale;
import sh.pcx.unified.i18n.core.LocaleRegistry;
import sh.pcx.unified.i18n.core.MessageKey;
import sh.pcx.unified.i18n.formatting.MessageFormatter;
import sh.pcx.unified.i18n.formatting.PluralRules;
import sh.pcx.unified.i18n.formatting.Replacement;
import sh.pcx.unified.i18n.messages.FileMessageSource;
import sh.pcx.unified.i18n.messages.MessageBundle;
import sh.pcx.unified.i18n.messages.MessageSource;
import sh.pcx.unified.i18n.player.PlayerLocale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Default implementation of the I18nService.
 *
 * <p>This implementation provides:
 * <ul>
 *   <li>Multi-locale message management</li>
 *   <li>Fallback chain resolution</li>
 *   <li>MiniMessage formatting</li>
 *   <li>Pluralization support</li>
 *   <li>Hot reload via file watching</li>
 *   <li>Per-player locale preferences</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class DefaultI18nService implements I18nService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultI18nService.class);
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final LocaleRegistry localeRegistry;
    private final Map<Locale, MessageBundle> bundles;
    private final Map<Locale, List<MessageSource>> sources;
    private final Map<UUID, PlayerLocale> playerLocales;
    private final MessageFormatter formatter;
    private final MissingKeyHandler missingKeyHandler;
    private final Locale fallbackLocale;
    private final Set<Consumer<Set<Locale>>> reloadCallbacks;

    private volatile Locale defaultLocale;
    private volatile boolean hotReloadEnabled;
    private Path watchDirectory;
    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean running;
    private final ExecutorService executor;

    /**
     * Creates a new DefaultI18nService from a builder.
     *
     * @param builder the builder
     */
    DefaultI18nService(I18nServiceBuilder builder) {
        this.localeRegistry = LocaleRegistry.create();
        this.bundles = new ConcurrentHashMap<>();
        this.sources = new ConcurrentHashMap<>();
        this.playerLocales = new ConcurrentHashMap<>();
        this.reloadCallbacks = ConcurrentHashMap.newKeySet();

        this.defaultLocale = builder.getDefaultLocale();
        this.fallbackLocale = builder.getFallbackLocale();
        this.hotReloadEnabled = builder.isHotReloadEnabled();
        this.missingKeyHandler = builder.getMissingKeyHandler();

        this.formatter = MessageFormatter.builder()
                .miniMessage(builder.isMiniMessageEnabled())
                .legacyColors(builder.isLegacyColorsEnabled())
                .build();

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "I18n-Reload");
            t.setDaemon(true);
            return t;
        });

        // Register locales and load sources
        localeRegistry.register(defaultLocale, defaultLocale.getCode());
        localeRegistry.register(fallbackLocale, fallbackLocale.getCode());

        for (I18nServiceBuilder.SourceEntry entry : builder.getSources()) {
            registerSource(entry.locale(), entry.source());
        }

        // Load from directory if specified
        if (builder.getDirectory() != null) {
            this.watchDirectory = builder.getDirectory();
            loadFromDirectory(builder.getDirectory());

            if (hotReloadEnabled) {
                startWatching();
            }
        }
    }

    // ===== Translation Methods =====

    @Override
    @NotNull
    public Component translate(@NotNull Locale locale, @NotNull MessageKey key,
                               @NotNull Replacement... replacements) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        String rawMessage = resolveMessage(locale, key, replacements);
        return formatter.format(rawMessage, locale, replacements);
    }

    @Override
    @NotNull
    public Component translate(@NotNull UUID playerId, @NotNull MessageKey key,
                               @NotNull Replacement... replacements) {
        Locale locale = getEffectiveLocale(playerId);
        return translate(locale, key, replacements);
    }

    @Override
    @NotNull
    public String translatePlain(@NotNull Locale locale, @NotNull MessageKey key,
                                  @NotNull Replacement... replacements) {
        Component component = translate(locale, key, replacements);
        return PLAIN_SERIALIZER.serialize(component);
    }

    @Override
    @NotNull
    public Component translateRaw(@NotNull Locale locale, @NotNull String message,
                                   @NotNull Replacement... replacements) {
        return formatter.format(message, locale, replacements);
    }

    @Override
    @NotNull
    public Optional<String> getRawMessage(@NotNull Locale locale, @NotNull MessageKey key) {
        MessageBundle bundle = bundles.get(locale);
        if (bundle != null) {
            return bundle.get(key);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasKey(@NotNull MessageKey key) {
        for (MessageBundle bundle : bundles.values()) {
            if (bundle.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasKey(@NotNull Locale locale, @NotNull MessageKey key) {
        MessageBundle bundle = bundles.get(locale);
        return bundle != null && bundle.contains(key);
    }

    // ===== Locale Management =====

    @Override
    @NotNull
    public LocaleRegistry getLocaleRegistry() {
        return localeRegistry;
    }

    @Override
    @NotNull
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public void setDefaultLocale(@NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");
        this.defaultLocale = locale;
        if (!localeRegistry.isRegistered(locale)) {
            localeRegistry.register(locale, locale.getCode());
        }
    }

    @Override
    @NotNull
    public Set<Locale> getLoadedLocales() {
        return Collections.unmodifiableSet(bundles.keySet());
    }

    @Override
    public boolean isLoaded(@NotNull Locale locale) {
        return bundles.containsKey(locale);
    }

    // ===== Message Sources =====

    @Override
    public void registerSource(@NotNull Locale locale, @NotNull MessageSource source) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(source, "source cannot be null");

        sources.computeIfAbsent(locale, k -> new CopyOnWriteArrayList<>()).add(source);

        try {
            Map<String, String> messages = source.load();
            MessageBundle bundle = bundles.computeIfAbsent(locale, MessageBundle::create);
            bundle.setFormatter(formatter);
            bundle.putAll(messages);

            if (!localeRegistry.isRegistered(locale)) {
                localeRegistry.register(locale, locale.getCode());
            }

            logger.debug("Loaded {} messages for locale {} from {}", messages.size(), locale, source.getDescription());
        } catch (IOException e) {
            logger.error("Failed to load messages from source: " + source.getDescription(), e);
        }
    }

    @Override
    public int loadFromDirectory(@NotNull Path directory) {
        Objects.requireNonNull(directory, "directory cannot be null");

        if (!Files.isDirectory(directory)) {
            logger.warn("Message directory does not exist: {}", directory);
            return 0;
        }

        int loaded = 0;
        try (var stream = Files.list(directory)) {
            for (Path file : stream.toList()) {
                String fileName = file.getFileName().toString();
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                // Extract locale from filename (e.g., "en_US.yml" -> "en_US")
                String baseName = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf('.'))
                        : fileName;

                Locale.tryParse(baseName).ifPresent(locale -> {
                    MessageSource source = FileMessageSource.of(locale, file);
                    registerSource(locale, source);
                });
                loaded++;
            }
        } catch (IOException e) {
            logger.error("Failed to load messages from directory: " + directory, e);
        }

        return loaded;
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> loadFromDirectoryAsync(@NotNull Path directory) {
        return CompletableFuture.supplyAsync(() -> loadFromDirectory(directory), executor);
    }

    @Override
    @NotNull
    public Optional<MessageBundle> getBundle(@NotNull Locale locale) {
        return Optional.ofNullable(bundles.get(locale));
    }

    @Override
    @NotNull
    public MessageBundle createBundle(@NotNull Locale locale) {
        return bundles.computeIfAbsent(locale, l -> {
            MessageBundle bundle = MessageBundle.create(l);
            bundle.setFormatter(formatter);
            return bundle;
        });
    }

    // ===== Player Locale Management =====

    @Override
    @NotNull
    public Optional<PlayerLocale> getPlayerLocale(@NotNull UUID playerId) {
        return Optional.ofNullable(playerLocales.get(playerId));
    }

    @Override
    @NotNull
    public Locale getEffectiveLocale(@NotNull UUID playerId) {
        PlayerLocale playerLocale = playerLocales.get(playerId);
        if (playerLocale != null) {
            return playerLocale.getEffectiveLocale(defaultLocale);
        }
        return defaultLocale;
    }

    @Override
    public void setPlayerLocale(@NotNull UUID playerId, @Nullable Locale locale) {
        Objects.requireNonNull(playerId, "playerId cannot be null");

        if (locale == null) {
            PlayerLocale existing = playerLocales.get(playerId);
            if (existing != null) {
                playerLocales.put(playerId, existing.clearPreference());
            }
        } else {
            playerLocales.compute(playerId, (id, existing) -> {
                if (existing != null) {
                    return existing.withPreferredLocale(locale);
                }
                return PlayerLocale.of(id, locale);
            });
        }
    }

    @Override
    public void clearPlayerLocale(@NotNull UUID playerId) {
        setPlayerLocale(playerId, null);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> loadPlayerLocale(@NotNull UUID playerId) {
        // In a full implementation, this would load from storage
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> savePlayerLocale(@NotNull UUID playerId) {
        // In a full implementation, this would save to storage
        return CompletableFuture.completedFuture(null);
    }

    // ===== Hot Reload =====

    @Override
    public int reload() {
        int total = 0;
        Set<Locale> reloaded = new HashSet<>();

        for (Map.Entry<Locale, List<MessageSource>> entry : sources.entrySet()) {
            Locale locale = entry.getKey();
            MessageBundle bundle = bundles.computeIfAbsent(locale, MessageBundle::create);
            bundle.clear();
            bundle.setFormatter(formatter);

            for (MessageSource source : entry.getValue()) {
                try {
                    Map<String, String> messages = source.reload();
                    bundle.putAll(messages);
                    total += messages.size();
                } catch (IOException e) {
                    logger.error("Failed to reload source: " + source.getDescription(), e);
                }
            }

            reloaded.add(locale);
        }

        // Notify callbacks
        for (Consumer<Set<Locale>> callback : reloadCallbacks) {
            try {
                callback.accept(reloaded);
            } catch (Exception e) {
                logger.error("Error in reload callback", e);
            }
        }

        logger.info("Reloaded {} messages across {} locales", total, reloaded.size());
        return total;
    }

    @Override
    public boolean reload(@NotNull Locale locale) {
        List<MessageSource> localeSources = sources.get(locale);
        if (localeSources == null || localeSources.isEmpty()) {
            return false;
        }

        MessageBundle bundle = bundles.computeIfAbsent(locale, MessageBundle::create);
        bundle.clear();
        bundle.setFormatter(formatter);

        for (MessageSource source : localeSources) {
            try {
                bundle.putAll(source.reload());
            } catch (IOException e) {
                logger.error("Failed to reload source: " + source.getDescription(), e);
                return false;
            }
        }

        // Notify callbacks
        for (Consumer<Set<Locale>> callback : reloadCallbacks) {
            try {
                callback.accept(Set.of(locale));
            } catch (Exception e) {
                logger.error("Error in reload callback", e);
            }
        }

        return true;
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> reloadAsync() {
        return CompletableFuture.supplyAsync(this::reload, executor);
    }

    @Override
    public void setHotReloadEnabled(boolean enabled) {
        this.hotReloadEnabled = enabled;
        if (enabled && watchDirectory != null && watchThread == null) {
            startWatching();
        } else if (!enabled && watchThread != null) {
            stopWatching();
        }
    }

    @Override
    public boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }

    @Override
    public void onReload(@NotNull Consumer<Set<Locale>> callback) {
        reloadCallbacks.add(callback);
    }

    // ===== Private Helper Methods =====

    /**
     * Resolves a message through the fallback chain.
     */
    private String resolveMessage(Locale locale, MessageKey key, Replacement[] replacements) {
        // Find count replacement for pluralization
        long count = 1;
        for (Replacement r : replacements) {
            if (r.isCount()) {
                count = r.getCount();
                break;
            }
        }

        // Try requested locale
        Optional<String> message = resolveFromBundle(locale, key, count);
        if (message.isPresent()) {
            return message.get();
        }

        // Try language-only fallback (e.g., de_DE -> de)
        if (locale.hasCountry()) {
            Locale languageOnly = locale.withoutCountry();
            MessageBundle langBundle = bundles.get(languageOnly);
            if (langBundle != null) {
                message = resolvePluralMessage(langBundle, key, count);
                if (message.isPresent()) {
                    return message.get();
                }
            }
        }

        // Try default locale
        if (!locale.equals(defaultLocale)) {
            message = resolveFromBundle(defaultLocale, key, count);
            if (message.isPresent()) {
                return message.get();
            }
        }

        // Try fallback locale (en_US)
        if (!locale.equals(fallbackLocale) && !defaultLocale.equals(fallbackLocale)) {
            message = resolveFromBundle(fallbackLocale, key, count);
            if (message.isPresent()) {
                return message.get();
            }
        }

        // Return missing key fallback
        return missingKeyHandler.handle(locale, key);
    }

    private Optional<String> resolveFromBundle(Locale locale, MessageKey key, long count) {
        MessageBundle bundle = bundles.get(locale);
        if (bundle == null) {
            return Optional.empty();
        }
        return resolvePluralMessage(bundle, key, count);
    }

    private Optional<String> resolvePluralMessage(MessageBundle bundle, MessageKey key, long count) {
        // Try plural form first
        Optional<String> plural = bundle.getPlural(key, count);
        if (plural.isPresent()) {
            return plural;
        }
        // Fall back to base key
        return bundle.get(key);
    }

    // ===== File Watching =====

    private void startWatching() {
        if (watchDirectory == null) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            watchDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);

            running = true;
            watchThread = new Thread(this::watchLoop, "I18n-FileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();

            logger.info("Started watching for language file changes in: {}", watchDirectory);
        } catch (IOException e) {
            logger.error("Failed to start file watcher", e);
        }
    }

    private void stopWatching() {
        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.debug("Error closing watch service", e);
            }
            watchService = null;
        }
    }

    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }

                // Debounce: wait a bit before reloading
                Thread.sleep(100);

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path changed = (Path) event.context();
                    String fileName = changed.toString();

                    // Check if it's a language file
                    String baseName = fileName.contains(".")
                            ? fileName.substring(0, fileName.lastIndexOf('.'))
                            : fileName;

                    Locale.tryParse(baseName).ifPresent(locale -> {
                        logger.info("Detected change in language file: {}", fileName);
                        reload(locale);
                    });
                }

                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }
    }

    /**
     * Shuts down the service and releases resources.
     */
    public void shutdown() {
        stopWatching();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        bundles.clear();
        sources.clear();
        playerLocales.clear();
    }
}
