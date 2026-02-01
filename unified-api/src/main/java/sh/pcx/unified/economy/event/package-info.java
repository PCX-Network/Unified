/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Economy events for the UnifiedPlugin API.
 *
 * <p>This package provides events related to economy operations including
 * deposits, withdrawals, transfers, and balance changes.
 *
 * <h2>Available Events</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.economy.event.EconomyTransactionEvent} - Base event for all transactions</li>
 *   <li>{@link sh.pcx.unified.economy.event.BalanceChangeEvent} - Fired when a balance changes</li>
 *   <li>{@link sh.pcx.unified.economy.event.TransferEvent} - Fired when money is transferred</li>
 *   <li>{@link sh.pcx.unified.economy.event.AccountCreateEvent} - Fired when an account is created</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onDeposit(BalanceChangeEvent event) {
 *     if (event.getType() == Transaction.Type.DEPOSIT) {
 *         // Log large deposits
 *         if (event.getAmount().compareTo(BigDecimal.valueOf(1000000)) > 0) {
 *             logger.info("Large deposit: {} received {}",
 *                 event.getPlayerId(), event.getAmount());
 *         }
 *     }
 * }
 *
 * @EventHandler
 * public void onTransfer(TransferEvent event) {
 *     // Notify recipient
 *     UnifiedPlayer recipient = UnifiedAPI.getPlayer(event.getToPlayerId());
 *     recipient.ifPresent(p -> p.sendMessage(
 *         Component.text("You received " + economy.format(event.getAmount()))
 *     ));
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.economy.event;
