/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Inventory management system for the Unified Plugin API.
 *
 * <p>This package provides comprehensive inventory management capabilities including
 * capturing, saving, loading, and transferring player inventories. It supports
 * full inventory snapshots with armor, offhand, and ender chest contents.
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.inventory.core} - Core interfaces and implementations</li>
 *   <li>{@link sh.pcx.unified.data.inventory.snapshot} - Snapshot management and storage</li>
 *   <li>{@link sh.pcx.unified.data.inventory.preset} - Preset/kit system</li>
 *   <li>{@link sh.pcx.unified.data.inventory.transfer} - Cross-server transfer</li>
 *   <li>{@link sh.pcx.unified.data.inventory.history} - History tracking and rollback</li>
 *   <li>{@link sh.pcx.unified.data.inventory.serialization} - Serialization utilities</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Full inventory snapshots with armor, offhand, and ender chest</li>
 *   <li>Multiple serialization formats (Base64, JSON, Binary)</li>
 *   <li>Cross-server inventory transfer support</li>
 *   <li>History tracking for rollback</li>
 *   <li>Preset/kit system with permissions and cooldowns</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private InventoryService inventories;
 *
 * // Capture inventory
 * InventorySnapshot snapshot = inventories.capture(player);
 *
 * // Save to database
 * inventories.save(player, "backup");
 *
 * // Load and apply
 * inventories.load(player, "backup").thenAccept(s ->
 *     s.applyTo(player, ApplyMode.REPLACE)
 * );
 *
 * // Apply a preset
 * inventories.applyPreset(player, "kit_pvp");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.data.inventory.core.InventoryService
 * @see sh.pcx.unified.data.inventory.core.InventorySnapshot
 */
package sh.pcx.unified.data.inventory;
