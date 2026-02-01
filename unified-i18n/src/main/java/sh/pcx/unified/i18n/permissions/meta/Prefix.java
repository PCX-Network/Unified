/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.meta;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a player's prefix for chat formatting.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface Prefix {

    /**
     * Returns the prefix value.
     *
     * @return the prefix string
     * @since 1.0.0
     */
    @NotNull
    String getValue();

    /**
     * Returns the priority of this prefix.
     *
     * <p>Higher priority prefixes take precedence when multiple are set.
     *
     * @return the priority
     * @since 1.0.0
     */
    int getPriority();
}
