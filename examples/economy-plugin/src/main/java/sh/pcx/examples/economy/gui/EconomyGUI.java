/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sh.pcx.examples.economy.config.EconomyConfig;
import sh.pcx.unified.economy.BalanceEntry;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.gui.GUIState;
import sh.pcx.unified.gui.Layout;
import sh.pcx.unified.gui.StateKey;
import sh.pcx.unified.gui.StatefulGUI;
import sh.pcx.unified.gui.component.Button;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive GUI for economy management.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Stateful GUI with pagination</li>
 *   <li>Dynamic content updates</li>
 *   <li>Interactive buttons with click handlers</li>
 *   <li>Input handling for transfers</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class EconomyGUI implements StatefulGUI {

    // State keys
    private static final StateKey<Integer> PAGE = StateKey.ofInt("page");
    private static final StateKey<String> CURRENT_VIEW = StateKey.ofString("current_view");
    private static final StateKey<String> SELECTED_CURRENCY = StateKey.ofString("selected_currency");
    private static final StateKey<UUID> SELECTED_PLAYER = StateKey.of("selected_player", UUID.class);

    // Views
    private static final String VIEW_MAIN = "main";
    private static final String VIEW_CURRENCIES = "currencies";
    private static final String VIEW_TRANSFER = "transfer";
    private static final String VIEW_LEADERBOARD = "leaderboard";
    private static final String VIEW_HISTORY = "history";

    private final UnifiedPlayer viewer;
    private final EconomyService economy;
    private final EconomyConfig config;
    private final GUIState state;

    /**
     * Creates a new economy GUI.
     *
     * @param viewer  the player viewing the GUI
     * @param economy the economy service
     * @param config  the economy configuration
     */
    public EconomyGUI(
            @NotNull UnifiedPlayer viewer,
            @NotNull EconomyService economy,
            @NotNull EconomyConfig config
    ) {
        this.viewer = viewer;
        this.economy = economy;
        this.config = config;
        this.state = GUIState.create();

        // Initialize default state
        state.set(PAGE, 1);
        state.set(CURRENT_VIEW, VIEW_MAIN);
        state.set(SELECTED_CURRENCY, economy.getDefaultCurrency().getIdentifier());
    }

    @Override
    @NotNull
    public GUIState getState() {
        return state;
    }

    /**
     * Builds the GUI content based on current state.
     *
     * @return a map of slot indices to buttons
     */
    @NotNull
    public Map<Integer, Button> build() {
        String currentView = getState(CURRENT_VIEW, VIEW_MAIN);

        return switch (currentView) {
            case VIEW_CURRENCIES -> buildCurrenciesView();
            case VIEW_TRANSFER -> buildTransferView();
            case VIEW_LEADERBOARD -> buildLeaderboardView();
            case VIEW_HISTORY -> buildHistoryView();
            default -> buildMainView();
        };
    }

    /**
     * Builds the main menu view.
     */
    @NotNull
    private Map<Integer, Button> buildMainView() {
        Map<Integer, Button> buttons = new java.util.HashMap<>();

        // Title bar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            buttons.put(i, createFillerButton(NamedTextColor.GOLD));
        }

        // Player balance display (slot 13)
        buttons.put(13, createBalanceDisplayButton());

        // View currencies button (slot 20)
        buttons.put(20, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:gold_ingot")
                        .name(Component.text("View Currencies", NamedTextColor.GOLD))
                        .lore(List.of(
                                Component.text("Click to view all currencies", NamedTextColor.GRAY),
                                Component.text("and your balances", NamedTextColor.GRAY)
                        ))
                        .build())
                .onClick(ctx -> {
                    setState(CURRENT_VIEW, VIEW_CURRENCIES);
                    // Trigger GUI refresh
                })
                .clickSound("minecraft:ui.button.click")
                .build());

        // Transfer money button (slot 22)
        buttons.put(22, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:paper")
                        .name(Component.text("Transfer Money", NamedTextColor.GREEN))
                        .lore(List.of(
                                Component.text("Click to send money", NamedTextColor.GRAY),
                                Component.text("to another player", NamedTextColor.GRAY)
                        ))
                        .build())
                .onClick(ctx -> {
                    setState(CURRENT_VIEW, VIEW_TRANSFER);
                })
                .clickSound("minecraft:ui.button.click")
                .build());

        // Leaderboard button (slot 24)
        buttons.put(24, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:diamond")
                        .name(Component.text("Leaderboard", NamedTextColor.AQUA))
                        .lore(List.of(
                                Component.text("Click to view the", NamedTextColor.GRAY),
                                Component.text("richest players", NamedTextColor.GRAY)
                        ))
                        .build())
                .onClick(ctx -> {
                    setState(CURRENT_VIEW, VIEW_LEADERBOARD);
                    setState(PAGE, 1);
                })
                .clickSound("minecraft:ui.button.click")
                .build());

        // Transaction history button (slot 31)
        buttons.put(31, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:book")
                        .name(Component.text("Transaction History", NamedTextColor.YELLOW))
                        .lore(List.of(
                                Component.text("Click to view your", NamedTextColor.GRAY),
                                Component.text("recent transactions", NamedTextColor.GRAY)
                        ))
                        .build())
                .onClick(ctx -> {
                    setState(CURRENT_VIEW, VIEW_HISTORY);
                    setState(PAGE, 1);
                })
                .clickSound("minecraft:ui.button.click")
                .build());

        // Close button (slot 49)
        buttons.put(49, createCloseButton());

        // Bottom filler (slots 45-53)
        for (int i = 45; i < 54; i++) {
            if (!buttons.containsKey(i)) {
                buttons.put(i, createFillerButton(NamedTextColor.GRAY));
            }
        }

        return buttons;
    }

    /**
     * Builds the currencies view.
     */
    @NotNull
    private Map<Integer, Button> buildCurrenciesView() {
        Map<Integer, Button> buttons = new java.util.HashMap<>();

        // Back button (slot 0)
        buttons.put(0, createBackButton());

        // Title
        for (int i = 1; i < 9; i++) {
            buttons.put(i, createFillerButton(NamedTextColor.GOLD));
        }

        // Currency items
        int slot = 10;
        for (Currency currency : economy.getCurrencies()) {
            if (slot > 43) break;

            BigDecimal balance = economy.getBalance(viewer.getUniqueId(), currency.getIdentifier());

            String material = switch (currency.getIdentifier()) {
                case "dollars" -> "minecraft:gold_ingot";
                case "coins" -> "minecraft:iron_nugget";
                case "gems" -> "minecraft:diamond";
                default -> "minecraft:emerald";
            };

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Balance: " + currency.format(balance), NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text("Symbol: " + currency.getSymbol(), NamedTextColor.GRAY));
            lore.add(Component.text("Decimals: " + currency.getDecimals(), NamedTextColor.GRAY));
            if (currency.isDefaultCurrency()) {
                lore.add(Component.empty());
                lore.add(Component.text("Default Currency", NamedTextColor.GREEN));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click to select", NamedTextColor.DARK_GRAY));

            final String currencyId = currency.getIdentifier();
            buttons.put(slot, Button.builder()
                    .item(() -> ItemBuilder.of(material)
                            .name(Component.text(currency.getNamePlural(), NamedTextColor.GOLD)
                                    .decoration(TextDecoration.BOLD, true))
                            .lore(lore)
                            .build())
                    .onClick(ctx -> {
                        setState(SELECTED_CURRENCY, currencyId);
                        ctx.playSound("minecraft:entity.experience_orb.pickup", 1.0f, 1.5f);
                    })
                    .build());

            slot++;
            // Skip to next row if at edge
            if (slot % 9 == 8) slot += 2;
        }

        // Close button
        buttons.put(49, createCloseButton());

        return buttons;
    }

    /**
     * Builds the transfer view.
     */
    @NotNull
    private Map<Integer, Button> buildTransferView() {
        Map<Integer, Button> buttons = new java.util.HashMap<>();

        // Back button
        buttons.put(0, createBackButton());

        // Title
        for (int i = 1; i < 9; i++) {
            buttons.put(i, createFillerButton(NamedTextColor.GREEN));
        }

        // Instructions
        buttons.put(13, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:paper")
                        .name(Component.text("How to Transfer", NamedTextColor.GREEN))
                        .lore(List.of(
                                Component.text("1. Click a player head below", NamedTextColor.GRAY),
                                Component.text("2. Enter the amount in chat", NamedTextColor.GRAY),
                                Component.text("3. Confirm the transfer", NamedTextColor.GRAY),
                                Component.empty(),
                                Component.text("Or use: /pay <player> <amount>", NamedTextColor.YELLOW)
                        ))
                        .build())
                .build());

        // Quick amount buttons
        double[] amounts = {10, 50, 100, 500, 1000};
        int[] amountSlots = {29, 30, 31, 32, 33};

        for (int i = 0; i < amounts.length; i++) {
            double amount = amounts[i];
            buttons.put(amountSlots[i], Button.builder()
                    .item(() -> ItemBuilder.of("minecraft:gold_nugget")
                            .name(Component.text("$" + (int) amount, NamedTextColor.GOLD))
                            .lore(List.of(
                                    Component.text("Click to set amount", NamedTextColor.GRAY)
                            ))
                            .amount((int) Math.min(amount / 10, 64))
                            .build())
                    .onClick(ctx -> {
                        ctx.sendMessage(Component.text("Selected amount: $" + (int) amount, NamedTextColor.GREEN));
                        ctx.sendMessage(Component.text("Now click a player to transfer to!", NamedTextColor.YELLOW));
                    })
                    .build());
        }

        // Close button
        buttons.put(49, createCloseButton());

        return buttons;
    }

    /**
     * Builds the leaderboard view.
     */
    @NotNull
    private Map<Integer, Button> buildLeaderboardView() {
        Map<Integer, Button> buttons = new java.util.HashMap<>();

        // Back button
        buttons.put(0, createBackButton());

        // Title
        for (int i = 1; i < 9; i++) {
            buttons.put(i, createFillerButton(NamedTextColor.AQUA));
        }

        // Get leaderboard data
        String currencyId = getState(SELECTED_CURRENCY, economy.getDefaultCurrency().getIdentifier());
        Currency currency = economy.getCurrency(currencyId).orElse(economy.getDefaultCurrency());
        List<BalanceEntry> topBalances = economy.getTopBalances(currencyId, 21);

        // Display entries
        int slot = 10;
        int rank = 1;
        for (BalanceEntry entry : topBalances) {
            if (slot > 43) break;

            String material = switch (rank) {
                case 1 -> "minecraft:gold_block";
                case 2 -> "minecraft:iron_block";
                case 3 -> "minecraft:copper_block";
                default -> "minecraft:player_head";
            };

            NamedTextColor rankColor = switch (rank) {
                case 1 -> NamedTextColor.GOLD;
                case 2 -> NamedTextColor.GRAY;
                case 3 -> NamedTextColor.RED;
                default -> NamedTextColor.WHITE;
            };

            final int finalRank = rank;
            buttons.put(slot, Button.builder()
                    .item(() -> ItemBuilder.of(material)
                            .name(Component.text("#" + finalRank + " " + entry.playerName(), rankColor)
                                    .decoration(TextDecoration.BOLD, finalRank <= 3))
                            .lore(List.of(
                                    Component.text(currency.format(entry.balance()), NamedTextColor.YELLOW)
                            ))
                            .build())
                    .build());

            slot++;
            rank++;
            // Skip to next row if at edge
            if (slot % 9 == 8) slot += 2;
        }

        // Navigation buttons
        int page = getState(PAGE, 1);
        if (page > 1) {
            buttons.put(45, createPreviousPageButton());
        }
        if (topBalances.size() >= 21) {
            buttons.put(53, createNextPageButton());
        }

        // Close button
        buttons.put(49, createCloseButton());

        return buttons;
    }

    /**
     * Builds the transaction history view.
     */
    @NotNull
    private Map<Integer, Button> buildHistoryView() {
        Map<Integer, Button> buttons = new java.util.HashMap<>();

        // Back button
        buttons.put(0, createBackButton());

        // Title
        for (int i = 1; i < 9; i++) {
            buttons.put(i, createFillerButton(NamedTextColor.YELLOW));
        }

        // Placeholder for transaction entries
        buttons.put(22, Button.builder()
                .item(() -> ItemBuilder.of("minecraft:book")
                        .name(Component.text("Transaction History", NamedTextColor.YELLOW))
                        .lore(List.of(
                                Component.text("Your recent transactions", NamedTextColor.GRAY),
                                Component.text("will appear here.", NamedTextColor.GRAY),
                                Component.empty(),
                                Component.text("Use /bal history for details", NamedTextColor.DARK_GRAY)
                        ))
                        .build())
                .build());

        // Close button
        buttons.put(49, createCloseButton());

        return buttons;
    }

    /**
     * Creates the main balance display button.
     */
    @NotNull
    private Button createBalanceDisplayButton() {
        return Button.builder()
                .item(() -> {
                    Currency currency = economy.getDefaultCurrency();
                    BigDecimal balance = economy.getBalance(viewer.getUniqueId());

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text(currency.format(balance), NamedTextColor.YELLOW));
                    lore.add(Component.empty());

                    // Add other currencies
                    for (Currency c : economy.getCurrencies()) {
                        if (!c.isDefaultCurrency()) {
                            BigDecimal bal = economy.getBalance(viewer.getUniqueId(), c.getIdentifier());
                            lore.add(Component.text(c.getNamePlural() + ": " + c.format(bal), NamedTextColor.GRAY));
                        }
                    }

                    return ItemBuilder.of("minecraft:player_head")
                            .name(Component.text(viewer.getName() + "'s Wallet", NamedTextColor.GOLD)
                                    .decoration(TextDecoration.BOLD, true))
                            .lore(lore)
                            .build();
                })
                .build();
    }

    /**
     * Creates a filler button.
     */
    @NotNull
    private Button createFillerButton(@NotNull NamedTextColor color) {
        String material = switch (color) {
            case GOLD -> "minecraft:orange_stained_glass_pane";
            case GREEN -> "minecraft:lime_stained_glass_pane";
            case AQUA -> "minecraft:light_blue_stained_glass_pane";
            case YELLOW -> "minecraft:yellow_stained_glass_pane";
            default -> "minecraft:gray_stained_glass_pane";
        };

        return Button.builder()
                .item(() -> ItemBuilder.of(material)
                        .name(Component.empty())
                        .build())
                .build();
    }

    /**
     * Creates a back button.
     */
    @NotNull
    private Button createBackButton() {
        return Button.builder()
                .item(() -> ItemBuilder.of("minecraft:arrow")
                        .name(Component.text("Back", NamedTextColor.RED))
                        .lore(List.of(Component.text("Return to main menu", NamedTextColor.GRAY)))
                        .build())
                .onClick(ctx -> {
                    setState(CURRENT_VIEW, VIEW_MAIN);
                })
                .clickSound("minecraft:ui.button.click")
                .build();
    }

    /**
     * Creates a close button.
     */
    @NotNull
    private Button createCloseButton() {
        return Button.builder()
                .item(() -> ItemBuilder.of("minecraft:barrier")
                        .name(Component.text("Close", NamedTextColor.RED))
                        .build())
                .onClick(Button.ClickContext::closeInventory)
                .clickSound("minecraft:ui.button.click")
                .build();
    }

    /**
     * Creates a previous page button.
     */
    @NotNull
    private Button createPreviousPageButton() {
        return Button.builder()
                .item(() -> ItemBuilder.of("minecraft:arrow")
                        .name(Component.text("Previous Page", NamedTextColor.YELLOW))
                        .build())
                .onClick(ctx -> {
                    int currentPage = getState(PAGE, 1);
                    if (currentPage > 1) {
                        setState(PAGE, currentPage - 1);
                    }
                })
                .clickSound("minecraft:ui.button.click")
                .build();
    }

    /**
     * Creates a next page button.
     */
    @NotNull
    private Button createNextPageButton() {
        return Button.builder()
                .item(() -> ItemBuilder.of("minecraft:arrow")
                        .name(Component.text("Next Page", NamedTextColor.YELLOW))
                        .build())
                .onClick(ctx -> {
                    int currentPage = getState(PAGE, 1);
                    setState(PAGE, currentPage + 1);
                })
                .clickSound("minecraft:ui.button.click")
                .build();
    }

    /**
     * Gets the GUI title based on current view.
     *
     * @return the title component
     */
    @NotNull
    public Component getTitle() {
        String currentView = getState(CURRENT_VIEW, VIEW_MAIN);

        return switch (currentView) {
            case VIEW_CURRENCIES -> Component.text("Your Currencies", NamedTextColor.GOLD);
            case VIEW_TRANSFER -> Component.text("Transfer Money", NamedTextColor.GREEN);
            case VIEW_LEADERBOARD -> Component.text("Leaderboard", NamedTextColor.AQUA);
            case VIEW_HISTORY -> Component.text("Transaction History", NamedTextColor.YELLOW);
            default -> Component.text("Economy", NamedTextColor.GOLD);
        };
    }

    /**
     * Gets the GUI layout.
     *
     * @return the layout
     */
    @NotNull
    public Layout getLayout() {
        return Layout.CHEST_54; // 6 rows
    }
}
