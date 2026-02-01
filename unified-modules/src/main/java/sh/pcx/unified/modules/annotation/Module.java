/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a plugin module within the UnifiedPlugin module system.
 *
 * <p>Modules are self-contained feature units that can be independently loaded,
 * enabled, disabled, and reloaded at runtime. Each module has its own lifecycle
 * and can declare dependencies on other modules.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic discovery via package scanning</li>
 *   <li>Dependency resolution with cycle detection</li>
 *   <li>Hot reload support without server restart</li>
 *   <li>Config-driven enable/disable via modules.yml</li>
 *   <li>Health monitoring with TPS awareness</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Module</h3>
 * <pre>{@code
 * @Module(name = "Teleportation")
 * @Listen
 * public class TeleportModule implements Listener {
 *
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         // Handle event
 *     }
 * }
 * }</pre>
 *
 * <h3>Full-Featured Module</h3>
 * <pre>{@code
 * @Module(
 *     name = "BattlePass",
 *     description = "Seasonal battle pass progression system",
 *     version = "2.1.0",
 *     authors = {"PCXNetwork"},
 *     dependencies = {"Economy", "PlayerData"},
 *     softDependencies = {"Cosmetics"},
 *     priority = ModulePriority.HIGH
 * )
 * @Listen
 * @Command
 * public class BattlePassModule implements
 *         Listener,
 *         TabExecutor,
 *         Initializable,
 *         Reloadable,
 *         Healthy {
 *     // Module implementation
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Listen
 * @see Command
 * @see Configurable
 * @see sh.pcx.unified.modules.lifecycle.Initializable
 * @see sh.pcx.unified.modules.lifecycle.Reloadable
 * @see sh.pcx.unified.modules.lifecycle.Healthy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Module {

    /**
     * The unique name of this module.
     *
     * <p>This name is used for:
     * <ul>
     *   <li>Identifying the module in logs and commands</li>
     *   <li>Configuration keys in modules.yml</li>
     *   <li>Dependency references from other modules</li>
     *   <li>Admin command arguments (/modules enable MyModule)</li>
     * </ul>
     *
     * <p>Module names should be concise, descriptive, and use PascalCase.
     *
     * @return the module name
     */
    String name();

    /**
     * A human-readable description of what this module does.
     *
     * <p>This description is displayed in admin commands and module info views.
     *
     * @return the module description, empty string if not provided
     */
    String description() default "";

    /**
     * The version of this module.
     *
     * <p>Version strings should follow semantic versioning (e.g., "1.0.0", "2.1.3").
     * This is displayed in module info and can be used for compatibility checks.
     *
     * @return the module version string
     */
    String version() default "1.0.0";

    /**
     * The authors of this module.
     *
     * <p>A list of names or identifiers of the module's authors/maintainers.
     *
     * @return an array of author names
     */
    String[] authors() default {};

    /**
     * Required dependencies that must be loaded before this module.
     *
     * <p>If any dependency is missing or fails to load, this module will not be enabled.
     * Dependencies are loaded in topological order based on the dependency graph.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * @Module(
     *     name = "Rewards",
     *     dependencies = {"Economy", "PlayerData"}
     * )
     * }</pre>
     *
     * @return an array of required module names
     */
    String[] dependencies() default {};

    /**
     * Optional dependencies that enhance functionality if present.
     *
     * <p>Soft dependencies do not prevent module loading if missing. Modules should
     * check for soft dependency availability at runtime and adapt accordingly.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * @Module(
     *     name = "Rewards",
     *     softDependencies = {"Discord", "WebPanel"}
     * )
     * public class RewardsModule implements Initializable {
     *     @Inject @Nullable private DiscordService discord;
     *
     *     @Override
     *     public void init(ModuleContext context) {
     *         if (discord != null) {
     *             discord.registerNotifications(this);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @return an array of optional module names
     */
    String[] softDependencies() default {};

    /**
     * The loading priority of this module.
     *
     * <p>Higher priority modules are loaded before lower priority ones,
     * after dependency resolution. This allows fine-grained control over
     * load order for modules at the same dependency level.
     *
     * @return the module priority
     */
    ModulePriority priority() default ModulePriority.NORMAL;

    /**
     * Whether this module should be isolated in its own classloader.
     *
     * <p>Isolated modules:
     * <ul>
     *   <li>Have their own classloader for library isolation</li>
     *   <li>Can load different versions of libraries than other modules</li>
     *   <li>Failures are contained and don't affect other modules</li>
     *   <li>Have higher memory overhead</li>
     * </ul>
     *
     * <p>Use isolation for third-party integrations or modules with
     * conflicting library requirements.
     *
     * @return {@code true} for isolated classloader, {@code false} for shared (default)
     */
    boolean isolated() default false;

    /**
     * Whether this module is enabled by default.
     *
     * <p>This value is used when the module is first registered and no
     * configuration exists. After initial registration, the config value
     * in modules.yml takes precedence.
     *
     * @return {@code true} if enabled by default, {@code false} otherwise
     */
    boolean enabledByDefault() default true;
}
