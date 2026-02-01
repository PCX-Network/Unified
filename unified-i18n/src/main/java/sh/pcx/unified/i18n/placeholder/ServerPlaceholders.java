/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

/**
 * Built-in server-related placeholders.
 *
 * <p>Provides placeholders for server information such as:
 * <ul>
 *   <li>{@code %server_name%} - Server name</li>
 *   <li>{@code %server_online%} - Online player count</li>
 *   <li>{@code %server_max_players%} - Maximum player slots</li>
 *   <li>{@code %server_tps%} - Server TPS</li>
 *   <li>{@code %server_version%} - Server version</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@PlaceholderExpansion(
        identifier = "server",
        author = "Supatuck",
        version = "1.0.0"
)
public final class ServerPlaceholders {

    @Placeholder(value = "name", description = "Server name")
    public String getName() {
        return "Minecraft Server";
    }

    @Placeholder(value = "online", description = "Online player count")
    public String getOnlineCount() {
        return "0"; // Would integrate with platform
    }

    @Placeholder(value = "max_players", description = "Maximum players")
    public String getMaxPlayers() {
        return "20"; // Would integrate with platform
    }

    @Placeholder(value = "tps", description = "Server TPS")
    public String getTps() {
        return "20.0"; // Would integrate with platform
    }

    @Placeholder(value = "version", description = "Server version")
    public String getVersion() {
        return "1.21.4"; // Would integrate with platform
    }

    @Placeholder(value = "motd", description = "Server MOTD")
    public String getMotd() {
        return "A Minecraft Server";
    }
}
