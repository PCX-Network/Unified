/*
 * Basic Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import sh.pcx.example.BasicPlugin;
import sh.pcx.example.config.BasicPluginConfig;
import sh.pcx.unified.commands.annotation.Arg;
import sh.pcx.unified.commands.annotation.Command;
import sh.pcx.unified.commands.annotation.Completions;
import sh.pcx.unified.commands.annotation.Default;
import sh.pcx.unified.commands.annotation.Permission;
import sh.pcx.unified.commands.annotation.Sender;
import sh.pcx.unified.commands.annotation.Subcommand;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Main command class demonstrating the command framework.
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Command(
    name = "basic",
    aliases = {"bp", "basicplugin"},
    description = "Main command for BasicPlugin",
    permission = "basicplugin.use"
)
public class BasicCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BasicPlugin plugin;

    public BasicCommand(@NotNull BasicPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    public void defaultCommand(@Sender Object sender) {
        sendMessage(sender, Component.text()
            .append(Component.text("=== BasicPlugin ===", NamedTextColor.GOLD))
            .appendNewline()
            .append(Component.text("Version: " + plugin.getVersion(), NamedTextColor.WHITE))
            .appendNewline()
            .append(Component.text("Use /basic help for commands", NamedTextColor.YELLOW))
            .build());
    }

    @Subcommand("help")
    public void help(@Sender Object sender) {
        sendMessage(sender, Component.text()
            .append(Component.text("=== BasicPlugin Help ===", NamedTextColor.GOLD))
            .appendNewline()
            .append(Component.text("/basic info - Plugin info", NamedTextColor.YELLOW))
            .appendNewline()
            .append(Component.text("/basic reload - Reload config", NamedTextColor.YELLOW))
            .appendNewline()
            .append(Component.text("/basic debug [on|off] - Toggle debug", NamedTextColor.YELLOW))
            .appendNewline()
            .append(Component.text("/basic gui - Open player list", NamedTextColor.YELLOW))
            .build());
    }

    @Subcommand("info")
    public void info(@Sender Object sender) {
        BasicPluginConfig config = plugin.getPluginConfig();
        sendMessage(sender, Component.text()
            .append(Component.text("Name: " + plugin.getName(), NamedTextColor.WHITE))
            .appendNewline()
            .append(Component.text("Version: " + plugin.getVersion(), NamedTextColor.WHITE))
            .appendNewline()
            .append(Component.text("Debug: " + (config.isDebug() ? "Enabled" : "Disabled"),
                config.isDebug() ? NamedTextColor.GREEN : NamedTextColor.RED))
            .build());
    }

    @Subcommand("reload")
    @Permission("basicplugin.admin.reload")
    public void reload(@Sender Object sender) {
        long start = System.currentTimeMillis();
        plugin.onReload();
        sendMessage(sender, Component.text("Reloaded in " + (System.currentTimeMillis() - start) + "ms", NamedTextColor.GREEN));
    }

    @Subcommand("debug")
    @Permission("basicplugin.admin.debug")
    public void debug(@Sender Object sender, @Arg(value = "state", suggestions = {"on", "off"}) @Default("toggle") String state) {
        BasicPluginConfig config = plugin.getPluginConfig();
        boolean newState = "on".equalsIgnoreCase(state) ? true : "off".equalsIgnoreCase(state) ? false : !config.isDebug();
        config.setDebug(newState);
        sendMessage(sender, Component.text("Debug mode " + (newState ? "enabled" : "disabled"),
            newState ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    @Subcommand("message")
    @Permission("basicplugin.message")
    public void message(@Sender UnifiedPlayer sender, @Arg("player") @Completions("@players") String target,
                        @Arg(value = "message", greedy = true) String message) {
        sender.sendMessage(MINI_MESSAGE.deserialize(plugin.getPluginConfig().getMessages().getPrefix() +
            "<gray>Message to <aqua>" + target + "</aqua>: <white>" + message));
    }

    @Subcommand("give")
    @Permission("basicplugin.admin.give")
    public void give(@Sender Object sender, @Arg("player") @Completions("@players") String target,
                     @Arg(value = "amount", min = 1, max = 1000) @Default("1") int amount) {
        sendMessage(sender, Component.text("Gave " + amount + " item(s) to " + target, NamedTextColor.GREEN));
    }

    @Subcommand("gui")
    @Permission("basicplugin.gui")
    public void openGui(@Sender UnifiedPlayer player) {
        player.sendMessage(Component.text("Opening player list GUI...", NamedTextColor.GREEN));
    }

    @Subcommand("admin stats")
    @Permission("basicplugin.admin")
    public void adminStats(@Sender Object sender) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long max = rt.maxMemory() / 1024 / 1024;
        sendMessage(sender, Component.text()
            .append(Component.text("Memory: " + used + "/" + max + " MB", NamedTextColor.WHITE))
            .appendNewline()
            .append(Component.text("Processors: " + rt.availableProcessors(), NamedTextColor.WHITE))
            .build());
    }

    private void sendMessage(Object sender, Component message) {
        if (sender instanceof UnifiedPlayer p) p.sendMessage(message);
        else plugin.getLogger().info(message.toString());
    }
}
