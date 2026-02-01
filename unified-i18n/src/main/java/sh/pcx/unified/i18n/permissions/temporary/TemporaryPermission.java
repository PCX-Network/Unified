/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.temporary;

import org.jetbrains.annotations.NotNull;
import sh.pcx.unified.i18n.permissions.core.Permission;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents a temporary permission that expires after a duration.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface TemporaryPermission {

    /**
     * Returns the underlying permission.
     *
     * @return the permission
     * @since 1.0.0
     */
    @NotNull
    Permission getPermission();

    /**
     * Returns when this permission expires.
     *
     * @return the expiration instant
     * @since 1.0.0
     */
    @NotNull
    Instant getExpiration();

    /**
     * Returns the remaining duration until expiration.
     *
     * @return the remaining duration, or empty if already expired
     * @since 1.0.0
     */
    @NotNull
    Optional<Duration> getRemainingDuration();

    /**
     * Checks if this permission has expired.
     *
     * @return true if expired
     * @since 1.0.0
     */
    boolean isExpired();
}
