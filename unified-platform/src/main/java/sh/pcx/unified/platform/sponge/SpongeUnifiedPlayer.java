/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.PlayerSession;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Sponge implementation of the {@link UnifiedPlayer} interface.
 *
 * <p>This class wraps Sponge's {@link ServerPlayer} object to provide full player
 * functionality through the unified API. It extends {@link SpongeOfflinePlayer}
 * to inherit offline player functionality.
 *
 * <h2>Adventure Integration</h2>
 * <p>Sponge natively supports Adventure components, so this implementation
 * directly delegates messaging operations to the underlying ServerPlayer
 * which implements {@link net.kyori.adventure.audience.Audience}.
 *
 * <h2>Session Management</h2>
 * <p>Each online player has an associated {@link SpongePlayerSession} that
 * stores transient session data.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are thread-safe. Write operations should typically
 * be performed on the main thread using the scheduler.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer
 * @see SpongeOfflinePlayer
 */
public final class SpongeUnifiedPlayer extends SpongeOfflinePlayer implements UnifiedPlayer {

    private final ServerPlayer player;
    private final SpongePlayerSession session;

    /**
     * Creates a new SpongeUnifiedPlayer wrapping the given ServerPlayer.
     *
     * @param player   the Sponge ServerPlayer to wrap
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeUnifiedPlayer(@NotNull ServerPlayer player, @NotNull SpongePlatformProvider provider) {
        super(player.user(), provider);
        this.player = player;
        this.session = new SpongePlayerSession(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Component getDisplayName() {
        return player.displayName().get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayName(@Nullable Component displayName) {
        if (displayName == null) {
            player.displayName().set(Component.text(player.name()));
        } else {
            player.displayName().set(displayName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        ServerLocation loc = player.serverLocation();
        Vector3d rotation = player.rotation();
        return new UnifiedLocation(
                provider.getOrCreateWorld(loc.world()),
                loc.x(),
                loc.y(),
                loc.z(),
                (float) rotation.y(), // yaw
                (float) rotation.x()  // pitch
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return provider.getOrCreateWorld(player.world());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedLocation location) {
        return CompletableFuture.supplyAsync(() -> {
            if (location.world() == null) {
                return false;
            }

            SpongeUnifiedWorld spongeWorld = (SpongeUnifiedWorld) location.world();
            ServerWorld serverWorld = spongeWorld.getHandle();
            ServerLocation serverLocation = ServerLocation.of(
                    serverWorld,
                    location.x(),
                    location.y(),
                    location.z()
            );

            return player.setLocation(serverLocation);
        });
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
        return player.health().get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHealth(double health) {
        player.health().set(health);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxHealth() {
        return player.maxHealth().get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFoodLevel() {
        return player.foodLevel().get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFoodLevel(int level) {
        player.foodLevel().set(level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLevel() {
        return player.get(Keys.EXPERIENCE_LEVEL).orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLevel(int level) {
        player.offer(Keys.EXPERIENCE_LEVEL, level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getExp() {
        int expSinceLevel = player.get(Keys.EXPERIENCE_SINCE_LEVEL).orElse(0);
        int expToNextLevel = player.get(Keys.EXPERIENCE_FROM_START_OF_LEVEL).orElse(1);
        if (expToNextLevel == 0) {
            return 0f;
        }
        return (float) expSinceLevel / (float) expToNextLevel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExp(float exp) {
        int expToNextLevel = player.get(Keys.EXPERIENCE_FROM_START_OF_LEVEL).orElse(0);
        player.offer(Keys.EXPERIENCE_SINCE_LEVEL, (int) (exp * expToNextLevel));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalExperience() {
        return player.get(Keys.EXPERIENCE).orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTotalExperience(int exp) {
        player.offer(Keys.EXPERIENCE, exp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void giveExp(int amount) {
        int current = getTotalExperience();
        setTotalExperience(current + amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public GameMode getGameMode() {
        org.spongepowered.api.entity.living.player.gamemode.GameMode spongeMode =
                player.gameMode().get();

        if (spongeMode.equals(GameModes.SURVIVAL.get())) {
            return GameMode.SURVIVAL;
        } else if (spongeMode.equals(GameModes.CREATIVE.get())) {
            return GameMode.CREATIVE;
        } else if (spongeMode.equals(GameModes.ADVENTURE.get())) {
            return GameMode.ADVENTURE;
        } else if (spongeMode.equals(GameModes.SPECTATOR.get())) {
            return GameMode.SPECTATOR;
        }
        return GameMode.SURVIVAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGameMode(@NotNull GameMode gameMode) {
        org.spongepowered.api.entity.living.player.gamemode.GameMode spongeMode = switch (gameMode) {
            case SURVIVAL -> GameModes.SURVIVAL.get();
            case CREATIVE -> GameModes.CREATIVE.get();
            case ADVENTURE -> GameModes.ADVENTURE.get();
            case SPECTATOR -> GameModes.SPECTATOR.get();
        };
        player.gameMode().set(spongeMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFlying() {
        return player.get(Keys.IS_FLYING).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlying(boolean flying) {
        player.offer(Keys.IS_FLYING, flying);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getAllowFlight() {
        return player.get(Keys.CAN_FLY).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllowFlight(boolean allow) {
        player.offer(Keys.CAN_FLY, allow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSneaking() {
        return player.get(Keys.IS_SNEAKING).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSprinting() {
        return player.get(Keys.IS_SPRINTING).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Locale getLocale() {
        return player.locale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPing() {
        // Sponge doesn't expose ping/latency directly on ServerSideConnection
        // Return -1 as unknown
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getClientBrand() {
        // Sponge may not expose client brand directly
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getAddress() {
        return player.connection().address().getAddress().getHostAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void kick(@NotNull Component reason) {
        player.kick(reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void kick() {
        player.kick();
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
    @NotNull
    public UnifiedItemStack getItemInMainHand() {
        ItemStack spongeItem = player.itemInHand(HandTypes.MAIN_HAND);
        return new SpongeUnifiedItemStack(spongeItem, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setItemInMainHand(@Nullable UnifiedItemStack item) {
        if (item == null || item.isEmpty()) {
            player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.empty());
        } else {
            player.setItemInHand(HandTypes.MAIN_HAND, item.getHandle());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedItemStack getItemInOffHand() {
        ItemStack spongeItem = player.itemInHand(HandTypes.OFF_HAND);
        return new SpongeUnifiedItemStack(spongeItem, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setItemInOffHand(@Nullable UnifiedItemStack item) {
        if (item == null || item.isEmpty()) {
            player.setItemInHand(HandTypes.OFF_HAND, ItemStack.empty());
        } else {
            player.setItemInHand(HandTypes.OFF_HAND, item.getHandle());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean giveItem(@NotNull UnifiedItemStack item) {
        ItemStack spongeItem = item.getHandle();
        PlayerInventory inventory = player.inventory();

        // Try to add to inventory
        var result = inventory.primary().offer(spongeItem);

        if (result.rejectedItems().isEmpty()) {
            return true;
        }

        // Drop items that couldn't be added
        for (var rejectedSnapshot : result.rejectedItems()) {
            // Create item stack from snapshot and drop it
            ItemStack rejectedItem = rejectedSnapshot.createStack();
            org.spongepowered.api.entity.Item itemEntity = player.world().createEntity(
                    org.spongepowered.api.entity.EntityTypes.ITEM.get(),
                    player.position()
            );
            itemEntity.offer(org.spongepowered.api.data.Keys.ITEM_STACK_SNAPSHOT, rejectedSnapshot);
            player.world().spawnEntity(itemEntity);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openInventory() {
        player.openInventory(player.inventory());
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
        try {
            return Sponge.server().commandManager()
                    .process(player, command)
                    .isSuccess();
        } catch (org.spongepowered.api.command.exception.CommandException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPluginMessage(@NotNull String channel, byte @NotNull [] data) {
        // Sponge's channel API requires plugin container for channel creation
        // Without it, we cannot send plugin messages
        // This is a limitation of using the service provider pattern without a plugin context
        // Log a warning and skip the message
        org.slf4j.LoggerFactory.getLogger(SpongeUnifiedPlayer.class)
                .warn("Cannot send plugin message on Sponge without plugin container context");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playSound(@NotNull String sound, float volume, float pitch) {
        ResourceKey soundKey = ResourceKey.resolve(sound);
        Optional<SoundType> soundType = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.SOUND_TYPE).findValue(soundKey);

        soundType.ifPresent(type -> player.playSound(
                net.kyori.adventure.sound.Sound.sound(type, net.kyori.adventure.sound.Sound.Source.MASTER, volume, pitch)
        ));
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

    /**
     * Returns the wrapped Sponge ServerPlayer.
     *
     * @return the Sponge ServerPlayer
     */
    @NotNull
    public ServerPlayer getServerPlayer() {
        return player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        // Online player is always present for SpongeUnifiedPlayer
        return Optional.of(this);
    }

