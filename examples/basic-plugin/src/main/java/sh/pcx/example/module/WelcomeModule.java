/*
 * Basic Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import sh.pcx.example.config.BasicPluginConfig;
import sh.pcx.unified.modules.annotation.Listen;
import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.annotation.ModulePriority;
import sh.pcx.unified.modules.core.ModuleContext;
import sh.pcx.unified.modules.lifecycle.Disableable;
import sh.pcx.unified.modules.lifecycle.Initializable;
import sh.pcx.unified.modules.lifecycle.Reloadable;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example module demonstrating the module system.
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Module(
    name = "Welcome",
    description = "Handles welcome messages and player events",
    version = "1.0.0",
    authors = {"Supatuck"},
    priority = ModulePriority.HIGH,
    enabledByDefault = true
)
@Listen
public class WelcomeModule implements Initializable, Reloadable, Disableable {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private ModuleContext context;
    private BasicPluginConfig.MessagesConfig messages;
    private BasicPluginConfig.FeaturesConfig features;
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();

    @Override
    public void init(@NotNull ModuleContext context) throws Exception {
        this.context = context;
        BasicPluginConfig config = context.loadConfig(BasicPluginConfig.class);
        this.messages = config.getMessages();
        this.features = config.getFeatures();
        context.getLogger().info("Welcome module initialized!");
    }

    @Override
    public void reload(@NotNull ModuleContext context) throws Exception {
        BasicPluginConfig config = context.loadConfig(BasicPluginConfig.class);
        this.messages = config.getMessages();
        this.features = config.getFeatures();
        context.getLogger().info("Welcome module reloaded!");
    }

    @Override
    public void onDisable(ModuleContext context) {
        playerJoinTimes.clear();
        context.getLogger().info("Welcome module disabled!");
    }

    public void handlePlayerJoin(@NotNull UnifiedPlayer player) {
        playerJoinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        if (features.isWelcomeMessage()) {
            String text = messages.getWelcome().replace("<player>", player.getName().orElse("Player"));
            player.sendMessage(MINI_MESSAGE.deserialize(messages.getPrefix() + text));
        }
    }

    public void handlePlayerQuit(@NotNull UnifiedPlayer player) {
        Long joinTime = playerJoinTimes.remove(player.getUniqueId());
        if (joinTime != null && context != null) {
            long minutes = (System.currentTimeMillis() - joinTime) / 60000;
            context.getLogger().fine(player.getName() + " played for " + minutes + " minutes");
        }
    }

    public long getSessionDuration(@NotNull UUID playerId) {
        Long joinTime = playerJoinTimes.get(playerId);
        return joinTime == null ? -1 : System.currentTimeMillis() - joinTime;
    }

    public int getActiveSessionCount() { return playerJoinTimes.size(); }
    @NotNull public BasicPluginConfig.MessagesConfig getMessages() { return messages; }
    @NotNull public BasicPluginConfig.FeaturesConfig getFeatures() { return features; }
}
