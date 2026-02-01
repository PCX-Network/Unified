/*
 * Basic Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.config;

import sh.pcx.unified.config.annotation.ConfigComment;
import sh.pcx.unified.config.annotation.ConfigSerializable;
import sh.pcx.unified.config.validation.constraint.Range;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Main configuration class for the BasicPlugin.
 *
 * @author Supatuck
 * @since 1.0.0
 */
@ConfigSerializable
@ConfigComment("BasicPlugin Configuration File")
public class BasicPluginConfig {

    @ConfigComment("Enable debug logging")
    private boolean debug = false;

    @ConfigComment("Plugin locale")
    private String locale = "en_US";

    @ConfigComment(value = "=== Database Settings ===", header = true)
    private DatabaseConfig database = new DatabaseConfig();

    @ConfigComment(value = "=== Message Settings ===", header = true)
    private MessagesConfig messages = new MessagesConfig();

    @ConfigComment(value = "=== Feature Settings ===", header = true)
    private FeaturesConfig features = new FeaturesConfig();

    @ConfigComment(value = "=== GUI Settings ===", header = true)
    private GuiConfig gui = new GuiConfig();

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }
    @NotNull public String getLocale() { return locale; }
    @NotNull public DatabaseConfig getDatabase() { return database; }
    @NotNull public MessagesConfig getMessages() { return messages; }
    @NotNull public FeaturesConfig getFeatures() { return features; }
    @NotNull public GuiConfig getGui() { return gui; }

    @ConfigSerializable
    public static class DatabaseConfig {
        @ConfigComment("Database hostname")
        private String host = "localhost";
        @ConfigComment("Database port")
        @Range(min = 1, max = 65535)
        private int port = 3306;
        @ConfigComment("Database name")
        private String database = "basicplugin";
        @ConfigComment("Username")
        private String username = "root";
        @ConfigComment("Password")
        private String password = "";
        @ConfigComment("Pool size")
        @Range(min = 1, max = 100)
        private int poolSize = 10;

        @NotNull public String getHost() { return host; }
        public int getPort() { return port; }
        @NotNull public String getDatabase() { return database; }
        @NotNull public String getUsername() { return username; }
        @NotNull public String getPassword() { return password; }
        public int getPoolSize() { return poolSize; }
        @NotNull public String getJdbcUrl() { return "jdbc:mysql://" + host + ":" + port + "/" + database; }
    }

    @ConfigSerializable
    public static class MessagesConfig {
        @ConfigComment("Message prefix")
        private String prefix = "<gradient:#5e4fa2:#f79459>[BasicPlugin]</gradient> ";
        @ConfigComment("Welcome message")
        private String welcome = "<green>Welcome, <player>!";
        @ConfigComment("Goodbye message")
        private String goodbye = "<gray><player> has left.";
        @ConfigComment("Success message")
        private String success = "<green>Success!";
        @ConfigComment("Error message")
        private String error = "<red>Error: <message>";
        @ConfigComment("No permission message")
        private String noPermission = "<red>No permission!";

        @NotNull public String getPrefix() { return prefix; }
        @NotNull public String getWelcome() { return welcome; }
        @NotNull public String getGoodbye() { return goodbye; }
        @NotNull public String getSuccess() { return success; }
        @NotNull public String getError() { return error; }
        @NotNull public String getNoPermission() { return noPermission; }
    }

    @ConfigSerializable
    public static class FeaturesConfig {
        @ConfigComment("Enable welcome messages")
        private boolean welcomeMessage = true;
        @ConfigComment("Enable player list GUI")
        private boolean playerListGui = true;
        @ConfigComment("Enable death messages")
        private boolean deathMessages = true;
        @ConfigComment("Enable join/quit messages")
        private boolean joinQuitMessages = true;
        @ConfigComment("Enabled features list")
        private List<String> enabledFeatures = new ArrayList<>(List.of("welcome", "playerlist", "death-messages"));

        public boolean isWelcomeMessage() { return welcomeMessage; }
        public boolean isPlayerListGui() { return playerListGui; }
        public boolean isDeathMessages() { return deathMessages; }
        public boolean isJoinQuitMessages() { return joinQuitMessages; }
        @NotNull public List<String> getEnabledFeatures() { return enabledFeatures; }
    }

    @ConfigSerializable
    public static class GuiConfig {
        @ConfigComment("Items per page")
        @Range(min = 1, max = 45)
        private int itemsPerPage = 28;
        @ConfigComment("Default title")
        private String defaultTitle = "Menu";
        @ConfigComment("Enable animations")
        private boolean animations = true;
        @ConfigComment("Enable sounds")
        private boolean sounds = true;
        @ConfigComment("Close button slot (-1 to disable)")
        private int closeButtonSlot = 49;

        public int getItemsPerPage() { return itemsPerPage; }
        @NotNull public String getDefaultTitle() { return defaultTitle; }
        public boolean isAnimations() { return animations; }
        public boolean isSounds() { return sounds; }
        public int getCloseButtonSlot() { return closeButtonSlot; }
    }
}
