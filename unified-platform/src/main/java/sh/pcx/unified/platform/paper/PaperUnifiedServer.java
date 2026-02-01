/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Paper/Spigot implementation of {@link UnifiedServer}.
 *
 * <p>This class wraps the Bukkit {@link Server} and provides a unified API for
 * server-wide operations including player management, world access, and server control.
 *
 * <h2>TPS and Performance</h2>
 * <p>TPS (Ticks Per Second) and MSPT (Milliseconds Per Tick) methods use Paper's
 * server timing APIs when available. On Spigot, these fall back to basic tick
 * time calculations.
 *
 * <h2>Thread Safety</h2>
 * <p>Most read operations are thread-safe. Operations that modify server state
 * (shutdown, restart, command execution) should be performed on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedServer
 * @see Server
 */
public final class PaperUnifiedServer implements UnifiedServer {

    private final PaperPlatformProvider provider;
    private final Server server;
    private final MinecraftVersion minecraftVersion;
    private final ServerType serverType;

    /**
     * Creates a new PaperUnifiedServer.
     *
     * @param provider the platform provider for creating related wrappers
     * @since 1.0.0
     */
    public PaperUnifiedServer(@NotNull PaperPlatformProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.server = Bukkit.getServer();
        this.minecraftVersion = parseMinecraftVersion();
        this.serverType = ServerType.detect();
    }

    /**
     * Parses the Minecraft version from the server.
     *
     * @return the parsed MinecraftVersion
     */
    @NotNull
    private MinecraftVersion parseMinecraftVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        int dashIndex = bukkitVersion.indexOf('-');
        if (dashIndex > 0) {
            String version = bukkitVersion.substring(0, dashIndex);
            MinecraftVersion parsed = MinecraftVersion.tryParse(version);
            if (parsed != null) {
                return parsed;
            }
        }
        return MinecraftVersion.MINIMUM_SUPPORTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName() {
        return server.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getVersion() {
        return server.getVersion();
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
    public ServerType getServerType() {
        return serverType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public InetSocketAddress getAddress() {
        String ip = server.getIp();
        int port = server.getPort();
        if (ip == null || ip.isEmpty()) {
            ip = "0.0.0.0";
        }
        return new InetSocketAddress(ip, port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return server.getPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getIp() {
        String ip = server.getIp();
        return ip != null && !ip.isEmpty() ? ip : "0.0.0.0";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxPlayers(int maxPlayers) {
        try {
            server.setMaxPlayers(maxPlayers);
        } catch (NoSuchMethodError e) {
            // Not supported on this version
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOnlinePlayerCount() {
        return server.getOnlinePlayers().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedPlayer> getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .map(provider::getOrCreatePlayer)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        return provider.getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        return provider.getPlayer(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull UUID uuid) {
        OfflinePlayer offlinePlayer = server.getOfflinePlayer(uuid);
        return provider.getOrCreateOfflinePlayer(offlinePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    @NotNull
    @SuppressWarnings("deprecation")
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull String name) {
        OfflinePlayer offlinePlayer = server.getOfflinePlayer(name);
        return provider.getOrCreateOfflinePlayer(offlinePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<OfflineUnifiedPlayer> getOfflinePlayers() {
        return java.util.Arrays.stream(server.getOfflinePlayers())
                .map(provider::getOrCreateOfflinePlayer)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPlayedBefore(@NotNull UUID uuid) {
        return server.getOfflinePlayer(uuid).hasPlayedBefore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedWorld> getWorlds() {
        return server.getWorlds().stream()
                .map(provider::getOrCreateWorld)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        return provider.getWorld(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        return provider.getWorld(uuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getDefaultWorld() {
        World world = server.getWorlds().get(0);
        return provider.getOrCreateWorld(world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(@NotNull Component message) {
        try {
            server.broadcast(message);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            String legacy = LegacyComponentSerializer.legacySection().serialize(message);
            server.broadcastMessage(legacy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(@NotNull Component message, @NotNull String permission) {
        try {
            server.broadcast(message, permission);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            String legacy = LegacyComponentSerializer.legacySection().serialize(message);
            server.broadcast(legacy, permission);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeCommand(@NotNull String command) {
        return server.dispatchCommand(server.getConsoleSender(), command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> executeCommandAsync(@NotNull String command) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (Bukkit.isPrimaryThread()) {
            future.complete(executeCommand(command));
        } else {
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugins()[0],
                    () -> future.complete(executeCommand(command))
            );
        }

        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Path getServerFolder() {
        return server.getWorldContainer().toPath().getParent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Path getPluginsFolder() {
        return server.getPluginManager().getPlugins()[0].getDataFolder().getParentFile().toPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Path getWorldsFolder() {
        return server.getWorldContainer().toPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTPS() {
        try {
            // Paper provides TPS
            double[] tps = server.getTPS();
            return tps.length > 0 ? Math.min(20.0, tps[0]) : 20.0;
        } catch (NoSuchMethodError e) {
            // Spigot doesn't expose TPS directly
            return 20.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double @NotNull [] getAverageTPS() {
        try {
            double[] tps = server.getTPS();
            // Cap at 20
            double[] capped = new double[Math.min(3, tps.length)];
            for (int i = 0; i < capped.length; i++) {
                capped[i] = Math.min(20.0, tps[i]);
            }
            return capped;
        } catch (NoSuchMethodError e) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMSPT() {
        try {
            // Paper provides MSPT
            return server.getAverageTickTime();
        } catch (NoSuchMethodError e) {
            // Calculate from TPS: 1000ms / TPS = MSPT
            double tps = getTPS();
            return tps > 0 ? 1000.0 / tps : 50.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnlineMode() {
        return server.getOnlineMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWhitelistEnabled() {
        return server.hasWhitelist();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server.setWhitelist(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadWhitelist() {
        server.reloadWhitelist();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Component getMotd() {
        try {
            return server.motd();
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            return Component.text(server.getMotd());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMotd(@NotNull Component motd) {
        // Note: Setting MOTD requires a server restart or plugin like ServerListPlus
        // Bukkit doesn't provide a direct method to change MOTD at runtime
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        server.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean restart() {
        try {
            // Paper supports restart
            server.getClass().getMethod("restart").invoke(server);
            return true;
        } catch (Exception e) {
            // Spigot/CraftBukkit uses spigot().restart()
            try {
                server.spigot().restart();
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStopping() {
        try {
            return server.isStopping();
        } catch (NoSuchMethodError e) {
            // Not available on older versions
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Thread getPrimaryThread() {
        // The thread that runs the main server tick
        // On Bukkit, this is Thread.currentThread() when called from the main thread
        // For Folia, this would be the global region thread
        return Thread.currentThread();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrimaryThread() {
        return server.isPrimaryThread();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) server;
    }

    /**
     * Returns a string representation of this server.
     *
     * @return a string containing server name and version
     */
    @Override
    public String toString() {
        return "PaperUnifiedServer{" +
                "name='" + server.getName() + '\'' +
                ", version='" + server.getVersion() + '\'' +
                ", minecraft=" + minecraftVersion +
                ", serverType=" + serverType +
                '}';
    }
}
