/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet;

import sh.pcx.unified.network.packet.event.PacketEvent;
import sh.pcx.unified.network.packet.listener.ListenerPriority;
import sh.pcx.unified.network.packet.listener.PacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Registry for packet listeners and handlers.
 *
 * <p>This class manages the registration and dispatch of packet listeners,
 * organizing them by packet type and priority. It provides efficient
 * lookup and invocation of listeners when packets are intercepted.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PacketRegistry registry = new PacketRegistry();
 *
 * // Register a listener for a specific packet type
 * registry.register(PacketType.PLAY_IN_USE_ENTITY, PacketPriority.NORMAL, event -> {
 *     // Handle entity interaction
 *     int entityId = event.getPacket().getIntegers().read(0);
 *     if (isProtectedEntity(entityId)) {
 *         event.setCancelled(true);
 *     }
 * });
 *
 * // Register a listener for multiple packet types
 * registry.register(
 *     Set.of(PacketType.PLAY_OUT_BLOCK_CHANGE, PacketType.PLAY_OUT_MULTI_BLOCK_CHANGE),
 *     PacketPriority.HIGH,
 *     event -> {
 *         // Handle block updates
 *     }
 * );
 *
 * // Unregister all listeners for a plugin
 * registry.unregisterAll(myPlugin);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Listeners can be registered and unregistered
 * from any thread, and dispatch operations are synchronized appropriately.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PacketListener
 * @see PacketPriority
 */
public class PacketRegistry {

    private final Map<PacketType, List<RegisteredListener>> listeners;
    private final Map<Object, List<RegisteredListener>> listenersByOwner;
    private final Object lock = new Object();

    /**
     * Constructs a new packet registry.
     *
     * @since 1.0.0
     */
    public PacketRegistry() {
        this.listeners = new ConcurrentHashMap<>();
        this.listenersByOwner = new ConcurrentHashMap<>();
    }

    /**
     * Registers a listener for a specific packet type.
     *
     * @param type     the packet type to listen for
     * @param priority the listener priority
     * @param handler  the packet handler
     * @return a registration handle for unregistering
     * @since 1.0.0
     */
    @NotNull
    public Registration register(
            @NotNull PacketType type,
            @NotNull PacketPriority priority,
            @NotNull Consumer<PacketEvent> handler
    ) {
        return register(null, type, priority, handler);
    }

    /**
     * Registers a listener for a specific packet type with an owner.
     *
     * @param owner    the listener owner (for bulk unregistration)
     * @param type     the packet type to listen for
     * @param priority the listener priority
     * @param handler  the packet handler
     * @return a registration handle for unregistering
     * @since 1.0.0
     */
    @NotNull
    public Registration register(
            @Nullable Object owner,
            @NotNull PacketType type,
            @NotNull PacketPriority priority,
            @NotNull Consumer<PacketEvent> handler
    ) {
        return register(owner, Set.of(type), priority, handler);
    }

    /**
     * Registers a listener for multiple packet types.
     *
     * @param types    the packet types to listen for
     * @param priority the listener priority
     * @param handler  the packet handler
     * @return a registration handle for unregistering
     * @since 1.0.0
     */
    @NotNull
    public Registration register(
            @NotNull Set<PacketType> types,
            @NotNull PacketPriority priority,
            @NotNull Consumer<PacketEvent> handler
    ) {
        return register(null, types, priority, handler);
    }

    /**
     * Registers a listener for multiple packet types with an owner.
     *
     * @param owner    the listener owner (for bulk unregistration)
     * @param types    the packet types to listen for
     * @param priority the listener priority
     * @param handler  the packet handler
     * @return a registration handle for unregistering
     * @since 1.0.0
     */
    @NotNull
    public Registration register(
            @Nullable Object owner,
            @NotNull Set<PacketType> types,
            @NotNull PacketPriority priority,
            @NotNull Consumer<PacketEvent> handler
    ) {
        Objects.requireNonNull(types, "types cannot be null");
        Objects.requireNonNull(priority, "priority cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        RegisteredListener registered = new RegisteredListener(owner, types, priority, handler);

        synchronized (lock) {
            for (PacketType type : types) {
                listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                        .add(registered);
                sortListeners(type);
            }

            if (owner != null) {
                listenersByOwner.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>())
                        .add(registered);
            }
        }

        return new Registration(registered, this);
    }

    /**
     * Registers a packet listener instance.
     *
     * @param listener the listener to register
     * @return a registration handle for unregistering
     * @since 1.0.0
     */
    @NotNull
    public Registration register(@NotNull PacketListener listener) {
        return register(
                listener,
                listener.getPacketTypes(),
                convertPriority(listener.getPriority()),
                listener::onPacket
        );
    }

    /**
     * Converts a ListenerPriority to PacketPriority.
     *
     * @param priority the listener priority
     * @return the corresponding packet priority
     */
    private static PacketPriority convertPriority(@NotNull ListenerPriority priority) {
        return switch (priority) {
            case LOWEST -> PacketPriority.LOWEST;
            case LOW -> PacketPriority.LOW;
            case NORMAL -> PacketPriority.NORMAL;
            case HIGH -> PacketPriority.HIGH;
            case HIGHEST -> PacketPriority.HIGHEST;
            case MONITOR -> PacketPriority.MONITOR;
        };
    }

