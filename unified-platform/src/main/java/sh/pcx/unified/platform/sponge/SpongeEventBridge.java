/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;

/**
 * Bridges Sponge events to the Unified event system.
 *
 * <p>This class listens for Sponge-specific events and translates them
 * into unified events that can be consumed by plugins using the unified API.
 *
 * <h2>Sponge Cause System</h2>
 * <p>Sponge uses a sophisticated cause tracking system that provides detailed
 * information about what triggered an event. This bridge extracts relevant
 * information from causes and contexts to populate unified events.
 *
 * <h2>Event Ordering</h2>
 * <p>By default, bridge listeners run at {@link Order#DEFAULT} priority.
 * This ensures that platform-specific modifications have been applied
 * before the unified event is fired.
 *
 * <h2>Lifecycle</h2>
 * <p>The event bridge must be registered with Sponge's event manager
 * during plugin initialization and unregistered during shutdown.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see org.spongepowered.api.event.Event
 * @see Cause
 */
public final class SpongeEventBridge {

    private final SpongePlatformProvider provider;
    private final PluginContainer pluginContainer;
    private boolean registered = false;

    /**
     * Creates a new SpongeEventBridge.
     *
     * @param provider        the platform provider
     * @param pluginContainer the plugin container for event registration
     * @since 1.0.0
     */
    public SpongeEventBridge(@NotNull SpongePlatformProvider provider,
                             @NotNull PluginContainer pluginContainer) {
        this.provider = provider;
        this.pluginContainer = pluginContainer;
    }

    /**
     * Registers this event bridge with the Sponge event manager.
     *
     * <p>This method should be called during plugin enable.
     *
     * @since 1.0.0
     */
    public void register() {
        if (registered) {
            return;
        }

        EventManager eventManager = Sponge.eventManager();
        eventManager.registerListeners(pluginContainer, this);
        registered = true;
    }

    /**
     * Unregisters this event bridge from the Sponge event manager.
     *
     * <p>This method should be called during plugin disable.
     *
     * @since 1.0.0
     */
    public void unregister() {
        if (!registered) {
            return;
        }

        Sponge.eventManager().unregisterListeners(this);
        registered = false;
    }

    /**
     * Checks if the event bridge is registered.
     *
     * @return true if registered with the event manager
     * @since 1.0.0
     */
    public boolean isRegistered() {
        return registered;
    }

    // ==================== Player Events ====================

    /**
     * Handles player join events.
     *
     * <p>This event is fired when a player successfully joins the server
     * and their data has been loaded.
     *
     * @param event the Sponge join event
     * @since 1.0.0
     */
    @Listener(order = Order.DEFAULT)
    public void onPlayerJoin(@NotNull ServerSideConnectionEvent.Join event) {
        ServerPlayer player = event.player();

        // The player is now cached in the provider
        SpongeUnifiedPlayer unifiedPlayer = provider.getOrCreatePlayer(player);

        // Fire unified player join event
        // UnifiedEventManager.call(new UnifiedPlayerJoinEvent(unifiedPlayer));
    }

    /**
     * Handles player disconnect events.
     *
     * <p>This event is fired when a player disconnects from the server.
     * The player cache is invalidated after unified event processing.
     *
     * @param event the Sponge disconnect event
     * @since 1.0.0
     */
    @Listener(order = Order.DEFAULT)
    public void onPlayerDisconnect(@NotNull ServerSideConnectionEvent.Disconnect event) {
        // In Sponge API 12, Disconnect event uses cause to get the player
        ServerPlayer player = event.cause().first(ServerPlayer.class).orElse(null);
        if (player == null) {
            return;
        }

        // Fire unified player quit event first
        // SpongeUnifiedPlayer unifiedPlayer = provider.getOrCreatePlayer(player);
        // UnifiedEventManager.call(new UnifiedPlayerQuitEvent(unifiedPlayer));

        // Then invalidate the cache
        provider.invalidatePlayer(player);
    }

