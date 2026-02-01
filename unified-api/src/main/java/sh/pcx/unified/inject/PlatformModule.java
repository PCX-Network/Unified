/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.AbstractModule;
import sh.pcx.unified.inject.Platform.PlatformType;

import java.util.logging.Logger;

/**
 * Base module for platform-specific bindings.
 *
 * <p>PlatformModule provides a framework for implementing platform-specific
 * functionality in the UnifiedPlugin API. Each supported platform (Paper, Spigot,
 * Folia, Sponge) can have its own module that binds platform-appropriate
 * implementations of shared interfaces.</p>
 *
 * <h2>Platform Detection</h2>
 * <p>Platform detection is performed automatically at runtime. The appropriate
 * PlatformModule subclass should be installed based on the detected platform.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Paper Platform Module</h3>
 * <pre>{@code
 * public class PaperModule extends PlatformModule {
 *
 *     @Override
 *     public PlatformType getPlatformType() {
 *         return PlatformType.PAPER;
 *     }
 *
 *     @Override
 *     protected void configurePlatform() {
 *         // Bind Paper-specific implementations
 *         bind(SchedulerService.class)
 *             .annotatedWith(Platform.class)
 *             .to(PaperSchedulerService.class)
 *             .in(Singleton.class);
 *
 *         bind(ChunkService.class)
 *             .annotatedWith(Platform.class)
 *             .to(PaperAsyncChunkService.class)
 *             .in(Singleton.class);
 *
 *         bind(ComponentService.class)
 *             .annotatedWith(Platform.class)
 *             .to(PaperAdventureService.class)
 *             .in(Singleton.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Folia Platform Module</h3>
 * <pre>{@code
 * public class FoliaModule extends PlatformModule {
 *
 *     @Override
 *     public PlatformType getPlatformType() {
 *         return PlatformType.FOLIA;
 *     }
 *
 *     @Override
 *     protected void configurePlatform() {
 *         // Folia requires region-aware scheduling
 *         bind(SchedulerService.class)
 *             .annotatedWith(Platform.class)
 *             .to(FoliaRegionScheduler.class)
 *             .in(Singleton.class);
 *
 *         // Bind region-aware services
 *         bind(RegionTaskExecutor.class).in(Singleton.class);
 *     }
 *
 *     @Override
 *     public boolean supportsFeature(String feature) {
 *         return switch (feature) {
 *             case "REGION_SCHEDULING" -> true;
 *             case "GLOBAL_TICK" -> false;  // Folia doesn't have global tick
 *             default -> super.supportsFeature(feature);
 *         };
 *     }
 * }
 * }</pre>
 *
 * <h3>Spigot Platform Module</h3>
 * <pre>{@code
 * public class SpigotModule extends PlatformModule {
 *
 *     @Override
 *     public PlatformType getPlatformType() {
 *         return PlatformType.SPIGOT;
 *     }
 *
 *     @Override
 *     protected void configurePlatform() {
 *         bind(SchedulerService.class)
 *             .annotatedWith(Platform.class)
 *             .to(BukkitSchedulerService.class)
 *             .in(Singleton.class);
 *
 *         // Spigot uses sync chunk loading
 *         bind(ChunkService.class)
 *             .annotatedWith(Platform.class)
 *             .to(SpigotSyncChunkService.class)
 *             .in(Singleton.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Installing Platform Modules</h3>
 * <pre>{@code
 * public class MyPluginModule extends UnifiedModule {
 *
 *     private final PlatformType platform;
 *
 *     public MyPluginModule(PlatformType platform) {
 *         this.platform = platform;
 *     }
 *
 *     @Override
 *     protected void configure() {
 *         super.configure();
 *
 *         // Install platform-specific module
 *         switch (platform) {
 *             case PAPER -> install(new PaperModule());
 *             case FOLIA -> install(new FoliaModule());
 *             case SPIGOT -> install(new SpigotModule());
 *             case SPONGE -> install(new SpongeModule());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Platform Detection Utility</h3>
 * <pre>{@code
 * public class PlatformDetector {
 *     public static PlatformType detect() {
 *         try {
 *             Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
 *             return PlatformType.FOLIA;
 *         } catch (ClassNotFoundException ignored) {}
 *
 *         try {
 *             Class.forName("io.papermc.paper.configuration.PaperConfigurations");
 *             return PlatformType.PAPER;
 *         } catch (ClassNotFoundException ignored) {}
 *
 *         try {
 *             Class.forName("org.spongepowered.api.Sponge");
 *             return PlatformType.SPONGE;
 *         } catch (ClassNotFoundException ignored) {}
 *
 *         return PlatformType.SPIGOT;
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see UnifiedModule
 * @see FeatureModule
 * @see Platform
 */
