/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.check;

import sh.pcx.unified.i18n.permissions.core.Permission;
import sh.pcx.unified.i18n.permissions.core.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the result of a permission check.
 *
 * <p>PermissionCheck provides detailed information about how a permission
 * check was resolved, including:
 * <ul>
 *   <li>The resulting TriState value</li>
 *   <li>The source of the permission (direct, group, wildcard)</li>
 *   <li>The actual permission node that was matched</li>
 *   <li>When the check was performed</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Perform a check
 * PermissionCheck check = permService.checkPermission(playerId, "myplugin.admin");
 *
 * // Get the result
 * if (check.isAllowed()) {
 *     // Permission granted
 * } else if (check.isDenied()) {
 *     // Permission explicitly denied
 * } else {
 *     // Permission undefined - use default
 * }
 *
 * // Get detailed information
 * String source = check.getSource().orElse("unknown");
 * String matchedNode = check.getMatchedNode().orElse(check.getNode());
 *
 * // Log the check
 * logger.debug("Permission {} for player {} resolved to {} via {}",
 *     check.getNode(), check.getPlayerId(), check.getValue(), source);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PermissionChecker
 * @see TriState
 */
public final class PermissionCheck {

    /**
     * Source type for permission checks.
     */
    public enum Source {
        /**
         * Permission was set directly on the player.
         */
        DIRECT,

        /**
         * Permission came from a group membership.
         */
        GROUP,

        /**
         * Permission matched via a wildcard node.
         */
        WILDCARD,

        /**
         * Permission came from operator status.
         */
        OPERATOR,

        /**
         * Permission was inherited from a parent permission.
         */
        INHERITED,

        /**
         * Permission came from a default value.
         */
        DEFAULT,

        /**
         * Permission source is unknown.
         */
        UNKNOWN
    }

    private final UUID playerId;
    private final String node;
    private final TriState value;
    private final PermissionContext context;
    private final Source sourceType;
    private final String sourceName;
    private final String matchedNode;
    private final Instant checkedAt;

    /**
     * Creates a new PermissionCheck.
     *
     * @param playerId    the player's UUID
     * @param node        the permission node that was checked
     * @param value       the resulting value
     * @param context     the context used for the check
     * @param sourceType  the type of source
     * @param sourceName  the name of the source (e.g., group name)
     * @param matchedNode the actual node that was matched
     */
    private PermissionCheck(
            @NotNull UUID playerId,
            @NotNull String node,
            @NotNull TriState value,
            @NotNull PermissionContext context,
            @NotNull Source sourceType,
            @Nullable String sourceName,
            @Nullable String matchedNode
    ) {
        this.playerId = playerId;
        this.node = node;
        this.value = value;
        this.context = context;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.matchedNode = matchedNode;
        this.checkedAt = Instant.now();
    }

    /**
     * Creates a new PermissionCheck with the result.
     *
     * @param playerId the player's UUID
     * @param node     the permission node
     * @param value    the resulting value
     * @return the permission check
     * @since 1.0.0
     */
    @NotNull
    public static PermissionCheck of(
            @NotNull UUID playerId,
            @NotNull String node,
            @NotNull TriState value
    ) {
        return new PermissionCheck(playerId, node, value, PermissionContext.global(),
                Source.UNKNOWN, null, null);
    }

    /**
     * Creates a new PermissionCheck with context.
     *
     * @param playerId the player's UUID
     * @param node     the permission node
     * @param value    the resulting value
     * @param context  the context
     * @return the permission check
     * @since 1.0.0
     */
    @NotNull
    public static PermissionCheck of(
            @NotNull UUID playerId,
            @NotNull String node,
            @NotNull TriState value,
            @NotNull PermissionContext context
    ) {
        return new PermissionCheck(playerId, node, value, context, Source.UNKNOWN, null, null);
    }

