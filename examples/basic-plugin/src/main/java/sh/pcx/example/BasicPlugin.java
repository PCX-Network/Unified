/*
 * Basic Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example;

import sh.pcx.example.config.BasicPluginConfig;
import sh.pcx.example.listener.PlayerEventListener;
import sh.pcx.unified.UnifiedPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main plugin class demonstrating the UnifiedPlugin API.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class BasicPlugin extends UnifiedPlugin {

    private BasicPluginConfig config;
    private PlayerEventListener playerListener;

    @Override
    public void onLoad() {
        getLogger().info("BasicPlugin loading...");
        try {
            Files.createDirectories(getDataFolder());
        } catch (IOException e) {
            getLogger().severe("Failed to create data folder: " + e.getMessage());
        }
        saveDefaultResource("config.yml");
    }

    @Override
    public void onEnable() {
        getLogger().info("BasicPlugin enabling...");
        loadConfiguration();
        if (config.isDebug()) {
            getLogger().info("Debug mode is ENABLED");
        }
        this.playerListener = new PlayerEventListener(this, config);
        getLogger().info("BasicPlugin v" + getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BasicPlugin disabling...");
        if (config != null) saveConfiguration();
        this.playerListener = null;
        this.config = null;
        getLogger().info("BasicPlugin disabled.");
    }

    @Override
    public void onReload() {
        getLogger().info("Reloading BasicPlugin...");
        loadConfiguration();
        if (playerListener != null) playerListener.updateConfig(config);
        getLogger().info("BasicPlugin reloaded!");
    }

    private void loadConfiguration() {
        this.config = new BasicPluginConfig();
        getLogger().info("Configuration loaded");
    }

    private void saveConfiguration() {
        getLogger().info("Configuration saved.");
    }

    @NotNull
    public BasicPluginConfig getPluginConfig() {
        return config;
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
        Path targetPath = getDataFolder().resolve(resourcePath);
        if (Files.exists(targetPath) && !replace) return;
        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                getLogger().warning("Resource not found: " + resourcePath);
                return;
            }
            Files.createDirectories(targetPath.getParent());
            Files.copy(in, targetPath);
        } catch (IOException e) {
            getLogger().severe("Could not save resource: " + e.getMessage());
        }
    }
}
