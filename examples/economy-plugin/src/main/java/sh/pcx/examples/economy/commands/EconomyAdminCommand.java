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
import sh.pcx.unified.commands.annotation.Permission;
import sh.pcx.unified.commands.annotation.Sender;
import sh.pcx.unified.commands.annotation.Subcommand;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.TransactionResult;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * Economy admin command for managing player balances.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Admin commands with elevated permissions</li>
 *   <li>Give, take, and set operations</li>
 *   <li>Multi-currency management</li>
 *   <li>Economy statistics</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Command(
    name = "eco",
    aliases = {"economy", "econ"},
    description = "Economy administration commands",
    permission = "economy.admin"
)
@Permission("economy.admin")
public class EconomyAdminCommand {

    private final EconomyService economy;
    private final EconomyConfig config;
    private final MiniMessage miniMessage;

    /**
     * Creates a new economy admin command.
     *
     * @param economy the economy service
     * @param config  the economy configuration
     */
    public EconomyAdminCommand(@NotNull EconomyService economy, @NotNull EconomyConfig config) {
        this.economy = economy;
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Shows economy help.
     *
     * @param sender the command sender
     */
    @Subcommand("help")
    public void showHelp(@Sender UnifiedPlayer sender) {
        sender.sendMessage(formatMessage("<gold><bold>Economy Admin Commands:</bold></gold>"));
        sender.sendMessage(formatMessage("<gray>/eco give <player> <amount> [currency]</gray> - Give money to a player"));
        sender.sendMessage(formatMessage("<gray>/eco take <player> <amount> [currency]</gray> - Take money from a player"));
        sender.sendMessage(formatMessage("<gray>/eco set <player> <amount> [currency]</gray> - Set a player's balance"));
        sender.sendMessage(formatMessage("<gray>/eco reset <player> [currency]</gray> - Reset a player's balance"));
        sender.sendMessage(formatMessage("<gray>/eco stats</gray> - View economy statistics"));
        sender.sendMessage(formatMessage("<gray>/eco currencies</gray> - List all currencies"));
    }

    /**
     * Gives money to a player.
     *
     * @param sender the command sender
     * @param target the target player
     * @param amount the amount to give
     */
    @Subcommand("give")
    public void give(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount
    ) {
        give(sender, target, amount, null);
    }

    /**
     * Gives money to a player in a specific currency.
     *
     * @param sender   the command sender
     * @param target   the target player
     * @param amount   the amount to give
     * @param currency the currency
     */
    @Subcommand("give currency")
    public void giveCurrency(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        give(sender, target, amount, currency);
    }

    /**
     * Internal give implementation.
     */
    private void give(@NotNull UnifiedPlayer sender, @NotNull UnifiedPlayer target, double amountDouble, String currencyId) {
        EconomyConfig.Messages messages = config.getMessages();

        if (amountDouble <= 0) {
            sender.sendMessage(formatMessage(messages.getPrefix() + messages.getInvalidAmount()));
            return;
        }

        Currency currency = getCurrency(sender, currencyId);
        if (currency == null) return;

        BigDecimal amount = BigDecimal.valueOf(amountDouble);
        String reason = "Admin give by " + sender.getName();

        TransactionResult result = economy.deposit(
                target.getUniqueId(),
                amount,
                currency.getIdentifier(),
                reason
        );

        if (result.isSuccess()) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + messages.getEconomyGive(),
                    Placeholder.unparsed("amount", currency.format(amount)),
                    Placeholder.unparsed("player", target.getName())
            ));