    /**
     * Creates a builder for constructing PermissionCheck instances.
     *
     * @param playerId the player's UUID
     * @param node     the permission node
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull UUID playerId, @NotNull String node) {
        return new Builder(playerId, node);
    }

    /**
     * Creates an allowed result.
     *
     * @param playerId the player's UUID
     * @param node     the permission node
     * @return an allowed permission check
     * @since 1.0.0
     */
    @NotNull
    public static PermissionCheck allowed(@NotNull UUID playerId, @NotNull String node) {
        return of(playerId, node, TriState.TRUE);
    }

    /**
     * Creates a denied result.
     *
     * @param playerId the player's UUID
     * @param node     the permission node
     * @return a denied permission check
     * @since 1.0.0
     */
    @NotNull
    public static PermissionCheck denied(@NotNull UUID playerId, @NotNull String node) {
        return of(playerId, node, TriState.FALSE);
    }

    /**
     * Creates an undefined result.
     *
     * @param playerId the player's UUID
     * @param node     the permission node
     * @return an undefined permission check
     * @since 1.0.0
     */
    @NotNull
    public static PermissionCheck undefined(@NotNull UUID playerId, @NotNull String node) {
        return of(playerId, node, TriState.UNDEFINED);
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
     * Returns the permission node that was checked.
     *
     * @return the permission node
     * @since 1.0.0
     */
    @NotNull
    public String getNode() {
        return node;
    }

    /**
     * Returns the result value.
     *
     * @return the TriState result
     * @since 1.0.0
     */
    @NotNull
    public TriState getValue() {
        return value;
    }

    /**
     * Returns the result as a boolean with a default value.
     *
     * @param defaultValue the value to use if undefined
     * @return the boolean result
     * @since 1.0.0
     */
    public boolean asBoolean(boolean defaultValue) {
        return value.asBoolean(defaultValue);
    }

    /**
     * Checks if the permission is allowed (TRUE).
     *
     * @return true if the permission is granted
     * @since 1.0.0
     */
    public boolean isAllowed() {
        return value == TriState.TRUE;
    }

    /**
     * Checks if the permission is denied (FALSE).
     *
     * @return true if the permission is explicitly denied
     * @since 1.0.0
     */
    public boolean isDenied() {
        return value == TriState.FALSE;
    }

    /**
     * Checks if the permission is undefined.
     *
     * @return true if the permission has no explicit value
     * @since 1.0.0
     */
    public boolean isUndefined() {
        return value == TriState.UNDEFINED;
    }

    /**
     * Returns the context used for the check.
     *
     * @return the permission context
     * @since 1.0.0
     */
    @NotNull
    public PermissionContext getContext() {
        return context;
    }

    /**
     * Returns the source type.
     *
     * @return the source type
     * @since 1.0.0
     */
    @NotNull
    public Source getSourceType() {
        return sourceType;
    }

    /**
     * Returns the source name if available.
     *
     * <p>For GROUP sources, this is the group name.
     *
     * @return an Optional containing the source name
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getSource() {
        return Optional.ofNullable(sourceName);
    }

    /**
     * Returns the actual node that matched.
     *
     * <p>This may differ from {@link #getNode()} if a wildcard matched.
     * For example, if checking "myplugin.admin.teleport" and it matched
     * via "myplugin.admin.*", this returns "myplugin.admin.*".
     *
     * @return an Optional containing the matched node
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getMatchedNode() {
        return Optional.ofNullable(matchedNode);
    }

    /**
     * Returns the effective matched node.
     *
     * <p>Returns the matched node if set, otherwise the original node.
     *
     * @return the effective node
     * @since 1.0.0
     */
    @NotNull
    public String getEffectiveNode() {
        return matchedNode != null ? matchedNode : node;
    }

    /**
     * Returns when this check was performed.
     *
     * @return the check timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getCheckedAt() {
        return checkedAt;
    }

    /**
     * Checks if this result came from a wildcard match.
     *
     * @return true if matched via wildcard
     * @since 1.0.0
     */
    public boolean isWildcardMatch() {
        return sourceType == Source.WILDCARD ||
                (matchedNode != null && matchedNode.endsWith(".*"));
    }

    /**
     * Checks if this result came from a group.
     *
     * @return true if from a group
     * @since 1.0.0
     */
    public boolean isFromGroup() {
        return sourceType == Source.GROUP;
    }

    /**
     * Checks if this result is from a direct assignment.
     *
     * @return true if directly assigned
     * @since 1.0.0
     */
    public boolean isDirect() {
        return sourceType == Source.DIRECT;
    }

    /**
     * Converts this check result to a Permission object.
     *
     * @return a Permission representing this check
     * @since 1.0.0
     */
    @NotNull
    public Permission toPermission() {
        return Permission.builder(node)
                .value(value)
                .world(context.getWorld().orElse(null))
                .server(context.getServer().orElse(null))
                .source(sourceName)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionCheck that = (PermissionCheck) o;
        return Objects.equals(playerId, that.playerId)
                && Objects.equals(node, that.node)
                && value == that.value
                && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, node, value, context);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PermissionCheck{");
        sb.append("player=").append(playerId);
        sb.append(", node='").append(node).append("'");
        sb.append(", value=").append(value);
        if (!context.isGlobal()) {
            sb.append(", context=").append(context);
        }
        if (sourceName != null) {
            sb.append(", source='").append(sourceName).append("'");
        }
        if (matchedNode != null && !matchedNode.equals(node)) {
            sb.append(", matchedNode='").append(matchedNode).append("'");
        }
        return sb.append("}").toString();
    }

    /**
     * Builder for constructing PermissionCheck instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final UUID playerId;
        private final String node;
        private TriState value = TriState.UNDEFINED;
        private PermissionContext context = PermissionContext.global();
        private Source sourceType = Source.UNKNOWN;
        private String sourceName;
        private String matchedNode;

        private Builder(@NotNull UUID playerId, @NotNull String node) {
            this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
            this.node = Objects.requireNonNull(node, "node cannot be null");
        }

        /**
         * Sets the result value.
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
         * Sets the result value.
         *
         * @param value true for allowed, false for denied
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder value(boolean value) {
            this.value = TriState.of(value);
            return this;
        }

        /**
         * Sets the result as allowed.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder allowed() {
            this.value = TriState.TRUE;
            return this;
        }

        /**
         * Sets the result as denied.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder denied() {
            this.value = TriState.FALSE;
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
            this.context = Objects.requireNonNull(context, "context cannot be null");
            return this;
        }

        /**
         * Sets the source type.
         *
         * @param sourceType the source type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder sourceType(@NotNull Source sourceType) {
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            return this;
        }

        /**
         * Sets the source name.
         *
         * @param sourceName the source name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder sourceName(@Nullable String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        /**
         * Sets both source type and name.
         *
         * @param sourceType the source type
         * @param sourceName the source name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder source(@NotNull Source sourceType, @Nullable String sourceName) {
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            this.sourceName = sourceName;
            return this;
        }

        /**
         * Sets the source as a group.
         *
         * @param groupName the group name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder fromGroup(@NotNull String groupName) {
            this.sourceType = Source.GROUP;
            this.sourceName = groupName;
            return this;
        }

        /**
         * Sets the source as direct.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder direct() {
            this.sourceType = Source.DIRECT;
            return this;
        }

        /**
         * Sets the source as wildcard.
         *
         * @param wildcardNode the wildcard node that matched
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder wildcard(@NotNull String wildcardNode) {
            this.sourceType = Source.WILDCARD;
            this.matchedNode = wildcardNode;
            return this;
        }

        /**
         * Sets the matched node.
         *
         * @param matchedNode the matched node
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder matchedNode(@Nullable String matchedNode) {
            this.matchedNode = matchedNode;
            return this;
        }

        /**
         * Builds the PermissionCheck.
         *
         * @return the constructed permission check
         * @since 1.0.0
         */
        @NotNull
        public PermissionCheck build() {
            return new PermissionCheck(playerId, node, value, context, sourceType, sourceName, matchedNode);
        }
    }
}
