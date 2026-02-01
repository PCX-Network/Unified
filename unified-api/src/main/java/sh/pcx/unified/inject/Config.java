/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting configuration values from the plugin configuration.
 *
 * <p>This annotation allows direct injection of configuration values into fields,
 * constructor parameters, or method parameters. Values are resolved from the plugin's
 * configuration file using a dot-notation path.</p>
 *
 * <h2>Supported Types</h2>
 * <ul>
 *   <li>Primitives: {@code int}, {@code long}, {@code double}, {@code boolean}</li>
 *   <li>Strings: {@code String}</li>
 *   <li>Lists: {@code List<String>}, {@code List<Integer>}, etc.</li>
 *   <li>Maps: {@code Map<String, Object>}</li>
 *   <li>Complex types: Any type with a registered deserializer</li>
 *   <li>Duration: {@code java.time.Duration} (e.g., "30s", "5m", "1h")</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Field Injection</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class PlayerManager {
 *     @Inject
 *     @Config("settings.cache-ttl")
 *     private int cacheTtl;
 *
 *     @Inject
 *     @Config("settings.max-players")
 *     private int maxPlayers;
 *
 *     @Inject
 *     @Config("messages.welcome")
 *     private String welcomeMessage;
 *
 *     @Inject
 *     @Config("features.enabled-worlds")
 *     private List<String> enabledWorlds;
 * }
 * }</pre>
 *
 * <h3>Constructor Injection</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class DatabaseConfig {
 *     private final String host;
 *     private final int port;
 *     private final String database;
 *     private final int poolSize;
 *
 *     @Inject
 *     public DatabaseConfig(
 *             @Config("database.host") String host,
 *             @Config("database.port") int port,
 *             @Config("database.name") String database,
 *             @Config("database.pool-size") int poolSize) {
 *         this.host = host;
 *         this.port = port;
 *         this.database = database;
 *         this.poolSize = poolSize;
 *     }
 * }
 * }</pre>
 *
 * <h3>With Default Values</h3>
 * <pre>{@code
 * @Service
 * public class OptionalFeature {
 *     @Inject
 *     @Config(value = "features.experimental", defaultValue = "false")
 *     private boolean experimentalEnabled;
 *
 *     @Inject
 *     @Config(value = "settings.timeout", defaultValue = "30")
 *     private int timeoutSeconds;
 *
 *     @Inject
 *     @Config(value = "messages.prefix", defaultValue = "[Server] ")
 *     private String messagePrefix;
 * }
 * }</pre>
 *
 * <h3>Complex Types</h3>
 * <pre>{@code
 * @Service
 * public class ScheduleService {
 *     // Duration parsing: "30s", "5m", "2h", "1d"
 *     @Inject
 *     @Config("schedule.cleanup-interval")
 *     private Duration cleanupInterval;
 *
 *     @Inject
 *     @Config("limits.rates")
 *     private Map<String, Integer> rateLimits;
 * }
 * }</pre>
 *
 * <h3>Configuration File Example</h3>
 * <pre>{@code
 * # config.yml
 * settings:
 *   cache-ttl: 300
 *   max-players: 100
 *
 * database:
 *   host: localhost
 *   port: 3306
 *   name: minecraft
 *   pool-size: 10
 *
 * features:
 *   enabled-worlds:
 *     - world
 *     - world_nether
 *     - world_the_end
 *
 * messages:
 *   welcome: "Welcome to the server!"
 *   prefix: "&7[&6Server&7] &f"
 *
 * schedule:
 *   cleanup-interval: 30m
 * }</pre>
 *
 * <h2>Reload Support</h2>
 * <p>Config values are resolved at injection time. To support hot-reload, combine
 * with {@link OnReload} annotation:</p>
 *
 * <pre>{@code
 * @Service
 * @Singleton
 * public class ConfigurableService {
 *     @Inject
 *     private ConfigService config;
 *
 *     private int maxRetries;
 *
 *     @PostConstruct
 *     public void init() {
 *         loadConfig();
 *     }
 *
 *     @OnReload
 *     public void onReload() {
 *         loadConfig();
 *     }
 *
 *     private void loadConfig() {
 *         this.maxRetries = config.get("settings.max-retries", 3);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see OnReload
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@BindingAnnotation
public @interface Config {

    /**
     * The configuration path using dot notation.
     *
     * <p>Paths are resolved relative to the plugin's root configuration.
     * Nested paths use dots as separators.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "debug"} - root level key</li>
     *   <li>{@code "database.host"} - nested key</li>
     *   <li>{@code "worlds.overworld.spawn"} - deeply nested key</li>
     * </ul>
     *
     * @return the configuration path
     */
    String value();

    /**
     * Default value if the configuration path does not exist.
     *
     * <p>The string value is automatically converted to the target type.
     * If empty and the path doesn't exist, an exception is thrown during injection.</p>
     *
     * @return the default value as a string, empty for required values
     */
    String defaultValue() default "";

    /**
     * Whether to throw an exception if the value is missing and no default is provided.
     *
     * <p>When {@code true} (default), a missing value without a default causes an
     * injection error. When {@code false}, the field is left with its Java default
     * (null for objects, 0 for numbers, false for booleans).</p>
     *
     * @return {@code true} if the value is required, {@code false} if optional
     */
    boolean required() default true;
}
