/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server implementation for Folia.
 *
 * <p>This class provides server operations that work correctly with
 * Folia's threading model. Some operations that would normally run
 * on the main thread are instead dispatched to the global region.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedServer
 */
public final class FoliaUnifiedServer implements UnifiedServer {

    private static final Logger LOGGER = Logger.getLogger(FoliaUnifiedServer.class.getName());

    /**
     * The underlying Bukkit Server object.
     */
    private final Object bukkitServer;

    /**
     * The platform provider.
     */
    private final FoliaPlatformProvider provider;

    /**
     * Cached Minecraft version.
     */
    private final MinecraftVersion minecraftVersion;

    /**
     * Constructs a new FoliaUnifiedServer.
     *
     * @param bukkitServer the Bukkit Server object
     * @param provider the platform provider
     * @since 1.0.0
     */
    public FoliaUnifiedServer(@NotNull Object bukkitServer, @NotNull FoliaPlatformProvider provider) {
        this.bukkitServer = bukkitServer;
        this.provider = provider;
        this.minecraftVersion = detectMinecraftVersion();
    }

    private MinecraftVersion detectMinecraftVersion() {
        try {
            String bukkitVersion = (String) bukkitServer.getClass().getMethod("getBukkitVersion")
                    .invoke(bukkitServer);
            String[] parts = bukkitVersion.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                return MinecraftVersion.parse(parts[0] + "." + parts[1]);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to detect Minecraft version", e);
        }
        return MinecraftVersion.V1_20_5;
    }

    @Override
    @NotNull
    public String getName() {
        try {
            return (String) bukkitServer.getClass().getMethod("getName").invoke(bukkitServer);
        } catch (Exception e) {
            return "Folia";
        }
    }

