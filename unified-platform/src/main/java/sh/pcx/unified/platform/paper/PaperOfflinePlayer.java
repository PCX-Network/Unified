/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paper/Spigot implementation of {@link OfflineUnifiedPlayer}.
 *
 * <p>This class wraps a Bukkit {@link OfflinePlayer} and provides a unified API
 * for accessing persistent player data. It handles both online and offline players,
 * with online players being able to access more data directly.
 *
 * <h2>Data Access</h2>
 * <p>Many operations on offline players require loading data from disk or database.
 * These operations return CompletableFuture to allow for asynchronous loading.
 * On Paper servers with async chunk loading, these operations are truly asynchronous.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are thread-safe. Some write operations may need to be
 * performed on the main thread depending on the underlying Bukkit implementation.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see OfflineUnifiedPlayer
 * @see OfflinePlayer
 */
public class PaperOfflinePlayer implements OfflineUnifiedPlayer {

    protected final OfflinePlayer offlinePlayer;
    protected final PaperPlatformProvider provider;

    /**
     * Creates a new PaperOfflinePlayer wrapping the given Bukkit offline player.
     *
     * @param offlinePlayer the Bukkit offline player to wrap
     * @param provider      the platform provider for creating related wrappers
     * @since 1.0.0
     */
    public PaperOfflinePlayer(@NotNull OfflinePlayer offlinePlayer,
                               @NotNull PaperPlatformProvider provider) {
        this.offlinePlayer = Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /**
     * Returns the underlying Bukkit offline player.
     *
     * @return the Bukkit offline player
     */
    @NotNull
    public OfflinePlayer getBukkitOfflinePlayer() {
        return offlinePlayer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getUniqueId() {
        return offlinePlayer.getUniqueId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getName() {
        return Optional.ofNullable(offlinePlayer.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnline() {
        return offlinePlayer.isOnline();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            return Optional.of(provider.getOrCreatePlayer(player));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPlayedBefore() {
        return offlinePlayer.hasPlayedBefore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<Instant> getFirstPlayed() {
        long firstPlayed = offlinePlayer.getFirstPlayed();
        if (firstPlayed > 0) {
            return Optional.of(Instant.ofEpochMilli(firstPlayed));
        }
        return Optional.empty();
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
        long lastSeen = offlinePlayer.getLastSeen();
        if (lastSeen > 0) {
            return Optional.of(Instant.ofEpochMilli(lastSeen));
        }
        // Fallback to last played
        long lastPlayed = offlinePlayer.getLastPlayed();
        if (lastPlayed > 0) {
            return Optional.of(Instant.ofEpochMilli(lastPlayed));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getLastLocation() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location location = offlinePlayer.getLocation();
                if (location != null) {
                    return Optional.of(PaperConversions.toUnifiedLocation(location, provider));
                }
            } catch (Exception e) {
                // Location may not be available for some offline players
            }
            return Optional.empty();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getBedSpawnLocation() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location location = offlinePlayer.getBedSpawnLocation();
                if (location != null) {
                    return Optional.of(PaperConversions.toUnifiedLocation(location, provider));
                }
            } catch (Exception e) {
                // Bed spawn may not be available
            }
            return Optional.empty();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBanned() {
        return offlinePlayer.isBanned();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBanned(boolean banned) {
        if (banned) {
            ban("Banned by server", null, null);
        } else {
            pardon();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
    public void ban(@NotNull String reason, @Nullable Instant expiration, @Nullable String source) {
        Date expirationDate = expiration != null ? Date.from(expiration) : null;

        // Use the appropriate ban list based on available API
        try {
            // Try profile-based ban (Paper API)
            Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            BanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
            Object profile = Bukkit.class.getMethod("createProfile", UUID.class, String.class)
                    .invoke(null, offlinePlayer.getUniqueId(), offlinePlayer.getName());
            banList.addBan(profile, reason, expirationDate, source);
        } catch (Exception e) {
            // Fallback to name-based ban using reflection to avoid ambiguity
            try {
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                String name = offlinePlayer.getName();
                if (name != null) {
                    // Use reflection to call the correct method
                    java.lang.reflect.Method addBanMethod = BanList.class.getMethod("addBan",
                            Object.class, String.class, Date.class, String.class);
                    addBanMethod.invoke(banList, name, reason, expirationDate, source);
                }
            } catch (Exception ex) {
                // Last resort fallback
                offlinePlayer.getPlayer();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
    public void pardon() {
        try {
            // Try profile-based pardon (Paper API)
            Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            BanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
            Object profile = Bukkit.class.getMethod("createProfile", UUID.class, String.class)
                    .invoke(null, offlinePlayer.getUniqueId(), offlinePlayer.getName());
            banList.pardon(profile);
        } catch (Exception e) {
            // Fallback to name-based pardon using reflection to avoid ambiguity
            try {
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                String name = offlinePlayer.getName();
                if (name != null) {
                    // Use reflection to call the correct method
                    java.lang.reflect.Method pardonMethod = BanList.class.getMethod("pardon", Object.class);
                    pardonMethod.invoke(banList, name);
                }
            } catch (Exception ex) {
                // Last resort fallback - do nothing
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWhitelisted() {
        return offlinePlayer.isWhitelisted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWhitelisted(boolean whitelisted) {
        offlinePlayer.setWhitelisted(whitelisted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOp() {
        return offlinePlayer.isOp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOp(boolean op) {
        offlinePlayer.setOp(op);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Integer> getStatistic(@NotNull String statistic) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try to find the statistic
                Statistic stat = Statistic.valueOf(statistic.toUpperCase().replace("MINECRAFT:", ""));

                // For online players, get directly
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    return player.getStatistic(stat);
                }

                // For offline players, try the offline player method
                // Note: This may require the player data to be loaded
                return offlinePlayer.getStatistic(stat);
            } catch (IllegalArgumentException e) {
                // Unknown statistic
                return 0;
            } catch (Exception e) {
                // Error retrieving statistic
                return 0;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Void> incrementStatistic(@NotNull String statistic, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                Statistic stat = Statistic.valueOf(statistic.toUpperCase().replace("MINECRAFT:", ""));

                // Can only increment for online players
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    player.incrementStatistic(stat, amount);
                }
            } catch (IllegalArgumentException e) {
                // Unknown statistic - ignore
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) offlinePlayer;
    }

    /**
     * Checks equality based on player UUID.
     *
     * @param o the object to compare
     * @return true if the other object represents the same player
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperOfflinePlayer that)) return false;
        return offlinePlayer.getUniqueId().equals(that.offlinePlayer.getUniqueId());
    }

    /**
     * Returns a hash code based on the player UUID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return offlinePlayer.getUniqueId().hashCode();
    }

    /**
     * Returns a string representation of this offline player.
     *
     * @return a string containing the player's name and UUID
     */
    @Override
    public String toString() {
        return "PaperOfflinePlayer{" +
                "name='" + offlinePlayer.getName() + '\'' +
                ", uuid=" + offlinePlayer.getUniqueId() +
                ", online=" + offlinePlayer.isOnline() +
                '}';
    }
}
