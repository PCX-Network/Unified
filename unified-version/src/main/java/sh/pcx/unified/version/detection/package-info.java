/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Version detection and registry services.
 *
 * <p>This package provides runtime version detection and service registry
 * for version-specific implementations.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.version.detection.MinecraftVersionDetector} - Detects server version</li>
 *   <li>{@link sh.pcx.unified.version.detection.VersionRegistry} - Manages version-specific implementations</li>
 *   <li>{@link sh.pcx.unified.version.detection.VersionConstants} - Version constant definitions</li>
 * </ul>
 *
 * <h2>Detection Process</h2>
 * <ol>
 *   <li>Server version is detected at plugin enable time</li>
 *   <li>Appropriate implementation classes are loaded from version packages</li>
 *   <li>Services are cached for performance</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@NullMarked
package sh.pcx.unified.version.detection;

import org.jspecify.annotations.NullMarked;
