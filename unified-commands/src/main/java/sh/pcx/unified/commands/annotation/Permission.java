/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies permission requirements for a command or subcommand.
 *
 * <p>This annotation can be applied to command classes or individual methods
 * to restrict access based on permission nodes. Permissions are checked before
 * command execution, and players without the required permission receive an
 * appropriate error message.</p>
 *
 * <h2>Permission Logic</h2>
 * <ul>
 *   <li>Multiple permissions can be specified</li>
 *   <li>By default, the player needs ALL specified permissions (AND logic)</li>
 *   <li>Set {@link #any()} to {@code true} for ANY permission (OR logic)</li>
 *   <li>Class-level permissions apply to all subcommands</li>
 *   <li>Method-level permissions are additional to class-level</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Single Permission</h3>
 * <pre>{@code
 * @Command(name = "fly")
 * @Permission("essentials.fly")
 * public class FlyCommand {
 *
 *     @Default
 *     public void toggle(@Sender Player player) {
 *         player.setAllowFlight(!player.getAllowFlight());
 *     }
 * }
 * }</pre>
 *
 * <h3>Multiple Permissions (AND)</h3>
 * <pre>{@code
 * @Command(name = "modtools")
 * @Permission({"staff.moderator", "staff.tools"})
 * public class ModToolsCommand {
 *     // Requires BOTH permissions
 * }
 * }</pre>
 *
 * <h3>Multiple Permissions (OR)</h3>
 * <pre>{@code
 * @Command(name = "staff")
 * @Permission(value = {"staff.admin", "staff.moderator", "staff.helper"}, any = true)
 * public class StaffCommand {
 *     // Requires ANY of the permissions
 * }
 * }</pre>
 *
 * <h3>Hierarchical Permissions</h3>
 * <pre>{@code
 * @Command(name = "warp")
 * @Permission("warps.use")
 * public class WarpCommand {
 *
 *     @Subcommand("go")
 *     public void goToWarp(@Sender Player player, @Arg("name") String warp) {
 *         // Requires: warps.use
 *     }
 *
 *     @Subcommand("set")
 *     @Permission("warps.create")
 *     public void setWarp(@Sender Player player, @Arg("name") String warp) {
 *         // Requires: warps.use AND warps.create
 *     }
 *
 *     @Subcommand("delete")
 *     @Permission("warps.delete")
 *     public void deleteWarp(@Sender Player player, @Arg("name") String warp) {
 *         // Requires: warps.use AND warps.delete
 *     }
 * }
 * }</pre>
 *
 * <h3>Dynamic Permission Check</h3>
 * <pre>{@code
 * @Subcommand("setwarp")
 * @Permission("warps.set.{0}")  // {0} references first @Arg
 * public void setWarp(
 *     @Sender Player player,
 *     @Arg("name") String warpName
 * ) {
 *     // Permission checked: warps.set.<warpName>
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Command
 * @see Subcommand
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Permission {

    /**
     * The permission node(s) required to use this command.
     *
     * <p>Permission nodes follow the standard Minecraft permission format
     * (e.g., "plugin.command.action"). Supports placeholder syntax for
     * dynamic permissions based on arguments.</p>
     *
     * <pre>{@code
     * @Permission("essentials.fly")
     * @Permission({"admin.commands", "admin.tools"})
     * @Permission("kits.use.{0}")  // Dynamic based on first argument
     * }</pre>
     *
     * @return array of required permission nodes
     */
    String[] value();

    /**
     * Whether only one of the permissions is required (OR logic).
     *
     * <p>When {@code false} (default), the player must have ALL specified
     * permissions. When {@code true}, having ANY of the permissions is sufficient.</p>
     *
     * <pre>{@code
     * // Requires all permissions
     * @Permission({"perm.a", "perm.b"})
     *
     * // Requires any permission
     * @Permission(value = {"perm.a", "perm.b"}, any = true)
     * }</pre>
     *
     * @return {@code true} for OR logic, {@code false} for AND logic (default)
     */
    boolean any() default false;

    /**
     * The error message shown when permission is denied.
     *
     * <p>If empty, a default message is used. Supports color codes and
     * MiniMessage formatting.</p>
     *
     * <pre>{@code
     * @Permission(
     *     value = "admin.ban",
     *     message = "<red>You don't have permission to ban players!"
     * )
     * }</pre>
     *
     * @return custom permission denied message, empty for default
     */
    String message() default "";

    /**
     * Whether to inherit permissions from parent commands.
     *
     * <p>When {@code true} (default), a subcommand's permissions are combined
     * with its parent command's permissions. Set to {@code false} to ignore
     * parent permissions.</p>
     *
     * @return {@code true} to inherit parent permissions
     */
    boolean inherit() default true;
}
