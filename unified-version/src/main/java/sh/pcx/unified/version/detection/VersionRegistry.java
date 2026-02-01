/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.detection;

import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.version.api.ComponentBridge;
import sh.pcx.unified.version.api.NBTService;
import sh.pcx.unified.version.api.NMSBridge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for version-specific implementations.
 *
 * <p>This registry manages the mapping between NMS versions and their corresponding
 * implementation classes. It provides lazy loading and caching of version-specific
 * services.
 *
 * <h2>Architecture</h2>
 * <pre>
 * VersionRegistry
 *   ├── v1_20_R4/
 *   │   ├── NMSBridgeImpl
 *   │   ├── NBTServiceImpl
 *   │   └── ComponentBridgeImpl
 *   ├── v1_21_R1/
 *   │   ├── NMSBridgeImpl
 *   │   ├── NBTServiceImpl
 *   │   └── ComponentBridgeImpl
 *   └── ... (other versions)
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * VersionRegistry registry = VersionRegistry.getInstance();
 *
 * // Get NMS bridge for current version
 * NMSBridge nms = registry.getNMSBridge();
 *
 * // Get NBT service
 * NBTService nbt = registry.getNBTService();
 *
 * // Get component bridge
 * ComponentBridge components = registry.getComponentBridge();
 *
 * // Register custom implementation
 * registry.register("v1_21_R1", NMSBridge.class, MyCustomNMSBridge::new);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Implementations are loaded lazily and cached.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class VersionRegistry {

    private static final Logger LOGGER = Logger.getLogger(VersionRegistry.class.getName());

    // Singleton instance
    private static volatile VersionRegistry instance;

    // Implementation class mappings
    private final Map<String, Map<Class<?>, Supplier<?>>> registrations = new ConcurrentHashMap<>();

    // Cached instances
    private final Map<Class<?>, Object> cachedInstances = new ConcurrentHashMap<>();

    // Version detector
    private final MinecraftVersionDetector detector = MinecraftVersionDetector.getInstance();

    // Implementation package pattern
    private static final String IMPL_PACKAGE_PATTERN =
            "sh.pcx.unified.version.%s.%sImpl";

    /**
     * Private constructor - use {@link #getInstance()}.
     */
    private VersionRegistry() {
        registerDefaultImplementations();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the registry instance
     * @since 1.0.0
     */
    @NotNull
    public static VersionRegistry getInstance() {
        if (instance == null) {
            synchronized (VersionRegistry.class) {
                if (instance == null) {
                    instance = new VersionRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Gets the NMS bridge for the current server version.
     *
     * @return the NMS bridge implementation
     * @throws IllegalStateException if no implementation is available
     * @since 1.0.0
     */
    @NotNull
    public NMSBridge getNMSBridge() {
        return get(NMSBridge.class);
    }

    /**
     * Gets the NBT service for the current server version.
     *
     * @return the NBT service implementation
     * @throws IllegalStateException if no implementation is available
     * @since 1.0.0
     */
    @NotNull
    public NBTService getNBTService() {
        return get(NBTService.class);
    }

    /**
     * Gets the component bridge for the current server version.
     *
     * @return the component bridge implementation
     * @throws IllegalStateException if no implementation is available
     * @since 1.0.0
     */
    @NotNull
    public ComponentBridge getComponentBridge() {
        return get(ComponentBridge.class);
    }

    /**
     * Gets an implementation of the specified service type.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return the implementation
     * @throws IllegalStateException if no implementation is available
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull Class<T> serviceType) {
        // Check cache first
        Object cached = cachedInstances.get(serviceType);
        if (cached != null) {
            return (T) cached;
        }

        synchronized (cachedInstances) {
            // Double-check cache
            cached = cachedInstances.get(serviceType);
            if (cached != null) {
                return (T) cached;
            }

            // Try to create implementation
            T impl = createImplementation(serviceType);
            if (impl != null) {
                cachedInstances.put(serviceType, impl);
                return impl;
            }

            throw new IllegalStateException(
                    "No implementation available for " + serviceType.getSimpleName() +
                            " on version " + detector.getNmsVersion());
        }
    }

    /**
     * Gets an implementation as an optional.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return optional containing the implementation
     * @since 1.0.0
     */
    @NotNull
    public <T> Optional<T> getOptional(@NotNull Class<T> serviceType) {
        try {
            return Optional.of(get(serviceType));
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    /**
     * Registers a custom implementation supplier.
     *
     * @param <T>         the service type
     * @param nmsVersion  the NMS version string
     * @param serviceType the service interface class
     * @param supplier    the implementation supplier
     * @since 1.0.0
     */
    public <T> void register(@NotNull String nmsVersion,
                             @NotNull Class<T> serviceType,
                             @NotNull Supplier<T> supplier) {
        registrations.computeIfAbsent(nmsVersion, k -> new ConcurrentHashMap<>())
                .put(serviceType, supplier);

        // Clear cache if registering for current version
        if (nmsVersion.equals(detector.getNmsVersion())) {
            cachedInstances.remove(serviceType);
        }
    }

    /**
     * Checks if an implementation is available for the specified service.
     *
     * @param serviceType the service interface class
     * @return true if an implementation is available
     * @since 1.0.0
     */
    public boolean hasImplementation(@NotNull Class<?> serviceType) {
        String nmsVersion = detector.getNmsVersion();

        // Check registered suppliers
        Map<Class<?>, Supplier<?>> versionImpls = registrations.get(nmsVersion);
        if (versionImpls != null && versionImpls.containsKey(serviceType)) {
            return true;
        }

        // Check if class exists
        String className = getImplementationClassName(serviceType, nmsVersion);
        return classExists(className);
    }

    /**
     * Gets the current NMS version.
     *
     * @return the NMS version string
     * @since 1.0.0
     */
    @NotNull
    public String getCurrentNmsVersion() {
        return detector.getNmsVersion();
    }

    /**
     * Gets the current Minecraft version.
     *
     * @return the Minecraft version
     * @since 1.0.0
     */
    @NotNull
    public MinecraftVersion getCurrentVersion() {
        return detector.detect();
    }

    /**
     * Clears all cached instances.
     *
     * <p>Useful for testing or hot-reloading.
     *
     * @since 1.0.0
     */
    public void clearCache() {
        cachedInstances.clear();
    }

    // ===== Implementation Loading =====

    @Nullable
    @SuppressWarnings("unchecked")
    private <T> T createImplementation(@NotNull Class<T> serviceType) {
        String nmsVersion = detector.getNmsVersion();

        // Try registered supplier first
        Map<Class<?>, Supplier<?>> versionImpls = registrations.get(nmsVersion);
        if (versionImpls != null) {
            Supplier<?> supplier = versionImpls.get(serviceType);
            if (supplier != null) {
                try {
                    return (T) supplier.get();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to create implementation from supplier", e);
                }
            }
        }

        // Try reflective class loading
        String className = getImplementationClassName(serviceType, nmsVersion);
        try {
            Class<?> implClass = Class.forName(className);
            Constructor<?> constructor = implClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (ClassNotFoundException e) {
            LOGGER.fine("Implementation class not found: " + className);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to instantiate implementation: " + className, e);
        }

        return null;
    }

    @NotNull
    private String getImplementationClassName(@NotNull Class<?> serviceType, @NotNull String nmsVersion) {
        String simpleName = serviceType.getSimpleName();
        return String.format(IMPL_PACKAGE_PATTERN, nmsVersion.toLowerCase(), simpleName);
    }

    private boolean classExists(@NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void registerDefaultImplementations() {
        // Register default implementations for each version
        // These will be created dynamically based on available classes

        for (String nmsVersion : VersionConstants.getSupportedNmsVersions()) {
            // The actual registration happens dynamically when get() is called
            // This just initializes the version map
            registrations.computeIfAbsent(nmsVersion, k -> new ConcurrentHashMap<>());
        }

        LOGGER.fine("Initialized version registry for versions: " +
                String.join(", ", VersionConstants.getSupportedNmsVersions()));
    }

    /**
     * Factory interface for creating version-specific implementations.
     *
     * @param <T> the implementation type
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface ImplementationFactory<T> {
        /**
         * Creates a new implementation instance.
         *
         * @param version the Minecraft version
         * @return the implementation
         * @throws Exception if creation fails
         */
        T create(@NotNull MinecraftVersion version) throws Exception;
    }
}
