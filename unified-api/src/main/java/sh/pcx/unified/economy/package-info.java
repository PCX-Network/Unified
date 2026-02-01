/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Economy API interfaces for the UnifiedPlugin API.
 *
 * <p>This package provides the core interfaces for economy functionality including
 * multi-currency support, transactions, balance management, and Vault compatibility.
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.economy.EconomyService} - Main economy service</li>
 *   <li>{@link sh.pcx.unified.economy.Account} - Player/entity account</li>
 *   <li>{@link sh.pcx.unified.economy.Transaction} - Transaction records</li>
 *   <li>{@link sh.pcx.unified.economy.TransactionResult} - Transaction outcomes</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private EconomyService economy;
 *
 * // Get player balance
 * BigDecimal balance = economy.getBalance(player.getUniqueId());
 *
 * // Perform a transaction
 * TransactionResult result = economy.withdraw(player.getUniqueId(), BigDecimal.valueOf(100));
 * if (result.isSuccess()) {
 *     player.sendMessage(Component.text("Purchase complete!"));
 * }
 *
 * // Transfer between players
 * economy.transfer(fromPlayer, toPlayer, BigDecimal.valueOf(50), "Payment for items");
 *
 * // Multi-currency support
 * BigDecimal gems = economy.getBalance(player.getUniqueId(), "gems");
 * economy.deposit(player.getUniqueId(), BigDecimal.valueOf(500), "gems");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.economy;
