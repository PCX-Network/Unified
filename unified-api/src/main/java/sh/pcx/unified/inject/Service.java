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
 * Marks a class as an injectable service within the UnifiedPlugin dependency injection system.
 *
 * <p>Classes annotated with {@code @Service} are automatically discovered and registered
 * with the Guice injector during plugin initialization. This annotation can be combined
 * with scope annotations ({@link PlayerScoped}, {@link WorldScoped}, {@link PluginScoped})
 * to control instance lifecycle.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Service</h3>
 * <pre>{@code
 * @Service
 * public class PlayerManager {
 *     @Inject
 *     private DatabaseService database;
 *
 *     public void loadPlayer(UUID uuid) {
 *         // Service implementation
 *     }
 * }
 * }</pre>
 *
 * <h3>Singleton Service</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class CacheManager {
 *     private final Map<String, Object> cache = new ConcurrentHashMap<>();
 *
 *     public <T> T get(String key, Class<T> type) {
 *         return type.cast(cache.get(key));
 *     }
 * }
 * }</pre>
 *
 * <h3>Player-Scoped Service</h3>
 * <pre>{@code
 * @Service
 * @PlayerScoped
 * public class PlayerSession {
 *     private final UUID playerId;
 *     private long loginTime;
 *
 *     @Inject
 *     public PlayerSession(UnifiedPlayer player) {
 *         this.playerId = player.getUniqueId();
 *         this.loginTime = System.currentTimeMillis();
 *     }
 * }
 * }</pre>
 *
 * <h3>Service with Interface Binding</h3>
 * <pre>{@code
 * public interface EconomyProvider {
 *     double getBalance(UUID player);
 * }
 *
 * @Service
 * @Singleton
 * public class VaultEconomyProvider implements EconomyProvider {
 *     @Override
 *     public double getBalance(UUID player) {
 *         // Vault integration
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PlayerScoped
 * @see WorldScoped
 * @see PluginScoped
 * @see com.google.inject.Singleton
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@BindingAnnotation
public @interface Service {

    /**
     * Optional name for the service, used for named bindings.
     *
     * <p>When specified, the service is bound with this name and can be injected
     * using {@code @Named("name")} qualifier.</p>
     *
     * <pre>{@code
     * @Service(name = "vault")
     * public class VaultEconomy implements Economy { }
     *
     * @Service(name = "token")
     * public class TokenEconomy implements Economy { }
     *
     * // Injection
     * @Inject @Named("vault")
     * private Economy vaultEconomy;
     * }</pre>
     *
     * @return the service name, empty string for default binding
     */
    String name() default "";

    /**
     * Indicates whether this service should be eagerly initialized.
     *
     * <p>When {@code true}, the service is instantiated immediately when the
     * injector is created, rather than lazily on first use. This is useful
     * for services that need to perform initialization at startup.</p>
     *
     * <pre>{@code
     * @Service(eager = true)
     * @Singleton
     * public class DatabaseConnectionPool {
     *     @Inject
     *     public DatabaseConnectionPool(DatabaseConfig config) {
     *         // Initialize connection pool at startup
     *     }
     * }
     * }</pre>
     *
     * @return {@code true} for eager initialization, {@code false} for lazy (default)
     */
    boolean eager() default false;
}
