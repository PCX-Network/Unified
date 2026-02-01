/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import sh.pcx.examples.economy.config.EconomyConfig;
import sh.pcx.unified.commands.annotation.Arg;
import sh.pcx.unified.commands.annotation.Command;
import sh.pcx.unified.commands.annotation.Completions;
import sh.pcx.unified.commands.annotation.Cooldown;
import sh.pcx.unified.commands.annotation.Default;
import sh.pcx.unified.commands.annotation.Sender;
import sh.pcx.unified.commands.annotation.Subcommand;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.TransactionResult;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Pay command for transferring money between players.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Command cooldowns</li>
 *   <li>Argument parsing with validation</li>
 *   <li>Transaction handling</li>
 *   <li>Multi-currency transfers</li>
 *   <li>Fee calculation</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Command(
    name = "pay",
    aliases = {"send", "transfer"},
    description = "Send money to another player",
    permission = "economy.pay",
    playerOnly = true
)
public class PayCommand {

    private final EconomyService economy;
    private final EconomyConfig config;
    private final MiniMessage miniMessage;

    /**
     * Creates a new pay command.
     *
     * @param economy the economy service
     * @param config  the economy configuration
     */
    public PayCommand(@NotNull EconomyService economy, @NotNull EconomyConfig config) {
        this.economy = economy;
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Pays another player the specified amount.
     *
     * @param sender the command sender
     * @param target the recipient
     * @param amount the amount to send
     */
    @Default
    @Cooldown(value = 3, unit = TimeUnit.SECONDS, message = "<red>Please wait before sending money again!")
    public void pay(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount
    ) {
        pay(sender, target, amount, null);
    }

    /**
     * Pays another player in a specific currency.
     *
     * @param sender   the command sender
     * @param target   the recipient
     * @param amount   the amount to send
     * @param currency the currency to use
     */
    @Subcommand("currency")
    @Cooldown(value = 3, unit = TimeUnit.SECONDS, message = "<red>Please wait before sending money again!")
    public void payCurrency(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        pay(sender, target, amount, currency);
    }

    /**
     * Internal payment handler.
     */
    private void pay(
            @NotNull UnifiedPlayer sender,
            @NotNull UnifiedPlayer target,
            double amountDouble,
            String currencyId
    ) {
        EconomyConfig.Messages messages = config.getMessages();
        EconomyConfig.EconomySettings settings = config.getSettings();

        // Cannot pay self
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(formatMessage(messages.getPrefix() + messages.getCannotPaySelf()));
            return;
        }

        // Validate amount
        if (amountDouble <= 0) {
            sender.sendMessage(formatMessage(messages.getPrefix() + messages.getInvalidAmount()));
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(amountDouble);

        // Check minimum transfer amount
        if (amount.compareTo(BigDecimal.valueOf(settings.getMinTransferAmount())) < 0) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>Minimum transfer amount is <gold>{amount}</gold>",
                    Placeholder.unparsed("amount", String.valueOf(settings.getMinTransferAmount()))
            ));
            return;
        }

        // Check maximum transfer amount
        if (settings.getMaxTransferAmount() > 0 &&
            amount.compareTo(BigDecimal.valueOf(settings.getMaxTransferAmount())) > 0) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>Maximum transfer amount is <gold>{amount}</gold>",
                    Placeholder.unparsed("amount", String.valueOf(settings.getMaxTransferAmount()))
            ));
            return;
        }

        // Determine currency
        Currency currency;
        if (currencyId != null) {
            currency = economy.getCurrency(currencyId).orElse(null);
            if (currency == null) {
                sender.sendMessage(formatMessage(
                        messages.getPrefix() + "<red>Unknown currency: <yellow>{currency}</yellow>",
                        Placeholder.unparsed("currency", currencyId)
                ));
                return;
            }
        } else {
            currency = economy.getDefaultCurrency();
        }

        // Calculate fee
        BigDecimal fee = BigDecimal.ZERO;
        if (settings.getTransferFeePercent() > 0) {
            fee = amount.multiply(BigDecimal.valueOf(settings.getTransferFeePercent() / 100.0));
        }

        BigDecimal totalDeduction = amount.add(fee);

        // Check balance
        BigDecimal senderBalance = economy.getBalance(sender.getUniqueId(), currency.getIdentifier());
        if (senderBalance.compareTo(totalDeduction) < 0) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + messages.getInsufficientFunds(),
                    Placeholder.unparsed("balance", currency.format(senderBalance))
            ));
            return;
        }

        // Perform transfer
        String reason = "Payment from " + sender.getName() + " to " + target.getName();
        TransactionResult result = economy.transfer(
                sender.getUniqueId(),
                target.getUniqueId(),
                amount,
                currency.getIdentifier(),
                reason
        );

        if (result.isSuccess()) {
            // Deduct fee if applicable
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                economy.withdraw(sender.getUniqueId(), fee, currency.getIdentifier(), "Transfer fee");
            }

            String formattedAmount = currency.format(amount);

            // Notify sender
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + messages.getPaySent(),
                    Placeholder.unparsed("amount", formattedAmount),
                    Placeholder.unparsed("player", target.getName())
            ));

            // Notify recipient if online
            if (target.isOnline()) {
                target.sendMessage(formatMessage(
                        messages.getPrefix() + messages.getPayReceived(),
                        Placeholder.unparsed("amount", formattedAmount),
                        Placeholder.unparsed("player", sender.getName())
                ));
            }

            // Log fee if applicable
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                sender.sendMessage(formatMessage(
                        messages.getPrefix() + "<gray>Fee charged: <gold>{fee}</gold>",
                        Placeholder.unparsed("fee", currency.format(fee))
                ));
            }
        } else {
            // Transfer failed
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>Transfer failed: {reason}",
                    Placeholder.unparsed("reason", result.errorMessage().orElse("Unknown error"))
            ));
        }
    }

    /**
     * Formats a message using MiniMessage.
     */
    private Component formatMessage(String template, TagResolver... resolvers) {
        return miniMessage.deserialize(template, resolvers);
    }
}
