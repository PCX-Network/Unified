/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import sh.pcx.examples.economy.config.EconomyConfig;
import sh.pcx.unified.commands.annotation.Arg;
import sh.pcx.unified.commands.annotation.Command;
import sh.pcx.unified.commands.annotation.Completions;
import sh.pcx.unified.commands.annotation.Default;
import sh.pcx.unified.commands.annotation.Permission;
import sh.pcx.unified.commands.annotation.Sender;
import sh.pcx.unified.commands.annotation.Subcommand;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Balance command for viewing player balances.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Default command handler</li>
 *   <li>Subcommand for viewing other players</li>
 *   <li>Multi-currency support</li>
 *   <li>MiniMessage formatting</li>
 *   <li>Permission-based access</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Command(
    name = "balance",
    aliases = {"bal", "money"},
    description = "View your balance or another player's balance",
    permission = "economy.balance"
)
public class BalanceCommand {

    private final EconomyService economy;
    private final EconomyConfig config;
    private final MiniMessage miniMessage;

    /**
     * Creates a new balance command.
     *
     * @param economy the economy service
     * @param config  the economy configuration
     */
    public BalanceCommand(@NotNull EconomyService economy, @NotNull EconomyConfig config) {
        this.economy = economy;
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Shows the player's own balance.
     *
     * @param player the command sender
     */
    @Default
    public void showBalance(@Sender UnifiedPlayer player) {
        showBalance(player, null, null);
    }

    /**
     * Shows a player's balance, optionally for a specific currency.
     *
     * @param player   the command sender
     * @param currency the currency to check (optional)
     */
    @Subcommand("currency")
    public void showCurrencyBalance(
            @Sender UnifiedPlayer player,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        showBalance(player, null, currency);
    }

    /**
     * Shows another player's balance.
     *
     * @param sender the command sender
     * @param target the player whose balance to check
     */
    @Subcommand("player")
    @Permission("economy.balance.others")
    public void showOtherBalance(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target
    ) {
        showBalance(sender, target, null);
    }

    /**
     * Shows another player's balance for a specific currency.
     *
     * @param sender   the command sender
     * @param target   the player whose balance to check
     * @param currency the currency to check
     */
    @Subcommand("player currency")
    @Permission("economy.balance.others")
    public void showOtherCurrencyBalance(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        showBalance(sender, target, currency);
    }

    /**
     * Shows all balances across all currencies.
     *
     * @param player the command sender
     */
    @Subcommand("all")
    public void showAllBalances(@Sender UnifiedPlayer player) {
        showAllBalances(player, null);
    }

    /**
     * Shows all balances for another player.
     *
     * @param sender the command sender
     * @param target the player whose balances to check
     */
    @Subcommand("all player")
    @Permission("economy.balance.others")
    public void showAllOtherBalances(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target
    ) {
        showAllBalances(sender, target);
    }

    /**
     * Internal method to show a balance.
     */
    private void showBalance(
            @NotNull UnifiedPlayer sender,
            @Nullable UnifiedPlayer target,
            @Nullable String currencyId
    ) {
        UnifiedPlayer player = target != null ? target : sender;
        boolean self = target == null || target.getUniqueId().equals(sender.getUniqueId());

        // Determine currency
        Currency currency;
        if (currencyId != null) {
            currency = economy.getCurrency(currencyId).orElse(null);
            if (currency == null) {
                sender.sendMessage(formatMessage("<red>Unknown currency: <yellow>{currency}</yellow>",
                        Placeholder.unparsed("currency", currencyId)));
                return;
            }
        } else {
            currency = economy.getDefaultCurrency();
        }

        // Get balance
        BigDecimal balance = economy.getBalance(player.getUniqueId(), currency.getIdentifier());
        String formattedAmount = currency.format(balance);

        // Format and send message
        EconomyConfig.Messages messages = config.getMessages();
        String template;

        if (self) {
            if (currencyId != null) {
                template = messages.getBalanceCurrency();
            } else {
                template = messages.getBalance();
            }
        } else {
            template = messages.getBalanceOther();
        }

        sender.sendMessage(formatMessage(messages.getPrefix() + template,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("amount", formattedAmount),
                Placeholder.unparsed("currency", currency.getNamePlural())
        ));
    }

    /**
     * Internal method to show all balances.
     */
    private void showAllBalances(@NotNull UnifiedPlayer sender, @Nullable UnifiedPlayer target) {
        UnifiedPlayer player = target != null ? target : sender;
        boolean self = target == null || target.getUniqueId().equals(sender.getUniqueId());

        EconomyConfig.Messages messages = config.getMessages();

        // Header
        String header = self ? "<gold><bold>Your Balances:</bold></gold>" :
                "<gold><bold>{player}'s Balances:</bold></gold>";
        sender.sendMessage(formatMessage(messages.getPrefix() + header,
                Placeholder.unparsed("player", player.getName())));

        // Show each currency balance
        for (Currency currency : economy.getCurrencies()) {
            BigDecimal balance = economy.getBalance(player.getUniqueId(), currency.getIdentifier());
            String formattedAmount = currency.format(balance);

            sender.sendMessage(formatMessage(
                    "<gray>  - <yellow>{currency}</yellow>: <gold>{amount}</gold>",
                    Placeholder.unparsed("currency", currency.getNamePlural()),
                    Placeholder.unparsed("amount", formattedAmount)
            ));
        }
    }

    /**
     * Formats a message using MiniMessage.
     */
    private Component formatMessage(String template, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        return miniMessage.deserialize(template, resolvers);
    }
}
