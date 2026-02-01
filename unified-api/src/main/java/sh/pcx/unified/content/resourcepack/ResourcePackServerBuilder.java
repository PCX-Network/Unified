/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;

/**
 * Builder for hosting resource packs.
 *
 * @since 1.0.0
 */
public interface ResourcePackServerBuilder {

    /**
     * Sets the server port.
     *
     * @param port the HTTP port
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackServerBuilder port(int port);

    /**
     * Sets the URL path.
     *
     * @param path the URL path (e.g., "/resources")
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackServerBuilder path(@NotNull String path);

    /**
     * Sets a custom hostname.
     *
     * @param hostname the hostname
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ResourcePackServerBuilder hostname(@NotNull String hostname);

    /**
     * Starts the HTTP server.
     *
     * @return the running server
     * @since 1.0.0
     */
    @NotNull
    ResourcePackServer start();
}
