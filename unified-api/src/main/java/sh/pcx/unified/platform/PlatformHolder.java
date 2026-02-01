/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform;

import org.jetbrains.annotations.NotNull;

/**
 * Internal holder for the platform instance.
 */
public final class PlatformHolder {
    static volatile Platform INSTANCE;

    private PlatformHolder() {}

    /**
     * Sets the platform instance. Called by the platform implementation.
     *
     * @param platform the platform instance
     */
    public static void set(@NotNull Platform platform) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Platform has already been initialized");
        }
        INSTANCE = platform;
    }

    /**
     * Clears the platform instance. Called during shutdown.
     */
    public static void clear() {
        INSTANCE = null;
    }
}
