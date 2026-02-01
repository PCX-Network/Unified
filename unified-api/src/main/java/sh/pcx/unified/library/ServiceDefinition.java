/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Map;
import java.util.Objects;

/**
 * Metadata record for a discovered service.
 *
 * <p>ServiceDefinition contains all information needed to load and instantiate
 * a service discovered through the Service Provider Interface (SPI). It extends
 * the standard SPI mechanism with additional metadata like priority and configuration.
 *
 * <h2>Service Provider Interface (SPI)</h2>
 * <p>Services are discovered by looking for provider configuration files in
 * {@code META-INF/services/}. This record represents the parsed information
 * from those files, enhanced with additional metadata.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a service definition
 * ServiceDefinition def = new ServiceDefinition(
 *     DatabaseService.class,
 *     "com.example.MySQLDatabaseService",
 *     100,  // priority
 *     Map.of("driver", "mysql")
 * );
 *
 * // Access metadata
 * System.out.println("Service: " + def.serviceType().getSimpleName());
 * System.out.println("Implementation: " + def.implementationClass());
 * System.out.println("Priority: " + def.priority());
 *
 * // Use with service discovery
 * ServiceDiscovery discovery = // obtain discovery
 * List<ServiceDefinition> defs = discovery.discoverServices(MyService.class);
 * defs.stream()
 *     .sorted(Comparator.comparingInt(ServiceDefinition::priority).reversed())
 *     .forEach(d -> System.out.println(d.implementationClass()));
 * }</pre>
 *
 * <h2>Priority Values</h2>
 * <ul>
 *   <li><b>1000+</b>: Framework-level services</li>
 *   <li><b>500-999</b>: Platform-specific implementations</li>
 *   <li><b>100-499</b>: Plugin default implementations</li>
 *   <li><b>0-99</b>: Fallback implementations</li>
 *   <li><b>Negative</b>: Disabled implementations</li>
 * </ul>
 *
 * @param serviceType         The service interface or abstract class
 * @param implementationClass The fully qualified name of the implementation class
 * @param priority            The priority for ordering (higher = more preferred)
 * @param properties          Additional service properties
 * @author Supatuck
 * @since 1.0.0
 * @see ServiceDiscovery
 * @see UnifiedServiceLoader
 */
