/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of Minecraft platform types.
 *
 * <p>This enum categorizes different Minecraft server and client platforms
 * at a higher level than {@link sh.pcx.unified.server.ServerType}.
 * It is used by the SPI to determine which platform adapter to load.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlatformType type = Platform.current().getType();
 *
 * switch (type) {
 *     case BUKKIT -> {
 *         // Use Bukkit-based implementation
 *     }
 *     case SPONGE -> {
 *         // Use Sponge-based implementation
 *     }
 *     case VELOCITY, BUNGEECORD -> {
 *         // Proxy server - limited API
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Platform
 * @see PlatformProvider
 */
public enum PlatformType {

    /**
     * Bukkit-based platforms (Spigot, Paper, Folia, etc.).
     *
     * <p>These platforms use the Bukkit API and are the most common
     * server implementations for vanilla-style Minecraft servers.
     */
    BUKKIT("Bukkit", true, false),

    /**
     * Sponge-based platforms.
     *
     * <p>Sponge provides a modular API designed for modded servers
     * running on Forge or Fabric.
     */
    SPONGE("Sponge", true, false),

    /**
     * Velocity proxy platform.
     *
     * <p>A modern, high-performance Minecraft proxy server.
     * Has limited API compared to backend servers.
     */
    VELOCITY("Velocity", false, true),

    /**
     * BungeeCord proxy platform.
     *
     * <p>The original Minecraft proxy server.
     * Has limited API compared to backend servers.
     */
    BUNGEECORD("BungeeCord", false, true),

    /**
     * Fabric modding platform.
     *
     * <p>A lightweight modding platform for Minecraft.
     * Support is planned for future versions.
     */
    FABRIC("Fabric", true, false),

    /**
     * Minestom lightweight server.
     *
     * <p>A from-scratch Minecraft server implementation.
     * Support is under consideration.
     */
    MINESTOM("Minestom", true, false),

    /**
     * Unknown or unsupported platform.
     */
    UNKNOWN("Unknown", false, false);

    private final String displayName;
    private final boolean backendServer;
    private final boolean proxyServer;

    PlatformType(@NotNull String displayName, boolean backendServer, boolean proxyServer) {
        this.displayName = displayName;
        this.backendServer = backendServer;
        this.proxyServer = proxyServer;
    }

    /**
     * Returns the human-readable name of this platform type.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this is a backend (game) server platform.
     *
     * <p>Backend servers host actual Minecraft gameplay with worlds,
     * entities, and players in-game.
     *
     * @return true if this is a backend server
     * @since 1.0.0
     */
    public boolean isBackendServer() {
        return backendServer;
    }

    /**
     * Checks if this is a proxy server platform.
     *
     * <p>Proxy servers route players between backend servers and
     * have limited access to gameplay features.
     *
     * @return true if this is a proxy server
     * @since 1.0.0
     */
    public boolean isProxyServer() {
        return proxyServer;
    }

    /**
     * Checks if this platform type is currently supported.
     *
     * @return true if the platform is supported
     * @since 1.0.0
     */
    public boolean isSupported() {
        return this == BUKKIT || this == SPONGE;
    }

    /**
     * Checks if this platform type has world access.
     *
     * <p>Only backend servers have direct world access.
     *
     * @return true if worlds can be accessed
     * @since 1.0.0
     */
    public boolean hasWorldAccess() {
        return backendServer;
    }

    /**
     * Checks if this platform type has player inventory access.
     *
     * <p>Only backend servers have direct inventory access.
     *
     * @return true if inventories can be accessed
     * @since 1.0.0
     */
    public boolean hasInventoryAccess() {
        return backendServer;
    }

    /**
     * Attempts to detect the current platform type.
     *
     * @return the detected platform type
     * @since 1.0.0
     */
    @NotNull
    public static PlatformType detect() {
        // Check for Velocity
        try {
            Class.forName("com.velocitypowered.api.proxy.ProxyServer");
            return VELOCITY;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for BungeeCord
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            return BUNGEECORD;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Sponge
        try {
            Class.forName("org.spongepowered.api.Sponge");
            return SPONGE;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Bukkit (Spigot, Paper, Folia, etc.)
        try {
            Class.forName("org.bukkit.Bukkit");
            return BUKKIT;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Fabric
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return FABRIC;
        } catch (ClassNotFoundException ignored) {
        }

        // Check for Minestom
        try {
            Class.forName("net.minestom.server.MinecraftServer");
            return MINESTOM;
        } catch (ClassNotFoundException ignored) {
        }

        return UNKNOWN;
    }
}
