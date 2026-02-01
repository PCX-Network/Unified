/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sh.pcx.example.minigame.MinigamePlugin;
import sh.pcx.example.minigame.arena.Arena;
import sh.pcx.example.minigame.arena.ArenaManager;
import sh.pcx.example.minigame.arena.ArenaState;
import sh.pcx.example.minigame.game.Game;
import sh.pcx.example.minigame.game.GameManager;
import sh.pcx.unified.gui.pagination.ContentSlots;
import sh.pcx.unified.gui.pagination.PageContext;
import sh.pcx.unified.gui.pagination.PaginatedGUI;
import sh.pcx.unified.gui.pagination.PaginationConfig;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A paginated GUI for selecting and joining arenas.
 *
 * <p>This class demonstrates the UnifiedPlugin GUI framework including:
 * <ul>
 *   <li>Extending {@link PaginatedGUI} for paginated content</li>
 *   <li>Custom item rendering for arena entries</li>
 *   <li>Click handling to join arenas</li>
 *   <li>Dynamic state-based item display</li>
 * </ul>
 *
 * <h2>GUI Layout</h2>
 * <pre>
 * +---+---+---+---+---+---+---+---+---+
 * | Arena Icons (4 rows = 36 slots)  |
 * |                                   |
 * |                                   |
 * |                                   |
 * +---+---+---+---+---+---+---+---+---+
 * | < |   |   | I |   |   |   |   | > |
 * +---+---+---+---+---+---+---+---+---+
 * </pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class ArenaSelectionGUI extends PaginatedGUI<Arena> {

    private final MinigamePlugin plugin;
    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    /**
     * Creates a new ArenaSelectionGUI.
     *
     * @param plugin       the plugin instance
     * @param viewer       the player viewing the GUI
     * @param arenaManager the arena manager
     * @param gameManager  the game manager
     */
    public ArenaSelectionGUI(@NotNull MinigamePlugin plugin, @NotNull UnifiedPlayer viewer,
                             @NotNull ArenaManager arenaManager, @NotNull GameManager gameManager) {
        super(viewer, "Select Arena", 54);

        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;

        // Configure content slots (rows 0-3, 36 slots)
        setContentSlots(ContentSlots.rows(0, 4, 9));

        // Configure pagination buttons
        setPaginationConfig(PaginationConfig.builder()
                .previousButtonSlot(45)
                .nextButtonSlot(53)
                .pageInfoSlot(49)
                .build());

        // Set up the item renderer
        setItemRenderer(this::renderArenaItem);

        // Load arenas
        loadArenas();
    }

    /**
     * Loads arenas into the GUI.
     */
    private void loadArenas() {
        List<Arena> arenas = new ArrayList<>(arenaManager.getEnabledArenas());
        // Sort by name
        arenas.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        setItems(arenas);
    }

    /**
     * Renders an arena as an inventory item.
     *
     * @param arena   the arena to render
     * @param slot    the slot the item will be placed in
     * @param context the current page context
     * @return the rendered item
     */
    private UnifiedItemStack renderArenaItem(@NotNull Arena arena, int slot, @NotNull PageContext<Arena> context) {
        // Determine the material based on arena state
        String material = switch (arena.getState()) {
            case WAITING -> "LIME_CONCRETE";
            case STARTING -> "YELLOW_CONCRETE";
            case IN_GAME -> "ORANGE_CONCRETE";
            case RESETTING -> "LIGHT_GRAY_CONCRETE";
            default -> "RED_CONCRETE";
        };

        // Get player count if in game
        int playerCount = 0;
        Optional<Game> game = gameManager.getActiveGames().stream()
                .filter(g -> g.getArena().equals(arena))
                .findFirst();
        if (game.isPresent()) {
            playerCount = game.get().getPlayerCount();
        }

        // Build the item
        // In production, this would use ItemBuilder:
        //
        // return ItemBuilder.of(material)
        //     .name(Component.text(arena.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD))
        //     .lore(buildArenaLore(arena, playerCount))
        //     .glow(arena.getState() == ArenaState.WAITING)
        //     .build();

        // For demonstration, we return a placeholder
        return createPlaceholderItem(arena, playerCount);
    }

    /**
     * Builds the lore for an arena item.
     *
     * @param arena       the arena
     * @param playerCount current player count
     * @return the lore lines
     */
    private List<Component> buildArenaLore(@NotNull Arena arena, int playerCount) {
        List<Component> lore = new ArrayList<>();

        // Status line
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text(arena.getState().getDisplayName(), getStateColor(arena.getState()))));

        // Player count
        lore.add(Component.text("Players: ", NamedTextColor.GRAY)
                .append(Component.text(playerCount + "/" + arena.getMaxPlayers(), NamedTextColor.WHITE)));

        // Separator
        lore.add(Component.empty());

        // Description if present
        if (!arena.getDescription().isEmpty()) {
            lore.add(Component.text(arena.getDescription(), NamedTextColor.YELLOW));
            lore.add(Component.empty());
        }

        // Action hint
        if (arena.getState().canJoin()) {
            lore.add(Component.text("Click to join!", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("Currently unavailable", NamedTextColor.RED));
        }

        return lore;
    }

    /**
     * Gets the color for an arena state.
     *
     * @param state the arena state
     * @return the color
     */
    private NamedTextColor getStateColor(@NotNull ArenaState state) {
        return switch (state) {
            case WAITING -> NamedTextColor.GREEN;
            case STARTING -> NamedTextColor.YELLOW;
            case IN_GAME -> NamedTextColor.GOLD;
            case RESETTING -> NamedTextColor.GRAY;
            case DISABLED -> NamedTextColor.RED;
            case MAINTENANCE -> NamedTextColor.DARK_GRAY;
        };
    }

    /**
     * Creates a placeholder item for demonstration.
     *
     * <p>In production, this would use the ItemBuilder from unified-item.
     *
     * @param arena       the arena
     * @param playerCount the player count
     * @return a placeholder item
     */
    private UnifiedItemStack createPlaceholderItem(@NotNull Arena arena, int playerCount) {
        // This is a placeholder - in production you would use:
        // return ItemBuilder.of(Material.LIME_CONCRETE)
        //     .name(Component.text(arena.getDisplayName()))
        //     .lore(buildArenaLore(arena, playerCount))
        //     .build();

        return null; // Placeholder
    }

    @Override
    protected void handleItemClick(@NotNull Arena arena, int slot, @NotNull Object clickType) {
        // Check if arena is joinable
        if (!arena.getState().canJoin()) {
            viewer.sendMessage(Component.text("This arena is not available!", NamedTextColor.RED));
            return;
        }

        // Close the GUI
        close();

        // Try to join the game
        if (gameManager.isInGame(viewer)) {
            viewer.sendMessage(Component.text("You are already in a game!", NamedTextColor.RED));
            return;
        }

        Game game = gameManager.getOrCreateGame(arena);
        if (gameManager.joinGame(viewer, game)) {
            viewer.sendMessage(Component.text("Joined arena '" + arena.getDisplayName() + "'!", NamedTextColor.GREEN));
        }
    }

    @Override
    protected void setSlot(int slot, @NotNull UnifiedItemStack item) {
        // In production, this would set the item in the actual inventory:
        // inventory.setItem(slot, item.toBukkitItemStack());
    }

    @Override
    protected void clearSlots() {
        // In production, this would clear all content slots:
        // for (int slot : getContentSlots().getSlots()) {
        //     inventory.setItem(slot, null);
        // }
    }

    @Override
    public void open() {
        // In production, this would open the inventory for the player:
        // Inventory inv = Bukkit.createInventory(null, size, title);
        // viewer.getHandle().openInventory(inv);
        refresh();
        plugin.getLogger().info("Opened arena selection GUI for " + viewer.getName());
    }

    @Override
    public void close() {
        // In production, this would close the player's inventory:
        // viewer.closeInventory();
        plugin.getLogger().info("Closed arena selection GUI for " + viewer.getName());
    }

    @Override
    protected void onRefresh(@NotNull PageContext<Arena> context) {
        // Add decorative items
        addDecorativeItems();
    }

    /**
     * Adds decorative border and info items.
     */
    private void addDecorativeItems() {
        // In production, you would add glass pane borders, info items, etc.
        // For example:
        //
        // // Fill bottom row with glass panes
        // UnifiedItemStack filler = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
        //     .name(Component.empty())
        //     .build();
        //
        // for (int i = 46; i < 53; i++) {
        //     if (i != 49) { // Skip page info slot
        //         setSlot(i, filler);
        //     }
        // }
        //
        // // Add info item
        // UnifiedItemStack info = ItemBuilder.of(Material.BOOK)
        //     .name(Component.text("Arena Info", NamedTextColor.GOLD))
        //     .lore(List.of(
        //         Component.text("Click an arena to join!", NamedTextColor.GRAY),
        //         Component.text("Green = Available", NamedTextColor.GREEN),
        //         Component.text("Yellow = Starting", NamedTextColor.YELLOW),
        //         Component.text("Red = In Progress", NamedTextColor.RED)
        //     ))
        //     .build();
        // setSlot(4, info);
    }
}
