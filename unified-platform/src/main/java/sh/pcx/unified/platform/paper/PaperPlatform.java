/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.platform.Platform;
import sh.pcx.unified.platform.PlatformType;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Paper/Spigot implementation of the {@link Platform} interface.
 *
 * <p>This class provides platform detection and information for Bukkit-based servers
 * including Paper, Spigot, and Folia. It detects the server type, Minecraft version,
 * and available features at runtime.
 *
 * <h2>Platform Detection</h2>
 * <p>The implementation performs class existence checks to determine the exact
 * server type:
 * <ul>
 *   <li>Folia - Checks for {@code io.papermc.paper.threadedregions.RegionizedServer}</li>
 *   <li>Paper - Checks for {@code io.papermc.paper.configuration.Configuration}</li>
 *   <li>Spigot - Checks for {@code org.spigotmc.SpigotConfig}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable after construction and is thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Platform
 * @see PaperPlatformProvider
 */
public final class PaperPlatform implements Platform {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            ".*\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?\\).*"
    );

    private final ServerType serverType;
    private final MinecraftVersion minecraftVersion;
    private final String serverName;
    private final String serverVersion;
    private final String nmsVersion;
    private final boolean isPaper;
    private final boolean isFolia;
    private final boolean supportsAsyncChunks;
    private final boolean supportsAdventure;

    /**
     * Creates a new PaperPlatform instance by detecting the current environment.
     *
     * <p>This constructor performs all necessary detection to determine server type,
     * version, and capabilities.
     *
     * @since 1.0.0
     */
    public PaperPlatform() {
        this.serverType = ServerType.detect();
        this.serverName = Bukkit.getName();
        this.serverVersion = Bukkit.getVersion();
        this.minecraftVersion = parseMinecraftVersion(this.serverVersion);
        this.nmsVersion = detectNmsVersion();
        this.isPaper = checkIsPaper();
        this.isFolia = checkIsFolia();
        this.supportsAsyncChunks = isPaper;
        this.supportsAdventure = isPaper;
    }

    /**
     * Parses the Minecraft version from the server version string.
     *
     * @param versionString the server version string
     * @return the parsed MinecraftVersion
     */
    @NotNull
    private MinecraftVersion parseMinecraftVersion(@NotNull String versionString) {
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new MinecraftVersion(major, minor, patch);
        }
        // Fallback to Bukkit.getBukkitVersion()
        String bukkitVersion = Bukkit.getBukkitVersion();
        int dashIndex = bukkitVersion.indexOf('-');
        if (dashIndex > 0) {
            String version = bukkitVersion.substring(0, dashIndex);
            MinecraftVersion parsed = MinecraftVersion.tryParse(version);
            if (parsed != null) {
                return parsed;
            }
        }
        // Default to minimum supported version
        return MinecraftVersion.MINIMUM_SUPPORTED;
    }

    /**
     * Detects the NMS version string for the current server.
     *
     * @return the NMS version string
     */
    @NotNull
    private String detectNmsVersion() {
        // Try to get from CraftBukkit package name
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        // Format: org.bukkit.craftbukkit.v1_21_R1
        String[] parts = packageName.split("\\.");
        if (parts.length >= 4 && parts[3].startsWith("v")) {
            return parts[3];
        }
        // Paper 1.21.11+ uses Mojang mappings, return version-based
        return minecraftVersion.getNmsVersion();
    }

    /**
     * Checks if the server is running Paper.
     *
     * @return true if Paper API is available
     */
    private boolean checkIsPaper() {
        try {
            Class.forName("io.papermc.paper.configuration.Configuration");
            return true;
        } catch (ClassNotFoundException e) {
            // Try legacy check
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

    /**
     * Checks if the server is running Folia.
     *
     * @return true if Folia's region threading is active
     */
    private boolean checkIsFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public PlatformType getType() {
        return PlatformType.BUKKIT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ServerType getServerType() {
        return serverType;
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
     */
    @Override
    public boolean isPaper() {
        return isPaper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSponge() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsAsyncChunks() {
        return supportsAsyncChunks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsAdventure() {
        return supportsAdventure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsPluginMessaging() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getNmsVersion() {
        return nmsVersion;
    }

    /**
     * Returns a string representation of this platform.
     *
     * @return a descriptive string including server type and version
     */
    @Override
    public String toString() {
        return "PaperPlatform{" +
                "serverType=" + serverType +
                ", minecraftVersion=" + minecraftVersion +
                ", serverName='" + serverName + '\'' +
                ", isPaper=" + isPaper +
                ", isFolia=" + isFolia +
                '}';
    }
}
