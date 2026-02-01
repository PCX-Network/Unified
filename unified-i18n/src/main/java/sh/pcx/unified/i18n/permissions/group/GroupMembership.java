/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.group;

import sh.pcx.unified.i18n.permissions.check.PermissionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player's membership in a permission group.
 *
 * <p>GroupMembership contains information about a player's association with
 * a group, including:
 * <ul>
 *   <li>The group reference</li>
 *   <li>Whether the membership is temporary</li>
 *   <li>Expiration time for temporary memberships</li>
 *   <li>Context restrictions (world, server)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get player's group memberships
 * Collection<GroupMembership> memberships = permService.getGroupMemberships(playerId);
 *
 * for (GroupMembership membership : memberships) {
 *     System.out.println("Group: " + membership.getGroupName());
 *
 *     if (membership.isTemporary()) {
 *         Duration remaining = membership.getRemainingDuration().orElse(Duration.ZERO);
 *         System.out.println("Expires in: " + remaining);
 *     }
 *
 *     if (membership.hasContext()) {
 *         membership.getWorld().ifPresent(world ->
 *             System.out.println("Only in world: " + world)
 *         );
 *     }
 * }
 *
 * // Find primary group
 * GroupMembership primary = memberships.stream()
 *     .filter(GroupMembership::isPrimary)
 *     .findFirst()
 *     .orElseThrow();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Group
 * @see GroupManager
 */
public final class GroupMembership {

    private final UUID playerId;
    private final String groupName;
    private final Group group;
    private final boolean primary;
    private final Instant expiration;
    private final String world;
    private final String server;
    private final Instant assignedAt;
    private final String assignedBy;

