/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.event;

import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.testing.command.CommandResult;
import sh.pcx.unified.testing.player.MockPlayer;
import sh.pcx.unified.testing.server.MockServer;
import sh.pcx.unified.testing.world.MockBlock;
import sh.pcx.unified.testing.world.MockWorld;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Mock implementation of the plugin manager for event handling and command dispatch.
 *
 * <p>MockPluginManager manages event listeners, fires events, and handles
 * command execution for the mock server environment.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Event registration and dispatching</li>
 *   <li>Event collection for testing</li>
 *   <li>Command execution</li>
 *   <li>Tab completion</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockServer server = MockServer.start();
 * MockPluginManager pluginManager = server.getPluginManager();
 *
 * // Collect events
 * EventCollector<PlayerJoinEvent> collector = pluginManager.collectEvents(PlayerJoinEvent.class);
 *
 * // Add player triggers event
 * MockPlayer player = server.addPlayer("Steve");
 *
 * // Verify event
 * assertThat(collector.getEvents()).hasSize(1);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MockPluginManager {

    private final MockServer server;
    private final Map<Class<?>, List<Consumer<?>>> eventListeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, EventCollector<?>> eventCollectors = new ConcurrentHashMap<>();
    private final Map<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();
    private final Map<String, TabCompleter> tabCompleters = new ConcurrentHashMap<>();

    /**
     * Creates a new mock plugin manager.
     *
     * @param server the mock server
     */
    public MockPluginManager(@NotNull MockServer server) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
    }

    // ==================== Event Management ====================

    /**
     * Registers an event listener.
     *
     * @param <E>       the event type
     * @param eventType the event class
     * @param handler   the event handler
     */
    public <E> void registerEvent(
        @NotNull Class<E> eventType,
        @NotNull Consumer<E> handler
    ) {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(handler);
    }

    /**
     * Unregisters all event listeners for a specific event type.
     *
     * @param eventType the event class
     */
    public void unregisterEvent(@NotNull Class<?> eventType) {
        eventListeners.remove(eventType);
    }

    /**
     * Fires an event to all registered listeners.
     *
     * @param <E>   the event type
     * @param event the event to fire
     */
    @SuppressWarnings("unchecked")
    public <E> void callEvent(@NotNull E event) {
        Objects.requireNonNull(event, "event cannot be null");

        Class<?> eventClass = event.getClass();

        // Collect event if collector exists
        EventCollector<E> collector = (EventCollector<E>) eventCollectors.get(eventClass);
        if (collector != null) {
            collector.collect(event);
        }

        // Fire to listeners
        List<Consumer<?>> listeners = eventListeners.get(eventClass);
        if (listeners != null) {
            for (Consumer<?> listener : listeners) {
                try {
                    ((Consumer<E>) listener).accept(event);
                } catch (Exception e) {
                    System.err.println("Error handling event " + eventClass.getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // Also check parent classes
        Class<?> parent = eventClass.getSuperclass();
        while (parent != null && parent != Object.class) {
            List<Consumer<?>> parentListeners = eventListeners.get(parent);
            if (parentListeners != null) {
                for (Consumer<?> listener : parentListeners) {
                    try {
                        ((Consumer<E>) listener).accept(event);
                    } catch (Exception e) {
                        System.err.println("Error handling event: " + e.getMessage());
                    }
                }
            }
            parent = parent.getSuperclass();
        }
    }

    /**
     * Creates an event collector for testing.
     *
     * @param <E>       the event type
     * @param eventType the event class to collect
     * @return the event collector
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <E> EventCollector<E> collectEvents(@NotNull Class<E> eventType) {
        Objects.requireNonNull(eventType, "eventType cannot be null");

        return (EventCollector<E>) eventCollectors.computeIfAbsent(
            eventType,
            k -> new EventCollector<>(eventType)
        );
    }

    /**
     * Clears all event collectors.
     */
    public void clearEventCollectors() {
        eventCollectors.values().forEach(EventCollector::clear);
        eventCollectors.clear();
    }

    // ==================== Command Management ====================

    /**
     * Registers a command handler.
     *
     * @param command the command name
     * @param handler the command handler
     */
    public void registerCommand(
        @NotNull String command,
        @NotNull CommandHandler handler
    ) {
        Objects.requireNonNull(command, "command cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        commandHandlers.put(command.toLowerCase(), handler);
    }

    /**
     * Registers a tab completer for a command.
     *
     * @param command   the command name
     * @param completer the tab completer
     */
    public void registerTabCompleter(
        @NotNull String command,
        @NotNull TabCompleter completer
    ) {
        Objects.requireNonNull(command, "command cannot be null");
        Objects.requireNonNull(completer, "completer cannot be null");

        tabCompleters.put(command.toLowerCase(), completer);
    }

    /**
     * Executes a command.
     *
     * @param sender  the command sender (null for console)
     * @param command the command string
     * @return the command result
     */
    @NotNull
    public CommandResult executeCommand(@Nullable Object sender, @NotNull String command) {
        Objects.requireNonNull(command, "command cannot be null");

        String[] parts = command.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];

        CommandHandler handler = commandHandlers.get(commandName);
        if (handler == null) {
            return CommandResult.failure("Unknown command: " + commandName);
        }

        try {
            return handler.execute(sender, commandName, args);
        } catch (Exception e) {
            return CommandResult.failure("Error executing command: " + e.getMessage());
        }
    }

    /**
     * Gets tab completions for a command.
     *
     * @param sender  the command sender
     * @param command the partial command
     * @return the list of completions
     */
    @NotNull
    public List<String> tabComplete(@Nullable Object sender, @NotNull String command) {
        Objects.requireNonNull(command, "command cannot be null");

        String[] parts = command.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split(" ", -1) : new String[]{""};

        TabCompleter completer = tabCompleters.get(commandName);
        if (completer == null) {
            return Collections.emptyList();
        }

        try {
            return completer.complete(sender, commandName, args);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ==================== Built-in Event Firing ====================

    /**
     * Fires a player join event.
     *
     * @param player the player who joined
     */
    public void firePlayerJoinEvent(@NotNull MockPlayer player) {
        callEvent(new MockPlayerJoinEvent(player));
    }

    /**
     * Fires a player quit event.
     *
     * @param player the player who quit
     */
    public void firePlayerQuitEvent(@NotNull MockPlayer player) {
        callEvent(new MockPlayerQuitEvent(player));
    }

    /**
     * Fires a player chat event.
     *
     * @param player  the player who sent the message
     * @param message the message
     */
    public void firePlayerChatEvent(@NotNull MockPlayer player, @NotNull String message) {
        callEvent(new MockPlayerChatEvent(player, message));
    }

    /**
     * Fires a block damage event (left click).
     *
     * @param player the player
     * @param block  the block
     */
    public void fireBlockDamageEvent(@NotNull MockPlayer player, @NotNull MockBlock block) {
        callEvent(new MockBlockDamageEvent(player, block));
    }

    /**
     * Fires a player interact event (right click).
     *
     * @param player the player
     * @param block  the block
     */
    public void firePlayerInteractEvent(@NotNull MockPlayer player, @NotNull MockBlock block) {
        callEvent(new MockPlayerInteractEvent(player, block));
    }

    /**
     * Fires a player drop item event.
     *
     * @param player the player
     * @param item   the item dropped
     */
    public void firePlayerDropItemEvent(@NotNull MockPlayer player, @NotNull UnifiedItemStack item) {
        callEvent(new MockPlayerDropItemEvent(player, item));
    }

    /**
     * Fires a player damage event.
     *
     * @param attacker the attacking player
     * @param victim   the victim player
     */
    public void firePlayerDamageEvent(@NotNull MockPlayer attacker, @NotNull MockPlayer victim) {
        callEvent(new MockPlayerDamageEvent(attacker, victim));
    }

    /**
     * Fires a player death event.
     *
     * @param player the player who died
     */
    public void firePlayerDeathEvent(@NotNull MockPlayer player) {
        callEvent(new MockPlayerDeathEvent(player));
    }

    /**
     * Fires an explosion event.
     *
     * @param world    the world
     * @param location the explosion location
     * @param power    the explosion power
     */
    public void fireExplosionEvent(
        @NotNull MockWorld world,
        @NotNull UnifiedLocation location,
        float power
    ) {
        callEvent(new MockExplosionEvent(world, location, power));
    }

    /**
     * Fires a lightning strike event.
     *
     * @param world    the world
     * @param location the strike location
     * @param effect   whether it's just visual
     */
    public void fireLightningStrikeEvent(
        @NotNull MockWorld world,
        @NotNull UnifiedLocation location,
        boolean effect
    ) {
        callEvent(new MockLightningStrikeEvent(world, location, effect));
    }

    // ==================== Functional Interfaces ====================

    /**
     * Functional interface for command handlers.
     */
    @FunctionalInterface
    public interface CommandHandler {
        /**
         * Executes a command.
         *
         * @param sender  the command sender
         * @param command the command name
         * @param args    the command arguments
         * @return the command result
         */
        CommandResult execute(@Nullable Object sender, String command, String[] args);
    }

    /**
     * Functional interface for tab completers.
     */
    @FunctionalInterface
    public interface TabCompleter {
        /**
         * Gets tab completions.
         *
         * @param sender  the command sender
         * @param command the command name
         * @param args    the current arguments
         * @return the list of completions
         */
        List<String> complete(@Nullable Object sender, String command, String[] args);
    }

    // ==================== Mock Event Classes ====================

    /**
     * Mock player join event.
     */
    public record MockPlayerJoinEvent(MockPlayer player) implements MockEvent {}

    /**
     * Mock player quit event.
     */
    public record MockPlayerQuitEvent(MockPlayer player) implements MockEvent {}

    /**
     * Mock player chat event.
     */
    public record MockPlayerChatEvent(MockPlayer player, String message) implements MockEvent {}

    /**
     * Mock block damage event.
     */
    public record MockBlockDamageEvent(MockPlayer player, MockBlock block) implements MockEvent {}

    /**
     * Mock player interact event.
     */
    public record MockPlayerInteractEvent(MockPlayer player, MockBlock block) implements MockEvent {}

    /**
     * Mock player drop item event.
     */
    public record MockPlayerDropItemEvent(MockPlayer player, UnifiedItemStack item) implements MockEvent {}

    /**
     * Mock player damage event.
     */
    public record MockPlayerDamageEvent(MockPlayer attacker, MockPlayer victim) implements MockEvent {}

    /**
     * Mock player death event.
     */
    public record MockPlayerDeathEvent(MockPlayer player) implements MockEvent {}

    /**
     * Mock explosion event.
     */
    public record MockExplosionEvent(MockWorld world, UnifiedLocation location, float power) implements MockEvent {}

    /**
     * Mock lightning strike event.
     */
    public record MockLightningStrikeEvent(MockWorld world, UnifiedLocation location, boolean effect) implements MockEvent {}

    /**
     * Marker interface for mock events.
     */
    public interface MockEvent {}
}
