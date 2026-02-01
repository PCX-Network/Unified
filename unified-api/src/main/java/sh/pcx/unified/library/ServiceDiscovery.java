/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Interface for SPI-based service discovery.
 *
 * <p>ServiceDiscovery provides a unified way to discover and load service
 * implementations using Java's Service Provider Interface (SPI) mechanism,
 * enhanced with priority-based selection and filtering capabilities.
 *
 * <h2>Service Provider Interface (SPI)</h2>
 * <p>Services are registered by placing provider configuration files in
 * {@code META-INF/services/}. The file name is the fully qualified service
 * interface name, and the contents list implementation class names.
 *
 * <h3>Example: Registering a Service Provider</h3>
 * <pre>
 * // File: META-INF/services/com.example.MyService
 * com.example.impl.DefaultMyServiceImpl
 * com.example.impl.AlternativeMyServiceImpl
 * </pre>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * ServiceDiscovery discovery = // obtain discovery instance
 *
 * // Discover all implementations of a service
 * List<ServiceDefinition> definitions = discovery.discoverServices(MyService.class);
 * for (ServiceDefinition def : definitions) {
 *     System.out.println("Found: " + def.implementationClass());
 * }
 *
 * // Get the highest priority implementation
 * Optional<MyService> service = discovery.loadService(MyService.class);
 * service.ifPresent(s -> s.doSomething());
 *
 * // Load all implementations
 * List<MyService> allServices = discovery.loadAllServices(MyService.class);
 *
 * // Filter by properties
 * List<MyService> filtered = discovery.loadServices(
 *     MyService.class,
 *     def -> "mysql".equals(def.getProperty("driver"))
 * );
 *
 * // Discover with custom classloader
 * List<ServiceDefinition> customDefs = discovery.discoverServices(
 *     MyService.class,
 *     customClassLoader
 * );
 * }</pre>
 *
 * <h2>Priority-Based Selection</h2>
 * <p>When multiple implementations are available, they are ordered by priority.
 * Higher priority values are preferred. Use negative priority to disable
 * an implementation.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ServiceDefinition
 * @see UnifiedServiceLoader
 */
public interface ServiceDiscovery {

    /**
     * Discovers all service implementations for a given type.
     *
     * <p>This method scans {@code META-INF/services/} for provider configuration
     * files matching the service type and returns definitions for all listed
     * implementations.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return A list of service definitions, sorted by priority (highest first)
     * @throws NullPointerException if serviceType is null
     */
    <T> List<ServiceDefinition> discoverServices(Class<T> serviceType);

    /**
     * Discovers service implementations using a specific classloader.
     *
     * @param serviceType The service interface or abstract class
     * @param classLoader The classloader to use for discovery
     * @param <T>         The service type
     * @return A list of service definitions, sorted by priority (highest first)
     * @throws NullPointerException if serviceType or classLoader is null
     */
    <T> List<ServiceDefinition> discoverServices(Class<T> serviceType, ClassLoader classLoader);

    /**
     * Loads the highest priority implementation of a service.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return An Optional containing the service, or empty if none found
     * @throws NullPointerException if serviceType is null
     */
    <T> Optional<T> loadService(Class<T> serviceType);

    /**
     * Loads all implementations of a service.
     *
     * @param serviceType The service interface or abstract class
     * @param <T>         The service type
     * @return A list of service instances, sorted by priority (highest first)
     * @throws NullPointerException if serviceType is null
     */
    <T> List<T> loadAllServices(Class<T> serviceType);

    /**
     * Loads service implementations matching a filter.
     *
     * @param serviceType The service interface or abstract class
     * @param filter      A predicate to filter service definitions
     * @param <T>         The service type
     * @return A list of matching service instances
     * @throws NullPointerException if serviceType or filter is null
     */
    <T> List<T> loadServices(Class<T> serviceType, Predicate<ServiceDefinition> filter);

    /**
     * Loads a service implementation by class name.
     *
     * @param serviceType         The service interface or abstract class
     * @param implementationClass The fully qualified implementation class name
     * @param <T>                 The service type
     * @return An Optional containing the service, or empty if not found
     * @throws NullPointerException if serviceType or implementationClass is null
     */
    <T> Optional<T> loadServiceByClass(Class<T> serviceType, String implementationClass);

    /**
     * Checks if a service type has any registered implementations.
     *
     * @param serviceType The service interface or abstract class
     * @return true if at least one implementation is available
     * @throws NullPointerException if serviceType is null
     */
    boolean hasService(Class<?> serviceType);

    /**
     * Gets the number of registered implementations for a service.
     *
     * @param serviceType The service interface or abstract class
     * @return The count of available implementations
     * @throws NullPointerException if serviceType is null
     */
    int getServiceCount(Class<?> serviceType);

    /**
     * Registers a service implementation programmatically.
     *
     * <p>This allows services to be registered without using SPI files.
     * Registered services are merged with SPI-discovered services.
     *
     * @param definition The service definition to register
     * @throws NullPointerException  if definition is null
     * @throws IllegalStateException if a service with the same class is already registered
     */
    void register(ServiceDefinition definition);

    /**
     * Registers a service instance directly.
     *
     * <p>The instance is wrapped in a definition and registered with
     * the given priority.
     *
     * @param serviceType The service interface
     * @param instance    The service instance
     * @param priority    The priority value
     * @param <T>         The service type
     * @throws NullPointerException if serviceType or instance is null
     */
    <T> void registerInstance(Class<T> serviceType, T instance, int priority);

    /**
     * Unregisters a service implementation.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @return true if the service was unregistered
     * @throws NullPointerException if serviceType or implementationClass is null
     */
    boolean unregister(Class<?> serviceType, String implementationClass);

    /**
     * Clears all registered and cached services.
     *
     * <p>After calling this method, services will be rediscovered
     * on next access.
     */
    void clearCache();

    /**
     * Gets the classloader used for default service discovery.
     *
     * @return The default classloader
     */
    ClassLoader getDefaultClassLoader();

    /**
     * Sets the classloader used for default service discovery.
     *
     * @param classLoader The classloader to use
     * @throws NullPointerException if classLoader is null
     */
    void setDefaultClassLoader(ClassLoader classLoader);
}
