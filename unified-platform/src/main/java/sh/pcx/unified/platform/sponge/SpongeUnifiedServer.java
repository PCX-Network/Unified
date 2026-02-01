/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.whitelist.WhitelistService;
import org.spongepowered.api.user.UserManager;
import org.spongepowered.api.world.server.ServerWorld;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Sponge implementation of the {@link UnifiedServer} interface.
 *
 * <p>This class provides server-level operations for Sponge servers,
 * including player management, world access, and server configuration.
 *
 * <h2>Service Integration</h2>
 * <p>This implementation integrates with Sponge's service architecture:
 * <ul>
 *   <li>UserManager for player data</li>
 *   <li>WhitelistService for whitelist management</li>
 *   <li>WorldManager for world operations</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Most read operations are thread-safe. Operations that modify server
 * state should be performed on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedServer
 */
public final class SpongeUnifiedServer implements UnifiedServer {

    private final SpongePlatformProvider provider;
    private final MinecraftVersion minecraftVersion;

    /**
     * Creates a new SpongeUnifiedServer.
     *
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeUnifiedServer(@NotNull SpongePlatformProvider provider) {
        this.provider = provider;
        this.minecraftVersion = provider.getSpongePlatform().getMinecraftVersion();
    }

    /**
     * Returns the Sponge Server instance.
     *
     * @return the Sponge Server
     */
    @NotNull
    private Server server() {
        return Sponge.server();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName() {
        return provider.getSpongePlatform().getServerName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getVersion() {
        return provider.getSpongePlatform().getServerVersion();
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
        return ServerType.SPONGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public InetSocketAddress getAddress() {
        return server().boundAddress().orElse(new InetSocketAddress("0.0.0.0", 25565));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return getAddress().getPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getIp() {
        return getAddress().getAddress().getHostAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxPlayers() {
        return server().maxPlayers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxPlayers(int maxPlayers) {
        // Sponge doesn't expose a direct setter for max players
        // This would require modifying server.properties
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOnlinePlayerCount() {
        return server().onlinePlayers().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedPlayer> getOnlinePlayers() {
        return server().onlinePlayers().stream()
                .map(provider::getOrCreatePlayer)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        return server().player(uuid).map(provider::getOrCreatePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        return server().player(name).map(provider::getOrCreatePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull UUID uuid) {
        UserManager userManager = server().userManager();

        // In Sponge API 12, loadOrCreate takes UUID directly
        User user = userManager.loadOrCreate(uuid).join();
        return provider.getOrCreateOfflinePlayer(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    @NotNull
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull String name) {
        UserManager userManager = server().userManager();
        // In Sponge API 12, we need to look up by UUID through game profiles
        Optional<User> userOpt = userManager.streamAll()
                .filter(profile -> profile.name().map(n -> n.equalsIgnoreCase(name)).orElse(false))
                .findFirst()
                .flatMap(profile -> userManager.load(profile.uuid()).join());

        if (userOpt.isPresent()) {
            return provider.getOrCreateOfflinePlayer(userOpt.get());
        }

        // Cannot create user without UUID, return a placeholder
        throw new IllegalArgumentException("Player with name '" + name + "' not found. Use UUID instead.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<OfflineUnifiedPlayer> getOfflinePlayers() {
        UserManager userManager = server().userManager();
        return userManager.streamAll()
                .map(profile -> userManager.load(profile.uuid()).join())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(provider::getOrCreateOfflinePlayer)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPlayedBefore(@NotNull UUID uuid) {
        UserManager userManager = server().userManager();
        // Check if user exists by trying to load them
        return userManager.load(uuid).join().isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedWorld> getWorlds() {
        return server().worldManager().worlds().stream()
                .map(provider::getOrCreateWorld)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        return server().worldManager().worlds().stream()
                .filter(world -> world.key().value().equals(name) ||
                                 world.key().asString().equals(name))
                .findFirst()
                .map(provider::getOrCreateWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        return server().worldManager().worlds().stream()
                .filter(world -> world.uniqueId().equals(uuid))
                .findFirst()
                .map(provider::getOrCreateWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getDefaultWorld() {
        // Get the default world (usually overworld)
        ServerWorld defaultWorld = server().worldManager().worlds().iterator().next();
        return provider.getOrCreateWorld(defaultWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(@NotNull Component message) {
        server().broadcastAudience().sendMessage(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(@NotNull Component message, @NotNull String permission) {
        server().onlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> player.sendMessage(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeCommand(@NotNull String command) {
        try {
            return server().commandManager()
                    .process(Sponge.systemSubject(), command)
                    .isSuccess();
        } catch (org.spongepowered.api.command.exception.CommandException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> executeCommandAsync(@NotNull String command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Path getServerFolder() {
        return Sponge.game().gameDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Path getPluginsFolder() {
        return Sponge.game().gameDirectory().resolve("mods");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Path getWorldsFolder() {
        return Sponge.game().gameDirectory().resolve("world");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTPS() {
        return server().ticksPerSecond();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double @NotNull [] getAverageTPS() {
        double currentTps = getTPS();
        // Sponge doesn't expose historical TPS averages directly
        return new double[]{currentTps, currentTps, currentTps};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMSPT() {
        return server().averageTickTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnlineMode() {
        return server().isOnlineModeEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWhitelistEnabled() {
        return server().isWhitelistEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server().setHasWhitelist(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadWhitelist() {
        Optional<WhitelistService> whitelistService = Sponge.serviceProvider().provide(WhitelistService.class);
        whitelistService.ifPresent(service -> {
            // Sponge doesn't have a direct reload method
            // The whitelist is typically stored in the service itself
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Component getMotd() {
        return server().motd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMotd(@NotNull Component motd) {
        // Sponge doesn't expose a direct setter for MOTD
        // This would require modifying server.properties
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        server().shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean restart() {
        // Sponge doesn't have a built-in restart mechanism
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStopping() {
        return !Sponge.isServerAvailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Thread getPrimaryThread() {
        return Thread.currentThread(); // Not ideal but Sponge doesn't expose this directly
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrimaryThread() {
        return server().onMainThread();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) server();
    }

    /**
     * Returns a string representation of this server.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeUnifiedServer{" +
                "name='" + getName() + '\'' +
                ", version='" + getVersion() + '\'' +
                ", mcVersion=" + minecraftVersion +
                ", players=" + getOnlinePlayerCount() + "/" + getMaxPlayers() +
                '}';
    }
}