    /**
     * Handles player respawn events.
     *
     * <p>This event is fired when a player respawns after death.
     *
     * @param event the Sponge respawn event
     * @since 1.0.0
     */
    @Listener(order = Order.DEFAULT)
    public void onPlayerRespawn(@NotNull RespawnPlayerEvent.Recreate event) {
        // In Sponge API 12, the entity() method gives us the new player
        ServerPlayer newPlayer = event.entity();

        // Cache the new player reference
        provider.getOrCreatePlayer(newPlayer);

        // Fire unified respawn event
        // UnifiedEventManager.call(new UnifiedPlayerRespawnEvent(...));
    }

    // ==================== World Events ====================

    /**
     * Handles world load events.
     *
     * <p>This event is fired when a world is loaded into memory.
     *
     * @param event the Sponge world load event
     * @since 1.0.0
     */
    @Listener(order = Order.DEFAULT)
    public void onWorldLoad(@NotNull LoadWorldEvent event) {
        ServerWorld world = event.world();
        SpongeUnifiedWorld unifiedWorld = provider.getOrCreateWorld(world);

        // Fire unified world load event
        // UnifiedEventManager.call(new UnifiedWorldLoadEvent(unifiedWorld));
    }

    /**
     * Handles world unload events.
     *
     * <p>This event is fired when a world is unloaded from memory.
     *
     * @param event the Sponge world unload event
     * @since 1.0.0
     */
    @Listener(order = Order.DEFAULT)
    public void onWorldUnload(@NotNull UnloadWorldEvent event) {
        ServerWorld world = event.world();

        // Fire unified world unload event first
        // SpongeUnifiedWorld unifiedWorld = provider.getOrCreateWorld(world);
        // UnifiedEventManager.call(new UnifiedWorldUnloadEvent(unifiedWorld));

        // Then invalidate the cache
        provider.invalidateWorld(world);
    }

    // ==================== Cause Utilities ====================

    /**
     * Extracts the root cause from a Sponge event cause.
     *
     * @param cause the Sponge cause
     * @return the root cause object, or null if none
     * @since 1.0.0
     */
    @Nullable
    public static Object getRootCause(@NotNull Cause cause) {
        return cause.root();
    }

    /**
     * Extracts a player from an event cause if one exists.
     *
     * @param cause the Sponge cause
     * @return the player, or null if no player in cause
     * @since 1.0.0
     */
    @Nullable
    public static ServerPlayer getPlayerFromCause(@NotNull Cause cause) {
        return cause.first(ServerPlayer.class).orElse(null);
    }

    /**
     * Checks if an event was caused by a player.
     *
     * @param cause the Sponge cause
     * @return true if a player is in the cause chain
     * @since 1.0.0
     */
    public static boolean isCausedByPlayer(@NotNull Cause cause) {
        return cause.first(ServerPlayer.class).isPresent();
    }

    /**
     * Checks if an event was caused by a plugin.
     *
     * @param cause the Sponge cause
     * @return true if a plugin container is in the cause chain
     * @since 1.0.0
     */
    public static boolean isCausedByPlugin(@NotNull Cause cause) {
        return cause.first(PluginContainer.class).isPresent();
    }

    /**
     * Gets the plugin container from a cause if present.
     *
     * @param cause the Sponge cause
     * @return the plugin container, or null if none
     * @since 1.0.0
     */
    @Nullable
    public static PluginContainer getPluginFromCause(@NotNull Cause cause) {
        return cause.first(PluginContainer.class).orElse(null);
    }

    /**
     * Creates a cause with the given root object.
     *
     * @param root the root cause object
     * @return a new Cause instance
     * @since 1.0.0
     */
    @NotNull
    public static Cause createCause(@NotNull Object root) {
        return Cause.of(EventContext.empty(), root);
    }

    /**
     * Creates a cause with a player and plugin context.
     *
     * @param player    the player involved
     * @param container the plugin container
     * @return a new Cause instance
     * @since 1.0.0
     */
    @NotNull
    public static Cause createPlayerCause(@NotNull ServerPlayer player,
                                          @NotNull PluginContainer container) {
        EventContext context = EventContext.builder()
                .add(EventContextKeys.PLUGIN, container)
                .build();
        return Cause.of(context, player);
    }

    /**
     * Returns a string representation of this event bridge.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeEventBridge{" +
                "registered=" + registered +
                ", plugin=" + pluginContainer.metadata().id() +
                '}';
    }
}
