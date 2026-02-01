/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Context information about the actor performing an auditable action.
 *
 * <p>AuditContext provides a convenient way to pass actor information
 * to audit entry builders without specifying each field separately.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create context for a player
 * AuditContext playerContext = AuditContext.player(player.getUniqueId(), player.getName());
 *
 * // Create context for console
 * AuditContext consoleContext = AuditContext.console();
 *
 * // Create context for system
 * AuditContext systemContext = AuditContext.system("BackupService");
 *
 * // Use in audit entry
 * AuditEntry entry = AuditEntry.builder()
 *     .actor(playerContext)
 *     .action(AuditAction.UPDATE)
 *     .target("Economy", playerId.toString())
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This record is immutable and therefore thread-safe.
 *
 * @param actorId   the actor's unique identifier (null for console/system)
 * @param actorName the actor's display name
 * @param actorType the type of actor
 * @since 1.0.0
 * @author Supatuck
 * @see AuditEntry
 */
public record AuditContext(
        @Nullable UUID actorId,
        @NotNull String actorName,
        @NotNull AuditEntry.ActorType actorType
) {

    /**
     * Validates the context.
     */
    public AuditContext {
        Objects.requireNonNull(actorName, "actorName cannot be null");
        Objects.requireNonNull(actorType, "actorType cannot be null");
        if (actorName.isBlank()) {
            throw new IllegalArgumentException("actorName cannot be blank");
        }
    }

    /**
     * Creates an audit context for a player.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     * @return a new audit context
     * @since 1.0.0
     */
    @NotNull
    public static AuditContext player(@NotNull UUID playerId, @NotNull String playerName) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");
        return new AuditContext(playerId, playerName, AuditEntry.ActorType.PLAYER);
    }

    /**
     * Creates an audit context for the console.
     *
     * @return a new audit context for console
     * @since 1.0.0
     */
    @NotNull
    public static AuditContext console() {
        return new AuditContext(null, "Console", AuditEntry.ActorType.CONSOLE);
    }

    /**
     * Creates an audit context for a system component.
     *
     * @param systemName the name of the system component
     * @return a new audit context
     * @since 1.0.0
     */
    @NotNull
    public static AuditContext system(@NotNull String systemName) {
        Objects.requireNonNull(systemName, "systemName cannot be null");
        return new AuditContext(null, systemName, AuditEntry.ActorType.SYSTEM);
    }

    /**
     * Creates an audit context for a plugin.
     *
     * @param pluginName the name of the plugin
     * @return a new audit context
     * @since 1.0.0
     */
    @NotNull
    public static AuditContext plugin(@NotNull String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName cannot be null");
        return new AuditContext(null, pluginName, AuditEntry.ActorType.PLUGIN);
    }

    /**
     * Checks if this context represents a player.
     *
     * @return true if the actor is a player
     * @since 1.0.0
     */
    public boolean isPlayer() {
        return actorType == AuditEntry.ActorType.PLAYER;
    }

    /**
     * Checks if this context represents the console.
     *
     * @return true if the actor is the console
     * @since 1.0.0
     */
    public boolean isConsole() {
        return actorType == AuditEntry.ActorType.CONSOLE;
    }

    /**
     * Checks if this context represents a system component.
     *
     * @return true if the actor is a system component
     * @since 1.0.0
     */
    public boolean isSystem() {
        return actorType == AuditEntry.ActorType.SYSTEM;
    }

    /**
     * Checks if this context represents a plugin.
     *
     * @return true if the actor is a plugin
     * @since 1.0.0
     */
    public boolean isPlugin() {
        return actorType == AuditEntry.ActorType.PLUGIN;
    }
}
