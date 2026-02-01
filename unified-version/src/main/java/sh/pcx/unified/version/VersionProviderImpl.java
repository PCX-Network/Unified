/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version;

import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.version.api.VersionProvider;
import sh.pcx.unified.version.detection.MinecraftVersionDetector;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of {@link VersionProvider}.
 *
 * <p>This implementation uses {@link MinecraftVersionDetector} for version detection
 * and caches results for performance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * VersionProvider version = new VersionProviderImpl();
 *
 * // Get current version
 * MinecraftVersion current = version.current();
 *
 * // Check capabilities
 * if (version.supports(Feature.MOJANG_MAPPINGS)) {
 *     // Using Paper 1.21.11+ with Mojang mappings
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class VersionProviderImpl implements VersionProvider {

    private final MinecraftVersionDetector detector;
    private volatile MinecraftVersion cachedVersion;

    /**
     * Creates a new version provider.
     */
    public VersionProviderImpl() {
        this.detector = MinecraftVersionDetector.getInstance();
    }

    /**
     * Creates a version provider with a custom detector.
     *
     * @param detector the version detector to use
     */
    public VersionProviderImpl(@NotNull MinecraftVersionDetector detector) {
        this.detector = detector;
    }

    @Override
    @NotNull
    public MinecraftVersion current() {
        if (cachedVersion == null) {
            synchronized (this) {
                if (cachedVersion == null) {
                    cachedVersion = detector.detect();
                }
            }
        }
        return cachedVersion;
    }

    @Override
    public boolean usesMojangMappings() {
        return detector.usesMojangMappings();
    }

    @Override
    @NotNull
    public String getPlatform() {
        return detector.getPlatform();
    }

    @Override
    @NotNull
    public String getServerVersion() {
        return detector.getServerVersion();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Clears the cached version.
     *
     * <p>This is mainly useful for testing.
     */
    public void clearCache() {
        synchronized (this) {
            cachedVersion = null;
        }
        detector.clearCache();
    }

    @Override
    public String toString() {
        return "VersionProviderImpl{" +
                "version=" + current() +
                ", platform=" + getPlatform() +
                ", mojangMappings=" + usesMojangMappings() +
                '}';
    }
}