public abstract class PlatformModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger(PlatformModule.class.getName());

    /**
     * Gets the platform type this module provides bindings for.
     *
     * @return the platform type
     */
    public abstract PlatformType getPlatformType();

    /**
     * {@inheritDoc}
     *
     * <p>This method logs the platform initialization and delegates to
     * {@link #configurePlatform()} for actual bindings.</p>
     */
    @Override
    protected final void configure() {
        LOGGER.info("Configuring platform bindings for: " + getPlatformType());

        // Bind the platform type for injection
        bind(PlatformType.class).toInstance(getPlatformType());

        // Configure platform-specific bindings
        configurePlatform();

        // Configure common cross-platform bindings
        configureCommon();
    }

    /**
     * Configures platform-specific bindings.
     *
     * <p>Override this method to set up bindings specific to your platform.
     * Use the {@link Platform} annotation to qualify platform-specific implementations.</p>
     */
    protected abstract void configurePlatform();

    /**
     * Configures common bindings that apply across all platforms.
     *
     * <p>Override to add bindings that should be available regardless of platform
     * but require platform detection to configure correctly.</p>
     */
    protected void configureCommon() {
        // Default: no common bindings
    }

    /**
     * Checks if this platform supports a specific feature.
     *
     * <p>Override to indicate platform-specific feature availability.
     * Common feature names include:</p>
     * <ul>
     *   <li>{@code ASYNC_CHUNKS} - Asynchronous chunk loading</li>
     *   <li>{@code REGION_SCHEDULING} - Folia region-based scheduling</li>
     *   <li>{@code ADVENTURE_NATIVE} - Native Adventure component support</li>
     *   <li>{@code MOJANG_MAPPINGS} - Mojang-mapped internals</li>
     *   <li>{@code GLOBAL_TICK} - Global server tick event</li>
     * </ul>
     *
     * @param feature the feature name to check
     * @return {@code true} if the feature is supported
     */
    public boolean supportsFeature(String feature) {
        // Default: feature not supported
        return false;
    }

    /**
     * Gets the platform's Minecraft version.
     *
     * <p>Override to return the actual server version.</p>
     *
     * @return the Minecraft version string (e.g., "1.21.1")
     */
    public String getMinecraftVersion() {
        return "unknown";
    }

    /**
     * Gets the platform's API version.
     *
     * <p>Override to return the platform-specific API version.</p>
     *
     * @return the API version string
     */
    public String getApiVersion() {
        return "unknown";
    }

    /**
     * Checks if the current Minecraft version is at least the specified version.
     *
     * @param major the major version (e.g., 1)
     * @param minor the minor version (e.g., 21)
     * @return {@code true} if current version is >= specified version
     */
    public boolean isAtLeastVersion(int major, int minor) {
        String version = getMinecraftVersion();
        try {
            String[] parts = version.split("\\.");
            int currentMajor = Integer.parseInt(parts[0]);
            int currentMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            if (currentMajor > major) return true;
            if (currentMajor < major) return false;
            return currentMinor >= minor;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the current Minecraft version is at least the specified version.
     *
     * @param major the major version (e.g., 1)
     * @param minor the minor version (e.g., 21)
     * @param patch the patch version (e.g., 1)
     * @return {@code true} if current version is >= specified version
     */
    public boolean isAtLeastVersion(int major, int minor, int patch) {
        String version = getMinecraftVersion();
        try {
            String[] parts = version.split("\\.");
            int currentMajor = Integer.parseInt(parts[0]);
            int currentMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int currentPatch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (currentMajor > major) return true;
            if (currentMajor < major) return false;
            if (currentMinor > minor) return true;
            if (currentMinor < minor) return false;
            return currentPatch >= patch;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the platform is Bukkit-based (Paper, Spigot, Folia).
     *
     * @return {@code true} if Bukkit-based
     */
    public boolean isBukkitBased() {
        return getPlatformType() != PlatformType.SPONGE;
    }

    /**
     * Checks if the platform supports async operations natively.
     *
     * @return {@code true} if async operations are supported
     */
    public boolean supportsAsync() {
        return getPlatformType() == PlatformType.PAPER ||
               getPlatformType() == PlatformType.FOLIA;
    }
}
