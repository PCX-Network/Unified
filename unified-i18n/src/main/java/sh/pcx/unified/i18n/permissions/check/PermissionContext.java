/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.check;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the context for a permission check.
 *
 * <p>Permission contexts allow permissions to be scoped to specific worlds,
 * servers, or custom contexts. This enables fine-grained permission control
 * based on where and how the permission is being checked.
 *
 * <h2>Standard Contexts</h2>
 * <ul>
 *   <li>{@code world} - The Minecraft world (e.g., "world", "world_nether")</li>
 *   <li>{@code server} - The server name in a network (e.g., "survival", "creative")</li>
 *   <li>{@code gamemode} - The player's game mode</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a world context
 * PermissionContext worldCtx = PermissionContext.world("world_nether");
 *
 * // Create a server context
 * PermissionContext serverCtx = PermissionContext.server("survival");
 *
 * // Create a combined context
 * PermissionContext ctx = PermissionContext.builder()
 *     .world("world_nether")
 *     .server("survival")
 *     .custom("region", "spawn")
 *     .build();
 *
 * // Use in permission check
 * boolean canBuild = permService.hasPermission(player, "build.nether", ctx);
 *
 * // Check context values
 * String world = ctx.getWorld().orElse("default");
 * }</pre>
 *
 * <h2>Context Inheritance</h2>
 * <p>When checking permissions, more specific contexts take precedence over
 * less specific ones. A permission set for "world_nether" will override a
 * global permission when checking in that world.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PermissionChecker
 * @see PermissionCheck
 */
public final class PermissionContext {

    /**
     * Standard context key for world.
     */
    public static final String KEY_WORLD = "world";

    /**
     * Standard context key for server.
     */
    public static final String KEY_SERVER = "server";

    /**
     * Standard context key for game mode.
     */
    public static final String KEY_GAMEMODE = "gamemode";

    /**
     * An empty context with no restrictions.
     */
    public static final PermissionContext GLOBAL = new PermissionContext(Collections.emptyMap());

    private final Map<String, String> values;

    /**
     * Creates a new PermissionContext with the given values.
     *
     * @param values the context values
     */
    private PermissionContext(@NotNull Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    /**
     * Returns the global (empty) context.
     *
     * @return the global context
     * @since 1.0.0
     */
    @NotNull
    public static PermissionContext global() {
        return GLOBAL;
    }

    /**
     * Creates a context with just a world value.
     *
     * @param world the world name
     * @return the context
     * @throws NullPointerException if world is null
     * @since 1.0.0
     */
    @NotNull
    public static PermissionContext world(@NotNull String world) {
        Objects.requireNonNull(world, "world cannot be null");
        return new PermissionContext(Map.of(KEY_WORLD, world));
    }

    /**
     * Creates a context with just a server value.
     *
     * @param server the server name
     * @return the context
     * @throws NullPointerException if server is null
     * @since 1.0.0
     */
    @NotNull
    public static PermissionContext server(@NotNull String server) {
        Objects.requireNonNull(server, "server cannot be null");
        return new PermissionContext(Map.of(KEY_SERVER, server));
    }

    /**
     * Creates a context with world and server values.
     *
     * @param world  the world name
     * @param server the server name
     * @return the context
     * @throws NullPointerException if either parameter is null
     * @since 1.0.0
     */
    @NotNull
    public static PermissionContext of(@NotNull String world, @NotNull String server) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(server, "server cannot be null");
        return new PermissionContext(Map.of(KEY_WORLD, world, KEY_SERVER, server));
    }

    /**
     * Creates a context from a map of values.
     *
     * @param values the context values
     * @return the context
     * @throws NullPointerException if values is null
     * @since 1.0.0
     */
    @NotNull
    public static PermissionContext of(@NotNull Map<String, String> values) {
        Objects.requireNonNull(values, "values cannot be null");
        if (values.isEmpty()) {
            return GLOBAL;
        }
        return new PermissionContext(values);
    }

    /**
     * Creates a new builder for constructing contexts.
     *
     * @return a new context builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the world value if set.
     *
     * @return an Optional containing the world if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getWorld() {
        return Optional.ofNullable(values.get(KEY_WORLD));
    }

    /**
     * Returns the server value if set.
     *
     * @return an Optional containing the server if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getServer() {
        return Optional.ofNullable(values.get(KEY_SERVER));
    }

    /**
     * Returns the game mode value if set.
     *
     * @return an Optional containing the game mode if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getGameMode() {
        return Optional.ofNullable(values.get(KEY_GAMEMODE));
    }

    /**
     * Returns a context value by key.
     *
     * @param key the context key
     * @return an Optional containing the value if set
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> get(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable(values.get(key));
    }

    /**
     * Checks if this context has a value for the given key.
     *
     * @param key the context key
     * @return true if a value is set
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    public boolean has(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return values.containsKey(key);
    }

    /**
     * Returns all context values.
     *
     * @return an unmodifiable map of context values
     * @since 1.0.0
     */
    @NotNull
    public Map<String, String> getValues() {
        return values;
    }

