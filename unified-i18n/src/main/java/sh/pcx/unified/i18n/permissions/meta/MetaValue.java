/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.permissions.meta;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a metadata value.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface MetaValue {

    /**
     * Returns the key of this metadata.
     *
     * @return the metadata key
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Returns the value of this metadata.
     *
     * @return the metadata value
     * @since 1.0.0
     */
    @NotNull
    String getValue();
}
