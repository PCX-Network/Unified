/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.audit;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of audit action types.
 *
 * <p>AuditAction defines the type of operation that was performed on a resource,
 * enabling categorization and filtering of audit log entries.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Record a create action
 * AuditEntry entry = AuditEntry.builder()
 *     .action(AuditAction.CREATE)
 *     .target("Player", playerId.toString())
 *     .build();
 *
 * // Query by action type
 * AuditResult result = auditQuery.filter()
 *     .action(AuditAction.DELETE)
 *     .execute();
 *
 * // Check if action modifies data
 * if (action.isModifying()) {
 *     // Requires additional permissions
 * }
 * }</pre>
 *
 * <h2>Action Categories</h2>
 * <ul>
 *   <li>{@link #CREATE} - New resource creation</li>
 *   <li>{@link #READ} - Resource access without modification</li>
 *   <li>{@link #UPDATE} - Modification of existing resource</li>
 *   <li>{@link #DELETE} - Resource removal</li>
 *   <li>{@link #ACCESS} - General access or login event</li>
 *   <li>{@link #EXPORT} - Data export operation</li>
 *   <li>{@link #IMPORT} - Data import operation</li>
 *   <li>{@link #EXECUTE} - Command or action execution</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AuditEntry
 * @see AuditFilter
 */
public enum AuditAction {

    /**
     * Creation of a new resource.
     *
     * <p>Used when a new entity, record, or resource is created. This action
     * typically captures the initial state of the created resource.
     */
    CREATE("create", "Created", true),

    /**
     * Reading or viewing a resource.
     *
     * <p>Used when a resource is accessed for viewing without modification.
     * This is useful for tracking access to sensitive data.
     */
    READ("read", "Read", false),

    /**
     * Modification of an existing resource.
     *
     * <p>Used when an existing entity, record, or resource is modified.
     * This action typically captures both before and after states.
     */
    UPDATE("update", "Updated", true),

    /**
     * Removal of a resource.
     *
     * <p>Used when an entity, record, or resource is deleted. This action
     * typically captures the final state before deletion.
     */
    DELETE("delete", "Deleted", true),

    /**
     * General access event.
     *
     * <p>Used for login, logout, or general access tracking that doesn't
     * fit into the CRUD categories.
     */
    ACCESS("access", "Accessed", false),

    /**
     * Data export operation.
     *
     * <p>Used when data is exported from the system. This tracks what
     * data was exported and by whom.
     */
    EXPORT("export", "Exported", false),

    /**
     * Data import operation.
     *
     * <p>Used when data is imported into the system. This tracks what
     * data was imported and by whom.
     */
    IMPORT("import", "Imported", true),

    /**
     * Command or action execution.
     *
     * <p>Used for tracking execution of commands, scripts, or other
     * actions that don't fit into other categories.
     */
    EXECUTE("execute", "Executed", false);

    private final String identifier;
    private final String pastTense;
    private final boolean modifying;

    /**
     * Creates a new audit action.
     *
     * @param identifier the unique identifier for this action
     * @param pastTense  the past tense form for display
     * @param modifying  whether this action modifies data
     */
    AuditAction(@NotNull String identifier, @NotNull String pastTense, boolean modifying) {
        this.identifier = identifier;
        this.pastTense = pastTense;
        this.modifying = modifying;
    }

    /**
     * Returns the unique identifier for this action.
     *
     * <p>The identifier is a lowercase string suitable for storage and filtering.
     *
     * @return the action identifier
     * @since 1.0.0
     */
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the past tense form of this action.
     *
     * <p>Useful for generating human-readable audit messages like
     * "User X Created resource Y".
     *
     * @return the past tense form
     * @since 1.0.0
     */
    @NotNull
    public String getPastTense() {
        return pastTense;
    }

    /**
     * Checks if this action modifies data.
     *
     * <p>Modifying actions include CREATE, UPDATE, DELETE, and IMPORT.
     * Non-modifying actions include READ, ACCESS, EXPORT, and EXECUTE.
     *
     * @return true if this action modifies data
     * @since 1.0.0
     */
    public boolean isModifying() {
        return modifying;
    }

    /**
     * Checks if this action creates data.
     *
     * @return true if this is a CREATE or IMPORT action
     * @since 1.0.0
     */
    public boolean isCreating() {
        return this == CREATE || this == IMPORT;
    }

    /**
     * Checks if this action removes data.
     *
     * @return true if this is a DELETE action
     * @since 1.0.0
     */
    public boolean isDeleting() {
        return this == DELETE;
    }

    /**
     * Checks if this action is a read-only operation.
     *
     * @return true if this action does not modify data
     * @since 1.0.0
     */
    public boolean isReadOnly() {
        return !modifying;
    }

    /**
     * Parses an audit action from a string identifier.
     *
     * <p>The parsing is case-insensitive.
     *
     * @param identifier the identifier to parse
     * @return the corresponding audit action
     * @throws IllegalArgumentException if the identifier is not recognized
     * @since 1.0.0
     */
    @NotNull
    public static AuditAction fromIdentifier(@NotNull String identifier) {
        String lower = identifier.toLowerCase().trim();
        for (AuditAction action : values()) {
            if (action.identifier.equals(lower) || action.name().equalsIgnoreCase(lower)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown audit action: " + identifier);
    }

    /**
     * Generates a formatted audit message.
     *
     * <p>Creates a message in the format "{actor} {pastTense} {targetType} {targetId}".
     *
     * @param actor      the actor who performed the action
     * @param targetType the type of target
     * @param targetId   the target identifier
     * @return the formatted message
     * @since 1.0.0
     */
    @NotNull
    public String formatMessage(@NotNull String actor, @NotNull String targetType,
                                @NotNull String targetId) {
        return String.format("%s %s %s %s", actor, pastTense, targetType, targetId);
    }

    @Override
    public String toString() {
        return identifier;
    }
}
