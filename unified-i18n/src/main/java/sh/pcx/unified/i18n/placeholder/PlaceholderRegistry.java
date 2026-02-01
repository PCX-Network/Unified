/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Registry for placeholder expansions and handlers.
 *
 * <p>The placeholder registry stores all registered placeholder expansions and their
 * handlers. It supports automatic discovery of annotated classes and methods.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlaceholderRegistry registry = PlaceholderRegistry.create();
 *
 * // Register an annotated expansion class
 * registry.register(new MyPlaceholders());
 *
 * // Register a simple placeholder
 * registry.register("server", "tps", context -> {
 *     return PlaceholderResult.success(String.format("%.2f", server.getTPS()));
 * });
 *
 * // Register with lambda
 * registry.register("player", "health", (ctx, id) -> {
 *     return ctx.getPlayer()
 *         .map(p -> PlaceholderResult.success(String.valueOf(p.getHealth())))
 *         .orElse(PlaceholderResult.empty());
 * });
 *
 * // Resolve a placeholder
 * PlaceholderResult result = registry.resolve("player", "health", context);
 *
 * // Get all expansions
 * Set<String> expansions = registry.getExpansions();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderExpansion
 * @see Placeholder
 * @see PlaceholderResolver
 */
public final class PlaceholderRegistry {

    private final Map<String, ExpansionEntry> expansions;
    private final Map<String, Map<String, HandlerEntry>> handlers;
    private final List<PlaceholderRegistrationListener> listeners;

