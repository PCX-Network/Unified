/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.scheduler.detection;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Detects the available scheduler type based on the server platform.
 *
 * <p>SchedulerDetector probes for platform-specific scheduler APIs and
 * determines the most appropriate scheduler implementation to use.
 *
 * <h2>Detection Order</h2>
 * <ol>
 *   <li>Folia - Check for Folia's region scheduler API</li>
 *   <li>Paper - Check for Paper's async scheduler enhancements</li>
 *   <li>Sponge - Check for Sponge's scheduler API</li>
 *   <li>Bukkit - Fall back to standard Bukkit scheduler</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * SchedulerType type = SchedulerDetector.detect();
 *
 * switch (type) {
 *     case FOLIA -> log.info("Using Folia region-aware scheduler");
 *     case PAPER -> log.info("Using Paper scheduler");
 *     case BUKKIT -> log.info("Using Bukkit scheduler");
 *     case SPONGE -> log.info("Using Sponge scheduler");
 * }
 *
 * // Check specific features
 * if (SchedulerDetector.isFolia()) {
 *     // Use region-aware scheduling
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchedulerAdapter
 */
public final class SchedulerDetector {

    private static final Logger LOGGER = Logger.getLogger(SchedulerDetector.class.getName());

    private static volatile SchedulerType cachedType;
    private static volatile Boolean isFolia;
    private static volatile Boolean isPaper;
    private static volatile Boolean isSponge;

    private SchedulerDetector() {
        // Utility class
    }

    /**
     * Detects the available scheduler type.
     *
     * <p>The result is cached after the first call.
     *
     * @return the detected scheduler type
     * @since 1.0.0
     */
    @NotNull
    public static SchedulerType detect() {
        if (cachedType != null) {
            return cachedType;
        }

        synchronized (SchedulerDetector.class) {
            if (cachedType != null) {
                return cachedType;
            }

            if (checkFolia()) {
                LOGGER.info("Detected Folia - using region-aware scheduler");
                cachedType = SchedulerType.FOLIA;
            } else if (checkSponge()) {
                LOGGER.info("Detected Sponge - using Sponge scheduler");
                cachedType = SchedulerType.SPONGE;
            } else if (checkPaper()) {
                LOGGER.info("Detected Paper - using Paper scheduler");
                cachedType = SchedulerType.PAPER;
            } else if (checkBukkit()) {
                LOGGER.info("Detected Bukkit/Spigot - using Bukkit scheduler");
                cachedType = SchedulerType.BUKKIT;
            } else {
                LOGGER.warning("Unknown platform - using fallback scheduler");
                cachedType = SchedulerType.UNKNOWN;
            }

            return cachedType;
        }
    }

    /**
     * Checks if the server is running Folia.
     *
     * @return true if Folia is detected
     * @since 1.0.0
     */
    public static boolean isFolia() {
        if (isFolia != null) {
            return isFolia;
        }

        synchronized (SchedulerDetector.class) {
            if (isFolia != null) {
                return isFolia;
            }
            isFolia = checkFolia();
            return isFolia;
        }
    }

    /**
     * Checks if the server is running Paper (or a Paper fork).
     *
     * @return true if Paper is detected
     * @since 1.0.0
     */
    public static boolean isPaper() {
        if (isPaper != null) {
            return isPaper;
        }

        synchronized (SchedulerDetector.class) {
            if (isPaper != null) {
                return isPaper;
            }
            isPaper = checkPaper();
            return isPaper;
        }
    }

    /**
     * Checks if the server is running Sponge.
     *
     * @return true if Sponge is detected
     * @since 1.0.0
     */
    public static boolean isSponge() {
        if (isSponge != null) {
            return isSponge;
        }

        synchronized (SchedulerDetector.class) {
            if (isSponge != null) {
                return isSponge;
            }
            isSponge = checkSponge();
            return isSponge;
        }
    }

    /**
     * Checks if the server supports async chunk loading.
     *
     * <p>Paper and its forks support async chunk loading.
     *
     * @return true if async chunk loading is available
     * @since 1.0.0
     */
    public static boolean supportsAsyncChunks() {
        return isPaper() || isFolia();
    }

    /**
     * Checks if the server supports region-based threading.
     *
     * <p>Only Folia uses region-based threading.
     *
     * @return true if region threading is available
     * @since 1.0.0
     */
    public static boolean supportsRegionThreading() {
        return isFolia();
    }

    /**
     * Checks if entity-bound task scheduling is available.
     *
     * <p>Folia provides native entity-bound scheduling.
     * Other platforms simulate this with main thread execution.
     *
     * @return true if native entity scheduling is available
     * @since 1.0.0
     */
    public static boolean supportsEntityScheduling() {
        return isFolia();
    }

    /**
     * Returns information about the detected scheduler.
     *
     * @return a string describing the scheduler
     * @since 1.0.0
     */
    @NotNull
    public static String getSchedulerInfo() {
        SchedulerType type = detect();
        StringBuilder info = new StringBuilder();
        info.append("Scheduler Type: ").append(type.getDisplayName());
        info.append("\nRegion Threading: ").append(supportsRegionThreading() ? "Yes" : "No");
        info.append("\nAsync Chunks: ").append(supportsAsyncChunks() ? "Yes" : "No");
        info.append("\nEntity Scheduling: ").append(supportsEntityScheduling() ? "Native" : "Simulated");
        return info.toString();
    }

    // ==================== Internal Detection Methods ====================

    private static boolean checkFolia() {
        try {
            // Check for Folia's RegionScheduler
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            // Also verify the scheduler API exists
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean checkPaper() {
        try {
            // Check for Paper-specific class
            Class.forName("io.papermc.paper.util.Tick");
            return true;
        } catch (ClassNotFoundException e) {
            // Try alternative Paper detection
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    private static boolean checkBukkit() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean checkSponge() {
        try {
            Class.forName("org.spongepowered.api.Sponge");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Resets the cached detection results.
     *
     * <p>This is primarily useful for testing.
     *
     * @since 1.0.0
     */
    public static void resetCache() {
        synchronized (SchedulerDetector.class) {
            cachedType = null;
            isFolia = null;
            isPaper = null;
            isSponge = null;
        }
    }

    /**
     * Enumeration of scheduler types.
     *
     * @since 1.0.0
     */
    public enum SchedulerType {

        /**
         * Folia's region-aware scheduler.
         */
        FOLIA("Folia", true, true),

        /**
         * Paper's enhanced scheduler.
         */
        PAPER("Paper", true, false),

        /**
         * Standard Bukkit scheduler.
         */
        BUKKIT("Bukkit", false, false),

        /**
         * Sponge scheduler.
         */
        SPONGE("Sponge", true, false),

        /**
         * Unknown/unsupported platform.
         */
        UNKNOWN("Unknown", false, false);

        private final String displayName;
        private final boolean asyncChunks;
        private final boolean regionThreading;

        SchedulerType(String displayName, boolean asyncChunks, boolean regionThreading) {
            this.displayName = displayName;
            this.asyncChunks = asyncChunks;
            this.regionThreading = regionThreading;
        }

        /**
         * Returns the human-readable name.
         *
         * @return the display name
         */
        @NotNull
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns whether this platform supports async chunk operations.
         *
         * @return true if async chunks are supported
         */
        public boolean supportsAsyncChunks() {
            return asyncChunks;
        }

        /**
         * Returns whether this platform uses region threading.
         *
         * @return true if region threading is used
         */
        public boolean usesRegionThreading() {
            return regionThreading;
        }
    }
}
