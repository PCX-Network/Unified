/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import sh.pcx.unified.player.UnifiedPlayer;

/**
 * Built-in player-related placeholders.
 *
 * <p>Provides placeholders for player information such as:
 * <ul>
 *   <li>{@code %player_name%} - Player name</li>
 *   <li>{@code %player_uuid%} - Player UUID</li>
 *   <li>{@code %player_health%} - Current health</li>
 *   <li>{@code %player_level%} - Experience level</li>
 *   <li>{@code %player_world%} - Current world name</li>
 *   <li>{@code %player_ping%} - Connection ping</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@PlaceholderExpansion(
        identifier = "player",
        author = "Supatuck",
        version = "1.0.0"
)
public final class PlayerPlaceholders {

    @Placeholder(value = "name", description = "Player's name")
    public String getName(UnifiedPlayer player) {
        return player.getName().orElse("Unknown");
    }

    @Placeholder(value = "uuid", description = "Player's UUID")
    public String getUuid(UnifiedPlayer player) {
        return player.getUniqueId().toString();
    }

    @Placeholder(value = "displayname", description = "Player's display name")
    public String getDisplayName(UnifiedPlayer player) {
        return player.getName().orElse("Unknown"); // Would get display name from platform
    }

    @Placeholder(value = "health", description = "Player's current health")
    public String getHealth(UnifiedPlayer player) {
        return String.format("%.1f", player.getHealth());
    }

    @Placeholder(value = "max_health", description = "Player's maximum health")
    public String getMaxHealth(UnifiedPlayer player) {
        return String.format("%.1f", player.getMaxHealth());
    }

    @Placeholder(value = "health_percent", description = "Health as percentage")
    public String getHealthPercent(UnifiedPlayer player) {
        double percent = (player.getHealth() / player.getMaxHealth()) * 100;
        return String.format("%.0f", percent);
    }

    @Placeholder(value = "level", description = "Experience level")
    public String getLevel(UnifiedPlayer player) {
        return String.valueOf(player.getLevel());
    }

    @Placeholder(value = "world", description = "Current world name")
    public String getWorld(UnifiedPlayer player) {
        return player.getWorld().getName();
    }

    @Placeholder(value = "x", description = "X coordinate")
    public String getX(UnifiedPlayer player) {
        return String.valueOf((int) player.getLocation().x());
    }

    @Placeholder(value = "y", description = "Y coordinate")
    public String getY(UnifiedPlayer player) {
        return String.valueOf((int) player.getLocation().y());
    }

    @Placeholder(value = "z", description = "Z coordinate")
    public String getZ(UnifiedPlayer player) {
        return String.valueOf((int) player.getLocation().z());
    }

    @Placeholder(value = "gamemode", description = "Current game mode")
    public String getGameMode(UnifiedPlayer player) {
        return player.getGameMode().name();
    }

    @Placeholder(value = "is_flying", description = "Whether player is flying")
    public String isFlying(UnifiedPlayer player) {
        return String.valueOf(player.isFlying());
    }

    @Placeholder(value = "is_sneaking", description = "Whether player is sneaking")
    public String isSneaking(UnifiedPlayer player) {
        return String.valueOf(player.isSneaking());
    }

    @Placeholder(value = "is_sprinting", description = "Whether player is sprinting")
    public String isSprinting(UnifiedPlayer player) {
        return String.valueOf(player.isSprinting());
    }
}
