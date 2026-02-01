/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * SQL database support with HikariCP connection pooling.
 *
 * <p>This package provides a comprehensive SQL database abstraction layer with:
 * <ul>
 *   <li>Support for multiple databases (SQLite, MySQL, MariaDB, PostgreSQL)</li>
 *   <li>Connection pooling via HikariCP</li>
 *   <li>Fluent query builders for type-safe SQL construction</li>
 *   <li>ORM-lite with annotation-based entity mapping</li>
 *   <li>Schema migration support</li>
 *   <li>Async operations with CompletableFuture</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Configure database
 * DatabaseConfig dbConfig = DatabaseConfig.builder()
 *     .type(DatabaseType.MYSQL)
 *     .host("localhost")
 *     .port(3306)
 *     .database("minecraft")
 *     .username("mc_user")
 *     .password("secret")
 *     .build();
 *
 * // Configure connection pool
 * PoolConfig poolConfig = PoolConfig.builder()
 *     .poolName("MyPlugin-Pool")
 *     .maximumPoolSize(10)
 *     .build();
 *
 * // Create pool manager
 * HikariPoolManager poolManager = new HikariPoolManager(dbConfig, poolConfig);
 *
 * // Use connections
 * try (DatabaseConnection conn = poolManager.getConnection()) {
 *     conn.executeUpdate("INSERT INTO players (uuid, name) VALUES (?, ?)", uuid, name);
 * }
 *
 * // Or use query builders
 * QueryBuilder query = QueryBuilder.create()
 *     .select("*")
 *     .from("players")
 *     .where("uuid", uuid);
 *
 * // Or use repositories
 * Repository<PlayerData, UUID> repo = database.getRepository(PlayerData.class);
 * Optional<PlayerData> player = repo.findById(uuid).join();
 * }</pre>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.sql} - Core interfaces and classes</li>
 *   <li>{@link sh.pcx.unified.data.sql.query} - Query builders</li>
 *   <li>{@link sh.pcx.unified.data.sql.orm} - ORM annotations and mapping</li>
 *   <li>{@link sh.pcx.unified.data.sql.migration} - Schema migrations</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.sql;
