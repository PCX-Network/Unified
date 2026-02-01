/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for detecting whether the server is running Folia.
 *
 * <p>Folia is a Paper fork that implements region-based multithreading for
 * Minecraft servers. This class provides methods to detect Folia at runtime
 * and access its specific APIs safely.
 *
 * <h2>Detection Mechanism</h2>
 * <p>Detection is performed by checking for the presence of Folia-specific
 * classes such as {@code io.papermc.paper.threadedregions.RegionizedServer}.
 * The detection result is cached after the first check for performance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * if (FoliaDetector.isFolia()) {
 *     // Use Folia's region-aware scheduling
 *     FoliaRegionScheduler scheduler = new FoliaRegionScheduler(plugin);
 *     scheduler.runAtLocation(location, () -> {
 *         // Task runs on the region that owns this location
 *     });
 * } else {
 *     // Fall back to standard Bukkit scheduler
 *     Bukkit.getScheduler().runTask(plugin, task);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this class are thread-safe. The detection result
 * is lazily computed and cached using atomic operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FoliaPlatform
 * @see FoliaFallback
 */
public final class FoliaDetector {

    private static final Logger LOGGER = Logger.getLogger(FoliaDetector.class.getName());

    /**
     * The fully qualified class name of Folia's RegionizedServer.
     */
    private static final String REGIONIZED_SERVER_CLASS =
            "io.papermc.paper.threadedregions.RegionizedServer";

    /**
     * The fully qualified class name of Folia's RegionScheduler.
     */
    private static final String REGION_SCHEDULER_CLASS =
            "io.papermc.paper.threadedregions.scheduler.RegionScheduler";

    /**
     * The fully qualified class name of Folia's AsyncScheduler.
     */
    private static final String ASYNC_SCHEDULER_CLASS =
            "io.papermc.paper.threadedregions.scheduler.AsyncScheduler";

    /**
     * The fully qualified class name of Folia's GlobalRegionScheduler.
     */
    private static final String GLOBAL_SCHEDULER_CLASS =
            "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler";

    /**
     * The fully qualified class name of Folia's EntityScheduler.
     */
    private static final String ENTITY_SCHEDULER_CLASS =
            "io.papermc.paper.threadedregions.scheduler.EntityScheduler";

    /**
     * Cached detection result. Null means not yet detected.
     */
    private static final AtomicReference<Boolean> FOLIA_DETECTED = new AtomicReference<>();

    /**
     * Cached RegionizedServer class reference.
     */
    private static final AtomicReference<Class<?>> REGIONIZED_SERVER = new AtomicReference<>();

    /**
     * Flag indicating if caching is complete.
     */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private FoliaDetector() {
        // Utility class - no instantiation
    }

    /**
     * Checks if the server is running Folia.
     *
     * <p>This method performs a one-time detection by checking for the
     * presence of Folia-specific classes. The result is cached for
     * subsequent calls.
     *
     * @return true if the server is running Folia
     * @since 1.0.0
     */
    public static boolean isFolia() {
        Boolean cached = FOLIA_DETECTED.get();
        if (cached != null) {
            return cached;
        }

        boolean detected = detectFolia();
        FOLIA_DETECTED.compareAndSet(null, detected);
        return FOLIA_DETECTED.get();
    }

    /**
     * Performs the actual Folia detection.
     *
     * @return true if Folia classes are present
     */
    private static boolean detectFolia() {
        try {
            Class<?> regionizedServer = Class.forName(REGIONIZED_SERVER_CLASS);
            REGIONIZED_SERVER.set(regionizedServer);

            // Verify additional Folia classes exist
            Class.forName(REGION_SCHEDULER_CLASS);
            Class.forName(GLOBAL_SCHEDULER_CLASS);
            Class.forName(ENTITY_SCHEDULER_CLASS);

            LOGGER.fine("Folia detected: RegionizedServer and scheduler classes found");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.fine("Folia not detected: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the Folia RegionizedServer class if available.
     *
     * @return an Optional containing the RegionizedServer class if Folia is detected
     * @since 1.0.0
     */
    @NotNull
    public static Optional<Class<?>> getRegionizedServerClass() {
        if (!isFolia()) {
            return Optional.empty();
        }
        return Optional.ofNullable(REGIONIZED_SERVER.get());
    }

    /**
     * Checks if the RegionScheduler API is available.
     *
     * @return true if the RegionScheduler can be used
     * @since 1.0.0
     */
    public static boolean hasRegionScheduler() {
        if (!isFolia()) {
            return false;
        }
        try {
            Class.forName(REGION_SCHEDULER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the AsyncScheduler API is available.
     *
     * @return true if the AsyncScheduler can be used
     * @since 1.0.0
     */
    public static boolean hasAsyncScheduler() {
        if (!isFolia()) {
            return false;
        }
        try {
            Class.forName(ASYNC_SCHEDULER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the GlobalRegionScheduler API is available.
     *
     * @return true if the GlobalRegionScheduler can be used
     * @since 1.0.0
     */
    public static boolean hasGlobalScheduler() {
        if (!isFolia()) {
            return false;
        }
        try {
            Class.forName(GLOBAL_SCHEDULER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the EntityScheduler API is available.
     *
     * @return true if the EntityScheduler can be used
     * @since 1.0.0
     */
    public static boolean hasEntityScheduler() {
        if (!isFolia()) {
            return false;
        }
        try {
            Class.forName(ENTITY_SCHEDULER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the current thread owns the region for a specific location.
     *
     * <p>In Folia, only the thread that owns a region can modify entities
     * and blocks within that region. This method checks ownership.
     *
     * @param world the world object (Bukkit World)
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return true if the current thread owns this region, or if not running Folia
     * @since 1.0.0
     */
    public static boolean isOwnedByCurrentRegion(@NotNull Object world, int chunkX, int chunkZ) {
        if (!isFolia()) {
            // On non-Folia servers, main thread owns everything
            return true;
        }

        try {
            // Use reflection to call Folia's ownership check
            Class<?> regionizedServer = Class.forName(REGIONIZED_SERVER_CLASS);
            Method isOwnedMethod = regionizedServer.getMethod(
                    "isOwnedByCurrentRegion",
                    Class.forName("org.bukkit.World"),
                    int.class,
                    int.class
            );
            return (boolean) isOwnedMethod.invoke(null, world, chunkX, chunkZ);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to check region ownership", e);
            return false;
        }
    }

    /**
     * Checks if the current thread is a region tick thread.
     *
     * @return true if the current thread is a Folia region tick thread
     * @since 1.0.0
     */
    public static boolean isTickThread() {
        if (!isFolia()) {
            return Thread.currentThread().getName().equals("Server thread");
        }

        try {
            Class<?> regionizedServer = Class.forName(REGIONIZED_SERVER_CLASS);
            Method isTickThreadMethod = regionizedServer.getMethod("isTickThread");
            return (boolean) isTickThreadMethod.invoke(null);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to check tick thread status", e);
            return false;
        }
    }

    /**
     * Checks if the current thread is the global region tick thread.
     *
     * @return true if on the global region thread
     * @since 1.0.0
     */
    public static boolean isGlobalTickThread() {
        if (!isFolia()) {
            return Thread.currentThread().getName().equals("Server thread");
        }

        try {
            Class<?> regionizedServer = Class.forName(REGIONIZED_SERVER_CLASS);
            Method isGlobalTickThreadMethod = regionizedServer.getMethod("isGlobalTickThread");
            return (boolean) isGlobalTickThreadMethod.invoke(null);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to check global tick thread status", e);
            return false;
        }
    }

    /**
     * Returns the Folia version string if available.
     *
     * @return an Optional containing the Folia version, or empty if not Folia
     * @since 1.0.0
     */
    @NotNull
    public static Optional<String> getFoliaVersion() {
        if (!isFolia()) {
            return Optional.empty();
        }

        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Method getVersionMethod = bukkitClass.getMethod("getVersion");
            String version = (String) getVersionMethod.invoke(null);

            // Extract Folia-specific version info if present
            if (version.contains("Folia")) {
                return Optional.of(version);
            }
            return Optional.of("Folia (version unknown)");
        } catch (Exception e) {
            return Optional.of("Folia");
        }
    }

    /**
     * Resets the cached detection state.
     *
     * <p>This method is primarily intended for testing purposes.
     * Under normal operation, detection should only happen once.
     *
     * @since 1.0.0
     */
    public static void resetCache() {
        FOLIA_DETECTED.set(null);
        REGIONIZED_SERVER.set(null);
        INITIALIZED.set(false);
    }
}
