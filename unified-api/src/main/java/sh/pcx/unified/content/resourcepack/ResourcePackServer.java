/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;

/**
 * Running resource pack HTTP server.
 *
 * @since 1.0.0
 */
public interface ResourcePackServer {

    /**
     * Returns the pack URL.
     *
     * @return the full URL to the pack
     * @since 1.0.0
     */
    @NotNull
    String getUrl();

    /**
     * Returns the pack SHA-1 hash.
     *
     * @return the hash string
     * @since 1.0.0
     */
    @NotNull
    String getHash();

    /**
     * Returns the server port.
     *
     * @return the HTTP port
     * @since 1.0.0
     */
    int getPort();

    /**
     * Stops the server.
     *
     * @since 1.0.0
     */
    void stop();

    /**
     * Checks if the server is running.
     *
     * @return true if running
     * @since 1.0.0
     */
    boolean isRunning();
}
