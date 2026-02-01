/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.detection;

import sh.pcx.unified.server.MinecraftVersion;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the Minecraft server version at runtime.
 *
 * <p>This class provides multiple detection strategies to determine the current
 * Minecraft version, handling differences between Paper, Spigot, and other
 * server implementations.
 *
 * <h2>Detection Strategies</h2>
 * <ol>
 *   <li>Paper API: {@code Bukkit.getMinecraftVersion()} (Paper 1.19.4+)</li>
 *   <li>Bukkit version string parsing from {@code Bukkit.getVersion()}</li>
 *   <li>CraftBukkit package version extraction</li>
 *   <li>Server class name analysis</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MinecraftVersionDetector detector = MinecraftVersionDetector.getInstance();
 *
 * // Get detected version
 * MinecraftVersion version = detector.detect();
 *
 * // Get NMS version string
 * String nmsVersion = detector.getNmsVersion();
 *
 * // Get server platform
 * String platform = detector.getPlatform();
 *
 * // Check for Mojang mappings
 * boolean mojang = detector.usesMojangMappings();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Detection results are cached after first detection.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MinecraftVersionDetector {

    private static final Logger LOGGER = Logger.getLogger(MinecraftVersionDetector.class.getName());

    // Singleton instance
    private static volatile MinecraftVersionDetector instance;

    // Patterns for version extraction
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "\\(MC: (\\d+\\.\\d+(?:\\.\\d+)?)\\)");
    private static final Pattern BUKKIT_VERSION_PATTERN = Pattern.compile(
            "(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern NMS_PACKAGE_PATTERN = Pattern.compile(
            "org\\.bukkit\\.craftbukkit\\.v(\\d+_\\d+_R\\d+)");

    // Cached detection results
    private volatile MinecraftVersion cachedVersion;
    private volatile String cachedNmsVersion;
    private volatile String cachedPlatform;
    private volatile String cachedServerVersion;
    private volatile Boolean cachedMojangMappings;

    /**
     * Private constructor - use {@link #getInstance()}.
     */
    private MinecraftVersionDetector() {
        // Private constructor
    }

    /**
     * Gets the singleton instance.
     *
     * @return the detector instance
     * @since 1.0.0
     */
    @NotNull
    public static MinecraftVersionDetector getInstance() {
        if (instance == null) {
            synchronized (MinecraftVersionDetector.class) {
                if (instance == null) {
                    instance = new MinecraftVersionDetector();
                }
            }
        }
        return instance;
    }

    /**
     * Detects the current Minecraft version.
     *
     * <p>This method tries multiple detection strategies and caches the result.
     *
     * @return the detected Minecraft version
     * @throws IllegalStateException if version cannot be detected
     * @since 1.0.0
     */
    @NotNull
    public MinecraftVersion detect() {
        if (cachedVersion != null) {
            return cachedVersion;
        }

        synchronized (this) {
            if (cachedVersion != null) {
                return cachedVersion;
            }

            MinecraftVersion version = null;

            // Strategy 1: Paper API (Paper 1.19.4+)
            version = detectViaPaperApi();
            if (version != null) {
                LOGGER.fine("Detected version via Paper API: " + version);
                cachedVersion = version;
                return version;
            }

            // Strategy 2: Parse Bukkit.getVersion()
            version = detectViaBukkitVersion();
            if (version != null) {
                LOGGER.fine("Detected version via Bukkit version: " + version);
                cachedVersion = version;
                return version;
            }

            // Strategy 3: Parse CraftBukkit package name
            version = detectViaNmsPackage();
            if (version != null) {
                LOGGER.fine("Detected version via NMS package: " + version);
                cachedVersion = version;
                return version;
            }

            // Strategy 4: Fall back to server class analysis
            version = detectViaClassAnalysis();
            if (version != null) {
                LOGGER.fine("Detected version via class analysis: " + version);
                cachedVersion = version;
                return version;
            }

            throw new IllegalStateException(
                    "Could not detect Minecraft version. Server: " + getServerVersion());
        }
    }

    /**
     * Gets the NMS version string for the current server.
     *
     * @return the NMS version string (e.g., "v1_21_R1")
     * @since 1.0.0
     */
    @NotNull
    public String getNmsVersion() {
        if (cachedNmsVersion != null) {
            return cachedNmsVersion;
        }

        synchronized (this) {
            if (cachedNmsVersion != null) {
                return cachedNmsVersion;
            }

            // Try to detect from CraftBukkit package
            String nmsVersion = detectNmsPackageVersion();
            if (nmsVersion != null) {
                cachedNmsVersion = nmsVersion;
                return nmsVersion;
            }

            // Fall back to calculating from Minecraft version
            cachedNmsVersion = detect().getNmsVersion();
            return cachedNmsVersion;
        }
    }

    /**
     * Gets the server platform type.
     *
     * @return the platform name (PAPER, SPIGOT, FOLIA, etc.)
     * @since 1.0.0
     */
    @NotNull
    public String getPlatform() {
        if (cachedPlatform != null) {
            return cachedPlatform;
        }

        synchronized (this) {
            if (cachedPlatform != null) {
                return cachedPlatform;
            }

            String platform = detectPlatform();
            cachedPlatform = platform;
            return platform;
        }
    }

    /**
     * Gets the full server version string.
     *
     * @return the server version string
     * @since 1.0.0
     */
    @NotNull
    public String getServerVersion() {
        if (cachedServerVersion != null) {
            return cachedServerVersion;
        }

        synchronized (this) {
            if (cachedServerVersion != null) {
                return cachedServerVersion;
            }

            try {
                cachedServerVersion = Bukkit.getVersion();
            } catch (Exception e) {
                cachedServerVersion = "Unknown";
            }
            return cachedServerVersion;
        }
    }

    /**
     * Checks if the server uses Mojang mappings (Paper 1.21.11+).
     *
     * @return true if using Mojang mappings
     * @since 1.0.0
     */
    public boolean usesMojangMappings() {
        if (cachedMojangMappings != null) {
            return cachedMojangMappings;
        }

        synchronized (this) {
            if (cachedMojangMappings != null) {
                return cachedMojangMappings;
            }

            // Paper 1.21.11+ uses Mojang mappings
            boolean mojang = false;
            if (getPlatform().contains("Paper")) {
                MinecraftVersion version = detect();
                mojang = version.isAtLeast(MinecraftVersion.V1_21_11);

                // Also check for relocated NMS classes
                if (mojang) {
                    try {
                        // In Mojang mappings, class names are different
                        Class.forName("net.minecraft.server.MinecraftServer");
                        mojang = true;
                    } catch (ClassNotFoundException e) {
                        // Still using Spigot mappings
                        mojang = false;
                    }
                }
            }

            cachedMojangMappings = mojang;
            return mojang;
        }
    }

    /**
     * Checks if the detected version is supported.
     *
     * @return true if the version is within supported range
     * @since 1.0.0
     */
    public boolean isSupported() {
        return detect().isSupported();
    }

    /**
     * Clears the cached detection results.
     *
     * <p>This is mainly useful for testing.
     *
     * @since 1.0.0
     */
    public void clearCache() {
        synchronized (this) {
            cachedVersion = null;
            cachedNmsVersion = null;
            cachedPlatform = null;
            cachedServerVersion = null;
            cachedMojangMappings = null;
        }
    }

    // ===== Detection Strategies =====

    @Nullable
    private MinecraftVersion detectViaPaperApi() {
        try {
            // Paper 1.19.4+ provides Bukkit.getMinecraftVersion()
            String versionString = Bukkit.getMinecraftVersion();
            if (versionString != null && !versionString.isBlank()) {
                return MinecraftVersion.parse(versionString);
            }
        } catch (NoSuchMethodError | Exception e) {
            LOGGER.log(Level.FINE, "Paper API detection failed", e);
        }
        return null;
    }

    @Nullable
    private MinecraftVersion detectViaBukkitVersion() {
        try {
            String bukkitVersion = Bukkit.getVersion();
            if (bukkitVersion == null) {
                return null;
            }

            // Try to extract MC version from "git-Paper-XXX (MC: 1.21.4)"
            Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion);
            if (matcher.find()) {
                return MinecraftVersion.parse(matcher.group(1));
            }

            // Try to parse from getBukkitVersion() "1.21.4-R0.1-SNAPSHOT"
            String bukkitVer = Bukkit.getBukkitVersion();
            if (bukkitVer != null) {
                Matcher verMatcher = BUKKIT_VERSION_PATTERN.matcher(bukkitVer);
                if (verMatcher.find()) {
                    int major = Integer.parseInt(verMatcher.group(1));
                    int minor = Integer.parseInt(verMatcher.group(2));
                    int patch = verMatcher.group(3) != null ?
                            Integer.parseInt(verMatcher.group(3)) : 0;
                    return new MinecraftVersion(major, minor, patch);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Bukkit version detection failed", e);
        }
        return null;
    }

    @Nullable
    private MinecraftVersion detectViaNmsPackage() {
        String nmsVersion = detectNmsPackageVersion();
        if (nmsVersion != null) {
            try {
                return VersionConstants.getMinVersion(nmsVersion);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.FINE, "Unknown NMS version: " + nmsVersion, e);
            }
        }
        return null;
    }

    @Nullable
    private MinecraftVersion detectViaClassAnalysis() {
        // Check for version-specific classes
        try {
            // 1.21.11+ has registry-based game rules
            Class.forName("net.minecraft.world.level.GameRules$Key");
            if (classExists("net.minecraft.core.registries.BuiltInRegistries")) {
                // Could be 1.21.11+, need more specific check
                // For now, return minimum supported
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        // Fall back to minimum supported version
        LOGGER.warning("Could not determine exact version, assuming minimum supported");
        return MinecraftVersion.MINIMUM_SUPPORTED;
    }

    @Nullable
    private String detectNmsPackageVersion() {
        try {
            // Get CraftServer class
            Class<?> craftServerClass = Bukkit.getServer().getClass();
            String className = craftServerClass.getName();

            Matcher matcher = NMS_PACKAGE_PATTERN.matcher(className);
            if (matcher.find()) {
                return "v" + matcher.group(1);
            }

            // Try to find the package directly
            Package pkg = craftServerClass.getPackage();
            if (pkg != null) {
                String pkgName = pkg.getName();
                if (pkgName.startsWith("org.bukkit.craftbukkit.")) {
                    String[] parts = pkgName.split("\\.");
                    if (parts.length >= 4) {
                        return parts[3];
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "NMS package detection failed", e);
        }
        return null;
    }

    @NotNull
    private String detectPlatform() {
        String version = getServerVersion().toLowerCase();

        if (version.contains("folia")) {
            return "FOLIA";
        } else if (version.contains("paper") || version.contains("papermc")) {
            return "PAPER";
        } else if (version.contains("purpur")) {
            return "PURPUR";
        } else if (version.contains("pufferfish")) {
            return "PUFFERFISH";
        } else if (version.contains("spigot")) {
            return "SPIGOT";
        } else if (version.contains("bukkit")) {
            return "CRAFTBUKKIT";
        }

        // Check for Paper-specific classes
        if (classExists("io.papermc.paper.configuration.Configuration")) {
            return "PAPER";
        }

        // Check for Folia
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            return "FOLIA";
        }

        return "UNKNOWN";
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