            // Notify target if different from sender
            if (!target.getUniqueId().equals(sender.getUniqueId())) {
                target.sendMessage(formatMessage(
                        messages.getPrefix() + "<green>You received <gold>{amount}</gold> from an admin.",
                        Placeholder.unparsed("amount", currency.format(amount))
                ));
            }
        } else {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>Failed to give money: {reason}",
                    Placeholder.unparsed("reason", result.errorMessage().orElse("Unknown error"))
            ));
        }
    }

    /**
     * Takes money from a player.
     *
     * @param sender the command sender
     * @param target the target player
     * @param amount the amount to take
     */
    @Subcommand("take")
    public void take(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount
    ) {
        take(sender, target, amount, null);
    }

    /**
     * Takes money from a player in a specific currency.
     *
     * @param sender   the command sender
     * @param target   the target player
     * @param amount   the amount to take
     * @param currency the currency
     */
    @Subcommand("take currency")
    public void takeCurrency(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        take(sender, target, amount, currency);
    }

    /**
     * Internal take implementation.
     */
    private void take(@NotNull UnifiedPlayer sender, @NotNull UnifiedPlayer target, double amountDouble, String currencyId) {
        EconomyConfig.Messages messages = config.getMessages();

        if (amountDouble <= 0) {
            sender.sendMessage(formatMessage(messages.getPrefix() + messages.getInvalidAmount()));
            return;
        }

        Currency currency = getCurrency(sender, currencyId);
        if (currency == null) return;

        BigDecimal amount = BigDecimal.valueOf(amountDouble);
        String reason = "Admin take by " + sender.getName();

        TransactionResult result = economy.withdraw(
                target.getUniqueId(),
                amount,
                currency.getIdentifier(),
                reason
        );

        if (result.isSuccess()) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + messages.getEconomyTake(),
                    Placeholder.unparsed("amount", currency.format(amount)),
                    Placeholder.unparsed("player", target.getName())
            ));
        } else {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>Failed to take money: {reason}",
                    Placeholder.unparsed("reason", result.errorMessage().orElse("Unknown error"))
            ));
        }
    }

    /**
     * Sets a player's balance.
     *
     * @param sender the command sender
     * @param target the target player
     * @param amount the new balance
     */
    @Subcommand("set")
    public void set(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount
    ) {
        set(sender, target, amount, null);
    }

    /**
     * Sets a player's balance in a specific currency.
     *
     * @param sender   the command sender
     * @param target   the target player
     * @param amount   the new balance
     * @param currency the currency
     */
    @Subcommand("set currency")
    public void setCurrency(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("amount") double amount,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        set(sender, target, amount, currency);
    }

    /**
     * Internal set implementation.
     */
    private void set(@NotNull UnifiedPlayer sender, @NotNull UnifiedPlayer target, double amountDouble, String currencyId) {
        EconomyConfig.Messages messages = config.getMessages();

        Currency currency = getCurrency(sender, currencyId);
        if (currency == null) return;

        BigDecimal amount = BigDecimal.valueOf(amountDouble);
        String reason = "Admin set by " + sender.getName();

        TransactionResult result = economy.setBalance(
                target.getUniqueId(),
                amount,
                currency.getIdentifier(),
                reason
        );

        if (result.isSuccess()) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + messages.getEconomySet(),
                    Placeholder.unparsed("amount", currency.format(amount)),
                    Placeholder.unparsed("player", target.getName())
            ));
        } else {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>Failed to set balance: {reason}",
                    Placeholder.unparsed("reason", result.errorMessage().orElse("Unknown error"))
            ));
        }
    }

    /**
     * Resets a player's balance.
     *
     * @param sender the command sender
     * @param target the target player
     */
    @Subcommand("reset")
    public void reset(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target
    ) {
        reset(sender, target, null);
    }

    /**
     * Resets a player's balance in a specific currency.
     *
     * @param sender   the command sender
     * @param target   the target player
     * @param currency the currency
     */
    @Subcommand("reset currency")
    public void resetCurrency(
            @Sender UnifiedPlayer sender,
            @Arg("player") @Completions("@players") UnifiedPlayer target,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        reset(sender, target, currency);
    }

    /**
     * Internal reset implementation.
     */
    private void reset(@NotNull UnifiedPlayer sender, @NotNull UnifiedPlayer target, String currencyId) {
        Currency currency = getCurrency(sender, currencyId);
        if (currency == null) return;

        BigDecimal startingBalance = currency.getStartingBalance();
        set(sender, target, startingBalance.doubleValue(), currencyId);
    }

    /**
     * Shows economy statistics.
     *
     * @param sender the command sender
     */
    @Subcommand("stats")
    public void stats(@Sender UnifiedPlayer sender) {
        EconomyConfig.Messages messages = config.getMessages();

        sender.sendMessage(formatMessage(messages.getPrefix() + "<gold><bold>Economy Statistics:</bold></gold>"));
        sender.sendMessage(Component.empty());

        sender.sendMessage(formatMessage(
                "<gray>Total Accounts: <yellow>{count}</yellow>",
                Placeholder.unparsed("count", String.valueOf(economy.getAccountCount()))
        ));

        for (Currency currency : economy.getCurrencies()) {
            BigDecimal totalSupply = economy.getTotalSupply(currency.getIdentifier());
            sender.sendMessage(formatMessage(
                    "<gray>{currency} Total Supply: <gold>{amount}</gold>",
                    Placeholder.unparsed("currency", currency.getNamePlural()),
                    Placeholder.unparsed("amount", currency.format(totalSupply))
            ));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(formatMessage(
                "<gray>Currencies: <yellow>{count}</yellow>",
                Placeholder.unparsed("count", String.valueOf(economy.getCurrencies().size()))
        ));
    }

    /**
     * Lists all registered currencies.
     *
     * @param sender the command sender
     */
    @Subcommand("currencies")
    public void currencies(@Sender UnifiedPlayer sender) {
        EconomyConfig.Messages messages = config.getMessages();

        sender.sendMessage(formatMessage(messages.getPrefix() + "<gold><bold>Registered Currencies:</bold></gold>"));
        sender.sendMessage(Component.empty());

        for (Currency currency : economy.getCurrencies()) {
            String defaultIndicator = currency.isDefaultCurrency() ? " <green>(default)</green>" : "";
            sender.sendMessage(formatMessage(
                    "<gray>- <yellow>{id}</yellow>: {symbol}{name}" + defaultIndicator,
                    Placeholder.unparsed("id", currency.getIdentifier()),
                    Placeholder.unparsed("symbol", currency.getSymbol()),
                    Placeholder.unparsed("name", currency.getNamePlural())
            ));
            sender.sendMessage(formatMessage(
                    "  <dark_gray>Decimals: {decimals}, Starting: {starting}",
                    Placeholder.unparsed("decimals", String.valueOf(currency.getDecimals())),
                    Placeholder.unparsed("starting", currency.format(currency.getStartingBalance()))
            ));
        }
    }

    /**
     * Gets a currency by ID, or default if null.
     */
    private Currency getCurrency(@NotNull UnifiedPlayer sender, String currencyId) {
        Currency currency;
        if (currencyId != null) {
            currency = economy.getCurrency(currencyId).orElse(null);
            if (currency == null) {
                sender.sendMessage(formatMessage(
                        config.getMessages().getPrefix() + "<red>Unknown currency: <yellow>{currency}</yellow>",
                        Placeholder.unparsed("currency", currencyId)
                ));
                return null;
            }
        } else {
            currency = economy.getDefaultCurrency();
        }
        return currency;
    }

    /**
     * Formats a message using MiniMessage.
     */
    private Component formatMessage(String template, TagResolver... resolvers) {
        return miniMessage.deserialize(template, resolvers);
    }
}
