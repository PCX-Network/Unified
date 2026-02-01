/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.PlayerSession;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.testing.command.CommandResult;
import sh.pcx.unified.testing.inventory.MockInventory;
import sh.pcx.unified.testing.server.MockServer;
import sh.pcx.unified.testing.world.MockBlock;
import sh.pcx.unified.testing.world.MockWorld;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock implementation of a Minecraft player for testing purposes.
 *
 * <p>MockPlayer provides a complete simulation of a Minecraft player,
 * allowing tests to verify player interactions, messages, inventory changes,
 * and other player-related behavior.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Message capture and verification</li>
 *   <li>Command execution</li>
 *   <li>Inventory manipulation</li>
 *   <li>Permission management</li>
 *   <li>Location and teleportation</li>
 *   <li>Health, food, and experience</li>
 *   <li>Action bar and title tracking</li>
 *   <li>Sound and particle effects tracking</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockPlayer player = server.addPlayer("Steve");
 *
 * // Set player state
 * player.setHealth(10.0);
 * player.setFoodLevel(15);
 * player.setOp(true);
 *
 * // Execute commands
 * CommandResult result = player.performCommand("spawn");
 * assertThat(result.isSuccess()).isTrue();
 *
 * // Verify messages
 * assertThat(player.getMessages()).contains("Teleported to spawn!");
 *
 * // Simulate actions
 * player.leftClick(block);
 * player.rightClick(block);
 * player.chat("Hello world!");
 *
 * // Check inventory
 * assertThat(player.getInventory().contains("minecraft:diamond")).isTrue();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockServer
 * @see MockInventory
 */
public final class MockPlayer implements UnifiedPlayer {

    private final MockServer server;
    private final String name;
    private final UUID uuid;
    private MockWorld world;
    private UnifiedLocation location;

    // Player state
    private Component displayName;
    private double health = 20.0;
    private double maxHealth = 20.0;
    private int foodLevel = 20;
    private float saturation = 5.0f;
    private int level = 0;
    private float exp = 0.0f;
    private int totalExperience = 0;
    private GameMode gameMode = GameMode.SURVIVAL;
    private boolean flying = false;
    private boolean allowFlight = false;
    private boolean sneaking = false;
    private boolean sprinting = false;
    private boolean op = false;
    private Locale locale = Locale.ENGLISH;
    private int ping = 0;
    private String clientBrand = "vanilla";

    // Inventory
    private final MockInventory inventory;
    private UnifiedItemStack mainHand;
    private UnifiedItemStack offHand;

    // Permissions
    private final Set<String> permissions = new HashSet<>();

    // Message tracking
    private final List<Component> messages = new CopyOnWriteArrayList<>();
    private final List<String> rawMessages = new CopyOnWriteArrayList<>();
    private Component actionBar;
    private Title lastTitle;
    private final List<Sound> playedSounds = new CopyOnWriteArrayList<>();
    private final List<BossBar> bossBars = new CopyOnWriteArrayList<>();

    // Session data
    private final Instant firstJoined;
    private Instant lastJoined;
    private long lastPlayed = 0;
    private boolean online = true;

    // Pointers for Adventure API
    private final Pointers pointers;

    /**
     * Creates a new mock player.
     *
     * @param server the mock server
     * @param name   the player name
     * @param uuid   the player UUID
     * @param world  the initial world
     */
    public MockPlayer(
        @NotNull MockServer server,
        @NotNull String name,
        @NotNull UUID uuid,
        @NotNull MockWorld world
    ) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.location = world.getSpawnLocation();
        this.displayName = Component.text(name);
        this.inventory = new MockInventory(this, 36);
        this.firstJoined = Instant.now();
        this.lastJoined = Instant.now();

