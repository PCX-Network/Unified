/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

/**
 * Resource pack response statuses.
 *
 * @since 1.0.0
 */
public enum ResourcePackStatus {
    ACCEPTED,
    DECLINED,
    FAILED_DOWNLOAD,
    SUCCESSFULLY_LOADED,
    DISCARDED,
    INVALID_URL,
    FAILED_RELOAD,
    DOWNLOADED
}
