/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.api.service.whitelist.WhitelistService;
import org.spongepowered.api.statistic.Statistic;
import org.spongepowered.api.statistic.Statistics;
import org.spongepowered.api.world.server.ServerLocation;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sponge implementation of the {@link OfflineUnifiedPlayer} interface.
 *
 * <p>This class wraps Sponge's {@link User} object to provide offline player
 * functionality. It handles operations that can be performed on players who
 * are not currently connected to the server.
 *
 * <h2>Sponge User System</h2>
 * <p>Sponge's User interface represents a player who may or may not be online.
 * When a player is online, they are represented by {@link ServerPlayer} which
 * extends User.
 *
 * <h2>Service Integration</h2>
 * <p>This implementation integrates with Sponge's service architecture:
 * <ul>
 *   <li>{@link BanService} - For ban management</li>
 *   <li>{@link WhitelistService} - For whitelist management</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe for read operations. Write operations should
 * typically be performed on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see OfflineUnifiedPlayer
 * @see SpongeUnifiedPlayer
 */
public class SpongeOfflinePlayer implements OfflineUnifiedPlayer {

    /**
     * The wrapped Sponge User.
     */
    protected final User user;

    /**
     * The platform provider for creating wrapped objects.
     */
    protected final SpongePlatformProvider provider;

