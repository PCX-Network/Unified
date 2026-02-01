/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;
import sh.pcx.unified.player.UnifiedPlayer;

/**
 * Event for resource pack responses.
 *
 * @since 1.0.0
 */
public interface ResourcePackResponseEvent {

    /**
     * Returns the player.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    UnifiedPlayer getPlayer();

    /**
     * Returns the response status.
     *
     * @return the status
     * @since 1.0.0
     */
    @NotNull
    ResourcePackStatus getStatus();
}
