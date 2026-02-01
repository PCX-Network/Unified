/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.server;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.PluginMeta;
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.service.ServiceRegistry;
import sh.pcx.unified.testing.command.CommandResult;
import sh.pcx.unified.testing.database.MockDatabase;
import sh.pcx.unified.testing.event.EventCollector;
import sh.pcx.unified.testing.event.MockPluginManager;
import sh.pcx.unified.testing.player.MockPlayer;
import sh.pcx.unified.testing.scheduler.MockScheduler;
import sh.pcx.unified.testing.service.MockServiceRegistry;
import sh.pcx.unified.testing.world.MockWorld;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Mock implementation of a Minecraft server for testing purposes.
 *
 * <p>MockServer provides a complete simulation of a Minecraft server environment,
 * allowing plugin tests to run without a real server. It supports player management,
 * world operations, event handling, command execution, and scheduled tasks.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Full server simulation with customizable properties</li>
 *   <li>Player creation and management</li>
 *   <li>World creation with block manipulation</li>
 *   <li>Event system with collection and assertion</li>
 *   <li>Command execution with result verification</li>
 *   <li>Scheduler with tick control</li>
 *   <li>Service registry mocking</li>
 *   <li>Database mocking</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Start mock server
 * MockServer server = MockServer.start();
 *
 * // Load your plugin
 * MyPlugin plugin = server.loadPlugin(MyPlugin.class);
 *
 * // Add a player
 * MockPlayer player = server.addPlayer("TestPlayer");
 *
 * // Simulate gameplay
 * player.performCommand("spawn");
 * server.advanceTicks(20);
 *
 * // Verify behavior
 * assertThat(player.getMessages()).contains("Teleported to spawn!");
 *
 * // Cleanup
 * server.stop();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockPlayer
 * @see MockWorld
 * @see MockScheduler
 */
public final class MockServer implements UnifiedServer {

    private static final Logger LOGGER = Logger.getLogger(MockServer.class.getName());
    private static volatile MockServer instance;

    private final Map<UUID, MockPlayer> players = new ConcurrentHashMap<>();
    private final Map<String, MockPlayer> playersByName = new ConcurrentHashMap<>();
    private final Map<UUID, MockPlayer> offlinePlayers = new ConcurrentHashMap<>();
    private final Map<String, MockWorld> worlds = new ConcurrentHashMap<>();
    private final Map<Class<? extends UnifiedPlugin>, UnifiedPlugin> loadedPlugins = new ConcurrentHashMap<>();

    private final MockPluginManager pluginManager;
    private final MockScheduler scheduler;
    private final MockServiceRegistry serviceRegistry;
    private final MockDatabase database;
    private final MockServerConfiguration config;

    private final Path serverFolder;
    private final Path pluginsFolder;
    private final Path worldsFolder;
    private final AtomicLong currentTick = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    private MockWorld defaultWorld;
    private Component motd = Component.text("A Minecraft Server");
    private int maxPlayers = 20;
    private boolean onlineMode = true;
    private boolean whitelistEnabled = false;
    private final Thread primaryThread;

