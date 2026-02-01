/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Fluent SQL query builders.
 *
 * <p>This package provides type-safe, fluent builders for constructing
 * SQL queries with proper parameter binding to prevent SQL injection.
 *
 * <h2>Available Builders</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.sql.query.QueryBuilder} - Generic query builder</li>
 *   <li>{@link sh.pcx.unified.data.sql.query.SelectBuilder} - SELECT with entity mapping</li>
 *   <li>{@link sh.pcx.unified.data.sql.query.InsertBuilder} - INSERT with batch support</li>
 *   <li>{@link sh.pcx.unified.data.sql.query.UpdateBuilder} - UPDATE with increment/decrement</li>
 *   <li>{@link sh.pcx.unified.data.sql.query.DeleteBuilder} - DELETE with safety checks</li>
 *   <li>{@link sh.pcx.unified.data.sql.query.WhereClause} - WHERE clause builder</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // SELECT query
 * List<PlayerData> players = selectBuilder
 *     .where("active", true)
 *     .and("level", ">", 10)
 *     .orderBy("balance", Order.DESC)
 *     .limit(100)
 *     .executeAsync()
 *     .join();
 *
 * // INSERT query
 * insertBuilder
 *     .columns("uuid", "name", "balance")
 *     .values(uuid, "Steve", 1000.0)
 *     .onDuplicateKeyUpdate("balance")
 *     .executeAsync();
 *
 * // UPDATE query
 * updateBuilder
 *     .increment("kills", 1)
 *     .set("last_kill", Instant.now())
 *     .where("uuid", uuid)
 *     .executeAsync();
 *
 * // DELETE query
 * deleteBuilder
 *     .where("last_login", "<", thirtyDaysAgo)
 *     .executeAsync();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.sql.query;
