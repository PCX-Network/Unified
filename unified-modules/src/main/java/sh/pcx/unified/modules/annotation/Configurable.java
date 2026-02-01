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
 * Marks a module as configurable with its own configuration file or section.
 *
 * <p>Modules with this annotation have their configuration automatically loaded
 * and managed by the module system. Configuration can be embedded within the
 * main modules.yml or stored in a separate file per module.
 *
 * <h2>Configuration Modes</h2>
 * <ul>
 *   <li><b>Embedded</b>: Configuration stored within modules.yml under the module's section</li>
 *   <li><b>Separate</b>: Configuration stored in a dedicated file (e.g., battlepass.yml)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Embedded Configuration (default)</h3>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * @Configurable
 * public class BattlePassModule implements Initializable {
 *
 *     private BattlePassConfig config;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         this.config = context.loadConfig(BattlePassConfig.class);
 *     }
 * }
 *
 * // modules.yml
 * modules:
 *   BattlePass:
 *     enabled: true
 *     config:
 *       season: 5
 *       xp-multiplier: 1.5
 *       tiers: 100
 * }</pre>
 *
 * <h3>Separate Configuration File</h3>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * @Configurable(
 *     file = "battlepass.yml",
 *     embedded = false
 * )
 * public class BattlePassModule implements Initializable, Reloadable {
 *
 *     @Inject
 *     private BattlePassConfig config;
 *
 *     @Override
 *     public void reload(ModuleContext context) {
 *         // Config is automatically reloaded before this method is called
 *     }
 * }
 * }</pre>
 *
 * <h3>Config Class Example</h3>
 * <pre>{@code
 * @ConfigSection("battlepass")
 * public class BattlePassConfig {
 *
 *     @ConfigValue("season")
 *     private int season = 1;
 *
 *     @ConfigValue("xp-multiplier")
 *     private double xpMultiplier = 1.0;
 *
 *     @ConfigValue("tiers")
 *     private int tiers = 50;
 *
 *     // Getters and setters
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Module
 * @see sh.pcx.unified.modules.lifecycle.Reloadable
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configurable {

    /**
     * The configuration file name for this module.
     *
     * <p>When {@link #embedded()} is {@code false}, this specifies the
     * filename for the module's configuration within the plugin's data folder.
     *
     * <p>If not specified, defaults to the module name in lowercase with
     * a .yml extension (e.g., "battlepass.yml" for module "BattlePass").
     *
     * @return the configuration file name
     */
    String file() default "";

    /**
     * Whether the configuration is embedded in modules.yml.
     *
     * <p>When {@code true} (default), the module's configuration is stored
     * under the module's section in the main modules.yml file:
     *
     * <pre>
     * modules:
     *   MyModule:
     *     enabled: true
     *     config:
     *       key: value
     * </pre>
     *
     * <p>When {@code false}, configuration is stored in a separate file
     * specified by {@link #file()}.
     *
     * @return {@code true} for embedded configuration
     */
    boolean embedded() default true;

    /**
     * Whether to automatically reload configuration on module reload.
     *
     * <p>When {@code true} (default), the module's configuration is automatically
     * reloaded from disk before the {@link sh.pcx.unified.modules.lifecycle.Reloadable#reload}
     * method is called.
     *
     * <p>Set to {@code false} if you need manual control over configuration
     * reloading or if the module handles its own configuration lifecycle.
     *
     * @return {@code true} to auto-reload configuration
     */
    boolean autoReload() default true;

    /**
     * Whether to save default configuration if it doesn't exist.
     *
     * <p>When {@code true} (default), the module system will extract a default
     * configuration file from the module's resources (if present) when the
     * configuration file doesn't exist.
     *
     * @return {@code true} to save defaults automatically
     */
    boolean saveDefaults() default true;

    /**
     * The configuration class type for this module.
     *
     * <p>If specified, the module system will automatically instantiate and
     * populate this class with configuration values. The class should have
     * appropriate annotations for field mapping.
     *
     * <p>If not specified (void.class), the module must manually load its
     * configuration using {@link sh.pcx.unified.modules.core.ModuleContext#loadConfig}.
     *
     * @return the configuration class, or void.class for manual loading
     */
    Class<?> configClass() default void.class;
}
