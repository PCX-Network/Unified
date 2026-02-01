/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.server;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of supported Minecraft server platforms.
 *
 * <p>This enum identifies the underlying server software running the plugin.
 * Different server types may have different capabilities, threading models,
 * and API behaviors.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ServerType type = UnifiedAPI.getServer().getServerType();
 *
 * switch (type) {
 *     case PAPER -> {
 *         // Use Paper-specific optimizations
 *     }
 *     case FOLIA -> {
 *         // Use region-aware scheduling
 *     }
 *     case SPONGE -> {
 *         // Use Sponge event system
 *     }
 *     case SPIGOT -> {
 *         // Fallback to Spigot API
 *     }
 * }
 *
 * // Check for specific capabilities
 * if (type.supportsFolia()) {
 *     // Server has Folia's region threading
 * }
 *
 * if (type.isPaperBased()) {
 *     // Server is Paper or a Paper fork
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedServer#getServerType()
 */
public enum ServerType {

    /**
     * Paper server - a high-performance fork of Spigot.
     *
     * <p>Features include:
     * <ul>
     *   <li>Async chunk loading</li>
     *   <li>Native Adventure support</li>
     *   <li>Enhanced API methods</li>
     *   <li>Performance optimizations</li>
     * </ul>
     */
    PAPER(true, false, false),

    /**
     * Folia server - Paper fork with region-based multithreading.
     *
     * <p>Features include:
     * <ul>
     *   <li>Region-aware task scheduling</li>
     *   <li>Parallel world processing</li>
     *   <li>Thread-safe entity handling</li>
     * </ul>
     *
     * <p><strong>Note:</strong> Many synchronous Bukkit APIs are not safe
     * to use on Folia. Use the unified scheduler for thread-safe operations.
     */
    FOLIA(true, true, false),

    /**
     * Spigot server - the most common Bukkit implementation.
     *
     * <p>Provides the baseline Bukkit API with some optimizations
     * over CraftBukkit.
     */
    SPIGOT(false, false, false),

    /**
     * Sponge server - modular API for modded servers.
     *
     * <p>Features include:
     * <ul>
     *   <li>Full Forge/Fabric mod support</li>
     *   <li>Event-based architecture</li>
     *   <li>Cause tracking</li>
     * </ul>
     */
    SPONGE(false, false, true),

    /**
     * Unknown or unsupported server type.
     *
     * <p>The plugin will attempt to use the most basic compatibility
     * layer when running on unknown servers.
     */
    UNKNOWN(false, false, false);

    private final boolean paperBased;
    private final boolean foliaCompatible;
    private final boolean sponge;

    ServerType(boolean paperBased, boolean foliaCompatible, boolean sponge) {
        this.paperBased = paperBased;
        this.foliaCompatible = foliaCompatible;
        this.sponge = sponge;
    }

    /**
     * Checks if this server type is based on Paper.
     *
     * <p>Paper-based servers include Paper itself and its forks (like Folia).
     *
     * @return true if the server is Paper or a Paper fork
     * @since 1.0.0
     */
    public boolean isPaperBased() {
        return paperBased;
    }

    /**
     * Checks if this server type supports Folia's region threading.
     *
     * @return true if region-aware scheduling is required
     * @since 1.0.0
     */
    public boolean supportsFolia() {
        return foliaCompatible;
    }

    /**
     * Checks if this server type is Sponge-based.
     *
     * @return true if the server uses Sponge API
     * @since 1.0.0
     */
    public boolean isSponge() {
        return sponge;
    }

    /**
     * Checks if this server type is Bukkit-compatible.
     *
     * <p>Bukkit-compatible servers include Spigot, Paper, and Folia.
     *
     * @return true if the server supports Bukkit API
     * @since 1.0.0
     */
    public boolean isBukkitCompatible() {
        return this == SPIGOT || this == PAPER || this == FOLIA;
    }

    /**
     * Checks if this is a known/supported server type.
     *
     * @return true if the server type is supported
     * @since 1.0.0
     */
    public boolean isSupported() {
        return this != UNKNOWN;
    }

    /**
     * Returns a human-readable name for this server type.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return switch (this) {
            case PAPER -> "Paper";
            case FOLIA -> "Folia";
            case SPIGOT -> "Spigot";
            case SPONGE -> "Sponge";
            case UNKNOWN -> "Unknown";
        };
    }

    /**
     * Attempts to detect the server type from the current environment.
     *
     * @return the detected server type
     * @since 1.0.0
     */
    @NotNull
    public static ServerType detect() {
        // Check for Folia first (it's also Paper-based)
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return FOLIA;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Paper
        try {
            Class.forName("io.papermc.paper.configuration.Configuration");
            return PAPER;
        } catch (ClassNotFoundException ignored) {
        }

        // Legacy Paper check
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return PAPER;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Sponge
        try {
            Class.forName("org.spongepowered.api.Sponge");
            return SPONGE;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Spigot
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return SPIGOT;
        } catch (ClassNotFoundException ignored) {
        }

        return UNKNOWN;
    }
}
