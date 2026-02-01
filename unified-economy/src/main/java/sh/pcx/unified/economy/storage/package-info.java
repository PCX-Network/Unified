/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Economy storage backends and persistence.
 *
 * <p>This package provides interfaces and implementations for persisting
 * economy data to various storage backends:
 * <ul>
 *   <li>{@link sh.pcx.unified.economy.storage.EconomyStorage} - Storage interface</li>
 * </ul>
 *
 * <h2>Available Backends</h2>
 * <p>Storage backend implementations are typically provided by platform-specific
 * modules and may include:
 * <ul>
 *   <li>SQL (MySQL, PostgreSQL, SQLite)</li>
 *   <li>NoSQL (MongoDB, Redis)</li>
 *   <li>File-based (JSON, YAML)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.economy.storage;
