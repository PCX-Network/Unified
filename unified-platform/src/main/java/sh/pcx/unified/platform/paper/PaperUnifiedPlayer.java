/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.PlayerSession;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paper/Spigot implementation of {@link UnifiedPlayer}.
 *
 * <p>This class wraps a Bukkit {@link Player} and provides a unified API for
 * player operations. It implements both the UnifiedPlayer interface and the
 * Adventure {@link Audience} interface for native Adventure support on Paper.
 *
 * <h2>Adventure Support</h2>
 * <p>On Paper servers (which have native Adventure support), this class directly
 * delegates to the player's Audience methods. On Spigot servers, Adventure
 * components are converted to legacy format.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are thread-safe. Write operations should be performed on
 * the main server thread. Use the scheduler for thread-safe modifications.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer
 * @see Player
 */
public class PaperUnifiedPlayer extends PaperOfflinePlayer implements UnifiedPlayer {

    private final Player player;
    private final PaperPlayerSession session;

    /**
     * Creates a new PaperUnifiedPlayer wrapping the given Bukkit player.
     *
     * @param player   the Bukkit player to wrap
     * @param provider the platform provider for creating related wrappers
     * @since 1.0.0
     */
    public PaperUnifiedPlayer(@NotNull Player player, @NotNull PaperPlatformProvider provider) {
        super(player, provider);
        this.player = Objects.requireNonNull(player, "player");
        this.session = new PaperPlayerSession(player);
    }