    /**
     * Creates a new GroupMembership.
     */
    private GroupMembership(
            @NotNull UUID playerId,
            @NotNull String groupName,
            @Nullable Group group,
            boolean primary,
            @Nullable Instant expiration,
            @Nullable String world,
            @Nullable String server,
            @Nullable Instant assignedAt,
            @Nullable String assignedBy
    ) {
        this.playerId = playerId;
        this.groupName = groupName;
        this.group = group;
        this.primary = primary;
        this.expiration = expiration;
        this.world = world;
        this.server = server;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    /**
     * Creates a permanent group membership.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @return the membership
     * @since 1.0.0
     */
    @NotNull
    public static GroupMembership of(@NotNull UUID playerId, @NotNull String groupName) {
        return new GroupMembership(playerId, groupName, null, false, null, null, null, Instant.now(), null);
    }

    /**
     * Creates a group membership with a group reference.
     *
     * @param playerId the player's UUID
     * @param group    the group
     * @return the membership
     * @since 1.0.0
     */
    @NotNull
    public static GroupMembership of(@NotNull UUID playerId, @NotNull Group group) {
        return new GroupMembership(playerId, group.getName(), group, false, null, null, null, Instant.now(), null);
    }

    /**
     * Creates a builder for constructing memberships.
     *
     * @param playerId  the player's UUID
     * @param groupName the group name
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull UUID playerId, @NotNull String groupName) {
        return new Builder(playerId, groupName);
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player's UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the group name.
     *
     * @return the group name
     * @since 1.0.0
     */
    @NotNull
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the group if available.
     *
     * @return an Optional containing the group
     * @since 1.0.0
     */
    @NotNull
    public Optional<Group> getGroup() {
        return Optional.ofNullable(group);
    }

    /**
     * Checks if this is the player's primary group.
     *
     * @return true if primary
     * @since 1.0.0
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Checks if this membership is temporary.
     *
     * @return true if temporary
     * @since 1.0.0
     */
    public boolean isTemporary() {
        return expiration != null;
    }

    /**
     * Checks if this membership is permanent.
     *
     * @return true if permanent
     * @since 1.0.0
     */
    public boolean isPermanent() {
        return expiration == null;
    }

    /**
     * Returns the expiration time for temporary memberships.
     *
     * @return an Optional containing the expiration
     * @since 1.0.0
     */
    @NotNull
    public Optional<Instant> getExpiration() {
        return Optional.ofNullable(expiration);
    }

    /**
     * Checks if this membership has expired.
     *
     * @return true if expired
     * @since 1.0.0
     */
    public boolean hasExpired() {
        return expiration != null && Instant.now().isAfter(expiration);
    }

    /**
     * Returns the remaining duration until expiration.
     *
     * @return an Optional containing the remaining duration
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
     * Returns the world context.
     *
     * @return an Optional containing the world
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getWorld() {
        return Optional.ofNullable(world);
    }

    /**
     * Returns the server context.
     *
     * @return an Optional containing the server
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getServer() {
        return Optional.ofNullable(server);
    }

    /**
     * Checks if this membership has context restrictions.
     *
     * @return true if world or server context is set
     * @since 1.0.0
     */
    public boolean hasContext() {
        return world != null || server != null;
    }

    /**
     * Checks if this membership is global (no context).
     *
     * @return true if global
     * @since 1.0.0
     */
    public boolean isGlobal() {
        return world == null && server == null;
    }

    /**
     * Returns the context for this membership.
     *
     * @return the permission context
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext getContext() {
        if (world == null && server == null) {
            return PermissionContext.global();
        }
        PermissionContext.Builder builder = PermissionContext.builder();
        if (world != null) {
            builder.world(world);
        }
        if (server != null) {
            builder.server(server);
        }
        return builder.build();
    }

    /**
     * Checks if this membership applies in the given context.
     *
     * @param context the context to check
     * @return true if this membership applies
     * @since 1.0.0
     */
    public boolean appliesIn(@NotNull PermissionContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        if (isGlobal()) {
            return true;
        }

        if (world != null) {
            Optional<String> ctxWorld = context.getWorld();
            if (ctxWorld.isEmpty() || !world.equals(ctxWorld.get())) {
                return false;
            }
        }

        if (server != null) {
            Optional<String> ctxServer = context.getServer();
            if (ctxServer.isEmpty() || !server.equals(ctxServer.get())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns when this membership was assigned.
     *
     * @return an Optional containing the assignment time
     * @since 1.0.0
     */
    @NotNull
    public Optional<Instant> getAssignedAt() {
        return Optional.ofNullable(assignedAt);
    }

    /**
     * Returns who assigned this membership.
     *
     * @return an Optional containing the assigner
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getAssignedBy() {
        return Optional.ofNullable(assignedBy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMembership that = (GroupMembership) o;
        return Objects.equals(playerId, that.playerId)
                && Objects.equals(groupName, that.groupName)
                && Objects.equals(world, that.world)
                && Objects.equals(server, that.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, groupName, world, server);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GroupMembership{");
        sb.append("player=").append(playerId);
        sb.append(", group='").append(groupName).append("'");
        if (primary) {
            sb.append(", primary");
        }
        if (expiration != null) {
            sb.append(", expires=").append(expiration);
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
     * Builder for constructing GroupMembership instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final UUID playerId;
        private final String groupName;
        private Group group;
        private boolean primary;
        private Instant expiration;
        private String world;
        private String server;
        private Instant assignedAt;
        private String assignedBy;

        private Builder(@NotNull UUID playerId, @NotNull String groupName) {
            this.playerId = Objects.requireNonNull(playerId);
            this.groupName = Objects.requireNonNull(groupName);
            this.assignedAt = Instant.now();
        }

        /**
         * Sets the group reference.
         *
         * @param group the group
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder group(@Nullable Group group) {
            this.group = group;
            return this;
        }

        /**
         * Sets whether this is the primary group.
         *
         * @param primary true for primary
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder primary(boolean primary) {
            this.primary = primary;
            return this;
        }

        /**
         * Marks this as the primary group.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder primary() {
            return primary(true);
        }

        /**
         * Sets the expiration time.
         *
         * @param expiration the expiration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder expiration(@Nullable Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * Sets the expiration as a duration from now.
         *
         * @param duration the duration
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder expiresIn(@NotNull Duration duration) {
            this.expiration = Instant.now().plus(duration);
            return this;
        }

        /**
         * Sets the world context.
         *
         * @param world the world
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
         * @param server the server
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder server(@Nullable String server) {
            this.server = server;
            return this;
        }

        /**
         * Sets the context.
         *
         * @param context the context
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder context(@NotNull PermissionContext context) {
            this.world = context.getWorld().orElse(null);
            this.server = context.getServer().orElse(null);
            return this;
        }

        /**
         * Sets when this membership was assigned.
         *
         * @param assignedAt the assignment time
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder assignedAt(@Nullable Instant assignedAt) {
            this.assignedAt = assignedAt;
            return this;
        }

        /**
         * Sets who assigned this membership.
         *
         * @param assignedBy the assigner
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder assignedBy(@Nullable String assignedBy) {
            this.assignedBy = assignedBy;
            return this;
        }

        /**
         * Builds the GroupMembership.
         *
         * @return the constructed membership
         * @since 1.0.0
         */
        @NotNull
        public GroupMembership build() {
            return new GroupMembership(playerId, groupName, group, primary, expiration,
                    world, server, assignedAt, assignedBy);
        }
    }
}