public record ServiceDefinition(
        Class<?> serviceType,
        String implementationClass,
        int priority,
        Map<String, String> properties
) {

    /**
     * Default priority for services without explicit priority.
     */
    public static final int DEFAULT_PRIORITY = 100;

    /**
     * Priority for framework-provided services.
     */
    public static final int FRAMEWORK_PRIORITY = 1000;

    /**
     * Priority for platform-specific implementations.
     */
    public static final int PLATFORM_PRIORITY = 500;

    /**
     * Priority for fallback implementations.
     */
    public static final int FALLBACK_PRIORITY = 0;

    /**
     * Priority value that disables a service.
     */
    public static final int DISABLED_PRIORITY = -1;

    /**
     * Creates a new ServiceDefinition with validation.
     *
     * @param serviceType         The service interface or abstract class
     * @param implementationClass The implementation class name
     * @param priority            The priority value
     * @param properties          Additional properties (may be null)
     * @throws NullPointerException     if serviceType or implementationClass is null
     * @throws IllegalArgumentException if implementationClass is empty
     */
    public ServiceDefinition {
        Objects.requireNonNull(serviceType, "Service type cannot be null");
        Objects.requireNonNull(implementationClass, "Implementation class cannot be null");

        if (implementationClass.isEmpty()) {
            throw new IllegalArgumentException("Implementation class cannot be empty");
        }

        // Make properties immutable
        properties = properties != null ? Map.copyOf(properties) : Map.of();
    }

    /**
     * Creates a ServiceDefinition with default priority.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @return A new ServiceDefinition with DEFAULT_PRIORITY
     */
    public static ServiceDefinition of(Class<?> serviceType, String implementationClass) {
        return new ServiceDefinition(serviceType, implementationClass, DEFAULT_PRIORITY, Map.of());
    }

    /**
     * Creates a ServiceDefinition with specified priority.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @param priority            The priority value
     * @return A new ServiceDefinition
     */
    public static ServiceDefinition of(Class<?> serviceType, String implementationClass, int priority) {
        return new ServiceDefinition(serviceType, implementationClass, priority, Map.of());
    }

    /**
     * Creates a framework-level ServiceDefinition.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @return A ServiceDefinition with FRAMEWORK_PRIORITY
     */
    public static ServiceDefinition framework(Class<?> serviceType, String implementationClass) {
        return new ServiceDefinition(serviceType, implementationClass, FRAMEWORK_PRIORITY, Map.of());
    }

    /**
     * Creates a platform-specific ServiceDefinition.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @return A ServiceDefinition with PLATFORM_PRIORITY
     */
    public static ServiceDefinition platform(Class<?> serviceType, String implementationClass) {
        return new ServiceDefinition(serviceType, implementationClass, PLATFORM_PRIORITY, Map.of());
    }

    /**
     * Creates a fallback ServiceDefinition.
     *
     * @param serviceType         The service interface
     * @param implementationClass The implementation class name
     * @return A ServiceDefinition with FALLBACK_PRIORITY
     */
    public static ServiceDefinition fallback(Class<?> serviceType, String implementationClass) {
        return new ServiceDefinition(serviceType, implementationClass, FALLBACK_PRIORITY, Map.of());
    }

    /**
     * Gets a property value.
     *
     * @param key The property key
     * @return The property value, or null if not found
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Gets a property value with a default.
     *
     * @param key          The property key
     * @param defaultValue The default value if not found
     * @return The property value or default
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if a property exists.
     *
     * @param key The property key
     * @return true if the property is set
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Checks if this service is enabled (priority >= 0).
     *
     * @return true if the service should be loaded
     */
    public boolean isEnabled() {
        return priority >= 0;
    }

    /**
     * Checks if this service is disabled (priority < 0).
     *
     * @return true if the service should not be loaded
     */
    public boolean isDisabled() {
        return priority < 0;
    }

    /**
     * Creates a copy with a different priority.
     *
     * @param newPriority The new priority value
     * @return A new ServiceDefinition with updated priority
     */
    public ServiceDefinition withPriority(int newPriority) {
        return new ServiceDefinition(serviceType, implementationClass, newPriority, properties);
    }

    /**
     * Creates a copy with additional properties.
     *
     * @param additionalProperties Properties to add
     * @return A new ServiceDefinition with merged properties
     */
    public ServiceDefinition withProperties(Map<String, String> additionalProperties) {
        Map<String, String> merged = new java.util.HashMap<>(properties);
        merged.putAll(additionalProperties);
        return new ServiceDefinition(serviceType, implementationClass, priority, merged);
    }

    /**
     * Creates a disabled copy of this definition.
     *
     * @return A new ServiceDefinition with DISABLED_PRIORITY
     */
    public ServiceDefinition disabled() {
        return new ServiceDefinition(serviceType, implementationClass, DISABLED_PRIORITY, properties);
    }

    /**
     * Gets the simple name of the implementation class.
     *
     * @return The class simple name
     */
    public String simpleClassName() {
        int lastDot = implementationClass.lastIndexOf('.');
        return lastDot >= 0 ? implementationClass.substring(lastDot + 1) : implementationClass;
    }

    /**
     * Gets the package name of the implementation class.
     *
     * @return The package name, or empty string if in default package
     */
    public String packageName() {
        int lastDot = implementationClass.lastIndexOf('.');
        return lastDot >= 0 ? implementationClass.substring(0, lastDot) : "";
    }

    /**
     * Returns a string representation of this service definition.
     *
     * @return A descriptive string
     */
    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "service=" + serviceType.getSimpleName() +
                ", impl=" + simpleClassName() +
                ", priority=" + priority +
                '}';
    }
}
