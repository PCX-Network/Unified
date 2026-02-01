/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.service;

/**
 * Marker interface for services that can be registered with the {@link ServiceRegistry}.
 *
 * <p>Services are singleton-like components that provide specific functionality
 * to plugins. They are registered by service providers and can be looked up
 * by their interface type.
 *
 * <h2>Implementing a Service</h2>
 * <pre>{@code
 * // Define the service interface
 * public interface EconomyService extends Service {
 *     double getBalance(UUID player);
 *     void setBalance(UUID player, double amount);
 *     CompletableFuture<Boolean> transfer(UUID from, UUID to, double amount);
 * }
 *
 * // Implement the service
 * public class VaultEconomyService implements EconomyService {
 *     private final Economy vaultEconomy;
 *
 *     public VaultEconomyService(Economy vaultEconomy) {
 *         this.vaultEconomy = vaultEconomy;
 *     }
 *
 *     @Override
 *     public double getBalance(UUID player) {
 *         return vaultEconomy.getBalance(Bukkit.getOfflinePlayer(player));
 *     }
 *
 *     // ... other methods
 * }
 *
 * // Register the service
 * services.register(EconomyService.class, new VaultEconomyService(vaultEco));
 *
 * // Use the service
 * EconomyService economy = services.get(EconomyService.class).orElseThrow();
 * double balance = economy.getBalance(player.getUniqueId());
 * }</pre>
 *
 * <h2>Service Lifecycle</h2>
 * <p>Services may optionally implement lifecycle methods by also implementing
 * additional interfaces:
 * <ul>
 *   <li>Implement initialization logic in the constructor or a setup method</li>
 *   <li>Implement {@link AutoCloseable} for cleanup when the service is unregistered</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Services should be thread-safe as they may be accessed from multiple threads
 * simultaneously. Use appropriate synchronization or immutable state.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ServiceRegistry
 * @see ServiceProvider
 */
public interface Service {

    /**
     * Returns the name of this service.
     *
     * <p>The default implementation returns the simple class name.
     * Override to provide a custom name.
     *
     * @return the service name
     * @since 1.0.0
     */
    default String getServiceName() {
        return getClass().getSimpleName();
    }

    /**
     * Returns whether this service is currently available.
     *
     * <p>Some services may become temporarily unavailable (e.g., database
     * connection lost). This method allows checking service health.
     *
     * @return true if the service is available
     * @since 1.0.0
     */
    default boolean isAvailable() {
        return true;
    }
}
