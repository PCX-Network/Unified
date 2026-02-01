/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Bridges Bukkit events to the Unified API event system.
 *
 * <p>This class listens for Bukkit events and converts them to unified events,
 * allowing plugins using the Unified API to react to platform events in a
 * platform-agnostic way.
 *
 * <h2>Supported Events</h2>
 * <ul>
 *   <li>Player join/quit events</li>
 *   <li>World load/unload events</li>
 *   <li>Block events (break, place, interact)</li>
 *   <li>Entity events (damage, death, spawn)</li>
 *   <li>Chat events</li>
 *   <li>Command events</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All event handlers are called on the main server thread by Bukkit.
 * For Folia, region-specific events may be called on region threads.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Listener
 */
public final class PaperEventBridge implements Listener {

    private final PaperPlatformProvider provider;
    private final Logger logger;
    private boolean registered = false;

    /**
     * Creates a new PaperEventBridge.
     *
     * @param provider the platform provider for creating wrappers
     * @param logger   the logger for event bridge messages
     * @since 1.0.0
     */
    public PaperEventBridge(@NotNull PaperPlatformProvider provider, @NotNull Logger logger) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Registers this event bridge with Bukkit.
     *
     * @param plugin the plugin to register with
     * @since 1.0.0
     */
    public void register(@NotNull Plugin plugin) {
        if (registered) {
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
        logger.fine("PaperEventBridge registered");
    }

    /**
     * Unregisters this event bridge.
     *
     * @since 1.0.0
     */
    public void unregister() {
        if (!registered) {
            return;
        }

        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        WorldLoadEvent.getHandlerList().unregister(this);
        WorldUnloadEvent.getHandlerList().unregister(this);

        registered = false;
        logger.fine("PaperEventBridge unregistered");
    }

    /**
     * Checks if this bridge is registered.
     *
     * @return true if registered
     * @since 1.0.0
     */
    public boolean isRegistered() {
        return registered;
    }

    // ==================== Player Events ====================

    /**
     * Handles player join events.
     *
     * <p>Creates a session for the player and fires a unified join event.
     *
     * @param event the Bukkit join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Create or update the player wrapper
        PaperUnifiedPlayer unifiedPlayer = provider.getOrCreatePlayer(player);

        // Fire unified player join event
        // TODO: Implement unified event system
        // UnifiedPlayerJoinEvent unifiedEvent = new UnifiedPlayerJoinEvent(unifiedPlayer);
        // UnifiedEventBus.fire(unifiedEvent);

        logger.fine(() -> "Player joined: " + player.getName());
    }

    /**
     * Handles player quit events.
     *
     * <p>Invalidates the player's session and cache entry.
     *
     * @param event the Bukkit quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Fire unified player quit event before invalidating
        // TODO: Implement unified event system
        // PaperUnifiedPlayer unifiedPlayer = provider.getOrCreatePlayer(player);
        // UnifiedPlayerQuitEvent unifiedEvent = new UnifiedPlayerQuitEvent(unifiedPlayer);
        // UnifiedEventBus.fire(unifiedEvent);

        // Invalidate the player cache entry
        provider.invalidatePlayer(player);

        logger.fine(() -> "Player quit: " + player.getName());
    }

    // ==================== World Events ====================

    /**
     * Handles world load events.
     *
     * @param event the Bukkit world load event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(@NotNull WorldLoadEvent event) {
        // Create wrapper for the new world
        PaperUnifiedWorld unifiedWorld = provider.getOrCreateWorld(event.getWorld());

        // Fire unified world load event
        // TODO: Implement unified event system
        // UnifiedWorldLoadEvent unifiedEvent = new UnifiedWorldLoadEvent(unifiedWorld);
        // UnifiedEventBus.fire(unifiedEvent);

        logger.fine(() -> "World loaded: " + event.getWorld().getName());
    }

    /**
     * Handles world unload events.
     *
     * @param event the Bukkit world unload event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(@NotNull WorldUnloadEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Fire unified world unload event before invalidating
        // TODO: Implement unified event system
        // PaperUnifiedWorld unifiedWorld = provider.getOrCreateWorld(event.getWorld());
        // UnifiedWorldUnloadEvent unifiedEvent = new UnifiedWorldUnloadEvent(unifiedWorld);
        // UnifiedEventBus.fire(unifiedEvent);

        // Invalidate the world cache entry
        provider.invalidateWorld(event.getWorld());

        logger.fine(() -> "World unloaded: " + event.getWorld().getName());
    }

    // ==================== Block Events ====================

    // TODO: Add block event handlers
    // - BlockBreakEvent
    // - BlockPlaceEvent
    // - PlayerInteractEvent
    // - SignChangeEvent

    // ==================== Entity Events ====================

    // TODO: Add entity event handlers
    // - EntityDamageEvent
    // - EntityDeathEvent
    // - EntitySpawnEvent
    // - CreatureSpawnEvent

    // ==================== Chat Events ====================

    // TODO: Add chat event handlers
    // Paper: AsyncChatEvent
    // Spigot: AsyncPlayerChatEvent

    // ==================== Command Events ====================

    // TODO: Add command event handlers
    // - PlayerCommandPreprocessEvent
    // - ServerCommandEvent

    // ==================== Inventory Events ====================

    // TODO: Add inventory event handlers
    // - InventoryClickEvent
    // - InventoryCloseEvent
    // - InventoryOpenEvent

    /**
     * Returns a string representation of this event bridge.
     *
     * @return a string indicating registration status
     */
    @Override
    public String toString() {
        return "PaperEventBridge{" +
                "registered=" + registered +
                '}';
    }
}