    /**
     * Returns the underlying Bukkit player.
     *
     * @return the Bukkit player
     */
    @NotNull
    public Player getBukkitPlayer() {
        return player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Component getDisplayName() {
        // Paper has native Adventure support
        try {
            return player.displayName();
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot - convert legacy string
            return Component.text(player.getDisplayName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayName(@Nullable Component displayName) {
        try {
            player.displayName(displayName);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            if (displayName == null) {
                player.setDisplayName(player.getName());
            } else {
                player.setDisplayName(PaperConversions.toLegacy(displayName));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        return PaperConversions.toUnifiedLocation(player.getLocation(), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return provider.getOrCreateWorld(player.getWorld());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedLocation location) {
        Location bukkitLocation = PaperConversions.toBukkitLocation(location);

        // Paper supports async teleportation
        try {
            return player.teleportAsync(bukkitLocation);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot - must run on main thread
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            if (Bukkit.isPrimaryThread()) {
                future.complete(player.teleport(bukkitLocation));
            } else {
                Bukkit.getScheduler().runTask(
                        Bukkit.getPluginManager().getPlugins()[0],
                        () -> future.complete(player.teleport(bukkitLocation))
                );
            }
            return future;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedPlayer target) {
        return teleport(target.getLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getHealth() {
        return player.getHealth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHealth(double health) {
        player.setHealth(health);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxHealth() {
        // In Paper 1.21+, the attribute was renamed from GENERIC_MAX_HEALTH to MAX_HEALTH
        Attribute maxHealthAttribute = null;
        try {
            // Try the new naming convention first (1.21+)
            maxHealthAttribute = Attribute.valueOf("MAX_HEALTH");
        } catch (IllegalArgumentException e) {
            try {
                // Fall back to old naming convention (pre-1.21)
                maxHealthAttribute = Attribute.valueOf("GENERIC_MAX_HEALTH");
            } catch (IllegalArgumentException ex) {
                return 20.0;
            }
        }
        var attribute = player.getAttribute(maxHealthAttribute);
        return attribute != null ? attribute.getValue() : 20.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFoodLevel() {
        return player.getFoodLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFoodLevel(int level) {
        player.setFoodLevel(level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLevel() {
        return player.getLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLevel(int level) {
        player.setLevel(level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getExp() {
        return player.getExp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExp(float exp) {
        player.setExp(exp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalExperience() {
        return player.getTotalExperience();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTotalExperience(int exp) {
        player.setTotalExperience(exp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void giveExp(int amount) {
        player.giveExp(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public GameMode getGameMode() {
        return switch (player.getGameMode()) {
            case SURVIVAL -> GameMode.SURVIVAL;
            case CREATIVE -> GameMode.CREATIVE;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGameMode(@NotNull GameMode gameMode) {
        org.bukkit.GameMode bukkitMode = switch (gameMode) {
            case SURVIVAL -> org.bukkit.GameMode.SURVIVAL;
            case CREATIVE -> org.bukkit.GameMode.CREATIVE;
            case ADVENTURE -> org.bukkit.GameMode.ADVENTURE;
            case SPECTATOR -> org.bukkit.GameMode.SPECTATOR;
        };
        player.setGameMode(bukkitMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFlying() {
        return player.isFlying();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlying(boolean flying) {
        player.setFlying(flying);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getAllowFlight() {
        return player.getAllowFlight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllowFlight(boolean allow) {
        player.setAllowFlight(allow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSneaking() {
        return player.isSneaking();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSprinting() {
        return player.isSprinting();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Locale getLocale() {
        try {
            return player.locale();
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            String locale = player.getLocale();
            if (locale != null && !locale.isEmpty()) {
                String[] parts = locale.split("_");
                if (parts.length >= 2) {
                    return new Locale(parts[0], parts[1]);
                }
                return new Locale(parts[0]);
            }
            return Locale.ENGLISH;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPing() {
        return player.getPing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getClientBrand() {
        try {
            return Optional.ofNullable(player.getClientBrandName());
        } catch (NoSuchMethodError e) {
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getAddress() {
        var address = player.getAddress();
        if (address != null) {
            return address.getAddress().getHostAddress();
        }
        return "0.0.0.0";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void kick(@NotNull Component reason) {
        try {
            player.kick(reason);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            player.kickPlayer(PaperConversions.toLegacy(reason));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void kick() {
        kick(Component.text("You have been kicked from the server."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPermission(@NotNull String permission) {
        return player.hasPermission(permission);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOp() {
        return player.isOp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOp(boolean op) {
        player.setOp(op);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack getItemInMainHand() {
        return new PaperUnifiedItemStack(player.getInventory().getItemInMainHand());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setItemInMainHand(@Nullable UnifiedItemStack item) {
        ItemStack bukkitItem = item == null ? null : item.getHandle();
        player.getInventory().setItemInMainHand(bukkitItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack getItemInOffHand() {
        return new PaperUnifiedItemStack(player.getInventory().getItemInOffHand());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setItemInOffHand(@Nullable UnifiedItemStack item) {
        ItemStack bukkitItem = item == null ? null : item.getHandle();
        player.getInventory().setItemInOffHand(bukkitItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean giveItem(@NotNull UnifiedItemStack item) {
        ItemStack bukkitItem = item.getHandle();
        var remaining = player.getInventory().addItem(bukkitItem);

        if (!remaining.isEmpty()) {
            // Drop items that didn't fit
            for (ItemStack drop : remaining.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openInventory() {
        player.openInventory(player.getInventory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeInventory() {
        player.closeInventory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performCommand(@NotNull String command) {
        return player.performCommand(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPluginMessage(@NotNull String channel, byte @NotNull [] data) {
        player.sendPluginMessage(
                Bukkit.getPluginManager().getPlugins()[0],
                channel,
                data
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playSound(@NotNull String sound, float volume, float pitch) {
        try {
            // Try to parse as a Sound enum first
            Sound bukkitSound = Sound.valueOf(sound.toUpperCase().replace("MINECRAFT:", ""));
            player.playSound(player.getLocation(), bukkitSound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Fallback to string-based sound
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public PlayerSession getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) player;
    }

    // ==================== Adventure Audience Methods ====================

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(@NotNull Component message) {
        try {
            player.sendMessage(message);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            player.sendMessage(PaperConversions.toLegacy(message));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(@NotNull Component message, @NotNull MessageType type) {
        sendMessage(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendActionBar(@NotNull Component message) {
        try {
            player.sendActionBar(message);
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot - use title API
            player.sendTitle("", PaperConversions.toLegacy(message), 0, 20, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        return Optional.of(this);
    }

    /**
     * Returns a string representation of this player.
     *
     * @return a string containing the player's name and UUID
     */
    @Override
    public String toString() {
        return "PaperUnifiedPlayer{" +
                "name='" + player.getName() + '\'' +
                ", uuid=" + player.getUniqueId() +
                '}';
    }
}
