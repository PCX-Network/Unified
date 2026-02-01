/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Cross-server messaging API interfaces.
 *
 * <p>This package contains the service interfaces for cross-server messaging.
 * Implementations are provided by the unified-network module.
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.messaging.MessagingService} - Main service for cross-server messaging</li>
 *   <li>{@link sh.pcx.unified.messaging.MessageChannel} - Channel abstraction for message routing</li>
 *   <li>{@link sh.pcx.unified.messaging.Message} - Annotation for defining typed messages</li>
 * </ul>
 *
 * @since 1.0.0
 */
@org.jetbrains.annotations.ApiStatus.Experimental
package sh.pcx.unified.messaging;