    /**
     * Creates a new MockServer with default configuration.
     *
     * @param config the server configuration
     */
    private MockServer(@NotNull MockServerConfiguration config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.primaryThread = Thread.currentThread();

        try {
            this.serverFolder = Files.createTempDirectory("mock-server-");
            this.pluginsFolder = Files.createDirectories(serverFolder.resolve("plugins"));
            this.worldsFolder = Files.createDirectories(serverFolder.resolve("worlds"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp directories for MockServer", e);
        }

        this.pluginManager = new MockPluginManager(this);
        this.scheduler = new MockScheduler(this);
        this.serviceRegistry = new MockServiceRegistry();
        this.database = new MockDatabase();

        // Create default world
        this.defaultWorld = createWorld("world");
    }

    /**
     * Starts a new mock server with default configuration.
     *
     * @return the started mock server
     * @throws IllegalStateException if a mock server is already running
     */
    @NotNull
    public static MockServer start() {
        return start(MockServerConfiguration.builder().build());
    }

    /**
     * Starts a new mock server with custom configuration.
     *
     * @param config the server configuration
     * @return the started mock server
     * @throws IllegalStateException if a mock server is already running
     */
    @NotNull
    public static synchronized MockServer start(@NotNull MockServerConfiguration config) {
        if (instance != null && instance.running.get()) {
            throw new IllegalStateException("A MockServer instance is already running. Call stop() first.");
        }
        instance = new MockServer(config);
        LOGGER.info("MockServer started");
        return instance;
    }

    /**
     * Returns the current mock server instance.
     *
     * @return the current mock server, or null if not running
     */
    @Nullable
    public static MockServer getInstance() {
        return instance;
    }

    /**
     * Stops the mock server and cleans up resources.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        stopping.set(true);

        // Disable all plugins
        loadedPlugins.values().forEach(plugin -> {
            try {
                plugin.onDisable();
                plugin.setEnabled(false);
            } catch (Exception e) {
                LOGGER.warning("Error disabling plugin " + plugin.getName() + ": " + e.getMessage());
            }
        });

        // Cancel all scheduled tasks
        scheduler.cancelAllTasks();

        // Disconnect all players
        new ArrayList<>(players.values()).forEach(player -> player.disconnect());

        // Clear all data
        players.clear();
        playersByName.clear();
        worlds.clear();
        loadedPlugins.clear();

        // Cleanup temp directories
        try {
            Files.walk(serverFolder)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {}
                });
        } catch (Exception ignored) {}

        synchronized (MockServer.class) {
            if (instance == this) {
                instance = null;
            }
        }

        LOGGER.info("MockServer stopped");
    }

    /**
     * Resets the server state between tests while keeping it running.
     *
     * <p>This method:
     * <ul>
     *   <li>Disconnects all players</li>
     *   <li>Clears scheduled tasks</li>
     *   <li>Resets the tick counter</li>
     *   <li>Clears event collectors</li>
     *   <li>Resets worlds to their initial state</li>
     * </ul>
     */
    public void reset() {
        // Disconnect all players
        new ArrayList<>(players.values()).forEach(player -> player.disconnect());
        players.clear();
        playersByName.clear();

        // Reset scheduler
        scheduler.reset();
        currentTick.set(0);

        // Clear event collectors
        pluginManager.clearEventCollectors();

        // Reset worlds
        worlds.values().forEach(MockWorld::reset);

        // Reset database
        database.reset();

        LOGGER.fine("MockServer reset");
    }

    // ==================== Plugin Management ====================

    /**
     * Loads and enables a plugin.
     *
     * @param <T>         the plugin type
     * @param pluginClass the plugin class to load
     * @return the loaded plugin instance
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends UnifiedPlugin> T loadPlugin(@NotNull Class<T> pluginClass) {
        Objects.requireNonNull(pluginClass, "pluginClass cannot be null");

        if (loadedPlugins.containsKey(pluginClass)) {
            return (T) loadedPlugins.get(pluginClass);
        }

        try {
            T plugin = pluginClass.getDeclaredConstructor().newInstance();

            // Extract plugin metadata
            PluginMeta meta = extractPluginMeta(pluginClass);
            Path dataFolder = Files.createDirectories(pluginsFolder.resolve(meta.name()));

            // Initialize the plugin
            plugin.initialize(
                meta,
                Logger.getLogger(meta.name()),
                dataFolder,
                this,
                serviceRegistry
            );

            // Call lifecycle methods
            plugin.onLoad();
            plugin.setEnabled(true);
            plugin.onEnable();

            loadedPlugins.put(pluginClass, plugin);
            LOGGER.info("Loaded plugin: " + meta.name() + " v" + meta.version());

            return plugin;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load plugin: " + pluginClass.getName(), e);
        }
    }

    /**
     * Unloads a plugin.
     *
     * @param <T>         the plugin type
     * @param pluginClass the plugin class to unload
     */
    public <T extends UnifiedPlugin> void unloadPlugin(@NotNull Class<T> pluginClass) {
        UnifiedPlugin plugin = loadedPlugins.remove(pluginClass);
        if (plugin != null) {
            try {
                plugin.onDisable();
                plugin.setEnabled(false);
            } catch (Exception e) {
                LOGGER.warning("Error unloading plugin: " + e.getMessage());
            }
        }
    }

    /**
     * Gets a loaded plugin by its class.
     *
     * @param <T>         the plugin type
     * @param pluginClass the plugin class
     * @return an Optional containing the plugin if loaded
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends UnifiedPlugin> Optional<T> getPlugin(@NotNull Class<T> pluginClass) {
        return Optional.ofNullable((T) loadedPlugins.get(pluginClass));
    }

    private PluginMeta extractPluginMeta(Class<? extends UnifiedPlugin> pluginClass) {
        // Try to get from annotation or return defaults
        String name = pluginClass.getSimpleName().replace("Plugin", "");
        return new PluginMeta(
            name,
            "1.0.0-TEST",
            "Test plugin",
            List.of("TestAuthor"),
            "",
            List.of(),
            List.of(),
            "1.21"
        );
    }

    // ==================== Player Management ====================

    /**
     * Adds a new player to the server.
     *
     * @param name the player name
     * @return the created mock player
     */
    @NotNull
    public MockPlayer addPlayer(@NotNull String name) {
        return addPlayer(name, UUID.nameUUIDFromBytes(("MockPlayer:" + name).getBytes()));
    }

    /**
     * Adds a new player with a specific UUID.
     *
     * @param name the player name
     * @param uuid the player UUID
     * @return the created mock player
     */
    @NotNull
    public MockPlayer addPlayer(@NotNull String name, @NotNull UUID uuid) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(uuid, "uuid cannot be null");

        if (players.containsKey(uuid)) {
            throw new IllegalArgumentException("Player with UUID " + uuid + " already exists");
        }
        if (playersByName.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("Player with name " + name + " already exists");
        }

        MockPlayer player = new MockPlayer(this, name, uuid, defaultWorld);
        players.put(uuid, player);
        playersByName.put(name.toLowerCase(), player);
        offlinePlayers.put(uuid, player);

        // Fire join event
        pluginManager.firePlayerJoinEvent(player);

        return player;
    }

    /**
     * Adds a new player with configuration.
     *
     * @param configurator the player configurator
     * @return the created mock player
     */
    @NotNull
    public MockPlayer addPlayer(@NotNull Consumer<MockPlayer.Builder> configurator) {
        MockPlayer.Builder builder = MockPlayer.builder(this);
        configurator.accept(builder);
        MockPlayer player = builder.build();

        players.put(player.getUniqueId(), player);
        playersByName.put(player.getPlayerName().toLowerCase(), player);
        offlinePlayers.put(player.getUniqueId(), player);

        pluginManager.firePlayerJoinEvent(player);

        return player;
    }

    /**
     * Removes a player from the server (simulates disconnect).
     *
     * @param player the player to remove
     */
    public void removePlayer(@NotNull MockPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");

        pluginManager.firePlayerQuitEvent(player);

        players.remove(player.getUniqueId());
        playersByName.remove(player.getPlayerName().toLowerCase());
    }

    // ==================== World Management ====================

    /**
     * Creates a new world.
     *
     * @param name the world name
     * @return the created mock world
     */
    @NotNull
    public MockWorld createWorld(@NotNull String name) {
        return createWorld(name, UnifiedWorld.Environment.NORMAL);
    }

    /**
     * Creates a new world with a specific environment.
     *
     * @param name        the world name
     * @param environment the world environment
     * @return the created mock world
     */
    @NotNull
    public MockWorld createWorld(@NotNull String name, @NotNull UnifiedWorld.Environment environment) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(environment, "environment cannot be null");

        if (worlds.containsKey(name)) {
            throw new IllegalArgumentException("World with name " + name + " already exists");
        }

        MockWorld world = new MockWorld(this, name, environment);
        worlds.put(name, world);
        return world;
    }