    /**
     * Creates a new SpongeOfflinePlayer wrapping the given User.
     *
     * @param user     the Sponge User to wrap
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeOfflinePlayer(@NotNull User user, @NotNull SpongePlatformProvider provider) {
        this.user = user;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getUniqueId() {
        return user.uniqueId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getName() {
        return Optional.of(user.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnline() {
        return user.isOnline();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        if (user instanceof ServerPlayer player) {
            return Optional.of(provider.getOrCreatePlayer(player));
        }
        return Sponge.server().player(user.uniqueId())
                .map(provider::getOrCreatePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPlayedBefore() {
        // In Sponge, if we have a User object, they have played before
        return user.get(Keys.FIRST_DATE_JOINED).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Instant> getFirstPlayed() {
        return user.get(Keys.FIRST_DATE_JOINED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Instant> getLastSeen() {
        if (isOnline()) {
            return Optional.of(Instant.now());
        }
        return user.get(Keys.LAST_DATE_PLAYED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getLastLocation() {
        return CompletableFuture.supplyAsync(() -> {
            org.spongepowered.api.ResourceKey worldKey = user.worldKey();
            Optional<org.spongepowered.api.world.server.ServerWorld> worldOpt =
                    Sponge.server().worldManager().world(worldKey);

            if (worldOpt.isEmpty()) {
                return Optional.empty();
            }

            org.spongepowered.api.world.server.ServerWorld world = worldOpt.get();
            return Optional.of(new UnifiedLocation(
                    provider.getOrCreateWorld(world),
                    user.position().x(),
                    user.position().y(),
                    user.position().z(),
                    (float) user.rotation().y(),
                    (float) user.rotation().x()
            ));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<Optional<UnifiedLocation>> getBedSpawnLocation() {
        return CompletableFuture.supplyAsync(() -> {
            // Sponge stores respawn locations per world
            // Get the raw respawn locations map
            Optional<?> respawnLocationsOpt = user.get(Keys.RESPAWN_LOCATIONS);

            if (respawnLocationsOpt.isEmpty()) {
                return Optional.empty();
            }

            // Try to get the respawn location for the current world
            org.spongepowered.api.ResourceKey worldKey = user.worldKey();
            java.util.Map<?, ?> respawnLocations = (java.util.Map<?, ?>) respawnLocationsOpt.get();
            Object respawnLocation = respawnLocations.get(worldKey);

            if (respawnLocation == null) {
                return Optional.empty();
            }

            // Use reflection to get position since the RespawnLocation type varies
            try {
                java.lang.reflect.Method positionMethod = respawnLocation.getClass().getMethod("position");
                org.spongepowered.math.vector.Vector3d position =
                        (org.spongepowered.math.vector.Vector3d) positionMethod.invoke(respawnLocation);

                Optional<org.spongepowered.api.world.server.ServerWorld> worldOpt =
                        Sponge.server().worldManager().world(worldKey);

                return worldOpt.map(world -> new UnifiedLocation(
                        provider.getOrCreateWorld(world),
                        position.x(),
                        position.y(),
                        position.z()
                ));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBanned() {
        Optional<BanService> banService = Sponge.serviceProvider().provide(BanService.class);
        if (banService.isEmpty()) {
            return false;
        }
        return banService.get().find(user.profile()).join().isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBanned(boolean banned) {
        Optional<BanService> banServiceOpt = Sponge.serviceProvider().provide(BanService.class);
        if (banServiceOpt.isEmpty()) {
            return;
        }
        BanService banService = banServiceOpt.get();

        if (banned) {
            banService.add(org.spongepowered.api.service.ban.Ban.builder()
                    .type(BanTypes.PROFILE.get())
                    .profile(user.profile())
                    .build());
        } else {
            banService.find(user.profile()).join().ifPresent(banService::remove);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ban(@NotNull String reason, @Nullable Instant expiration, @Nullable String source) {
        Optional<BanService> banServiceOpt = Sponge.serviceProvider().provide(BanService.class);
        if (banServiceOpt.isEmpty()) {
            return;
        }
        BanService banService = banServiceOpt.get();

        var builder = org.spongepowered.api.service.ban.Ban.builder()
                .type(BanTypes.PROFILE.get())
                .profile(user.profile())
                .reason(net.kyori.adventure.text.Component.text(reason));

        if (expiration != null) {
            builder.expirationDate(expiration);
        }

        if (source != null) {
            builder.source(net.kyori.adventure.text.Component.text(source));
        }

        banService.add(builder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pardon() {
        setBanned(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWhitelisted() {
        Optional<WhitelistService> whitelistService = Sponge.serviceProvider().provide(WhitelistService.class);
        if (whitelistService.isEmpty()) {
            return false;
        }
        return whitelistService.get().isWhitelisted(user.profile()).join();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWhitelisted(boolean whitelisted) {
        Optional<WhitelistService> whitelistServiceOpt = Sponge.serviceProvider().provide(WhitelistService.class);
        if (whitelistServiceOpt.isEmpty()) {
            return;
        }
        WhitelistService whitelistService = whitelistServiceOpt.get();

        if (whitelisted) {
            whitelistService.addProfile(user.profile());
        } else {
            whitelistService.removeProfile(user.profile());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOp() {
        // Sponge doesn't have a traditional op system, check for admin permission
        return user.hasPermission("minecraft.admin");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOp(boolean op) {
        // Sponge doesn't have a traditional op system
        // This would require a permission plugin integration
        // For now, this is a no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Integer> getStatistic(@NotNull String statistic) {
        return CompletableFuture.supplyAsync(() -> {
            // Try to parse the statistic from the string
            // Sponge uses a registry for statistics
            Optional<Statistic> stat = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.STATISTIC)
                    .findValue(org.spongepowered.api.ResourceKey.resolve(statistic));

            if (stat.isEmpty()) {
                return 0;
            }

            return user.get(Keys.STATISTICS)
                    .map(stats -> stats.getOrDefault(stat.get(), 0L).intValue())
                    .orElse(0);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Void> incrementStatistic(@NotNull String statistic, int amount) {
        return CompletableFuture.runAsync(() -> {
            Optional<Statistic> stat = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.STATISTIC)
                    .findValue(org.spongepowered.api.ResourceKey.resolve(statistic));

            if (stat.isEmpty()) {
                return;
            }

            user.get(Keys.STATISTICS).ifPresent(stats -> {
                long current = stats.getOrDefault(stat.get(), 0L);
                stats.put(stat.get(), current + amount);
                user.offer(Keys.STATISTICS, stats);
            });
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) user;
    }

    /**
     * Returns the wrapped Sponge User.
     *
     * @return the Sponge User
     */
    @NotNull
    public User getUser() {
        return user;
    }

    /**
     * Returns a string representation of this offline player.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeOfflinePlayer{" +
                "uuid=" + getUniqueId() +
                ", name=" + getName().orElse("unknown") +
                ", online=" + isOnline() +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpongeOfflinePlayer other)) return false;
        return user.uniqueId().equals(other.user.uniqueId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return user.uniqueId().hashCode();
    }
}