    /**
     * Checks if this is the global (empty) context.
     *
     * @return true if this context has no values
     * @since 1.0.0
     */
    public boolean isGlobal() {
        return values.isEmpty();
    }

    /**
     * Returns the number of context values.
     *
     * @return the number of values
     * @since 1.0.0
     */
    public int size() {
        return values.size();
    }

    /**
     * Checks if this context is a subset of another.
     *
     * <p>A context A is a subset of B if all values in A are present in B
     * with the same values.
     *
     * @param other the other context
     * @return true if this is a subset
     * @throws NullPointerException if other is null
     * @since 1.0.0
     */
    public boolean isSubsetOf(@NotNull PermissionContext other) {
        Objects.requireNonNull(other, "other cannot be null");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String otherValue = other.values.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), otherValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new context with additional values merged in.
     *
     * @param other the context to merge with
     * @return a new merged context
     * @throws NullPointerException if other is null
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext merge(@NotNull PermissionContext other) {
        Objects.requireNonNull(other, "other cannot be null");
        if (this.isGlobal()) {
            return other;
        }
        if (other.isGlobal()) {
            return this;
        }
        Map<String, String> merged = new HashMap<>(this.values);
        merged.putAll(other.values);
        return new PermissionContext(merged);
    }

    /**
     * Creates a new context with the specified value added or replaced.
     *
     * @param key   the context key
     * @param value the context value
     * @return a new context with the value set
     * @throws NullPointerException if key or value is null
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext with(@NotNull String key, @NotNull String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Map<String, String> newValues = new HashMap<>(this.values);
        newValues.put(key, value);
        return new PermissionContext(newValues);
    }

    /**
     * Creates a new context with the specified world value.
     *
     * @param world the world name
     * @return a new context with the world set
     * @throws NullPointerException if world is null
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext withWorld(@NotNull String world) {
        return with(KEY_WORLD, world);
    }

    /**
     * Creates a new context with the specified server value.
     *
     * @param server the server name
     * @return a new context with the server set
     * @throws NullPointerException if server is null
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext withServer(@NotNull String server) {
        return with(KEY_SERVER, server);
    }

    /**
     * Creates a new context with the specified key removed.
     *
     * @param key the key to remove
     * @return a new context without the key
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext without(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        if (!values.containsKey(key)) {
            return this;
        }
        Map<String, String> newValues = new HashMap<>(this.values);
        newValues.remove(key);
        if (newValues.isEmpty()) {
            return GLOBAL;
        }
        return new PermissionContext(newValues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionContext that = (PermissionContext) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        if (isGlobal()) {
            return "PermissionContext{GLOBAL}";
        }
        return "PermissionContext" + values;
    }

    /**
     * Builder for constructing PermissionContext instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final Map<String, String> values = new HashMap<>();

        private Builder() {
        }

        /**
         * Sets the world value.
         *
         * @param world the world name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder world(@Nullable String world) {
            if (world != null) {
                values.put(KEY_WORLD, world);
            }
            return this;
        }

        /**
         * Sets the server value.
         *
         * @param server the server name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder server(@Nullable String server) {
            if (server != null) {
                values.put(KEY_SERVER, server);
            }
            return this;
        }

        /**
         * Sets the game mode value.
         *
         * @param gameMode the game mode
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder gameMode(@Nullable String gameMode) {
            if (gameMode != null) {
                values.put(KEY_GAMEMODE, gameMode);
            }
            return this;
        }

        /**
         * Sets a custom context value.
         *
         * @param key   the context key
         * @param value the context value
         * @return this builder
         * @throws NullPointerException if key is null
         * @since 1.0.0
         */
        @NotNull
        public Builder custom(@NotNull String key, @Nullable String value) {
            Objects.requireNonNull(key, "key cannot be null");
            if (value != null) {
                values.put(key, value);
            }
            return this;
        }

        /**
         * Adds all values from a map.
         *
         * @param contextValues the values to add
         * @return this builder
         * @throws NullPointerException if contextValues is null
         * @since 1.0.0
         */
        @NotNull
        public Builder addAll(@NotNull Map<String, String> contextValues) {
            Objects.requireNonNull(contextValues, "contextValues cannot be null");
            values.putAll(contextValues);
            return this;
        }

        /**
         * Builds the PermissionContext.
         *
         * @return the constructed context
         * @since 1.0.0
         */
        @NotNull
        public PermissionContext build() {
            if (values.isEmpty()) {
                return GLOBAL;
            }
            return new PermissionContext(values);
        }
    }
}
