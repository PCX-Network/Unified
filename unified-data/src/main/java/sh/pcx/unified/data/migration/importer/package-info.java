/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Built-in data importers for popular Minecraft plugins.
 *
 * <p>This package provides importers for:
 * <ul>
 *   <li>{@link sh.pcx.unified.data.migration.importer.PerWorldInventoryImporter} - PerWorldInventory plugin</li>
 *   <li>{@link sh.pcx.unified.data.migration.importer.MultiVerseInventoriesImporter} - MultiVerse-Inventories plugin</li>
 *   <li>{@link sh.pcx.unified.data.migration.importer.EssentialsXImporter} - EssentialsX plugin</li>
 *   <li>{@link sh.pcx.unified.data.migration.importer.MyWorldsImporter} - MyWorlds plugin</li>
 * </ul>
 *
 * <h2>Creating Custom Importers</h2>
 * <p>To create a custom importer, extend {@link sh.pcx.unified.data.migration.importer.AbstractDataImporter}:
 * <pre>{@code
 * @Importer(
 *     name = "MyPlugin",
 *     description = "Import from MyPlugin",
 *     detectionFile = "plugins/MyPlugin/data"
 * )
 * public class MyPluginImporter extends AbstractDataImporter {
 *
 *     public MyPluginImporter() {
 *         super("my-plugin", "MyPlugin", "Import from MyPlugin",
 *               Set.of("inventory", "economy"), "plugins/MyPlugin/data");
 *     }
 *
 *     @Override
 *     public DataSchema getSourceSchema() {
 *         return DataSchema.builder()
 *             .name("my_plugin_data")
 *             .field("uuid", FieldType.UUID, true)
 *             .field("data", FieldType.OBJECT, false)
 *             .build();
 *     }
 *
 *     @Override
 *     public ImportResult doImport(ImportContext context) {
 *         // Implementation
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see sh.pcx.unified.migration.DataImporter
 * @see sh.pcx.unified.migration.Importer
 */
package sh.pcx.unified.data.migration.importer;