    private PlaceholderRegistry() {
        this.expansions = new ConcurrentHashMap<>();
        this.handlers = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * Creates a new placeholder registry.
     *
     * @return a new registry
     */
    @NotNull
    public static PlaceholderRegistry create() {
        return new PlaceholderRegistry();
    }

    /**
     * Registers an annotated placeholder expansion class.
     *
     * @param expansion the expansion instance
     * @throws IllegalArgumentException if the class is not a valid expansion
     */
    public void register(@NotNull Object expansion) {
        Objects.requireNonNull(expansion, "expansion cannot be null");

        Class<?> clazz = expansion.getClass();
        PlaceholderExpansion annotation = clazz.getAnnotation(PlaceholderExpansion.class);

        if (annotation == null) {
            throw new IllegalArgumentException("Class must be annotated with @PlaceholderExpansion: " + clazz.getName());
        }

        String identifier = annotation.identifier().toLowerCase();

        // Store expansion metadata
        ExpansionEntry entry = new ExpansionEntry(
            identifier,
            annotation.author(),
            annotation.version(),
            annotation.description(),
            annotation.requiresPlayer(),
            annotation.priority(),
            expansion
        );
        expansions.put(identifier, entry);

        // Register all placeholder methods
        Map<String, HandlerEntry> expansionHandlers = handlers.computeIfAbsent(identifier, k -> new ConcurrentHashMap<>());

        for (Method method : clazz.getDeclaredMethods()) {
            Placeholder placeholder = method.getAnnotation(Placeholder.class);
            if (placeholder == null) continue;

            if (!Modifier.isPublic(method.getModifiers())) {
                method.setAccessible(true);
            }

            boolean relational = method.isAnnotationPresent(Relational.class);
            Relational relationalAnnotation = method.getAnnotation(Relational.class);

            HandlerEntry handler = new HandlerEntry(
                placeholder.value(),
                placeholder.description(),
                placeholder.cacheable(),
                placeholder.cacheTTL() > 0 ? CacheTTL.ofMillis(placeholder.cacheTTL()) : CacheTTL.DEFAULT,
                placeholder.async(),
                placeholder.fallback(),
                relational,
                relationalAnnotation,
                createHandler(expansion, method)
            );

            expansionHandlers.put(placeholder.value().toLowerCase(), handler);
        }

        // Notify listeners
        for (PlaceholderRegistrationListener listener : listeners) {
            listener.onExpansionRegistered(identifier, entry);
        }
    }

    /**
     * Creates a handler function from a method.
     */
    private BiFunction<PlaceholderContext, String, PlaceholderResult> createHandler(Object instance, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();

        return (context, identifier) -> {
            try {
                Object result;

                if (paramTypes.length == 0) {
                    // No parameters
                    result = method.invoke(instance);
                } else if (paramTypes.length == 1 && paramTypes[0] == PlaceholderContext.class) {
                    // Single context parameter
                    result = method.invoke(instance, context);
                } else if (paramTypes.length == 1) {
                    // Single player parameter
                    Object player = context.getPlayer().orElse(null);
                    if (player == null) {
                        return PlaceholderResult.empty();
                    }
                    result = method.invoke(instance, player);
                } else if (paramTypes.length == 2 && paramTypes[1] == String.class) {
                    // Player + identifier parameter
                    Object player = context.getPlayer().orElse(null);
                    if (player == null) {
                        return PlaceholderResult.empty();
                    }
                    result = method.invoke(instance, player, identifier);
                } else if (paramTypes.length == 2) {
                    // Two player parameters (relational)
                    Object viewer = context.getPlayer().orElse(null);
                    Object target = context.getRelationalPlayer().orElse(null);
                    if (viewer == null || target == null) {
                        return PlaceholderResult.empty();
                    }
                    result = method.invoke(instance, viewer, target);
                } else {
                    return PlaceholderResult.error("Unsupported method signature");
                }

                if (result == null) {
                    return PlaceholderResult.empty();
                }
                return PlaceholderResult.success(String.valueOf(result));

            } catch (Exception e) {
                return PlaceholderResult.error(e);
            }
        };
    }

    /**
     * Registers a simple placeholder handler.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @param handler    the handler function
     */
    public void register(@NotNull String expansion, @NotNull String identifier,
                         @NotNull Function<PlaceholderContext, PlaceholderResult> handler) {
        register(expansion, identifier, (ctx, id) -> handler.apply(ctx));
    }

    /**
     * Registers a placeholder handler with identifier access.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @param handler    the handler function
     */
    public void register(@NotNull String expansion, @NotNull String identifier,
                         @NotNull BiFunction<PlaceholderContext, String, PlaceholderResult> handler) {
        Objects.requireNonNull(expansion, "expansion cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        String expansionKey = expansion.toLowerCase();
        String identifierKey = identifier.toLowerCase();

        Map<String, HandlerEntry> expansionHandlers = handlers.computeIfAbsent(expansionKey, k -> new ConcurrentHashMap<>());

        HandlerEntry entry = new HandlerEntry(
            identifierKey,
            "",
            true,
            CacheTTL.DEFAULT,
            false,
            "",
            false,
            null,
            handler
        );

        expansionHandlers.put(identifierKey, entry);
    }

    /**
     * Registers a simple string-returning placeholder.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @param supplier   the value supplier
     */
    public void registerSimple(@NotNull String expansion, @NotNull String identifier,
                               @NotNull java.util.function.Supplier<String> supplier) {
        register(expansion, identifier, ctx -> PlaceholderResult.success(supplier.get()));
    }

    /**
     * Unregisters an expansion.
     *
     * @param expansion the expansion identifier
     * @return {@code true} if the expansion was removed
     */
    public boolean unregister(@NotNull String expansion) {
        Objects.requireNonNull(expansion, "expansion cannot be null");
        String key = expansion.toLowerCase();

        ExpansionEntry removed = expansions.remove(key);
        handlers.remove(key);

        if (removed != null) {
            for (PlaceholderRegistrationListener listener : listeners) {
                listener.onExpansionUnregistered(key);
            }
            return true;
        }
        return false;
    }

    /**
     * Unregisters a specific placeholder.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @return {@code true} if the placeholder was removed
     */
    public boolean unregister(@NotNull String expansion, @NotNull String identifier) {
        Map<String, HandlerEntry> expansionHandlers = handlers.get(expansion.toLowerCase());
        if (expansionHandlers != null) {
            return expansionHandlers.remove(identifier.toLowerCase()) != null;
        }
        return false;
    }

    /**
     * Resolves a placeholder.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @param context    the resolution context
     * @return the resolution result
     */
    @NotNull
    public PlaceholderResult resolve(@NotNull String expansion, @NotNull String identifier,
                                      @NotNull PlaceholderContext context) {
        Objects.requireNonNull(expansion, "expansion cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        Map<String, HandlerEntry> expansionHandlers = handlers.get(expansion.toLowerCase());
        if (expansionHandlers == null) {
            return PlaceholderResult.empty();
        }

        // Try exact match first
        HandlerEntry handler = expansionHandlers.get(identifier.toLowerCase());
        if (handler != null) {
            return handler.handler.apply(context, identifier);
        }

        // Try prefix match (for handlers ending with _)
        for (Map.Entry<String, HandlerEntry> entry : expansionHandlers.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("_") && identifier.toLowerCase().startsWith(key)) {
                String remainder = identifier.substring(key.length());
                return entry.getValue().handler.apply(context, remainder);
            }
        }

        return PlaceholderResult.empty();
    }

    /**
     * Returns the cache TTL for a placeholder.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @return the TTL, or default if not specified
     */
    @NotNull
    public CacheTTL getCacheTTL(@NotNull String expansion, @NotNull String identifier) {
        Map<String, HandlerEntry> expansionHandlers = handlers.get(expansion.toLowerCase());
        if (expansionHandlers != null) {
            HandlerEntry handler = expansionHandlers.get(identifier.toLowerCase());
            if (handler != null) {
                return handler.cacheable ? handler.cacheTTL : CacheTTL.NONE;
            }
        }
        return CacheTTL.DEFAULT;
    }

    /**
     * Checks if an expansion is registered.
     *
     * @param expansion the expansion identifier
     * @return {@code true} if registered
     */
    public boolean hasExpansion(@NotNull String expansion) {
        return expansions.containsKey(expansion.toLowerCase());
    }

    /**
     * Checks if a placeholder is registered.
     *
     * @param expansion  the expansion identifier
     * @param identifier the placeholder identifier
     * @return {@code true} if registered
     */
    public boolean hasPlaceholder(@NotNull String expansion, @NotNull String identifier) {
        Map<String, HandlerEntry> expansionHandlers = handlers.get(expansion.toLowerCase());
        return expansionHandlers != null && expansionHandlers.containsKey(identifier.toLowerCase());
    }

    /**
     * Returns all registered expansion identifiers.
     *
     * @return a set of expansion identifiers
     */
    @NotNull
    public Set<String> getExpansions() {
        return Collections.unmodifiableSet(expansions.keySet());
    }

    /**
     * Returns information about an expansion.
     *
     * @param expansion the expansion identifier
     * @return an Optional containing the expansion info
     */
    @NotNull
    public Optional<ExpansionInfo> getExpansionInfo(@NotNull String expansion) {
        ExpansionEntry entry = expansions.get(expansion.toLowerCase());
        if (entry == null) return Optional.empty();

        Map<String, HandlerEntry> expansionHandlers = handlers.getOrDefault(expansion.toLowerCase(), Collections.emptyMap());

        return Optional.of(new ExpansionInfo(
            entry.identifier,
            entry.author,
            entry.version,
            entry.description,
            entry.requiresPlayer,
            entry.priority,
            expansionHandlers.keySet()
        ));
    }

    /**
     * Returns all placeholders for an expansion.
     *
     * @param expansion the expansion identifier
     * @return a set of placeholder identifiers
     */
    @NotNull
    public Set<String> getPlaceholders(@NotNull String expansion) {
        Map<String, HandlerEntry> expansionHandlers = handlers.get(expansion.toLowerCase());
        if (expansionHandlers == null) return Collections.emptySet();
        return Collections.unmodifiableSet(expansionHandlers.keySet());
    }

    /**
     * Returns all registered expansions sorted by priority.
     *
     * @return a list of expansion infos
     */
    @NotNull
    public List<ExpansionInfo> getAllExpansions() {
        List<ExpansionInfo> result = new ArrayList<>();
        for (String expansion : expansions.keySet()) {
            getExpansionInfo(expansion).ifPresent(result::add);
        }
        result.sort(Comparator.comparingInt(ExpansionInfo::getPriority).reversed());
        return result;
    }

    /**
     * Adds a registration listener.
     *
     * @param listener the listener
     */
    public void addListener(@NotNull PlaceholderRegistrationListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a registration listener.
     *
     * @param listener the listener
     */
    public void removeListener(@NotNull PlaceholderRegistrationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Clears all registered expansions.
     */
    public void clear() {
        expansions.clear();
        handlers.clear();
    }

    /**
     * Internal expansion entry.
     */
    private static final class ExpansionEntry {
        final String identifier;
        final String author;
        final String version;
        final String description;
        final boolean requiresPlayer;
        final int priority;
        final Object instance;

        ExpansionEntry(String identifier, String author, String version, String description,
                       boolean requiresPlayer, int priority, Object instance) {
            this.identifier = identifier;
            this.author = author;
            this.version = version;
            this.description = description;
            this.requiresPlayer = requiresPlayer;
            this.priority = priority;
            this.instance = instance;
        }
    }

    /**
     * Internal handler entry.
     */
    private static final class HandlerEntry {
        final String identifier;
        final String description;
        final boolean cacheable;
        final CacheTTL cacheTTL;
        final boolean async;
        final String fallback;
        final boolean relational;
        final Relational relationalAnnotation;
        final BiFunction<PlaceholderContext, String, PlaceholderResult> handler;

        HandlerEntry(String identifier, String description, boolean cacheable, CacheTTL cacheTTL,
                     boolean async, String fallback, boolean relational, Relational relationalAnnotation,
                     BiFunction<PlaceholderContext, String, PlaceholderResult> handler) {
            this.identifier = identifier;
            this.description = description;
            this.cacheable = cacheable;
            this.cacheTTL = cacheTTL;
            this.async = async;
            this.fallback = fallback;
            this.relational = relational;
            this.relationalAnnotation = relationalAnnotation;
            this.handler = handler;
        }
    }

    /**
     * Information about a registered expansion.
     *
     * @since 1.0.0
     */
    public static final class ExpansionInfo {

        private final String identifier;
        private final String author;
        private final String version;
        private final String description;
        private final boolean requiresPlayer;
        private final int priority;
        private final Collection<String> placeholders;

        ExpansionInfo(String identifier, String author, String version, String description,
                      boolean requiresPlayer, int priority, Collection<String> placeholders) {
            this.identifier = identifier;
            this.author = author;
            this.version = version;
            this.description = description;
            this.requiresPlayer = requiresPlayer;
            this.priority = priority;
            this.placeholders = Collections.unmodifiableCollection(new ArrayList<>(placeholders));
        }

        /**
         * Returns the expansion identifier.
         *
         * @return the identifier
         */
        @NotNull
        public String getIdentifier() {
            return identifier;
        }

        /**
         * Returns the author.
         *
         * @return the author
         */
        @NotNull
        public String getAuthor() {
            return author;
        }

        /**
         * Returns the version.
         *
         * @return the version
         */
        @NotNull
        public String getVersion() {
            return version;
        }

        /**
         * Returns the description.
         *
         * @return the description
         */
        @NotNull
        public String getDescription() {
            return description;
        }

        /**
         * Returns whether a player is required.
         *
         * @return {@code true} if player required
         */
        public boolean requiresPlayer() {
            return requiresPlayer;
        }

        /**
         * Returns the priority.
         *
         * @return the priority
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Returns the placeholder identifiers.
         *
         * @return the placeholders
         */
        @NotNull
        public Collection<String> getPlaceholders() {
            return placeholders;
        }
    }

    /**
     * Listener for expansion registration events.
     *
     * @since 1.0.0
     */
    public interface PlaceholderRegistrationListener {

        /**
         * Called when an expansion is registered.
         *
         * @param identifier the expansion identifier
         * @param entry      the expansion entry
         */
        void onExpansionRegistered(String identifier, Object entry);

        /**
         * Called when an expansion is unregistered.
         *
         * @param identifier the expansion identifier
         */
        void onExpansionUnregistered(String identifier);
    }
}