    /**
     * Gets a world by name.
     *
     * @param name the world name
     * @return the mock world
     * @throws IllegalArgumentException if world not found
     */
    @NotNull
    public MockWorld getMockWorld(@NotNull String name) {
        MockWorld world = worlds.get(name);
        if (world == null) {
            throw new IllegalArgumentException("World not found: " + name);
        }
        return world;
    }

    // ==================== Tick Control ====================

    /**
     * Advances the server by a number of ticks.
     *
     * <p>This executes all scheduled tasks that should run within the tick range.
     *
     * @param ticks the number of ticks to advance
     */
    public void advanceTicks(long ticks) {
        if (ticks <= 0) {
            return;
        }

        long targetTick = currentTick.get() + ticks;
        while (currentTick.get() < targetTick) {
            currentTick.incrementAndGet();
            scheduler.tick();
        }
    }

    /**
     * Advances time by a duration in seconds.
     *
     * @param seconds the number of seconds to advance
     */
    public void advanceSeconds(double seconds) {
        advanceTicks((long) (seconds * 20));
    }

    /**
     * Returns the current server tick.
     *
     * @return the current tick
     */
    public long getCurrentTick() {
        return currentTick.get();
    }

    /**
     * Waits for all async tasks to complete.
     */
    public void waitForAsyncTasks() {
        scheduler.waitForAsyncTasks();
    }