    // ==================== Adventure Audience Methods ====================

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(@NotNull Component message) {
        player.sendMessage(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(@NotNull Component message, @NotNull MessageType type) {
        player.sendMessage(message, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendActionBar(@NotNull Component message) {
        player.sendActionBar(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPlayerListHeaderAndFooter(@NotNull Component header, @NotNull Component footer) {
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTitle(@NotNull net.kyori.adventure.title.Title title) {
        player.showTitle(title);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearTitle() {
        player.clearTitle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetTitle() {
        player.resetTitle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showBossBar(@NotNull net.kyori.adventure.bossbar.BossBar bar) {
        player.showBossBar(bar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hideBossBar(@NotNull net.kyori.adventure.bossbar.BossBar bar) {
        player.hideBossBar(bar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playSound(@NotNull net.kyori.adventure.sound.Sound sound) {
        player.playSound(sound);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playSound(@NotNull net.kyori.adventure.sound.Sound sound, double x, double y, double z) {
        player.playSound(sound, x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopSound(@NotNull net.kyori.adventure.sound.SoundStop stop) {
        player.stopSound(stop);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public net.kyori.adventure.pointer.Pointers pointers() {
        return player.pointers();
    }

    /**
     * Returns a string representation of this player.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeUnifiedPlayer{" +
                "uuid=" + getUniqueId() +
                ", name=" + getName().orElse("unknown") +
                ", world=" + player.world().key().asString() +
                '}';
    }
}
