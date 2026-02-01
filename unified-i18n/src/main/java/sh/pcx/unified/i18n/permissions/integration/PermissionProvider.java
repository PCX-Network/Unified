/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.integration;

/**
 * Interface for permission backend providers.
 *
 * <p>This interface abstracts the underlying permission system (e.g., LuckPerms, Vault)
 * to provide a unified way to interact with different backends.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface PermissionProvider {

    /**
     * Returns the name of this permission provider.
     *
     * @return the provider name (e.g., "LuckPerms", "Vault")
     * @since 1.0.0
     */
    String getName();

    /**
     * Checks if this provider is available and ready to use.
     *
     * @return true if the provider is available
     * @since 1.0.0
     */
    boolean isAvailable();
}