        this.pointers = Pointers.builder()
            .withStatic(Identity.UUID, uuid)
            .withStatic(Identity.NAME, name)
            .withStatic(Identity.DISPLAY_NAME, displayName)
            .build();
    }

    // ==================== Message Operations ====================

    @Override
    public void sendMessage(@NotNull Component message) {
        messages.add(message);
        rawMessages.add(componentToString(message));
    }

    /**
     * Returns all messages sent to this player as Components.
     *
     * @return the list of messages
     */
    @NotNull
    public List<Component> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns all messages sent to this player as plain text.
     *
     * @return the list of raw message strings
     */
    @NotNull
    public List<String> getRawMessages() {
        return Collections.unmodifiableList(rawMessages);
    }

    /**
     * Returns the last message sent to this player.
     *
     * @return an Optional containing the last message
     */
    @NotNull
    public Optional<Component> getLastMessage() {
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.getLast());
    }

    /**
     * Clears all captured messages.
     */
    public void clearMessages() {
        messages.clear();
        rawMessages.clear();
    }

    /**
     * Returns the current action bar content.
     *
     * @return the action bar component, or null if not set
     */
    @Nullable
    public Component getActionBar() {
        return actionBar;
    }

    /**
     * Returns the last title sent to this player.
     *
     * @return the last title, or null if not sent
     */
    @Nullable
    public Title getLastTitle() {
        return lastTitle;
    }

    @Override
    public void sendActionBar(@NotNull Component message) {
        this.actionBar = message;
    }

    @Override
    public <T> void sendTitlePart(@NotNull TitlePart<T> part, @NotNull T value) {
        // Capture title parts for testing
    }

    @Override
    public void showTitle(@NotNull Title title) {
        this.lastTitle = title;
    }

    @Override
    public void clearTitle() {
        this.lastTitle = null;
    }

    @Override
    public void resetTitle() {
        this.lastTitle = null;
    }

    @Override
    public void showBossBar(@NotNull BossBar bar) {
        bossBars.add(bar);
    }

    @Override
    public void hideBossBar(@NotNull BossBar bar) {
        bossBars.remove(bar);
    }

    /**
     * Returns all active boss bars.
     *
     * @return the list of boss bars
     */
    @NotNull
    public List<BossBar> getBossBars() {
        return Collections.unmodifiableList(bossBars);
    }

    @Override
    public void playSound(@NotNull Sound sound) {
        playedSounds.add(sound);
    }

    @Override
    public void playSound(@NotNull Sound sound, double x, double y, double z) {
        playedSounds.add(sound);
    }

    @Override
    public void stopSound(@NotNull SoundStop stop) {
        // No-op for mock
    }

    /**
     * Returns all sounds played to this player.
     *
     * @return the list of played sounds
     */
    @NotNull
    public List<Sound> getPlayedSounds() {
        return Collections.unmodifiableList(playedSounds);
    }

    @Override
    public void openBook(@NotNull Book book) {
        // Track book opening if needed
    }

    // ==================== Command Execution ====================

    @Override
    public boolean performCommand(@NotNull String command) {
        CommandResult result = performCommandWithResult(command);
        return result.isSuccess();
    }

    /**
     * Performs a command and returns a detailed result.
     *
     * @param command the command string (without leading slash)
     * @return the command result
     */
    @NotNull
    public CommandResult performCommandWithResult(@NotNull String command) {
        return server.getPluginManager().executeCommand(this, command);
    }

    /**
     * Simulates the player sending a chat message.
     *
     * @param message the message to send
     */
    public void chat(@NotNull String message) {
        server.getPluginManager().firePlayerChatEvent(this, message);
    }

    // ==================== Player Actions ====================

    /**
     * Simulates a left click on a block.
     *
     * @param block the block to click
     */
    public void leftClick(@NotNull MockBlock block) {
        server.getPluginManager().fireBlockDamageEvent(this, block);
    }

    /**
     * Simulates a right click on a block.
     *
     * @param block the block to click
     */
    public void rightClick(@NotNull MockBlock block) {
        server.getPluginManager().firePlayerInteractEvent(this, block);
    }

    /**
     * Simulates the player dropping their held item.
     */
    public void dropItem() {
        if (mainHand != null && !mainHand.isEmpty()) {
            server.getPluginManager().firePlayerDropItemEvent(this, mainHand);
            mainHand = null;
        }
    }

    /**
     * Simulates the player attacking another player.
     *
     * @param target the player to attack
     */
    public void attack(@NotNull MockPlayer target) {
        server.getPluginManager().firePlayerDamageEvent(this, target);
    }

    /**
     * Disconnects this player from the server.
     */
    public void disconnect() {
        if (online) {
            online = false;
            lastPlayed = System.currentTimeMillis();
            server.removePlayer(this);
        }
    }

    // ==================== UnifiedPlayer Implementation ====================

    @Override
    @NotNull
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(@Nullable Component displayName) {
        this.displayName = displayName != null ? displayName : Component.text(name);
    }

    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        return location;
    }

    /**
     * Sets the player's location without teleporting.
     *
     * @param location the new location
     */
    public void setLocation(@NotNull UnifiedLocation location) {
        this.location = Objects.requireNonNull(location);
        if (location.world() instanceof MockWorld mockWorld) {
            this.world = mockWorld;
        }
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedLocation location) {
        this.location = location;
        if (location.world() instanceof MockWorld mockWorld) {
            this.world = mockWorld;
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> teleport(@NotNull UnifiedPlayer target) {
        return teleport(target.getLocation());
    }

    @Override
    public double getHealth() {
        return health;
    }

    @Override
    public void setHealth(double health) {
        if (health < 0) {
            throw new IllegalArgumentException("Health cannot be negative");
        }
        this.health = Math.min(health, maxHealth);
        if (this.health <= 0) {
            server.getPluginManager().firePlayerDeathEvent(this);
        }
    }

    @Override
    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Sets the maximum health.
     *
     * @param maxHealth the maximum health
     */
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    @Override
    public int getFoodLevel() {
        return foodLevel;
    }

    @Override
    public void setFoodLevel(int level) {
        this.foodLevel = Math.max(0, Math.min(20, level));
    }

    /**
     * Returns the saturation level.
     *
     * @return the saturation
     */
    public float getSaturation() {
        return saturation;
    }

    /**
     * Sets the saturation level.
     *
     * @param saturation the saturation
     */
    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    @Override
    public float getExp() {
        return exp;
    }

    @Override
    public void setExp(float exp) {
        this.exp = Math.max(0f, Math.min(1f, exp));
    }

    @Override
    public int getTotalExperience() {
        return totalExperience;
    }

    @Override
    public void setTotalExperience(int exp) {
        this.totalExperience = Math.max(0, exp);
    }

    @Override
    public void giveExp(int amount) {
        this.totalExperience += amount;
        // Simplified level calculation
        this.level = (int) Math.sqrt(totalExperience / 10.0);
    }

    @Override
    @NotNull
    public GameMode getGameMode() {
        return gameMode;
    }

    @Override
    public void setGameMode(@NotNull GameMode gameMode) {
        this.gameMode = Objects.requireNonNull(gameMode);
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            this.allowFlight = true;
        }
    }

    @Override
    public boolean isFlying() {
        return flying;
    }

    @Override
    public void setFlying(boolean flying) {
        if (flying && !allowFlight) {
            throw new IllegalStateException("Flight is not allowed for this player");
        }
        this.flying = flying;
    }

    @Override
    public boolean getAllowFlight() {
        return allowFlight;
    }

    @Override
    public void setAllowFlight(boolean allow) {
        this.allowFlight = allow;
        if (!allow) {
            this.flying = false;
        }
    }

    @Override
    public boolean isSneaking() {
        return sneaking;
    }

    /**
     * Sets whether the player is sneaking.
     *
     * @param sneaking the sneaking state
     */
    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    @Override
    public boolean isSprinting() {
        return sprinting;
    }

    /**
     * Sets whether the player is sprinting.
     *
     * @param sprinting the sprinting state
     */
    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    @Override
    @NotNull
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the player's locale.
     *
     * @param locale the locale
     */
    public void setLocale(@NotNull Locale locale) {
        this.locale = Objects.requireNonNull(locale);
    }

    @Override
    public int getPing() {
        return ping;
    }

    /**
     * Sets the player's ping.
     *
     * @param ping the ping in milliseconds
     */
    public void setPing(int ping) {
        this.ping = ping;
    }

    @Override
    @NotNull
    public Optional<String> getClientBrand() {
        return Optional.ofNullable(clientBrand);
    }

    /**
     * Sets the client brand.
     *
     * @param brand the client brand
     */
    public void setClientBrand(@Nullable String brand) {
        this.clientBrand = brand;
    }

    @Override
    @NotNull
    public String getAddress() {
        return "127.0.0.1";
    }

    @Override
    public void kick(@NotNull Component reason) {
        messages.add(reason);
        disconnect();
    }

    @Override
    public void kick() {
        kick(Component.text("Kicked from server"));
    }

    // ==================== Permissions ====================

    @Override
    public boolean hasPermission(@NotNull String permission) {
        if (op) {
            return true;
        }
        if (permissions.contains("*")) {
            return true;
        }
        return permissions.contains(permission);
    }

    /**
     * Adds a permission to this player.
     *
     * @param permission the permission to add
     */
    public void addPermission(@NotNull String permission) {
        permissions.add(Objects.requireNonNull(permission));
    }

    /**
     * Removes a permission from this player.
     *
     * @param permission the permission to remove
     */
    public void removePermission(@NotNull String permission) {
        permissions.remove(permission);
    }

    /**
     * Clears all permissions from this player.
     */
    public void clearPermissions() {
        permissions.clear();
    }

    /**
     * Returns all permissions this player has.
     *
     * @return the set of permissions
     */
    @NotNull
    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public boolean isOp() {
        return op;
    }

    @Override
    public void setOp(boolean op) {
        this.op = op;
    }

    // ==================== Inventory ====================

    @Override
    @NotNull
    public UnifiedItemStack getItemInMainHand() {
        return mainHand != null ? mainHand : UnifiedItemStack.empty();
    }

    @Override
    public void setItemInMainHand(@Nullable UnifiedItemStack item) {
        this.mainHand = item;
    }

    @Override
    @NotNull
    public UnifiedItemStack getItemInOffHand() {
        return offHand != null ? offHand : UnifiedItemStack.empty();
    }

    @Override
    public void setItemInOffHand(@Nullable UnifiedItemStack item) {
        this.offHand = item;
    }

    @Override
    public boolean giveItem(@NotNull UnifiedItemStack item) {
        return inventory.addItem(item);
    }

    /**
     * Returns the player's inventory.
     *
     * @return the mock inventory
     */
    @NotNull
    public MockInventory getInventory() {
        return inventory;
    }

    @Override
    public void openInventory() {
        // Track that inventory was opened
    }

    @Override
    public void closeInventory() {
        // Track that inventory was closed
    }

    @Override
    public void sendPluginMessage(@NotNull String channel, byte @NotNull [] data) {
        // Track plugin messages if needed
    }

    @Override
    public void playSound(@NotNull String sound, float volume, float pitch) {
        playedSounds.add(Sound.sound(
            net.kyori.adventure.key.Key.key(sound),
            Sound.Source.MASTER,
            volume,
            pitch
        ));
    }

    @Override
    @NotNull
    public PlayerSession getSession() {
        return new MockPlayerSession(this);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) this;
    }

    // ==================== OfflineUnifiedPlayer Implementation ====================

    /**
     * Returns the player's name directly (for internal use and UnifiedPlayer).
     *
     * @return the player name
     */
    @NotNull
    public String getPlayerName() {
        return name;
    }

    @Override
    @NotNull
    public Optional<String> getName() {
        return Optional.of(name);
    }

    @Override
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    @Override
    @NotNull
    public Optional<UnifiedPlayer> getPlayer() {
        return online ? Optional.of(this) : Optional.empty();
    }

    @Override
    @NotNull
    public Optional<Instant> getFirstPlayed() {
        return Optional.of(firstJoined);
    }

    @Override
    @NotNull
    public Optional<Instant> getLastSeen() {
        if (online) {
            return Optional.of(Instant.now());
        }
        return lastPlayed > 0 ? Optional.of(Instant.ofEpochMilli(lastPlayed)) : Optional.empty();
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getLastLocation() {
        return CompletableFuture.completedFuture(Optional.of(location));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<UnifiedLocation>> getBedSpawnLocation() {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public boolean hasPlayedBefore() {
        return true;
    }

    @Override
    public boolean isBanned() {
        return false;
    }

    @Override
    public void setBanned(boolean banned) {
        // No-op for mock
    }

    @Override
    public void ban(@NotNull String reason, @Nullable Instant expiration, @Nullable String source) {
        // No-op for mock
    }

    @Override
    public void pardon() {
        // No-op for mock
    }

    @Override
    public boolean isWhitelisted() {
        return true;
    }

    @Override
    public void setWhitelisted(boolean whitelisted) {
        // No-op for mock
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> getStatistic(@NotNull String statistic) {
        return CompletableFuture.completedFuture(0);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> incrementStatistic(@NotNull String statistic, int amount) {
        return CompletableFuture.completedFuture(null);
    }

    // ==================== Adventure API ====================

    @Override
    @NotNull
    public Pointers pointers() {
        return pointers;
    }

    // ==================== Utility Methods ====================

    private String componentToString(Component component) {
        // Simple conversion - in real implementation would use serializer
        StringBuilder sb = new StringBuilder();
        extractText(component, sb);
        return sb.toString();
    }

    private void extractText(Component component, StringBuilder sb) {
        if (component instanceof net.kyori.adventure.text.TextComponent textComponent) {
            sb.append(textComponent.content());
        }
        component.children().forEach(child -> extractText(child, sb));
    }

    // ==================== Builder ====================

    /**
     * Creates a new player builder.
     *
     * @param server the mock server
     * @return a new builder
     */
    @NotNull
    public static Builder builder(@NotNull MockServer server) {
        return new Builder(server);
    }

    /**
     * Builder for creating MockPlayer instances with custom configuration.
     */
    public static final class Builder {
        private final MockServer server;
        private String name = "Player" + System.currentTimeMillis();
        private UUID uuid;
        private MockWorld world;
        private double health = 20.0;
        private int foodLevel = 20;
        private GameMode gameMode = GameMode.SURVIVAL;
        private boolean op = false;
        private final Set<String> permissions = new HashSet<>();

        private Builder(@NotNull MockServer server) {
            this.server = Objects.requireNonNull(server);
            this.uuid = UUID.nameUUIDFromBytes(("MockPlayer:" + name).getBytes());
            this.world = (MockWorld) server.getDefaultWorld();
        }

        /**
         * Sets the player name.
         *
         * @param name the name
         * @return this builder
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = Objects.requireNonNull(name);
            this.uuid = UUID.nameUUIDFromBytes(("MockPlayer:" + name).getBytes());
            return this;
        }

        /**
         * Sets the player UUID.
         *
         * @param uuid the UUID
         * @return this builder
         */
        @NotNull
        public Builder uuid(@NotNull UUID uuid) {
            this.uuid = Objects.requireNonNull(uuid);
            return this;
        }

        /**
         * Sets the initial world.
         *
         * @param world the world
         * @return this builder
         */
        @NotNull
        public Builder world(@NotNull MockWorld world) {
            this.world = Objects.requireNonNull(world);
            return this;
        }

        /**
         * Sets the initial health.
         *
         * @param health the health
         * @return this builder
         */
        @NotNull
        public Builder health(double health) {
            this.health = health;
            return this;
        }

        /**
         * Sets the initial food level.
         *
         * @param foodLevel the food level
         * @return this builder
         */
        @NotNull
        public Builder foodLevel(int foodLevel) {
            this.foodLevel = foodLevel;
            return this;
        }

        /**
         * Sets the game mode.
         *
         * @param gameMode the game mode
         * @return this builder
         */
        @NotNull
        public Builder gameMode(@NotNull GameMode gameMode) {
            this.gameMode = Objects.requireNonNull(gameMode);
            return this;
        }

        /**
         * Sets whether the player is an operator.
         *
         * @param op the op status
         * @return this builder
         */
        @NotNull
        public Builder op(boolean op) {
            this.op = op;
            return this;
        }

        /**
         * Adds a permission.
         *
         * @param permission the permission
         * @return this builder
         */
        @NotNull
        public Builder withPermission(@NotNull String permission) {
            this.permissions.add(Objects.requireNonNull(permission));
            return this;
        }

        /**
         * Builds the MockPlayer instance.
         *
         * @return the created player
         */
        @NotNull
        public MockPlayer build() {
            MockPlayer player = new MockPlayer(server, name, uuid, world);
            player.setHealth(health);
            player.setFoodLevel(foodLevel);
            player.setGameMode(gameMode);
            player.setOp(op);
            permissions.forEach(player::addPermission);
            return player;
        }
    }

    /**
     * Mock implementation of PlayerSession.
     */
    private static final class MockPlayerSession implements PlayerSession {
        private final MockPlayer player;
        private final String sessionId;
        private final Map<String, Object> data = new ConcurrentHashMap<>();

        MockPlayerSession(MockPlayer player) {
            this.player = player;
            this.sessionId = UUID.randomUUID().toString();
        }

        @Override
        @NotNull
        public String getSessionId() {
            return sessionId;
        }

        @Override
        @NotNull
        public UUID getPlayerId() {
            return player.getUniqueId();
        }

        @Override
        @NotNull
        public Instant getJoinTime() {
            return player.lastJoined;
        }

        @Override
        @NotNull
        public java.time.Duration getDuration() {
            return java.time.Duration.between(player.lastJoined, Instant.now());
        }

        @Override
        public <T> void set(@NotNull String key, @Nullable T value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        @Override
        @NotNull
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }

        @Override
        @NotNull
        @SuppressWarnings("unchecked")
        public <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue) {
            Object value = data.get(key);
            if (value != null && defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }

        @Override
        public boolean has(@NotNull String key) {
            return data.containsKey(key);
        }

        @Override
        public boolean remove(@NotNull String key) {
            return data.remove(key) != null;
        }

        @Override
        public void clear() {
            data.clear();
        }

        @Override
        @NotNull
        public Set<String> getKeys() {
            return Collections.unmodifiableSet(data.keySet());
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        @NotNull
        @SuppressWarnings("unchecked")
        public <T> T compute(@NotNull String key, @NotNull Class<T> type,
                             @NotNull java.util.function.Function<Optional<T>, T> function) {
            T current = get(key, type).orElse(null);
            T newValue = function.apply(Optional.ofNullable(current));
            set(key, newValue);
            return newValue;
        }

        @Override
        public <T> boolean setIfAbsent(@NotNull String key, @NotNull T value) {
            if (!data.containsKey(key)) {
                data.put(key, value);
                return true;
            }
            return false;
        }

        @Override
        public int increment(@NotNull String key, int delta) {
            return compute(key, Integer.class, opt -> opt.orElse(0) + delta);
        }

        @Override
        @NotNull
        public Map<String, Object> toMap() {
            return Collections.unmodifiableMap(new HashMap<>(data));
        }

        @Override
        @NotNull
        public String getIpAddress() {
            return "127.0.0.1";
        }

        @Override
        @NotNull
        public String getServerName() {
            return "MockServer";
        }

        @Override
        public boolean isValid() {
            return player.isOnline();
        }
    }
}
