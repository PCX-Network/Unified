/*
 * Basic Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import sh.pcx.example.BasicPlugin;
import sh.pcx.example.config.BasicPluginConfig;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener demonstrating event handling patterns.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class PlayerEventListener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final long MESSAGE_COOLDOWN = 1000;

    private final BasicPlugin plugin;
    private BasicPluginConfig config;
    private final Map<UUID, Long> joinTimes = new HashMap<>();
    private final Map<UUID, Integer> messageCount = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();

    public PlayerEventListener(@NotNull BasicPlugin plugin, @NotNull BasicPluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(@NotNull BasicPluginConfig config) {
        this.config = config;
        plugin.getLogger().info("PlayerEventListener configuration updated");
    }

    public void handleJoin(@NotNull UnifiedPlayer player) {
        UUID id = player.getUniqueId();
        joinTimes.put(id, System.currentTimeMillis());
        messageCount.put(id, 0);

        if (config.isDebug()) plugin.getLogger().info("Player joined: " + player.getName());

        if (config.getFeatures().isWelcomeMessage()) {
            String text = config.getMessages().getWelcome().replace("<player>", player.getName().orElse("Player"));
            player.sendMessage(MINI_MESSAGE.deserialize(config.getMessages().getPrefix() + text));
        }

        if (config.getFeatures().isJoinQuitMessages()) {
            plugin.getLogger().info("[Join] " + player.getName());
        }
    }

    public void handleQuit(@NotNull UnifiedPlayer player) {
        UUID id = player.getUniqueId();
        Long joinTime = joinTimes.remove(id);
        if (joinTime != null && config.isDebug()) {
            plugin.getLogger().info(player.getName() + " played for " + (System.currentTimeMillis() - joinTime) / 60000 + " min");
        }
        messageCount.remove(id);
        lastMessageTime.remove(id);
        if (config.getFeatures().isJoinQuitMessages()) {
            plugin.getLogger().info("[Quit] " + player.getName());
        }
    }

    public boolean handleChat(@NotNull UnifiedPlayer player, @NotNull String message) {
        UUID id = player.getUniqueId();
        Long last = lastMessageTime.get(id);
        long now = System.currentTimeMillis();

        if (last != null && (now - last) < MESSAGE_COOLDOWN) {
            player.sendMessage(Component.text("Please wait before sending another message!", NamedTextColor.RED));
            return false;
        }

        lastMessageTime.put(id, now);
        messageCount.merge(id, 1, Integer::sum);
        if (config.isDebug()) plugin.getLogger().info("[Chat] " + player.getName() + ": " + message);
        return true;
    }

    public void handleDeath(@NotNull UnifiedPlayer player, @NotNull String deathMessage) {
        if (config.getFeatures().isDeathMessages()) {
            plugin.getLogger().info("[Death] " + player.getName() + ": " + deathMessage);
        }
    }

    public long getSessionDuration(@NotNull UUID id) {
        Long t = joinTimes.get(id);
        return t == null ? -1 : System.currentTimeMillis() - t;
    }

    public int getMessageCount(@NotNull UUID id) { return messageCount.getOrDefault(id, 0); }
    public boolean isOnCooldown(@NotNull UUID id) {
        Long t = lastMessageTime.get(id);
        return t != null && (System.currentTimeMillis() - t) < MESSAGE_COOLDOWN;
    }
    public int getTrackedPlayerCount() { return joinTimes.size(); }
}
