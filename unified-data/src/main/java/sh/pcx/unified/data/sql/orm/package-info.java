/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Lightweight ORM (Object-Relational Mapping) framework.
 *
 * <p>This package provides annotation-based entity mapping for easy
 * persistence of Java objects to database tables.
 *
 * <h2>Annotations</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.sql.orm.Table} - Marks a class as a database entity</li>
 *   <li>{@link sh.pcx.unified.data.sql.orm.Column} - Maps a field to a database column</li>
 *   <li>{@link sh.pcx.unified.data.sql.orm.Id} - Marks the primary key field</li>
 * </ul>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.sql.orm.EntityMapper} - Maps entities to/from ResultSets</li>
 *   <li>{@link sh.pcx.unified.data.sql.orm.Repository} - CRUD operations interface</li>
 * </ul>
 *
 * <h2>Example Entity</h2>
 * <pre>{@code
 * @Table("players")
 * public class PlayerData {
 *     @Id
 *     @Column("uuid")
 *     private UUID uuid;
 *
 *     @Column("name")
 *     private String name;
 *
 *     @Column("balance")
 *     private double balance;
 *
 *     @Column(value = "last_login", nullable = false)
 *     private Instant lastLogin;
 *
 *     @Column(value = "metadata", serialized = true)
 *     private Map<String, Object> metadata;
 *
 *     // Constructors, getters, setters...
 * }
 * }</pre>
 *
 * <h2>Example Repository Usage</h2>
 * <pre>{@code
 * Repository<PlayerData, UUID> repo = database.getRepository(PlayerData.class);
 *
 * // Save
 * PlayerData player = new PlayerData(uuid, "Steve", 1000.0);
 * repo.save(player).join();
 *
 * // Find
 * Optional<PlayerData> found = repo.findById(uuid).join();
 *
 * // Query
 * List<PlayerData> topPlayers = repo.findAll(q -> q
 *     .where("balance", ">", 1000)
 *     .orderBy("balance", Order.DESC)
 *     .limit(10)
 * ).join();
 *
 * // Delete
 * repo.deleteById(uuid).join();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.sql.orm;
