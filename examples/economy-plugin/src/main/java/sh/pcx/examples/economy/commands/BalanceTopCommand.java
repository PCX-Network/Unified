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
import sh.pcx.unified.commands.annotation.Async;
import sh.pcx.unified.commands.annotation.Command;
import sh.pcx.unified.commands.annotation.Completions;
import sh.pcx.unified.commands.annotation.Default;
import sh.pcx.unified.commands.annotation.Sender;
import sh.pcx.unified.commands.annotation.Subcommand;
import sh.pcx.unified.economy.BalanceEntry;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Balance top command for viewing the richest players.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Async command execution</li>
 *   <li>Leaderboard display</li>
 *   <li>Page-based navigation</li>
 *   <li>Multi-currency support</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Command(
    name = "baltop",
    aliases = {"balancetop", "moneytop", "richlist"},
    description = "View the richest players",
    permission = "economy.baltop"
)
public class BalanceTopCommand {

    private final EconomyService economy;
    private final EconomyConfig config;
    private final MiniMessage miniMessage;

    /**
     * Creates a new balance top command.
     *
     * @param economy the economy service
     * @param config  the economy configuration
     */
    public BalanceTopCommand(@NotNull EconomyService economy, @NotNull EconomyConfig config) {
        this.economy = economy;
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Shows the balance leaderboard for the default currency.
     *
     * @param sender the command sender
     */
    @Default
    @Async
    public void showBaltop(@Sender UnifiedPlayer sender) {
        showBaltop(sender, null, 1);
    }

    /**
     * Shows a specific page of the balance leaderboard.
     *
     * @param sender the command sender
     * @param page   the page number
     */
    @Subcommand("page")
    @Async
    public void showBaltopPage(
            @Sender UnifiedPlayer sender,
            @Arg("page") int page
    ) {
        showBaltop(sender, null, page);
    }

    /**
     * Shows the balance leaderboard for a specific currency.
     *
     * @param sender   the command sender
     * @param currency the currency to show
     */
    @Subcommand("currency")
    @Async
    public void showCurrencyBaltop(
            @Sender UnifiedPlayer sender,
            @Arg("currency") @Completions("@currencies") String currency
    ) {
        showBaltop(sender, currency, 1);
    }

    /**
     * Shows a specific page of a currency's leaderboard.
     *
     * @param sender   the command sender
     * @param currency the currency to show
     * @param page     the page number
     */
    @Subcommand("currency page")
    @Async
    public void showCurrencyBaltopPage(
            @Sender UnifiedPlayer sender,
            @Arg("currency") @Completions("@currencies") String currency,
            @Arg("page") int page
    ) {
        showBaltop(sender, currency, page);
    }

    /**
     * Internal method to display the balance leaderboard.
     */
    private void showBaltop(@NotNull UnifiedPlayer sender, @Nullable String currencyId, int page) {
        EconomyConfig.Messages messages = config.getMessages();
        EconomyConfig.EconomySettings settings = config.getSettings();

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

        // Validate page
        int entriesPerPage = settings.getBaltopEntries();
        if (page < 1) page = 1;

        // Calculate offset for pagination
        int offset = (page - 1) * entriesPerPage;

        // Get top balances (async already handled by @Async)
        List<BalanceEntry> topBalances = economy.getTopBalances(currency.getIdentifier(), offset + entriesPerPage);

        // Get entries for this page
        List<BalanceEntry> pageEntries = topBalances.stream()
                .skip(offset)
                .limit(entriesPerPage)
                .toList();

        if (pageEntries.isEmpty() && page > 1) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + "<red>No entries on page {page}",
                    Placeholder.unparsed("page", String.valueOf(page))
            ));
            return;
        }

        // Display header
        String currencyName = currency.getNamePlural();
        sender.sendMessage(formatMessage(
                messages.getPrefix() + messages.getBaltopHeader() +
                        " <gray>({currency}) - Page {page}",
                Placeholder.unparsed("currency", currencyName),
                Placeholder.unparsed("page", String.valueOf(page))
        ));

        sender.sendMessage(Component.empty());

        // Display entries
        if (pageEntries.isEmpty()) {
            sender.sendMessage(formatMessage("<gray>  No entries yet."));
        } else {
            for (int i = 0; i < pageEntries.size(); i++) {
                BalanceEntry entry = pageEntries.get(i);
                int rank = offset + i + 1;

                String rankColor = switch (rank) {
                    case 1 -> "<gold><bold>";
                    case 2 -> "<gray>";
                    case 3 -> "<#CD7F32>"; // Bronze color
                    default -> "<white>";
                };

                sender.sendMessage(formatMessage(
                        rankColor + messages.getBaltopEntry().replace("<gray>", ""),
                        Placeholder.unparsed("rank", String.valueOf(rank)),
                        Placeholder.unparsed("player", entry.playerName()),
                        Placeholder.unparsed("amount", currency.format(entry.balance()))
                ));
            }
        }

        sender.sendMessage(Component.empty());

        // Display sender's rank
        int senderRank = economy.getBalanceRank(sender.getUniqueId(), currency.getIdentifier());
        if (senderRank > 0) {
            sender.sendMessage(formatMessage(
                    messages.getPrefix() + messages.getBaltopFooter(),
                    Placeholder.unparsed("rank", String.valueOf(senderRank))
            ));
        }

        // Navigation hints
        int totalAccounts = economy.getAccountCount();
        int totalPages = (int) Math.ceil((double) totalAccounts / entriesPerPage);

        if (totalPages > 1) {
            StringBuilder nav = new StringBuilder("<gray>Pages: ");
            if (page > 1) {
                nav.append("<click:run_command:'/baltop page ").append(page - 1).append("'><yellow>[<< Prev]</yellow></click> ");
            }
            nav.append("<white>").append(page).append("/").append(totalPages).append("</white>");
            if (page < totalPages) {
                nav.append(" <click:run_command:'/baltop page ").append(page + 1).append("'><yellow>[Next >>]</yellow></click>");
            }
            sender.sendMessage(formatMessage(nav.toString()));
        }
    }

    /**
     * Formats a message using MiniMessage.
     */
    private Component formatMessage(String template, TagResolver... resolvers) {
        return miniMessage.deserialize(template, resolvers);
    }
}
