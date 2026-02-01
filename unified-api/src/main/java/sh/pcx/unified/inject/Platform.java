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
 * Qualifier annotation for injecting platform-specific implementations.
 *
 * <p>The UnifiedPlugin API supports multiple Minecraft server platforms (Paper, Spigot,
 * Folia, Sponge). This annotation is used to inject the correct implementation based on
 * the current runtime platform, or to explicitly request a specific platform's implementation.</p>
 *
 * <h2>Platform Detection</h2>
 * <p>When used without a value, the current runtime platform is automatically detected
 * and the appropriate implementation is injected.</p>
 *
 * <h2>Supported Platforms</h2>
 * <ul>
 *   <li>{@code PAPER} - Paper and Paper-based forks</li>
 *   <li>{@code SPIGOT} - Spigot and CraftBukkit</li>
 *   <li>{@code FOLIA} - Folia (multi-threaded regions)</li>
 *   <li>{@code SPONGE} - SpongeAPI implementations</li>
 *   <li>{@code AUTO} - Automatically detect platform (default)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Auto-Detection (Recommended)</h3>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     // Automatically injects the correct platform implementation
 *     @Inject
 *     @Platform
 *     private SchedulerService scheduler;
 *
 *     @Inject
 *     @Platform
 *     private ChunkService chunks;
 * }
 * }</pre>
 *
 * <h3>Explicit Platform Selection</h3>
 * <pre>{@code
 * @Service
 * public class PlatformComparison {
 *     @Inject
 *     @Platform(PlatformType.PAPER)
 *     private ChunkService paperChunks;
 *
 *     @Inject
 *     @Platform(PlatformType.FOLIA)
 *     private ChunkService foliaChunks;
 *
 *     // Compare platform-specific behavior
 *     public void analyzeDifferences() {
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * <h3>Platform-Specific Provider Methods</h3>
 * <pre>{@code
 * public class PlatformModule extends AbstractModule {
 *     @Override
 *     protected void configure() {
 *         // Bindings are configured based on detected platform
 *     }
 *
 *     @Provides
 *     @Platform(PlatformType.PAPER)
 *     public SchedulerService providePaperScheduler() {
 *         return new PaperScheduler();
 *     }
 *
 *     @Provides
 *     @Platform(PlatformType.FOLIA)
 *     public SchedulerService provideFoliaScheduler() {
 *         return new FoliaRegionScheduler();
 *     }
 * }
 * }</pre>
 *
 * <h3>Conditional Feature Support</h3>
 * <pre>{@code
 * @Service
 * public class FeatureService {
 *     @Inject
 *     private PlatformService platform;
 *
 *     public void useAdvancedFeature() {
 *         if (platform.supports(Feature.ASYNC_CHUNKS)) {
 *             // Use Paper's async chunk loading
 *         } else {
 *             // Fall back to sync loading
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PlatformType
 * @see PlatformModule
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@BindingAnnotation
public @interface Platform {

    /**
     * The target platform for injection.
     *
     * <p>When set to {@link PlatformType#AUTO}, the current runtime platform
     * is automatically detected and used. For explicit platform selection,
     * specify the desired platform type.</p>
     *
     * @return the platform type, defaults to AUTO
     */
    PlatformType value() default PlatformType.AUTO;

    /**
     * Enumeration of supported Minecraft server platforms.
     */
    enum PlatformType {
        /**
         * Automatically detect the current platform at runtime.
         */
        AUTO,

        /**
         * Paper server and Paper-based forks (Purpur, Pufferfish, etc.).
         * <p>Supports async chunk loading, improved APIs, and Adventure components natively.</p>
         */
        PAPER,

        /**
         * Spigot and CraftBukkit servers.
         * <p>Base Bukkit API with Spigot enhancements.</p>
         */
        SPIGOT,

        /**
         * Folia server with multi-threaded region support.
         * <p>Requires region-aware scheduling and thread-safe implementations.</p>
         */
        FOLIA,

        /**
         * Sponge API implementations (SpongeForge, SpongeVanilla).
         * <p>Uses SpongeAPI which differs significantly from Bukkit.</p>
         */
        SPONGE
    }
}
