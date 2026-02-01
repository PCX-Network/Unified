/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Region management API for defining and protecting areas in worlds.
 *
 * <p>This package provides interfaces for creating and managing protected regions
 * with customizable flags, priorities, and behaviors. The region system supports
 * various shapes including cuboids, spheres, cylinders, and polygons.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.region.RegionService} - Main service for region management</li>
 *   <li>{@link sh.pcx.unified.region.Region} - Base interface for all region types</li>
 *   <li>{@link sh.pcx.unified.region.RegionFlag} - Typed flags for region behaviors</li>
 *   <li>{@link sh.pcx.unified.region.RegionBuilder} - Fluent builders for region creation</li>
 * </ul>
 *
 * <h2>Region Types</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.region.CuboidRegion} - Box-shaped regions</li>
 *   <li>{@link sh.pcx.unified.region.SphereRegion} - Radius-based spherical regions</li>
 *   <li>{@link sh.pcx.unified.region.CylinderRegion} - Vertical cylindrical regions</li>
 *   <li>{@link sh.pcx.unified.region.PolygonRegion} - Complex polygon-based regions</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private RegionService regions;
 *
 * // Create a spawn protection region
 * Region spawn = regions.cuboid("spawn")
 *     .world(world)
 *     .min(new UnifiedLocation(world, -50, 60, -50))
 *     .max(new UnifiedLocation(world, 50, 120, 50))
 *     .flag(RegionFlag.PVP, false)
 *     .flag(RegionFlag.BUILD, false)
 *     .flag(RegionFlag.BREAK, false)
 *     .priority(10)
 *     .create();
 *
 * // Check if a location is protected
 * boolean canBuild = regions.queryFlag(location, RegionFlag.BUILD, true);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.region;