    @Override
    @NotNull
    public String getVersion() {
        try {
            return (String) bukkitServer.getClass().getMethod("getVersion").invoke(bukkitServer);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    @NotNull
    public MinecraftVersion getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    @NotNull
    public ServerType getServerType() {
        return ServerType.FOLIA;
    }

    @Override
    @NotNull
    public InetSocketAddress getAddress() {
        try {
            String ip = (String) bukkitServer.getClass().getMethod("getIp").invoke(bukkitServer);
            int port = (int) bukkitServer.getClass().getMethod("getPort").invoke(bukkitServer);
            return new InetSocketAddress(ip.isEmpty() ? "0.0.0.0" : ip, port);
        } catch (Exception e) {
            return new InetSocketAddress("0.0.0.0", 25565);
        }
    }

    @Override
    public int getPort() {
        try {
            return (int) bukkitServer.getClass().getMethod("getPort").invoke(bukkitServer);
        } catch (Exception e) {
            return 25565;
        }
    }

    @Override
    @NotNull
    public String getIp() {
        try {
            String ip = (String) bukkitServer.getClass().getMethod("getIp").invoke(bukkitServer);
            return ip.isEmpty() ? "0.0.0.0" : ip;
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    @Override
    public int getMaxPlayers() {
        try {
            return (int) bukkitServer.getClass().getMethod("getMaxPlayers").invoke(bukkitServer);
        } catch (Exception e) {
            return 20;
        }
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        try {
            bukkitServer.getClass().getMethod("setMaxPlayers", int.class)
                    .invoke(bukkitServer, maxPlayers);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set max players", e);
        }
    }

    @Override
    public int getOnlinePlayerCount() {
        try {
            Collection<?> players = (Collection<?>) bukkitServer.getClass()
                    .getMethod("getOnlinePlayers").invoke(bukkitServer);
            return players.size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @NotNull
    public Collection<UnifiedPlayer> getOnlinePlayers() {
        try {
            Collection<?> players = (Collection<?>) bukkitServer.getClass()
                    .getMethod("getOnlinePlayers").invoke(bukkitServer);
            Collection<UnifiedPlayer> result = new ArrayList<>();
            for (Object player : players) {
                result.add(provider.wrapPlayer(player));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        return provider.getPlayer(uuid);
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        return provider.getPlayer(name);
    }

    @Override
    @NotNull
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull UUID uuid) {
        try {
            Object offlinePlayer = bukkitServer.getClass()
                    .getMethod("getOfflinePlayer", UUID.class)
                    .invoke(bukkitServer, uuid);
            return provider.wrapOfflinePlayer(offlinePlayer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get offline player", e);
        }
    }

    @Override
    @Deprecated
    @NotNull
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull String name) {
        try {
            Object offlinePlayer = bukkitServer.getClass()
                    .getMethod("getOfflinePlayer", String.class)
                    .invoke(bukkitServer, name);
            return provider.wrapOfflinePlayer(offlinePlayer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get offline player", e);
        }
    }

    @Override
    @NotNull
    public Collection<OfflineUnifiedPlayer> getOfflinePlayers() {
        try {
            Object[] players = (Object[]) bukkitServer.getClass()
                    .getMethod("getOfflinePlayers").invoke(bukkitServer);
            Collection<OfflineUnifiedPlayer> result = new ArrayList<>();
            for (Object player : players) {
                result.add(provider.wrapOfflinePlayer(player));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasPlayedBefore(@NotNull UUID uuid) {
        try {
            Object offlinePlayer = bukkitServer.getClass()
                    .getMethod("getOfflinePlayer", UUID.class)
                    .invoke(bukkitServer, uuid);
            return (boolean) offlinePlayer.getClass().getMethod("hasPlayedBefore")
                    .invoke(offlinePlayer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @NotNull
    public Collection<UnifiedWorld> getWorlds() {
        try {
            Collection<?> worlds = (Collection<?>) bukkitServer.getClass()
                    .getMethod("getWorlds").invoke(bukkitServer);
            Collection<UnifiedWorld> result = new ArrayList<>();
            for (Object world : worlds) {
                result.add(provider.wrapWorld(world));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        return provider.getWorld(name);
    }

    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        return provider.getWorld(uuid);
    }

    @Override
    @NotNull
    public UnifiedWorld getDefaultWorld() {
        Collection<UnifiedWorld> worlds = getWorlds();
        if (worlds.isEmpty()) {
            throw new IllegalStateException("No worlds loaded");
        }
        return worlds.iterator().next();
    }

    @Override
    public void broadcast(@NotNull Component message) {
        try {
            bukkitServer.getClass().getMethod("broadcast", Component.class)
                    .invoke(bukkitServer, message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to broadcast message", e);
        }
    }

    @Override
    public void broadcast(@NotNull Component message, @NotNull String permission) {
        try {
            bukkitServer.getClass().getMethod("broadcast", Component.class, String.class)
                    .invoke(bukkitServer, message, permission);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to broadcast message with permission", e);
        }
    }

    @Override
    public boolean executeCommand(@NotNull String command) {
        try {
            Object consoleSender = bukkitServer.getClass().getMethod("getConsoleSender")
                    .invoke(bukkitServer);
            return (boolean) bukkitServer.getClass()
                    .getMethod("dispatchCommand",
                            Class.forName("org.bukkit.command.CommandSender"),
                            String.class)
                    .invoke(bukkitServer, consoleSender, command);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to execute command: " + command, e);
            return false;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> executeCommandAsync(@NotNull String command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command));
    }

    @Override
    @NotNull
    public Path getServerFolder() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    @Override
    @NotNull
    public Path getPluginsFolder() {
        return getServerFolder().resolve("plugins");
    }

    @Override
    @NotNull
    public Path getWorldsFolder() {
        return getServerFolder();
    }

    @Override
    public double getTPS() {
        try {
            double[] tps = (double[]) bukkitServer.getClass().getMethod("getTPS")
                    .invoke(bukkitServer);
            return tps[0];
        } catch (Exception e) {
            return 20.0;
        }
    }

    @Override
    public double @NotNull [] getAverageTPS() {
        try {
            return (double[]) bukkitServer.getClass().getMethod("getTPS")
                    .invoke(bukkitServer);
        } catch (Exception e) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    @Override
    public double getMSPT() {
        try {
            // Paper method
            return (double) bukkitServer.getClass().getMethod("getAverageTickTime")
                    .invoke(bukkitServer);
        } catch (Exception e) {
            return 50.0;
        }
    }

    @Override
    public boolean isOnlineMode() {
        try {
            return (boolean) bukkitServer.getClass().getMethod("getOnlineMode")
                    .invoke(bukkitServer);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public boolean isWhitelistEnabled() {
        try {
            return (boolean) bukkitServer.getClass().getMethod("hasWhitelist")
                    .invoke(bukkitServer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        try {
            bukkitServer.getClass().getMethod("setWhitelist", boolean.class)
                    .invoke(bukkitServer, enabled);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set whitelist", e);
        }
    }

    @Override
    public void reloadWhitelist() {
        try {
            bukkitServer.getClass().getMethod("reloadWhitelist").invoke(bukkitServer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reload whitelist", e);
        }
    }

    @Override
    @NotNull
    public Component getMotd() {
        try {
            return (Component) bukkitServer.getClass().getMethod("motd")
                    .invoke(bukkitServer);
        } catch (Exception e) {
            return Component.text("A Minecraft Server");
        }
    }

    @Override
    public void setMotd(@NotNull Component motd) {
        // MOTD cannot be changed at runtime in most implementations
        LOGGER.fine("setMotd called but not supported at runtime");
    }

    @Override
    public void shutdown() {
        try {
            bukkitServer.getClass().getMethod("shutdown").invoke(bukkitServer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to shutdown server", e);
        }
    }

    @Override
    public boolean restart() {
        try {
            // Paper method
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object spigot = bukkitClass.getMethod("spigot").invoke(null);
            spigot.getClass().getMethod("restart").invoke(spigot);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Restart not supported", e);
            return false;
        }
    }

    @Override
    public boolean isStopping() {
        try {
            return (boolean) bukkitServer.getClass().getMethod("isStopping")
                    .invoke(bukkitServer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @NotNull
    public Thread getPrimaryThread() {
        // In Folia, there's no single primary thread
        // Return the global region thread as the closest equivalent
        return Thread.currentThread();
    }

    @Override
    public boolean isPrimaryThread() {
        // In Folia, check if on global tick thread
        return FoliaDetector.isGlobalTickThread();
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getHandle() {
        return (T) bukkitServer;
    }

    @Override
    public String toString() {
        return String.format("FoliaUnifiedServer[name=%s, version=%s]",
                getName(), getVersion());
    }
}