    /**
     * Unregisters a specific listener registration.
     *
     * @param registered the registered listener to remove
     * @since 1.0.0
     */
    public void unregister(@NotNull RegisteredListener registered) {
        synchronized (lock) {
            for (PacketType type : registered.types) {
                List<RegisteredListener> list = listeners.get(type);
                if (list != null) {
                    list.remove(registered);
                    if (list.isEmpty()) {
                        listeners.remove(type);
                    }
                }
            }

            if (registered.owner != null) {
                List<RegisteredListener> ownerList = listenersByOwner.get(registered.owner);
                if (ownerList != null) {
                    ownerList.remove(registered);
                    if (ownerList.isEmpty()) {
                        listenersByOwner.remove(registered.owner);
                    }
                }
            }
        }
    }

    /**
     * Unregisters all listeners owned by the specified owner.
     *
     * @param owner the listener owner
     * @since 1.0.0
     */
    public void unregisterAll(@NotNull Object owner) {
        synchronized (lock) {
            List<RegisteredListener> ownerListeners = listenersByOwner.remove(owner);
            if (ownerListeners != null) {
                for (RegisteredListener registered : ownerListeners) {
                    for (PacketType type : registered.types) {
                        List<RegisteredListener> list = listeners.get(type);
                        if (list != null) {
                            list.remove(registered);
                            if (list.isEmpty()) {
                                listeners.remove(type);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Unregisters all listeners.
     *
     * @since 1.0.0
     */
    public void unregisterAll() {
        synchronized (lock) {
            listeners.clear();
            listenersByOwner.clear();
        }
    }

    /**
     * Dispatches a packet event to all registered listeners.
     *
     * @param event the packet event
     * @since 1.0.0
     */
    public void dispatch(@NotNull PacketEvent event) {
        List<RegisteredListener> list = listeners.get(event.getPacket().getType());
        if (list == null || list.isEmpty()) {
            return;
        }

        for (RegisteredListener registered : list) {
            if (event.isCancelled() && registered.priority.isIgnoreCancelled()) {
                continue;
            }

            try {
                registered.handler.accept(event);
            } catch (Exception e) {
                handleListenerException(registered, event, e);
            }
        }
    }

    /**
     * Checks if there are any listeners for the specified packet type.
     *
     * @param type the packet type
     * @return true if there are listeners
     * @since 1.0.0
     */
    public boolean hasListeners(@NotNull PacketType type) {
        List<RegisteredListener> list = listeners.get(type);
        return list != null && !list.isEmpty();
    }

    /**
     * Returns the number of listeners for a packet type.
     *
     * @param type the packet type
     * @return the listener count
     * @since 1.0.0
     */
    public int getListenerCount(@NotNull PacketType type) {
        List<RegisteredListener> list = listeners.get(type);
        return list != null ? list.size() : 0;
    }

    /**
     * Returns all packet types with registered listeners.
     *
     * @return an unmodifiable set of packet types
     * @since 1.0.0
     */
    @NotNull
    public Set<PacketType> getRegisteredTypes() {
        return Collections.unmodifiableSet(listeners.keySet());
    }

    /**
     * Returns the total number of registered listeners.
     *
     * @return the total listener count
     * @since 1.0.0
     */
    public int getTotalListenerCount() {
        return listeners.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    private void sortListeners(PacketType type) {
        List<RegisteredListener> list = listeners.get(type);
        if (list != null && list.size() > 1) {
            list.sort(Comparator.comparingInt(l -> l.priority.ordinal()));
        }
    }

    private void handleListenerException(RegisteredListener listener, PacketEvent event, Exception e) {
        System.err.println("[PacketRegistry] Exception in packet listener for " +
                event.getPacket().getType() + ": " + e.getMessage());
        e.printStackTrace();
    }

    /**
     * Represents a registered packet listener.
     *
     * @since 1.0.0
     */
    public static final class RegisteredListener {
        private final Object owner;
        private final Set<PacketType> types;
        private final PacketPriority priority;
        private final Consumer<PacketEvent> handler;

        RegisteredListener(
                @Nullable Object owner,
                @NotNull Set<PacketType> types,
                @NotNull PacketPriority priority,
                @NotNull Consumer<PacketEvent> handler
        ) {
            this.owner = owner;
            this.types = Set.copyOf(types);
            this.priority = priority;
            this.handler = handler;
        }

        /**
         * Returns the listener owner.
         *
         * @return the owner, or null if none
         * @since 1.0.0
         */
        @Nullable
        public Object getOwner() {
            return owner;
        }

        /**
         * Returns the packet types this listener handles.
         *
         * @return the packet types
         * @since 1.0.0
         */
        @NotNull
        public Set<PacketType> getTypes() {
            return types;
        }

        /**
         * Returns the listener priority.
         *
         * @return the priority
         * @since 1.0.0
         */
        @NotNull
        public PacketPriority getPriority() {
            return priority;
        }
    }

    /**
     * Handle for managing a listener registration.
     *
     * @since 1.0.0
     */
    public static final class Registration implements AutoCloseable {
        private final RegisteredListener registered;
        private final PacketRegistry registry;
        private volatile boolean active = true;

        Registration(@NotNull RegisteredListener registered, @NotNull PacketRegistry registry) {
            this.registered = registered;
            this.registry = registry;
        }

        /**
         * Unregisters this listener.
         *
         * @since 1.0.0
         */
        public void unregister() {
            if (active) {
                active = false;
                registry.unregister(registered);
            }
        }

        /**
         * Checks if this registration is still active.
         *
         * @return true if active
         * @since 1.0.0
         */
        public boolean isActive() {
            return active;
        }

        @Override
        public void close() {
            unregister();
        }
    }
}
