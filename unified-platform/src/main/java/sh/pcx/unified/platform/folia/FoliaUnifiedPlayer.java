/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.PlayerSession;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Folia-aware player wrapper implementation.
 *
 * <p>This class wraps a Bukkit Player object and provides thread-safe access
 * in Folia's multi-threaded environment. Operations that modify player state
 * are automatically scheduled on the entity's owning thread.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations (getters) are generally safe from any thread in Folia.
 * Write operations (setters) must be performed on the thread that owns the
 * player entity. This wrapper handles scheduling automatically when needed.
 *
 * <h2>Teleportation</h2>
 * <p>Teleportation in Folia requires special handling because the player may
 * move between regions. This implementation uses Folia's async teleport API.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer
 * @see FoliaPlayerSession
 */
public final class FoliaUnifiedPlayer implements UnifiedPlayer {

    private static final Logger LOGGER = Logger.getLogger(FoliaUnifiedPlayer.class.getName());

    /**
     * The underlying Bukkit Player object.
     */
    private final Object bukkitPlayer;

    /**
     * The platform provider for creating other wrappers.
     */
    private final FoliaPlatformProvider provider;

    /**
     * The player's session.
     */
    private final FoliaPlayerSession session;

    /**
     * The entity scheduler for this player.
     */
    private final FoliaEntityScheduler entityScheduler;

    /**
     * Cached UUID for performance.
     */
    private final UUID uuid;

    /**
     * Cached name for performance.
     */
    private volatile String cachedName;

    /**
     * Constructs a new FoliaUnifiedPlayer.
     *
     * @param bukkitPlayer the Bukkit Player object
     * @param provider the platform provider
     * @throws IllegalArgumentException if the player object is invalid
     * @since 1.0.0
     */
    public FoliaUnifiedPlayer(@NotNull Object bukkitPlayer, @NotNull FoliaPlatformProvider provider) {
        this.bukkitPlayer = bukkitPlayer;
        this.provider = provider;

        try {
            this.uuid = (UUID) bukkitPlayer.getClass().getMethod("getUniqueId").invoke(bukkitPlayer);
            this.cachedName = (String) bukkitPlayer.getClass().getMethod("getName").invoke(bukkitPlayer);

            // Get plugin for scheduler
            Object plugin = getPluginForScheduler();
            this.entityScheduler = plugin != null ? new FoliaEntityScheduler(plugin) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Bukkit Player object", e);
        }

        this.session = new FoliaPlayerSession(this);
    }