    // ==================== Event Management ====================

    /**
     * Returns the plugin manager for event handling.
     *
     * @return the mock plugin manager
     */
    @NotNull
    public MockPluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Creates an event collector for a specific event type.
     *
     * @param <E>       the event type
     * @param eventType the event class to collect
     * @return the event collector
     */
    @NotNull
    public <E> EventCollector<E> collectEvents(@NotNull Class<E> eventType) {
        return pluginManager.collectEvents(eventType);
    }

    // ==================== Command Testing ====================

    /**
     * Gets tab completions for a command.
     *
     * @param sender  the command sender
     * @param command the partial command string
     * @return the list of completions
     */
    @NotNull
    public List<String> tabComplete(@NotNull Object sender, @NotNull String command) {
        return pluginManager.tabComplete(sender, command);
    }

    // ==================== Service Mocking ====================

    /**
     * Mocks a service for testing.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return the mock service
     */
    @NotNull
    public <T> T mockService(@NotNull Class<T> serviceType) {
        return serviceRegistry.createMock(serviceType);
    }

    /**
     * Registers a mock service implementation.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param service     the mock implementation
     */
    public <T> void registerMockService(@NotNull Class<T> serviceType, @NotNull T service) {
        serviceRegistry.registerMock(serviceType, service);
    }

    // ==================== Database Mocking ====================

    /**
     * Returns the mock database for testing.
     *
     * @return the mock database
     */
    @NotNull
    public MockDatabase getMockDatabase() {
        return database;
    }

    // ==================== Scheduler Access ====================

    /**
     * Returns the mock scheduler.
     *
     * @return the mock scheduler
     */
    @NotNull
    public MockScheduler getScheduler() {
        return scheduler;
    }

    // ==================== UnifiedServer Implementation ====================

    @Override
    @NotNull
    public String getName() {
        return "MockServer";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0-TEST";
    }

    @Override
    @NotNull
    public MinecraftVersion getMinecraftVersion() {
        return config.minecraftVersion();
    }

    @Override
    @NotNull
    public ServerType getServerType() {
        return ServerType.PAPER;
    }

    @Override
    @NotNull
    public InetSocketAddress getAddress() {
        return new InetSocketAddress("127.0.0.1", 25565);
    }

    @Override
    public int getPort() {
        return 25565;
    }

    @Override
    @NotNull
    public String getIp() {
        return "127.0.0.1";
    }

    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    @Override
    public int getOnlinePlayerCount() {
        return players.size();
    }

