/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Main service for placeholder operations in the UnifiedPlugin framework.
 *
 * <p>PlaceholderService provides a unified API for placeholder registration, resolution,
 * and management. It integrates with PlaceholderAPI when available through the PAPI bridge.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the placeholder service
 * PlaceholderService placeholders = PlaceholderService.getInstance();
 *
 * // Register an expansion
 * placeholders.registerExpansion(new MyPlaceholders());
 *
 * // Resolve placeholders in text
 * String message = placeholders.resolve("Hello, %player_name%!", player);
 *
 * // Resolve with relational context
 * String relational = placeholders.resolveRelational(
 *     "Relation: %rel_factions_relation%",
 *     viewer,
 *     target
 * );
 *
 * // Async resolution
 * CompletableFuture<String> async = placeholders.resolveAsync(
 *     "Balance: %vault_balance%",
 *     player
 * );
 *
 * // Register simple placeholder
 * placeholders.register("myPlugin", "version", () -> "1.0.0");
 * }</pre>
 *
 * <h2>Built-in Placeholders</h2>
 * <p>The service comes with built-in placeholders:
 * <ul>
 *   <li>{@code %server_name%}, {@code %server_tps%}, {@code %server_online%}</li>
 *   <li>{@code %player_name%}, {@code %player_health%}, {@code %player_world%}</li>
 *   <li>{@code %time_now%}, {@code %time_date%}</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderRegistry
 * @see PlaceholderResolver
 * @see PAPIBridge
 */
public final class PlaceholderService {

    private static volatile PlaceholderService instance;

    private final PlaceholderRegistry registry;
    private final PlaceholderCache cache;
    private final PlaceholderResolver resolver;
    private final PlaceholderParser parser;
    private PAPIBridge papiBridge;
    private boolean papiAvailable;

    private PlaceholderService() {
        this.registry = PlaceholderRegistry.create();
        this.cache = PlaceholderCache.create();
        this.parser = PlaceholderParser.withBrackets();
        this.resolver = PlaceholderResolver.builder()
            .registry(registry)
            .cache(cache)
            .parser(parser)
            .preserveUnknown(false)
            .build();
        this.papiAvailable = false;
    }

