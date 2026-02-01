/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Comprehensive serialization system for Minecraft objects.
 *
 * <p>This package provides a complete serialization framework for Minecraft data,
 * supporting multiple formats (JSON, Base64, binary) with full NBT preservation,
 * compression, and schema versioning for forward compatibility.
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.serialization.Serializer} - Base interface for all serializers</li>
 *   <li>{@link sh.pcx.unified.data.serialization.Serializers} - Central registry for accessing serializers</li>
 *   <li>{@link sh.pcx.unified.data.serialization.SerializationContext} - Configuration for serialization operations</li>
 *   <li>{@link sh.pcx.unified.data.serialization.SerializationException} - Exception for serialization failures</li>
 * </ul>
 *
 * <h2>Item Serializers</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.serialization.ItemStackSerializer} - Full ItemStack with NBT</li>
 *   <li>{@link sh.pcx.unified.data.serialization.ItemMetaSerializer} - ItemMeta properties</li>
 *   <li>{@link sh.pcx.unified.data.serialization.EnchantmentSerializer} - Enchantment data</li>
 *   <li>{@link sh.pcx.unified.data.serialization.PotionEffectSerializer} - Potion effects</li>
 * </ul>
 *
 * <h2>World Serializers</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.serialization.LocationSerializer} - Locations with world reference</li>
 *   <li>{@link sh.pcx.unified.data.serialization.BlockDataSerializer} - Block data and states</li>
 *   <li>{@link sh.pcx.unified.data.serialization.ChunkSerializer} - Chunk positions</li>
 * </ul>
 *
 * <h2>Component Serializers</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.serialization.ComponentSerializer} - Adventure text components</li>
 *   <li>{@link sh.pcx.unified.data.serialization.MiniMessageSerializer} - MiniMessage format</li>
 *   <li>{@link sh.pcx.unified.data.serialization.LegacyTextSerializer} - Legacy color codes</li>
 * </ul>
 *
 * <h2>Binary Format</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.serialization.BinarySerializer} - Base for binary serialization</li>
 *   <li>{@link sh.pcx.unified.data.serialization.BinaryBuffer} - Efficient read/write buffer</li>
 *   <li>{@link sh.pcx.unified.data.serialization.CompressedSerializer} - GZIP/LZ4 compression wrapper</li>
 * </ul>
 *
 * <h2>Versioning</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.serialization.SchemaVersion} - Semantic version tracking</li>
 *   <li>{@link sh.pcx.unified.data.serialization.SchemaMigration} - Migration between versions</li>
 *   <li>{@link sh.pcx.unified.data.serialization.VersionedData} - Data with version metadata</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Get a serializer
 * ItemStackSerializer serializer = Serializers.itemStack();
 *
 * // Serialize to different formats
 * String json = serializer.toJson(itemData);
 * String base64 = serializer.toBase64(itemData);
 *
 * // Deserialize
 * Map<String, Object> restored = serializer.fromJson(json);
 *
 * // With compression
 * SerializationContext context = SerializationContext.builder()
 *     .format(SerializationContext.Format.BASE64)
 *     .compression(SerializationContext.CompressionType.GZIP)
 *     .build();
 * String compressed = serializer.serialize(itemData, context);
 *
 * // Register custom serializer
 * Serializers.register(MyClass.class, new MySerializer());
 * }</pre>
 *
 * <h2>Versioning Example</h2>
 * <pre>{@code
 * // Create versioned data
 * VersionedData<PlayerData> versioned = VersionedData.of(playerData, SchemaVersion.of(1, 0));
 *
 * // Register migrations
 * SchemaMigration<Map<String, Object>> migration = SchemaMigration.create();
 * migration.register(SchemaVersion.of(1, 0), SchemaVersion.of(2, 0), data -> {
 *     // Migrate from v1 to v2
 *     data.put("newField", "defaultValue");
 *     return data;
 * });
 *
 * // Migrate data
 * Map<String, Object> migrated = migration.migrate(oldData, SchemaVersion.of(1, 0), SchemaVersion.of(2, 0));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.serialization;