    /**
     * Gets a plugin for the entity scheduler.
     */
    private Object getPluginForScheduler() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object pluginManager = bukkitClass.getMethod("getPluginManager").invoke(null);
            Object[] plugins = (Object[]) pluginManager.getClass().getMethod("getPlugins").invoke(pluginManager);
            return plugins.length > 0 ? plugins[0] : null;
        } catch (Exception e) {
            return null;
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
        return Optional.of(cachedName);
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        try {
            Method displayNameMethod = bukkitPlayer.getClass().getMethod("displayName");
            return (Component) displayNameMethod.invoke(bukkitPlayer);
        } catch (Exception e) {
            return Component.text(cachedName);
        }
    }

    @Override
    public void setDisplayName(@Nullable Component displayName) {
        scheduleEntityTask(() -> {
            try {
                if (displayName == null) {
                    Method method = bukkitPlayer.getClass().getMethod("displayName", Component.class);
                    method.invoke(bukkitPlayer, (Object) null);
                } else {
                    Method method = bukkitPlayer.getClass().getMethod("displayName", Component.class);
                    method.invoke(bukkitPlayer, displayName);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set display name", e);
            }
        });
    }

    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        try {
            Object location = bukkitPlayer.getClass().getMethod("getLocation").invoke(bukkitPlayer);
            return convertLocation(location);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get player location", e);
        }
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        try {
            Object world = bukkitPlayer.getClass().getMethod("getWorld").invoke(bukkitPlayer);
            return provider.wrapWorld(world);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get player world", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedLocation location) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // Convert to Bukkit location
            Object bukkitLocation = toBukkitLocation(location);

            // Use Folia's async teleport
            Method teleportAsyncMethod = bukkitPlayer.getClass().getMethod(
                    "teleportAsync",
                    Class.forName("org.bukkit.Location")
            );

            CompletableFuture<Boolean> result = (CompletableFuture<Boolean>)
                    teleportAsyncMethod.invoke(bukkitPlayer, bukkitLocation);

            result.whenComplete((success, error) -> {
                if (error != null) {
                    future.completeExceptionally(error);
                } else {
                    future.complete(success);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedPlayer target) {
        return teleport(target.getLocation());
    }

    @Override
    public double getHealth() {
        return invokeDouble("getHealth", 20.0);
    }

    @Override
    public void setHealth(double health) {
        scheduleEntityTask(() -> invokeVoid("setHealth", new Class[]{double.class}, health));
    }

    @Override
    public double getMaxHealth() {
        try {
            Method getAttributeMethod = bukkitPlayer.getClass().getMethod("getAttribute",
                    Class.forName("org.bukkit.attribute.Attribute"));
            Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
            Object maxHealthAttr = attributeClass.getField("GENERIC_MAX_HEALTH").get(null);
            Object attribute = getAttributeMethod.invoke(bukkitPlayer, maxHealthAttr);
            if (attribute != null) {
                return (double) attribute.getClass().getMethod("getValue").invoke(attribute);
            }
        } catch (Exception e) {
            // Fallback
        }
        return 20.0;
    }

    @Override
    public int getFoodLevel() {
        return invokeInt("getFoodLevel", 20);
    }

    @Override
    public void setFoodLevel(int level) {
        scheduleEntityTask(() -> invokeVoid("setFoodLevel", new Class[]{int.class}, level));
    }

    @Override
    public int getLevel() {
        return invokeInt("getLevel", 0);
    }

    @Override
    public void setLevel(int level) {
        scheduleEntityTask(() -> invokeVoid("setLevel", new Class[]{int.class}, level));
    }

    @Override
    public float getExp() {
        return invokeFloat("getExp", 0.0f);
    }

    @Override
    public void setExp(float exp) {
        scheduleEntityTask(() -> invokeVoid("setExp", new Class[]{float.class}, exp));
    }

    @Override
    public int getTotalExperience() {
        return invokeInt("getTotalExperience", 0);
    }

    @Override
    public void setTotalExperience(int exp) {
        scheduleEntityTask(() -> invokeVoid("setTotalExperience", new Class[]{int.class}, exp));
    }

    @Override
    public void giveExp(int amount) {
        scheduleEntityTask(() -> invokeVoid("giveExp", new Class[]{int.class}, amount));
    }

    @Override
    @NotNull
    public GameMode getGameMode() {
        try {
            Object gameMode = bukkitPlayer.getClass().getMethod("getGameMode").invoke(bukkitPlayer);
            String name = gameMode.toString();
            return switch (name) {
                case "CREATIVE" -> GameMode.CREATIVE;
                case "ADVENTURE" -> GameMode.ADVENTURE;
                case "SPECTATOR" -> GameMode.SPECTATOR;
                default -> GameMode.SURVIVAL;
            };
        } catch (Exception e) {
            return GameMode.SURVIVAL;
        }
    }

    @Override
    public void setGameMode(@NotNull GameMode gameMode) {
        scheduleEntityTask(() -> {
            try {
                Class<?> gameModeClass = Class.forName("org.bukkit.GameMode");
                Object bukkitGameMode = gameModeClass.getField(gameMode.name()).get(null);
                bukkitPlayer.getClass().getMethod("setGameMode", gameModeClass)
                        .invoke(bukkitPlayer, bukkitGameMode);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set game mode", e);
            }
        });
    }

    @Override
    public boolean isFlying() {
        return invokeBoolean("isFlying", false);
    }

    @Override
    public void setFlying(boolean flying) {
        scheduleEntityTask(() -> invokeVoid("setFlying", new Class[]{boolean.class}, flying));
    }

    @Override
    public boolean getAllowFlight() {
        return invokeBoolean("getAllowFlight", false);
    }

    @Override
    public void setAllowFlight(boolean allow) {
        scheduleEntityTask(() -> invokeVoid("setAllowFlight", new Class[]{boolean.class}, allow));
    }

    @Override
    public boolean isSneaking() {
        return invokeBoolean("isSneaking", false);
    }

    @Override
    public boolean isSprinting() {
        return invokeBoolean("isSprinting", false);
    }

    @Override
    @NotNull
    public Locale getLocale() {
        try {
            Method localeMethod = bukkitPlayer.getClass().getMethod("locale");
            return (Locale) localeMethod.invoke(bukkitPlayer);
        } catch (Exception e) {
            return Locale.ENGLISH;
        }
    }

    @Override
    public int getPing() {
        return invokeInt("getPing", -1);
    }

    @Override
    @NotNull
    public Optional<String> getClientBrand() {
        try {
            Method brandMethod = bukkitPlayer.getClass().getMethod("getClientBrandName");
            String brand = (String) brandMethod.invoke(bukkitPlayer);
            return Optional.ofNullable(brand);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public String getAddress() {
        try {
            Object address = bukkitPlayer.getClass().getMethod("getAddress").invoke(bukkitPlayer);
            if (address != null) {
                Object inetAddress = address.getClass().getMethod("getAddress").invoke(address);
                return (String) inetAddress.getClass().getMethod("getHostAddress").invoke(inetAddress);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }

    @Override
    public void kick(@NotNull Component reason) {
        scheduleEntityTask(() -> {
            try {
                bukkitPlayer.getClass().getMethod("kick", Component.class)
                        .invoke(bukkitPlayer, reason);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to kick player", e);
            }
        });
    }

    @Override
    public void kick() {
        kick(Component.text("You have been kicked from the server."));
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return invokeBoolean("hasPermission", new Class[]{String.class}, false, permission);
    }

    @Override
    public boolean isOp() {
        return invokeBoolean("isOp", false);
    }

    @Override
    public void setOp(boolean op) {
        scheduleEntityTask(() -> invokeVoid("setOp", new Class[]{boolean.class}, op));
    }

    @Override
    @NotNull
    public UnifiedItemStack getItemInMainHand() {
        // TODO: Implement with UnifiedItemStack wrapper
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setItemInMainHand(@Nullable UnifiedItemStack item) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @NotNull
    public UnifiedItemStack getItemInOffHand() {
        // TODO: Implement
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setItemInOffHand(@Nullable UnifiedItemStack item) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean giveItem(@NotNull UnifiedItemStack item) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void openInventory() {
        scheduleEntityTask(() -> {
            try {
                invokeVoid("openInventory",
                        new Class[]{Class.forName("org.bukkit.inventory.Inventory")},
                        getPlayerInventory());
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Failed to open inventory - Inventory class not found", e);
            }
        });
    }

    @Override
    public void closeInventory() {
        scheduleEntityTask(() -> invokeVoid("closeInventory"));
    }

    @Override
    public boolean performCommand(@NotNull String command) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        scheduleEntityTask(() -> {
            try {
                boolean success = (boolean) bukkitPlayer.getClass()
                        .getMethod("performCommand", String.class)
                        .invoke(bukkitPlayer, command);
                result.complete(success);
            } catch (Exception e) {
                result.complete(false);
            }
        });
        return result.join();
    }

    @Override
    public void sendPluginMessage(@NotNull String channel, byte @NotNull [] data) {
        try {
            Object plugin = getPluginForScheduler();
            if (plugin != null) {
                bukkitPlayer.getClass()
                        .getMethod("sendPluginMessage",
                                Class.forName("org.bukkit.plugin.Plugin"),
                                String.class, byte[].class)
                        .invoke(bukkitPlayer, plugin, channel, data);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send plugin message", e);
        }
    }

    @Override
    public void playSound(@NotNull String sound, float volume, float pitch) {
        scheduleEntityTask(() -> {
            try {
                Object location = bukkitPlayer.getClass().getMethod("getLocation").invoke(bukkitPlayer);
                bukkitPlayer.getClass().getMethod("playSound",
                        Class.forName("org.bukkit.Location"),
                        String.class, float.class, float.class)
                        .invoke(bukkitPlayer, location, sound, volume, pitch);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to play sound", e);
            }
        });
    }

    @Override
    @NotNull
    public PlayerSession getSession() {
        return session;
    }

    @Override
    public boolean hasPlayedBefore() {
        return invokeBoolean("hasPlayedBefore", true);
    }

    @Override
    @NotNull
    public Optional<Instant> getFirstPlayed() {
        try {
            long time = (long) bukkitPlayer.getClass().getMethod("getFirstPlayed").invoke(bukkitPlayer);
            return time > 0 ? Optional.of(Instant.ofEpochMilli(time)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public Optional<Instant> getLastSeen() {
        return Optional.of(Instant.now()); // Online player, so now
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getLastLocation() {
        return CompletableFuture.completedFuture(Optional.of(getLocation()));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getBedSpawnLocation() {
        try {
            Object bedLocation = bukkitPlayer.getClass().getMethod("getBedSpawnLocation").invoke(bukkitPlayer);
            if (bedLocation != null) {
                return CompletableFuture.completedFuture(Optional.of(convertLocation(bedLocation)));
            }
        } catch (Exception e) {
            // Ignore
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public boolean isBanned() {
        return invokeBoolean("isBanned", false);
    }

    @Override
    public void setBanned(boolean banned) {
        // TODO: Implement through BanList
    }

    @Override
    public void ban(@NotNull String reason, @Nullable Instant expiration, @Nullable String source) {
        // TODO: Implement through BanList
    }

    @Override
    public void pardon() {
        // TODO: Implement through BanList
    }

    @Override
    public boolean isWhitelisted() {
        return invokeBoolean("isWhitelisted", false);
    }

    @Override
    public void setWhitelisted(boolean whitelisted) {
        invokeVoid("setWhitelisted", new Class[]{boolean.class}, whitelisted);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> getStatistic(@NotNull String statistic) {
        // TODO: Implement statistics
        return CompletableFuture.completedFuture(0);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> incrementStatistic(@NotNull String statistic, int amount) {
        // TODO: Implement statistics
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        return Optional.of(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getHandle() {
        return (T) bukkitPlayer;
    }

    // Audience implementation delegated to Bukkit Player

    @Override
    public void sendMessage(@NotNull Component message) {
        try {
            bukkitPlayer.getClass().getMethod("sendMessage", Component.class)
                    .invoke(bukkitPlayer, message);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to send message", e);
        }
    }

    /**
     * Checks if the player is still valid (online).
     *
     * @return true if the player is online
     * @since 1.0.0
     */
    public boolean isValid() {
        return invokeBoolean("isOnline", false);
    }

    /**
     * Schedules a task to run on the entity's owning thread.
     */
    private void scheduleEntityTask(Runnable task) {
        if (entityScheduler != null) {
            entityScheduler.executeOrSchedule(bukkitPlayer, task);
        } else {
            task.run(); // Fallback
        }
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

    /**
     * Converts UnifiedLocation to Bukkit Location.
     */
    private Object toBukkitLocation(UnifiedLocation location) throws Exception {
        Class<?> locationClass = Class.forName("org.bukkit.Location");
        Class<?> worldClass = Class.forName("org.bukkit.World");

        Object world = location.world() != null ? location.world().getHandle() : null;

        return locationClass.getConstructor(worldClass, double.class, double.class, double.class, float.class, float.class)
                .newInstance(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }

    /**
     * Gets the player's inventory.
     */
    private Object getPlayerInventory() {
        try {
            return bukkitPlayer.getClass().getMethod("getInventory").invoke(bukkitPlayer);
        } catch (Exception e) {
            return null;
        }
    }

    // Reflection helper methods

    private boolean invokeBoolean(String method, boolean defaultValue) {
        return invokeBoolean(method, new Class[0], defaultValue);
    }

    private boolean invokeBoolean(String method, Class<?>[] params, boolean defaultValue, Object... args) {
        try {
            Method m = bukkitPlayer.getClass().getMethod(method, params);
            return (boolean) m.invoke(bukkitPlayer, args);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int invokeInt(String method, int defaultValue) {
        try {
            return (int) bukkitPlayer.getClass().getMethod(method).invoke(bukkitPlayer);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double invokeDouble(String method, double defaultValue) {
        try {
            return (double) bukkitPlayer.getClass().getMethod(method).invoke(bukkitPlayer);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private float invokeFloat(String method, float defaultValue) {
        try {
            return (float) bukkitPlayer.getClass().getMethod(method).invoke(bukkitPlayer);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void invokeVoid(String method) {
        invokeVoid(method, new Class[0]);
    }

    private void invokeVoid(String method, Class<?>[] params, Object... args) {
        try {
            bukkitPlayer.getClass().getMethod(method, params).invoke(bukkitPlayer, args);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to invoke " + method, e);
        }
    }
}