    /**
     * Returns the singleton instance of the placeholder service.
     *
     * @return the placeholder service instance
     */
    @NotNull
    public static PlaceholderService getInstance() {
        if (instance == null) {
            synchronized (PlaceholderService.class) {
                if (instance == null) {
                    instance = new PlaceholderService();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new independent placeholder service instance.
     *
     * @return a new placeholder service
     */
    @NotNull
    public static PlaceholderService create() {
        return new PlaceholderService();
    }

    /**
     * Initializes the service and registers built-in placeholders.
     */
    public void initialize() {
        // Register built-in placeholders
        registry.register(new ServerPlaceholders());
        registry.register(new PlayerPlaceholders());
        registry.register(new TimePlaceholders());

        // Try to initialize PAPI bridge
        initializePAPIBridge();
    }

    /**
     * Attempts to initialize the PlaceholderAPI bridge.
     */
    private void initializePAPIBridge() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            this.papiBridge = new PAPIBridge(this);
            this.papiAvailable = true;
        } catch (ClassNotFoundException e) {
            this.papiAvailable = false;
        }
    }

    /**
     * Registers a placeholder expansion.
     *
     * @param expansion the expansion instance
     */
    public void registerExpansion(@NotNull Object expansion) {
        Objects.requireNonNull(expansion, "expansion cannot be null");
        registry.register(expansion);

        // Register with PAPI if available
        if (papiAvailable && papiBridge != null) {
            PlaceholderExpansion annotation = expansion.getClass().getAnnotation(PlaceholderExpansion.class);
            if (annotation != null && annotation.registerWithPAPI()) {
                papiBridge.registerExpansion(annotation.identifier());
            }
        }
    }

    /**
     * Registers a simple placeholder.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @param supplier   the value supplier
     */
    public void register(@NotNull String expansion, @NotNull String identifier,
                         @NotNull java.util.function.Supplier<String> supplier) {
        registry.registerSimple(expansion, identifier, supplier);
    }

    /**
     * Registers a player-aware placeholder.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @param function   the function taking player and returning value
     */
    public void register(@NotNull String expansion, @NotNull String identifier,
                         @NotNull Function<UnifiedPlayer, String> function) {
        registry.register(expansion, identifier, context -> {
            return context.getPlayer()
                .map(p -> PlaceholderResult.success(function.apply(p)))
                .orElse(PlaceholderResult.empty());
        });
    }

    /**
     * Unregisters an expansion.
     *
     * @param expansion the expansion identifier
     * @return {@code true} if unregistered
     */
    public boolean unregisterExpansion(@NotNull String expansion) {
        boolean removed = registry.unregister(expansion);
        if (removed && papiAvailable && papiBridge != null) {
            papiBridge.unregisterExpansion(expansion);
        }
        return removed;
    }

    /**
     * Resolves placeholders in text for a player.
     *
     * @param text   the text containing placeholders
     * @param player the player context
     * @return the resolved text
     */
    @NotNull
    public String resolve(@NotNull String text, @Nullable UnifiedPlayer player) {
        PlaceholderContext context = player != null
            ? PlaceholderContext.of(player)
            : PlaceholderContext.EMPTY;

        // Try PAPI first if available
        String resolved = text;
        if (papiAvailable && papiBridge != null) {
            resolved = papiBridge.setPlaceholders(player, text);
        }

        // Then resolve our placeholders
        return resolver.resolve(resolved, context);
    }

    /**
     * Resolves placeholders without a player context.
     *
     * @param text the text containing placeholders
     * @return the resolved text
     */
    @NotNull
    public String resolve(@NotNull String text) {
        return resolve(text, (UnifiedPlayer) null);
    }

    /**
     * Resolves placeholders with a custom context.
     *
     * @param text    the text containing placeholders
     * @param context the resolution context
     * @return the resolved text
     */
    @NotNull
    public String resolve(@NotNull String text, @NotNull PlaceholderContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        String resolved = text;
        if (papiAvailable && papiBridge != null && context.getPlayer().isPresent()) {
            resolved = papiBridge.setPlaceholders(context.getPlayer().get(), text);
        }

        return resolver.resolve(resolved, context);
    }

    /**
     * Resolves relational placeholders between two players.
     *
     * @param text   the text containing placeholders
     * @param viewer the viewing player
     * @param target the target player
     * @return the resolved text
     */
    @NotNull
    public String resolveRelational(@NotNull String text, @NotNull UnifiedPlayer viewer, @NotNull UnifiedPlayer target) {
        Objects.requireNonNull(viewer, "viewer cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        PlaceholderContext context = PlaceholderContext.relational(viewer, target);

        String resolved = text;
        if (papiAvailable && papiBridge != null) {
            resolved = papiBridge.setRelationalPlaceholders(viewer, target, text);
        }

        return resolver.resolve(resolved, context);
    }

    /**
     * Resolves placeholders asynchronously.
     *
     * @param text   the text containing placeholders
     * @param player the player context
     * @return a future with the resolved text
     */
    @NotNull
    public CompletableFuture<String> resolveAsync(@NotNull String text, @Nullable UnifiedPlayer player) {
        PlaceholderContext context = player != null
            ? PlaceholderContext.of(player)
            : PlaceholderContext.EMPTY;
        return resolver.resolveAsync(text, context);
    }

    /**
     * Checks if placeholders are present in the text.
     *
     * @param text the text to check
     * @return {@code true} if placeholders are found
     */
    public boolean containsPlaceholders(@NotNull String text) {
        return parser.containsPlaceholders(text);
    }

    /**
     * Returns all registered expansion identifiers.
     *
     * @return a set of expansion identifiers
     */
    @NotNull
    public Set<String> getExpansions() {
        return registry.getExpansions();
    }

    /**
     * Returns all placeholders for an expansion.
     *
     * @param expansion the expansion identifier
     * @return a set of placeholder identifiers
     */
    @NotNull
    public Set<String> getPlaceholders(@NotNull String expansion) {
        return registry.getPlaceholders(expansion);
    }

    /**
     * Returns information about all registered expansions.
     *
     * @return a collection of expansion infos
     */
    @NotNull
    public Collection<PlaceholderRegistry.ExpansionInfo> getExpansionInfo() {
        return registry.getAllExpansions();
    }

    /**
     * Returns the underlying registry.
     *
     * @return the placeholder registry
     */
    @NotNull
    public PlaceholderRegistry getRegistry() {
        return registry;
    }

    /**
     * Returns the cache.
     *
     * @return the placeholder cache
     */
    @NotNull
    public PlaceholderCache getCache() {
        return cache;
    }

    /**
     * Returns the resolver.
     *
     * @return the placeholder resolver
     */
    @NotNull
    public PlaceholderResolver getResolver() {
        return resolver;
    }

    /**
     * Checks if PlaceholderAPI is available.
     *
     * @return {@code true} if PAPI is available
     */
    public boolean isPAPIAvailable() {
        return papiAvailable;
    }

    /**
     * Returns the PAPI bridge if available.
     *
     * @return the PAPI bridge, or null if not available
     */
    @Nullable
    public PAPIBridge getPAPIBridge() {
        return papiBridge;
    }

    /**
     * Invalidates all cached values.
     */
    public void invalidateCache() {
        cache.invalidateAll();
    }

    /**
     * Invalidates cached values for a specific player.
     *
     * @param player the player
     */
    public void invalidateCache(@NotNull UnifiedPlayer player) {
        cache.invalidatePlayer(player.getUniqueId());
    }

    /**
     * Shuts down the placeholder service.
     */
    public void shutdown() {
        cache.shutdown();
        registry.clear();

        if (papiBridge != null) {
            papiBridge.unregisterAll();
        }
    }
}
