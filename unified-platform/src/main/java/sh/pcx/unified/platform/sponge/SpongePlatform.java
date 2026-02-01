/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.platform.Platform;
import sh.pcx.unified.platform.PlatformType;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Sponge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sponge implementation of the {@link Platform} interface.
 *
 * <p>This class provides platform detection and information for Sponge-based servers.
 * Sponge is a modular API designed for modded Minecraft servers running on Forge or Fabric.
 *
 * <h2>Platform Detection</h2>
 * <p>The implementation retrieves version information from the Sponge platform API:
 * <ul>
 *   <li>Minecraft version from the Game component</li>
 *   <li>Sponge API version from the API component</li>
 *   <li>Implementation details from the Implementation component</li>
 * </ul>
 *
 * <h2>Sponge Specific Features</h2>
 * <ul>
 *   <li>Service-based architecture with ServiceProvider</li>
 *   <li>Cause tracking for events</li>
 *   <li>Full mod support (Forge/Fabric)</li>
 *   <li>Native Adventure component support</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable after construction and is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Platform
 * @see SpongePlatformProvider
 */
public final class SpongePlatform implements Platform {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(\\d+)\\.(\\d+)(?:\\.(\\d+))?"
    );

    private final MinecraftVersion minecraftVersion;
    private final String serverName;
    private final String serverVersion;
    private final String spongeApiVersion;
    private final boolean supportsAsyncChunks;

    /**
     * Creates a new SpongePlatform instance by detecting the current environment.
     *
     * <p>This constructor retrieves all necessary information from the Sponge Platform API
     * to determine version and capabilities.
     *
     * @since 1.0.0
     */
    public SpongePlatform() {
        org.spongepowered.api.Platform platform = Sponge.platform();

        // Get Minecraft version from the Game component
        String mcVersion = platform.container(Component.GAME).metadata().version().toString();
        this.minecraftVersion = parseMinecraftVersion(mcVersion);

        // Get server implementation name and version
        this.serverName = platform.container(Component.IMPLEMENTATION).metadata().name()
                .orElse("Sponge");
        this.serverVersion = platform.container(Component.IMPLEMENTATION).metadata().version().toString();

        // Get Sponge API version
        this.spongeApiVersion = platform.container(Component.API).metadata().version().toString();

        // Sponge supports async chunk operations
        this.supportsAsyncChunks = true;
    }

    /**
     * Parses the Minecraft version from the version string.
     *
     * @param versionString the version string from Sponge
     * @return the parsed MinecraftVersion
     */
    @NotNull
    private MinecraftVersion parseMinecraftVersion(@NotNull String versionString) {
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new MinecraftVersion(major, minor, patch);
        }
        // Default to minimum supported version if parsing fails
        return MinecraftVersion.MINIMUM_SUPPORTED;
    }

    /**
     * Returns the Sponge API version.
     *
     * @return the Sponge API version string
     * @since 1.0.0
     */
    @NotNull
    public String getSpongeApiVersion() {
        return spongeApiVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public PlatformType getType() {
        return PlatformType.SPONGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ServerType getServerType() {
        return ServerType.SPONGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public MinecraftVersion getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getServerName() {
        return serverName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code false} for Sponge as it is not Paper-based.
     */
    @Override
    public boolean isPaper() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code false} for Sponge as Folia is Bukkit-specific.
     */
    @Override
    public boolean isFolia() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code true} for Sponge platforms.
     */
    @Override
    public boolean isSponge() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sponge supports async chunk loading through its API.
     */
    @Override
    public boolean supportsAsyncChunks() {
        return supportsAsyncChunks;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sponge has native Adventure support through its API.
     */
    @Override
    public boolean supportsAdventure() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sponge supports plugin messaging through its channel API.
     */
    @Override
    public boolean supportsPluginMessaging() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty string for Sponge as NMS versioning is Bukkit-specific.
     * Sponge uses its own abstraction over Minecraft internals.
     */
    @Override
    @NotNull
    public String getNmsVersion() {
        // Sponge does not use NMS versioning - it has its own abstraction
        return "";
    }

    /**
     * Returns a string representation of this platform.
     *
     * @return a descriptive string including server name and versions
     */
    @Override
    public String toString() {
        return "SpongePlatform{" +
                "minecraftVersion=" + minecraftVersion +
                ", serverName='" + serverName + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                ", spongeApiVersion='" + spongeApiVersion + '\'' +
                '}';
    }
}