    @Override
    @NotNull
    public Collection<UnifiedPlayer> getOnlinePlayers() {
        return Collections.unmodifiableCollection(new ArrayList<>(players.values()));
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer(@NotNull String name) {
        return Optional.ofNullable(playersByName.get(name.toLowerCase()));
    }

    @Override
    @NotNull
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull UUID uuid) {
        return offlinePlayers.computeIfAbsent(uuid, id ->
            new MockPlayer(this, "OfflinePlayer", id, defaultWorld));
    }

    @Override
    @NotNull
    @Deprecated
    public OfflineUnifiedPlayer getOfflinePlayer(@NotNull String name) {
        return offlinePlayers.values().stream()
            .filter(p -> p.getPlayerName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> {
                UUID uuid = UUID.nameUUIDFromBytes(("MockPlayer:" + name).getBytes());
                MockPlayer player = new MockPlayer(this, name, uuid, defaultWorld);
                offlinePlayers.put(uuid, player);
                return player;
            });
    }

    @Override
    @NotNull
    public Collection<OfflineUnifiedPlayer> getOfflinePlayers() {
        return Collections.unmodifiableCollection(new ArrayList<>(offlinePlayers.values()));
    }

    @Override
    public boolean hasPlayedBefore(@NotNull UUID uuid) {
        return offlinePlayers.containsKey(uuid);
    }

    @Override
    @NotNull
    public Collection<UnifiedWorld> getWorlds() {
        return Collections.unmodifiableCollection(new ArrayList<>(worlds.values()));
    }

    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull String name) {
        return Optional.ofNullable(worlds.get(name));
    }

    @Override
    @NotNull
    public Optional<UnifiedWorld> getWorld(@NotNull UUID uuid) {
        return worlds.values().stream()
            .filter(w -> w.getUniqueId().equals(uuid))
            .map(w -> (UnifiedWorld) w)
            .findFirst();
    }

    @Override
    @NotNull
    public UnifiedWorld getDefaultWorld() {
        return defaultWorld;
    }

    @Override
    public void broadcast(@NotNull Component message) {
        players.values().forEach(player -> player.sendMessage(message));
    }

    @Override
    public void broadcast(@NotNull Component message, @NotNull String permission) {
        players.values().stream()
            .filter(player -> player.hasPermission(permission))
            .forEach(player -> player.sendMessage(message));
    }

    @Override
    public boolean executeCommand(@NotNull String command) {
        CommandResult result = pluginManager.executeCommand(null, command);
        return result.isSuccess();
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> executeCommandAsync(@NotNull String command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command));
    }

    @Override
    @NotNull
    public Path getServerFolder() {
        return serverFolder;
    }

    @Override
    @NotNull
    public Path getPluginsFolder() {
        return pluginsFolder;
    }

    @Override
    @NotNull
    public Path getWorldsFolder() {
        return worldsFolder;
    }

    @Override
    public double getTPS() {
        return 20.0; // Mock always returns perfect TPS
    }

    @Override
    public double @NotNull [] getAverageTPS() {
        return new double[]{20.0, 20.0, 20.0};
    }

    @Override
    public double getMSPT() {
        return 50.0; // Perfect MSPT
    }

    @Override
    public boolean isOnlineMode() {
        return onlineMode;
    }

    /**
     * Sets the online mode for testing.
     *
     * @param onlineMode the online mode
     */
    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

    @Override
    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
    }

    @Override
    public void reloadWhitelist() {
        // No-op for mock
    }

    @Override
    @NotNull
    public Component getMotd() {
        return motd;
    }

    @Override
    public void setMotd(@NotNull Component motd) {
        this.motd = Objects.requireNonNull(motd);
    }

    @Override
    public void shutdown() {
        stop();
    }

    @Override
    public boolean restart() {
        reset();
        return true;
    }

    @Override
    public boolean isStopping() {
        return stopping.get();
    }

    @Override
    @NotNull
    public Thread getPrimaryThread() {
        return primaryThread;
    }

    @Override
    public boolean isPrimaryThread() {
        return Thread.currentThread() == primaryThread;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) this;
    }
}
