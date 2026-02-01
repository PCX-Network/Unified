/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a permission node with its associated value and metadata.
 *
 * <p>A permission consists of:
 * <ul>
 *   <li>A permission node (e.g., "myplugin.admin.teleport")</li>
 *   <li>A value (true/false/undefined)</li>
 *   <li>Optional expiration for temporary permissions</li>
 *   <li>Optional context (world, server)</li>
 * </ul>
 *
 * <h2>Permission Node Format</h2>
 * <p>Permission nodes follow a hierarchical dot-notation format:
 * <pre>{@code
 * myplugin.feature.action
 * myplugin.admin.*
 * myplugin.*
 * }</pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a simple permission
 * Permission perm = Permission.of("myplugin.admin");
 *
 * // Create a negated permission
 * Permission negated = Permission.of("myplugin.feature", false);
 *
 * // Create a temporary permission
 * Permission temp = Permission.builder("myplugin.vip")
 *     .value(true)
 *     .expiration(Instant.now().plus(Duration.ofDays(30)))
 *     .world("world")
 *     .build();
 *
 * // Check permission properties
 * if (perm.isWildcard()) {
 *     // Handle wildcard permission
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PermissionService
 * @see TriState
 */
public final class Permission {

    private final String node;
    private final TriState value;
    private final Instant expiration;
    private final String world;
    private final String server;
    private final String source;

    /**
     * Creates a new Permission instance.
     *
     * @param node       the permission node
     * @param value      the permission value
     * @param expiration the expiration time, or null for permanent
     * @param world      the world context, or null for all worlds
     * @param server     the server context, or null for all servers
     * @param source     the source that set this permission, or null
     */
    private Permission(
            @NotNull String node,
            @NotNull TriState value,
            @Nullable Instant expiration,
            @Nullable String world,
            @Nullable String server,
            @Nullable String source
    ) {
        this.node = Objects.requireNonNull(node, "node cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.expiration = expiration;
        this.world = world;
        this.server = server;
        this.source = source;
    }

    /**
     * Creates a new permission with the given node and TRUE value.
     *
     * @param node the permission node
     * @return the created permission
     * @throws NullPointerException if node is null
     * @since 1.0.0
     */
    @NotNull
    public static Permission of(@NotNull String node) {
        return new Permission(node, TriState.TRUE, null, null, null, null);
    }

    /**
     * Creates a new permission with the given node and value.
     *
     * @param node  the permission node
     * @param value the permission value (true = granted, false = denied)
     * @return the created permission
     * @throws NullPointerException if node is null
     * @since 1.0.0
     */
    @NotNull
    public static Permission of(@NotNull String node, boolean value) {
        return new Permission(node, TriState.of(value), null, null, null, null);
    }

    /**
     * Creates a new permission with the given node and TriState value.
     *
     * @param node  the permission node
     * @param value the permission value
     * @return the created permission
     * @throws NullPointerException if node or value is null
     * @since 1.0.0
     */
    @NotNull
    public static Permission of(@NotNull String node, @NotNull TriState value) {
        return new Permission(node, value, null, null, null, null);
    }

    /**
     * Creates a new builder for constructing permissions.
     *
     * @param node the permission node
     * @return a new permission builder
     * @throws NullPointerException if node is null
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull String node) {
        return new Builder(node);
    }

    /**
     * Returns the permission node.
     *
     * @return the permission node string
     * @since 1.0.0
     */
    @NotNull
    public String getNode() {
        return node;
    }

    /**
     * Returns the permission value.
     *
     * @return the permission value as a TriState
     * @since 1.0.0
     */
    @NotNull
    public TriState getValue() {
        return value;
    }

    /**
     * Returns the permission value as a boolean.
     *
     * @return true if the permission is granted
     * @since 1.0.0
     */
    public boolean isGranted() {
        return value == TriState.TRUE;
    }

    /**
     * Returns the permission value as a boolean.
     *
     * @return true if the permission is explicitly denied
     * @since 1.0.0
     */
    public boolean isDenied() {
        return value == TriState.FALSE;
    }

    /**
     * Returns the expiration time for temporary permissions.
     *
     * @return an Optional containing the expiration time if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Instant> getExpiration() {
        return Optional.ofNullable(expiration);
    }

    /**
     * Checks if this permission is temporary (has an expiration).
     *
     * @return true if this permission has an expiration time
     * @since 1.0.0
     */
    public boolean isTemporary() {
        return expiration != null;
    }

    /**
     * Checks if this permission is permanent (no expiration).
     *
     * @return true if this permission has no expiration time
     * @since 1.0.0
     */
    public boolean isPermanent() {
        return expiration == null;
    }

    /**
     * Checks if this permission has expired.
     *
     * @return true if the permission has expired, false if permanent or not yet expired
     * @since 1.0.0
     */
    public boolean hasExpired() {
        return expiration != null && Instant.now().isAfter(expiration);
    }

    /**
     * Returns the remaining duration until expiration.
     *
     * @return an Optional containing the remaining duration, or empty if permanent
     * @since 1.0.0
     */
    @NotNull
    public Optional<Duration> getRemainingDuration() {
        if (expiration == null) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(Instant.now(), expiration);
        return Optional.of(remaining.isNegative() ? Duration.ZERO : remaining);
    }

    /**
     * Returns the world context for this permission.
     *
     * @return an Optional containing the world name if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getWorld() {
        return Optional.ofNullable(world);
    }

    /**
     * Returns the server context for this permission.
     *
     * @return an Optional containing the server name if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getServer() {
        return Optional.ofNullable(server);
    }

    /**
     * Returns the source that set this permission.
     *
     * @return an Optional containing the source if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getSource() {
        return Optional.ofNullable(source);
    }

    /**
     * Checks if this permission applies globally (no context restrictions).
     *
     * @return true if no world or server context is set
     * @since 1.0.0
     */
    public boolean isGlobal() {
        return world == null && server == null;
    }

    /**
     * Checks if this permission has any context restrictions.
     *
     * @return true if world or server context is set
     * @since 1.0.0
     */
    public boolean hasContext() {
        return world != null || server != null;
    }

    /**
     * Checks if this is a wildcard permission.
     *
     * <p>Wildcard permissions end with ".*" or are just "*".
     *
     * @return true if this is a wildcard permission
     * @since 1.0.0
     */
    public boolean isWildcard() {
        return node.endsWith(".*") || node.equals("*");
    }

    /**
     * Returns the parent permission node.
     *
     * <p>For "myplugin.admin.teleport", returns "myplugin.admin".
     * For "myplugin", returns an empty Optional.
     *
     * @return an Optional containing the parent node if it exists
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getParentNode() {
        int lastDot = node.lastIndexOf('.');
        if (lastDot <= 0) {
            return Optional.empty();
        }
        return Optional.of(node.substring(0, lastDot));
    }

    /**
     * Checks if this permission matches or is a parent of the given node.
     *
     * <p>For example, "myplugin.*" matches "myplugin.admin" and "myplugin.admin.teleport".
     *
     * @param otherNode the node to check
     * @return true if this permission matches or is a parent of the other node
     * @since 1.0.0
     */
    public boolean matches(@NotNull String otherNode) {
        Objects.requireNonNull(otherNode, "otherNode cannot be null");

        if (node.equals(otherNode)) {
            return true;
        }

        if (node.equals("*")) {
            return true;
        }

        if (node.endsWith(".*")) {
            String prefix = node.substring(0, node.length() - 1);
            return otherNode.startsWith(prefix) || otherNode.equals(prefix.substring(0, prefix.length() - 1));
        }

        return false;
    }

    /**
     * Creates a copy of this permission with a new value.
     *
     * @param newValue the new value
     * @return a new Permission with the updated value
     * @since 1.0.0
     */
    @NotNull
    public Permission withValue(@NotNull TriState newValue) {
        return new Permission(node, newValue, expiration, world, server, source);
    }

    /**
     * Creates a copy of this permission with a new expiration.
     *
     * @param newExpiration the new expiration, or null for permanent
     * @return a new Permission with the updated expiration
     * @since 1.0.0
     */
    @NotNull
    public Permission withExpiration(@Nullable Instant newExpiration) {
        return new Permission(node, value, newExpiration, world, server, source);
    }

    /**
     * Creates a copy of this permission with no expiration (permanent).
     *
     * @return a new Permission with no expiration
     * @since 1.0.0
     */
    @NotNull
    public Permission asPermanent() {
        return new Permission(node, value, null, world, server, source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(node, that.node)
                && value == that.value
                && Objects.equals(expiration, that.expiration)
                && Objects.equals(world, that.world)
                && Objects.equals(server, that.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, value, expiration, world, server);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Permission{node='");
        sb.append(node).append("', value=").append(value);
        if (expiration != null) {
            sb.append(", expiration=").append(expiration);
        }
        if (world != null) {
            sb.append(", world='").append(world).append("'");
        }
        if (server != null) {
            sb.append(", server='").append(server).append("'");
        }
        return sb.append("}").toString();
    }

    /**
     * Builder for constructing Permission instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final String node;
        private TriState value = TriState.TRUE;
        private Instant expiration;
        private String world;
        private String server;
        private String source;

        private Builder(@NotNull String node) {
            this.node = Objects.requireNonNull(node, "node cannot be null");
        }

        /**
         * Sets the permission value.
         *
         * @param value true for granted, false for denied
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder value(boolean value) {
            this.value = TriState.of(value);
            return this;
        }

        /**
         * Sets the permission value.
         *
         * @param value the TriState value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder value(@NotNull TriState value) {
            this.value = Objects.requireNonNull(value, "value cannot be null");
            return this;
        }

        /**
         * Sets the expiration time for a temporary permission.
         *
         * @param expiration the expiration time
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder expiration(@Nullable Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * Sets the expiration duration from now.
         *
         * @param duration the duration until expiration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder expiresIn(@NotNull Duration duration) {
            Objects.requireNonNull(duration, "duration cannot be null");
            this.expiration = Instant.now().plus(duration);
            return this;
        }

        /**
         * Sets the world context.
         *
         * @param world the world name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder world(@Nullable String world) {
            this.world = world;
            return this;
        }

        /**
         * Sets the server context.
         *
         * @param server the server name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder server(@Nullable String server) {
            this.server = server;
            return this;
        }

        /**
         * Sets the source that created this permission.
         *
         * @param source the source identifier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder source(@Nullable String source) {
            this.source = source;
            return this;
        }

        /**
         * Builds the Permission instance.
         *
         * @return the constructed Permission
         * @since 1.0.0
         */
        @NotNull
        public Permission build() {
            return new Permission(node, value, expiration, world, server, source);
        }
    }
}
