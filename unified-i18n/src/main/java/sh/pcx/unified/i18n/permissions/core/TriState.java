/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.core;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a tri-state boolean value for permission checks.
 *
 * <p>Unlike a regular boolean, TriState can represent three distinct states:
 * <ul>
 *   <li>{@link #TRUE} - The permission is explicitly granted</li>
 *   <li>{@link #FALSE} - The permission is explicitly denied</li>
 *   <li>{@link #UNDEFINED} - The permission has no explicit value set</li>
 * </ul>
 *
 * <p>This is particularly useful in permission systems where the absence of a
 * permission node should be distinguishable from an explicit denial.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PermissionCheck result = permissionChecker.check(player, "myplugin.admin");
 * TriState state = result.getValue();
 *
 * switch (state) {
 *     case TRUE:
 *         // Permission explicitly granted
 *         break;
 *     case FALSE:
 *         // Permission explicitly denied
 *         break;
 *     case UNDEFINED:
 *         // Check parent permission or use default
 *         break;
 * }
 *
 * // Convert to boolean with default
 * boolean allowed = state.asBoolean(false);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public enum TriState {

    /**
     * The permission is explicitly granted.
     *
     * <p>When converted to boolean, this always results in {@code true}.
     */
    TRUE(true),

    /**
     * The permission is explicitly denied.
     *
     * <p>When converted to boolean, this always results in {@code false}.
     */
    FALSE(false),

    /**
     * The permission has no explicit value.
     *
     * <p>When converted to boolean, this uses the provided default value.
     */
    UNDEFINED(null);

    private final Boolean booleanValue;

    /**
     * Constructs a TriState with the associated boolean value.
     *
     * @param booleanValue the boolean value, or null for UNDEFINED
     */
    TriState(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    /**
     * Creates a TriState from a boolean value.
     *
     * @param value the boolean value
     * @return {@link #TRUE} if value is true, {@link #FALSE} otherwise
     * @since 1.0.0
     */
    @NotNull
    public static TriState of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Creates a TriState from a nullable Boolean.
     *
     * @param value the boolean value, or null for UNDEFINED
     * @return the corresponding TriState
     * @since 1.0.0
     */
    @NotNull
    public static TriState fromNullable(Boolean value) {
        if (value == null) {
            return UNDEFINED;
        }
        return value ? TRUE : FALSE;
    }

    /**
     * Converts this TriState to a boolean, using a default value if UNDEFINED.
     *
     * @param defaultValue the value to use if this is {@link #UNDEFINED}
     * @return the boolean value
     * @since 1.0.0
     */
    public boolean asBoolean(boolean defaultValue) {
        if (this == UNDEFINED) {
            return defaultValue;
        }
        return this == TRUE;
    }

    /**
     * Converts this TriState to a nullable Boolean.
     *
     * @return {@code true} for TRUE, {@code false} for FALSE, {@code null} for UNDEFINED
     * @since 1.0.0
     */
    public Boolean asNullableBoolean() {
        return booleanValue;
    }

    /**
     * Checks if this TriState is defined (not UNDEFINED).
     *
     * @return true if this is TRUE or FALSE
     * @since 1.0.0
     */
    public boolean isDefined() {
        return this != UNDEFINED;
    }

    /**
     * Returns the negation of this TriState.
     *
     * <p>TRUE becomes FALSE, FALSE becomes TRUE, and UNDEFINED remains UNDEFINED.
     *
     * @return the negated TriState
     * @since 1.0.0
     */
    @NotNull
    public TriState negate() {
        switch (this) {
            case TRUE:
                return FALSE;
            case FALSE:
                return TRUE;
            default:
                return UNDEFINED;
        }
    }

    /**
     * Performs a logical AND with another TriState.
     *
     * <p>Returns TRUE only if both values are TRUE. Returns FALSE if either is FALSE.
     * Returns UNDEFINED only if both are UNDEFINED or one is UNDEFINED and the other is TRUE.
     *
     * @param other the other TriState
     * @return the result of the AND operation
     * @since 1.0.0
     */
    @NotNull
    public TriState and(@NotNull TriState other) {
        if (this == FALSE || other == FALSE) {
            return FALSE;
        }
        if (this == TRUE && other == TRUE) {
            return TRUE;
        }
        return UNDEFINED;
    }

    /**
     * Performs a logical OR with another TriState.
     *
     * <p>Returns TRUE if either value is TRUE. Returns FALSE only if both are FALSE.
     * Returns UNDEFINED if one is UNDEFINED and the other is FALSE.
     *
     * @param other the other TriState
     * @return the result of the OR operation
     * @since 1.0.0
     */
    @NotNull
    public TriState or(@NotNull TriState other) {
        if (this == TRUE || other == TRUE) {
            return TRUE;
        }
        if (this == FALSE && other == FALSE) {
            return FALSE;
        }
        return UNDEFINED;
    }
}
