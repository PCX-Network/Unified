/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Offline player wrapper for Folia.
 *
 * <p>This class wraps a Bukkit OfflinePlayer object and provides access
 * to offline player data. Operations that access player files are
 * performed asynchronously to avoid blocking region threads.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see OfflineUnifiedPlayer
 * @see FoliaUnifiedPlayer
 */
public final class FoliaOfflineUnifiedPlayer implements OfflineUnifiedPlayer {

    private static final Logger LOGGER = Logger.getLogger(FoliaOfflineUnifiedPlayer.class.getName());

    /**
     * The underlying Bukkit OfflinePlayer object.
     */
    private final Object bukkitOfflinePlayer;

    /**
     * The platform provider.
     */
    private final FoliaPlatformProvider provider;

    /**
     * Cached UUID.
     */
    private final UUID uuid;

    /**
     * Constructs a new FoliaOfflineUnifiedPlayer.
     *
     * @param bukkitOfflinePlayer the Bukkit OfflinePlayer object
     * @param provider the platform provider
     * @since 1.0.0
     */
    public FoliaOfflineUnifiedPlayer(@NotNull Object bukkitOfflinePlayer,
                                      @NotNull FoliaPlatformProvider provider) {
        this.bukkitOfflinePlayer = bukkitOfflinePlayer;
        this.provider = provider;

        try {
            this.uuid = (UUID) bukkitOfflinePlayer.getClass().getMethod("getUniqueId")
                    .invoke(bukkitOfflinePlayer);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OfflinePlayer object", e);
        }
    }

