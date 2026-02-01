/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Migration API for importing data from other plugins and migrating between storage backends.
 *
 * <p>This package provides interfaces for:
 * <ul>
 *   <li>Plugin data importers - Import data from popular plugins</li>
 *   <li>Storage migration - Migrate between database backends</li>
 *   <li>Field mapping - Map fields between formats</li>
 *   <li>Dry run preview - Preview changes before applying</li>
 *   <li>Progress tracking - Monitor migration progress</li>
 *   <li>Custom importer API - Create importers for custom plugins</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the migration service
 * @Inject
 * private MigrationService migration;
 *
 * // Check available importers
 * List<DataImporter> available = migration.getAvailableImporters();
 *
 * // Import from PerWorldInventory
 * ImportResult result = migration.importFrom(Importers.PER_WORLD_INVENTORY)
 *     .sourceFolder(Paths.get("plugins/PerWorldInventory"))
 *     .mapping(FieldMapping.builder()
 *         .map("inventory", "main_inventory")
 *         .map("armour", "armor")
 *         .ignore("economy")
 *         .build())
 *     .dryRun(true)
 *     .execute();
 *
 * // Migrate storage backend
 * MigrationResult result = migration.migrateStorage()
 *     .from(StorageType.YAML)
 *     .to(StorageType.MYSQL)
 *     .batchSize(100)
 *     .onProgress(this::updateProgress)
 *     .execute();
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.migration.MigrationService
 * @see sh.pcx.unified.migration.DataImporter
 * @see sh.pcx.unified.migration.FieldMapping
 */
@org.jetbrains.annotations.ApiStatus.Experimental
package sh.pcx.unified.migration;