    @Override
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    @NotNull
    public Optional<String> getName() {
        try {
            String name = (String) bukkitOfflinePlayer.getClass().getMethod("getName")
                    .invoke(bukkitOfflinePlayer);
            return Optional.ofNullable(name);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isOnline() {
        try {
            return (boolean) bukkitOfflinePlayer.getClass().getMethod("isOnline")
                    .invoke(bukkitOfflinePlayer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        if (!isOnline()) {
            return Optional.empty();
        }

        try {
            Object player = bukkitOfflinePlayer.getClass().getMethod("getPlayer")
                    .invoke(bukkitOfflinePlayer);
            if (player != null) {
                return Optional.of(provider.wrapPlayer(player));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to get online player", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasPlayedBefore() {
        try {
            return (boolean) bukkitOfflinePlayer.getClass().getMethod("hasPlayedBefore")
                    .invoke(bukkitOfflinePlayer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @NotNull
    public Optional<Instant> getFirstPlayed() {
        try {
            long time = (long) bukkitOfflinePlayer.getClass().getMethod("getFirstPlayed")
                    .invoke(bukkitOfflinePlayer);
            if (time > 0) {
                return Optional.of(Instant.ofEpochMilli(time));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to get first played", e);
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    public Optional<Instant> getLastSeen() {
        if (isOnline()) {
            return Optional.of(Instant.now());
        }

        try {
            long time = (long) bukkitOfflinePlayer.getClass().getMethod("getLastSeen")
                    .invoke(bukkitOfflinePlayer);
            if (time > 0) {
                return Optional.of(Instant.ofEpochMilli(time));
            }
        } catch (NoSuchMethodException e) {
            // Fallback for older versions
            try {
                long time = (long) bukkitOfflinePlayer.getClass().getMethod("getLastPlayed")
                        .invoke(bukkitOfflinePlayer);
                if (time > 0) {
                    return Optional.of(Instant.ofEpochMilli(time));
                }
            } catch (Exception ex) {
                // Ignore
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to get last seen", e);
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getLastLocation() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object location = bukkitOfflinePlayer.getClass().getMethod("getLastLocation")
                        .invoke(bukkitOfflinePlayer);
                if (location != null) {
                    return Optional.of(convertLocation(location));
                }
            } catch (NoSuchMethodException e) {
                // Method not available in older versions
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to get last location", e);
            }
            return Optional.empty();
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getBedSpawnLocation() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object location = bukkitOfflinePlayer.getClass().getMethod("getBedSpawnLocation")
                        .invoke(bukkitOfflinePlayer);
                if (location != null) {
                    return Optional.of(convertLocation(location));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to get bed spawn location", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public boolean isBanned() {
        try {
            return (boolean) bukkitOfflinePlayer.getClass().getMethod("isBanned")
                    .invoke(bukkitOfflinePlayer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setBanned(boolean banned) {
        // Use BanList for proper ban management
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object banListClass = Class.forName("org.bukkit.BanList$Type").getField("NAME").get(null);
            Object banList = bukkitClass.getMethod("getBanList", Class.forName("org.bukkit.BanList$Type"))
                    .invoke(null, banListClass);

            String name = getName().orElse(null);
            if (name == null) return;

            if (banned) {
                banList.getClass().getMethod("addBan", String.class, String.class,
                        java.util.Date.class, String.class)
                        .invoke(banList, name, "Banned", null, null);
            } else {
                banList.getClass().getMethod("pardon", String.class).invoke(banList, name);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set ban status", e);
        }
    }

    @Override
    public void ban(@NotNull String reason, @Nullable Instant expiration, @Nullable String source) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object banListClass = Class.forName("org.bukkit.BanList$Type").getField("NAME").get(null);
            Object banList = bukkitClass.getMethod("getBanList", Class.forName("org.bukkit.BanList$Type"))
                    .invoke(null, banListClass);

            String name = getName().orElse(null);
            if (name == null) return;

            java.util.Date expirationDate = expiration != null ?
                    java.util.Date.from(expiration) : null;

            banList.getClass().getMethod("addBan", String.class, String.class,
                    java.util.Date.class, String.class)
                    .invoke(banList, name, reason, expirationDate, source);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to ban player", e);
        }
    }

    @Override
    public void pardon() {
        setBanned(false);
    }

    @Override
    public boolean isWhitelisted() {
        try {
            return (boolean) bukkitOfflinePlayer.getClass().getMethod("isWhitelisted")
                    .invoke(bukkitOfflinePlayer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setWhitelisted(boolean whitelisted) {
        try {
            bukkitOfflinePlayer.getClass().getMethod("setWhitelisted", boolean.class)
                    .invoke(bukkitOfflinePlayer, whitelisted);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set whitelist status", e);
        }
    }

    @Override
    public boolean isOp() {
        try {
            return (boolean) bukkitOfflinePlayer.getClass().getMethod("isOp")
                    .invoke(bukkitOfflinePlayer);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setOp(boolean op) {
        try {
            bukkitOfflinePlayer.getClass().getMethod("setOp", boolean.class)
                    .invoke(bukkitOfflinePlayer, op);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set op status", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> getStatistic(@NotNull String statistic) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Class<?> statisticClass = Class.forName("org.bukkit.Statistic");
                Object stat = statisticClass.getMethod("valueOf", String.class)
                        .invoke(null, statistic.toUpperCase());
                return (int) bukkitOfflinePlayer.getClass().getMethod("getStatistic", statisticClass)
                        .invoke(bukkitOfflinePlayer, stat);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to get statistic: " + statistic, e);
                return 0;
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> incrementStatistic(@NotNull String statistic, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                Class<?> statisticClass = Class.forName("org.bukkit.Statistic");
                Object stat = statisticClass.getMethod("valueOf", String.class)
                        .invoke(null, statistic.toUpperCase());

                // Get current value
                int current = (int) bukkitOfflinePlayer.getClass()
                        .getMethod("getStatistic", statisticClass)
                        .invoke(bukkitOfflinePlayer, stat);

                // Set new value
                bukkitOfflinePlayer.getClass()
                        .getMethod("setStatistic", statisticClass, int.class)
                        .invoke(bukkitOfflinePlayer, stat, current + amount);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to increment statistic: " + statistic, e);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getHandle() {
        return (T) bukkitOfflinePlayer;
    }

    /**
     * Converts a Bukkit Location to UnifiedLocation.
     */
    private UnifiedLocation convertLocation(Object bukkitLocation) throws Exception {
        Object world = bukkitLocation.getClass().getMethod("getWorld").invoke(bukkitLocation);
        double x = (double) bukkitLocation.getClass().getMethod("getX").invoke(bukkitLocation);
        double y = (double) bukkitLocation.getClass().getMethod("getY").invoke(bukkitLocation);
        double z = (double) bukkitLocation.getClass().getMethod("getZ").invoke(bukkitLocation);
        float yaw = (float) bukkitLocation.getClass().getMethod("getYaw").invoke(bukkitLocation);
        float pitch = (float) bukkitLocation.getClass().getMethod("getPitch").invoke(bukkitLocation);

        return new UnifiedLocation(provider.wrapWorld(world), x, y, z, yaw, pitch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FoliaOfflineUnifiedPlayer other)) return false;
        return uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return String.format("FoliaOfflineUnifiedPlayer[uuid=%s, name=%s]",
                uuid, getName().orElse("unknown"));
    }
}
