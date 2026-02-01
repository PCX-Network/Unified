# GUI Framework Guide

The UnifiedPlugin GUI framework provides tools for creating inventory-based graphical interfaces with pagination, input dialogs, buttons, and state management.

## Table of Contents

- [Overview](#overview)
- [Basic GUI Creation](#basic-gui-creation)
- [Buttons and Components](#buttons-and-components)
- [Pagination](#pagination)
- [Input Dialogs](#input-dialogs)
- [State Management](#state-management)
- [Filters and Sorting](#filters-and-sorting)
- [Complete Examples](#complete-examples)

---

## Overview

### Key Features

- **Paginated GUIs** for displaying large collections
- **Button components** with click handlers and states
- **Input dialogs** using anvil, sign, and chat input
- **State management** for dynamic GUIs
- **Filtering and sorting** built into pagination
- **Async loading** with loading indicators

### Core Components

| Component | Description |
|-----------|-------------|
| `PaginatedGUI` | Base class for paginated inventories |
| `Button` | Clickable button component |
| `AnvilInput` | Text input via anvil GUI |
| `StatefulGUI` | GUI with reactive state |
| `Pagination` | Page management utility |

---

## Basic GUI Creation

### Extending PaginatedGUI

```java
import sh.pcx.unified.gui.pagination.PaginatedGUI;
import sh.pcx.unified.gui.pagination.ContentSlots;
import sh.pcx.unified.gui.pagination.PaginationConfig;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.player.UnifiedPlayer;
import net.kyori.adventure.text.Component;

public class PlayerListGUI extends PaginatedGUI<UUID> {

    private final UnifiedServer server;

    public PlayerListGUI(UnifiedPlayer viewer, UnifiedServer server) {
        super(viewer, "Online Players", 54);  // 6 rows
        this.server = server;

        // Configure content slots (rows 0-4, leaving row 5 for navigation)
        setContentSlots(ContentSlots.rows(0, 4, 9));  // 45 slots

        // Set up pagination config with navigation buttons
        setPaginationConfig(PaginationConfig.builder()
            .previousButtonSlot(45)
            .nextButtonSlot(53)
            .pageInfoSlot(49)
            .build());

        // Set the item renderer
        setItemRenderer((uuid, slot, context) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return null;

            return ItemBuilder.skull()
                .skullOwner(uuid)
                .name(Component.text(player.getName()))
                .lore(
                    Component.text("Level: " + player.getLevel()),
                    Component.text("Health: " + (int) player.getHealth()),
                    Component.empty(),
                    Component.text("Click to teleport")
                )
                .build();
        });

        // Load players
        loadPlayers();
    }

    private void loadPlayers() {
        List<UUID> players = server.getOnlinePlayers().stream()
            .map(UnifiedPlayer::getUniqueId)
            .toList();
        setItems(players);
    }

    @Override
    protected void handleItemClick(UUID uuid, int slot, Object clickType) {
        Player target = Bukkit.getPlayer(uuid);
        if (target != null) {
            viewer.teleport(target.getLocation());
            viewer.sendMessage(Component.text("Teleported to " + target.getName()));
            close();
        }
    }

    @Override
    protected void setSlot(int slot, UnifiedItemStack item) {
        // Platform-specific inventory slot setting
    }

    @Override
    protected void clearSlots() {
        // Platform-specific inventory clearing
    }

    @Override
    public void open() {
        // Platform-specific inventory opening
    }

    @Override
    public void close() {
        // Platform-specific inventory closing
    }
}
```

### Using the GUI

```java
// In a command
@Command(name = "players", description = "View online players")
public class PlayersCommand {

    @Inject
    private UnifiedServer server;

    @Default
    public void openPlayerList(@Sender Player player) {
        UnifiedPlayer viewer = server.getPlayer(player.getUniqueId()).orElseThrow();
        new PlayerListGUI(viewer, server).open();
    }
}
```

---

## Buttons and Components

### Basic Button

```java
import sh.pcx.unified.gui.component.Button;
import sh.pcx.unified.gui.component.Button.ClickContext;

// Simple button with click handler
Button simple = Button.builder()
    .item(ItemBuilder.of("minecraft:diamond")
        .name(Component.text("Click Me!"))
        .build())
    .onClick(ctx -> ctx.player().sendMessage("Clicked!"))
    .build();

// Static factory method
Button diamond = Button.of(
    ItemBuilder.of("minecraft:diamond")
        .name(Component.text("Diamond"))
        .build(),
    ctx -> ctx.sendMessage("You clicked a diamond!")
);
```

### Different Click Types

```java
Button multiClick = Button.builder()
    .item(ItemBuilder.of("minecraft:chest")
        .name(Component.text("Multi-Action Chest"))
        .lore(
            Component.text("Left-click: Open"),
            Component.text("Right-click: Info"),
            Component.text("Shift-click: Delete")
        )
        .build())
    .onClick(Button.ClickType.LEFT, ctx -> openChest(ctx.player()))
    .onClick(Button.ClickType.RIGHT, ctx -> showInfo(ctx.player()))
    .onClick(Button.ClickType.SHIFT_LEFT, ctx -> confirmDelete(ctx.player()))
    .build();
```

### Button with Permission

```java
Button adminButton = Button.builder()
    .item(ItemBuilder.of("minecraft:command_block")
        .name(Component.text("Admin Settings"))
        .build())
    .permission("myplugin.admin")
    .onClick(ctx -> openAdminMenu(ctx.player()))
    .build();

// Check visibility
if (adminButton.isVisibleTo(player)) {
    // Show button
}
```

### Button with Cooldown

```java
Button cooldownButton = Button.builder()
    .item(ItemBuilder.of("minecraft:ender_pearl")
        .name(Component.text("Teleport"))
        .build())
    .cooldown(5000)  // 5 second cooldown
    .onClick(ctx -> {
        ctx.player().teleport(getRandomLocation());
        ctx.sendMessage("Teleported!");
    })
    .build();
```

### Button with Sound

```java
Button soundButton = Button.builder()
    .item(myItem)
    .clickSound("minecraft:ui.button.click")
    .soundVolume(1.0f)
    .soundPitch(1.0f)
    .onClick(ctx -> doAction())
    .build();
```

### Conditional Visibility

```java
Button vipButton = Button.builder()
    .item(ItemBuilder.of("minecraft:nether_star")
        .name(Component.text("VIP Features"))
        .build())
    .visibleWhen(player -> player.hasPermission("vip.access"))
    .onClick(ctx -> openVipMenu(ctx.player()))
    .build();
```

### Button States

```java
import sh.pcx.unified.gui.component.Button.ButtonState;

Button statefulButton = Button.builder()
    .item(normalItem)
    .stateItem(ButtonState.LOADING, loadingItem)
    .stateItem(ButtonState.SUCCESS, successItem)
    .stateItem(ButtonState.ERROR, errorItem)
    .stateItem(ButtonState.DISABLED, disabledItem)
    .initialState(ButtonState.NORMAL)
    .onClick(ctx -> {
        Button button = ctx.button();
        button.setState(ButtonState.LOADING);

        // Async operation
        doAsyncWork().thenAccept(result -> {
            button.setState(result.isSuccess()
                ? ButtonState.SUCCESS
                : ButtonState.ERROR);
        });
    })
    .build();
```

### Toggle Button

```java
import sh.pcx.unified.gui.component.ToggleButton;

ToggleButton flyToggle = ToggleButton.builder()
    .onItem(ItemBuilder.of("minecraft:feather")
        .name(Component.text("Flying: ON", NamedTextColor.GREEN))
        .enchant(Enchantment.DURABILITY, 1)
        .hideEnchants()
        .build())
    .offItem(ItemBuilder.of("minecraft:feather")
        .name(Component.text("Flying: OFF", NamedTextColor.RED))
        .build())
    .initialState(player.getAllowFlight())
    .onToggle((ctx, enabled) -> {
        ctx.player().setAllowFlight(enabled);
        ctx.player().setFlying(enabled);
        ctx.sendMessage(enabled ? "Flight enabled!" : "Flight disabled!");
    })
    .build();
```

### Cycle Button

```java
import sh.pcx.unified.gui.component.CycleButton;

CycleButton<GameMode> gamemodeButton = CycleButton.<GameMode>builder()
    .values(GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE, GameMode.SPECTATOR)
    .itemFactory(gameMode -> ItemBuilder.of(getIconForGamemode(gameMode))
        .name(Component.text("Mode: " + gameMode.name()))
        .build())
    .initialValue(player.getGameMode())
    .onChange((ctx, newMode) -> {
        ctx.player().setGameMode(newMode);
        ctx.sendMessage("Game mode set to " + newMode.name());
    })
    .build();
```

---

## Pagination

### Basic Pagination

```java
import sh.pcx.unified.gui.pagination.Pagination;
import sh.pcx.unified.gui.pagination.PageContext;

// Create pagination
Pagination<Item> pagination = new Pagination<>(items, 45);  // 45 items per page

// Navigation
pagination.nextPage();
pagination.previousPage();
pagination.goToPage(2);
pagination.firstPage();
pagination.lastPage();

// Check navigation
boolean hasNext = pagination.hasNextPage();
boolean hasPrev = pagination.hasPreviousPage();
int currentPage = pagination.getCurrentPage();
int totalPages = pagination.getTotalPages();

// Get current page context
PageContext<Item> context = pagination.getContext();
List<Item> pageItems = context.getItems();
```

### Content Slots Configuration

```java
import sh.pcx.unified.gui.pagination.ContentSlots;

// Full inventory (all slots)
ContentSlots full = ContentSlots.full(54);

// Specific rows (0-indexed)
ContentSlots rows = ContentSlots.rows(0, 4, 9);  // Rows 0-4, 9 slots per row

// Custom slots
ContentSlots custom = ContentSlots.of(
    10, 11, 12, 13, 14, 15, 16,
    19, 20, 21, 22, 23, 24, 25,
    28, 29, 30, 31, 32, 33, 34
);

// Border layout (center slots only)
ContentSlots bordered = ContentSlots.bordered(54);

// Apply to GUI
gui.setContentSlots(rows);
```

### Pagination Configuration

```java
import sh.pcx.unified.gui.pagination.PaginationConfig;

PaginationConfig config = PaginationConfig.builder()
    // Navigation button positions
    .previousButtonSlot(45)
    .nextButtonSlot(53)
    .pageInfoSlot(49)

    // Button items
    .previousButton(ItemBuilder.of("minecraft:arrow")
        .name(Component.text("Previous Page"))
        .build())
    .nextButton(ItemBuilder.of("minecraft:arrow")
        .name(Component.text("Next Page"))
        .build())
    .disabledPreviousButton(ItemBuilder.of("minecraft:gray_stained_glass_pane")
        .name(Component.text("No Previous Page"))
        .build())
    .disabledNextButton(ItemBuilder.of("minecraft:gray_stained_glass_pane")
        .name(Component.text("No Next Page"))
        .build())

    // Page info display
    .pageInfoBuilder(context -> ItemBuilder.of("minecraft:paper")
        .name(Component.text("Page " + (context.getCurrentPage() + 1)
            + " / " + context.getTotalPages()))
        .lore(Component.text("Total items: " + context.getTotalItems()))
        .build())

    .build();

gui.setPaginationConfig(config);
```

### Item Renderer

```java
import sh.pcx.unified.gui.pagination.ItemRenderer;

// Define how items are rendered
ItemRenderer<ShopItem> renderer = (item, slot, context) -> {
    return ItemBuilder.of(item.getMaterial())
        .name(Component.text(item.getName()))
        .lore(
            Component.text("Price: $" + item.getPrice()),
            Component.text("Stock: " + item.getStock()),
            Component.empty(),
            Component.text("Click to purchase")
        )
        .build();
};

gui.setItemRenderer(renderer);
```

### Empty State

```java
import sh.pcx.unified.gui.pagination.EmptyState;

// Configure empty state display
gui.setEmptyState(EmptyState.builder()
    .item(ItemBuilder.of("minecraft:barrier")
        .name(Component.text("No Items Found", NamedTextColor.RED))
        .lore(Component.text("Try adjusting your filters"))
        .build())
    .build());
```

---

## Input Dialogs

### Anvil Input

```java
import sh.pcx.unified.gui.input.AnvilInput;
import sh.pcx.unified.gui.input.AnvilInputResult;

// Simple text input
AnvilInput.builder()
    .title(Component.text("Enter Item Name"))
    .defaultText("My Item")
    .onComplete(result -> {
        if (result.isConfirmed()) {
            String name = result.getText().orElse("");
            player.sendMessage(Component.text("Named: " + name));
        }
    })
    .open(player);
```

### Anvil Input with Validation

```java
AnvilInput.builder()
    .title(Component.text("Enter Amount"))
    .defaultText("1")
    .validator(text -> {
        try {
            int amount = Integer.parseInt(text);
            return amount > 0 && amount <= 64;
        } catch (NumberFormatException e) {
            return false;
        }
    })
    .validationFailedMessage(Component.text("Enter a number between 1-64"))
    .onComplete(result -> {
        if (result.isConfirmed()) {
            int amount = Integer.parseInt(result.getTextOrDefault("1"));
            giveItems(player, amount);
        }
    })
    .open(player);
```

### Anvil Input with Previous GUI

```java
AnvilInput.builder()
    .title(Component.text("Search"))
    .previousGui(() -> createSearchResultsGui(player))
    .onComplete(result -> {
        if (result.isConfirmed()) {
            performSearch(result.getText().orElse(""));
        }
        // Will return to previous GUI automatically
    })
    .open(player);
```

### Dynamic Output Item

```java
AnvilInput.builder()
    .title(Component.text("Rename Item"))
    .defaultText(originalName)
    .leftItem(originalItem)
    .outputItem(text -> ItemBuilder.from(originalItem)
        .name(Component.text(text))
        .build())
    .onTextChange(text -> {
        // Called as player types
        // Can be used for live preview
    })
    .onComplete(result -> {
        if (result.isConfirmed()) {
            renameItem(originalItem, result.getText().orElse(originalName));
        }
    })
    .open(player);
```

### Cancellation Handling

```java
import sh.pcx.unified.gui.input.InputCancellation;

AnvilInput.builder()
    .title(Component.text("Enter Name"))
    .cancellation(InputCancellation.builder()
        .allowEscape(true)
        .escapeMessage(Component.text("Cancelled!"))
        .onCancel(() -> player.sendMessage("Input cancelled"))
        .build())
    .closeOnComplete(true)
    .preventClose(false)  // Allow closing with ESC
    .onComplete(result -> {
        // Handle result
    })
    .open(player);
```

---

## State Management

### Stateful GUI

```java
import sh.pcx.unified.gui.StatefulGUI;
import sh.pcx.unified.gui.StateKey;
import sh.pcx.unified.gui.GUIState;

// Define state keys
StateKey<Integer> SELECTED_PAGE = StateKey.of("selectedPage", Integer.class);
StateKey<String> SEARCH_QUERY = StateKey.of("searchQuery", String.class);
StateKey<SortOrder> SORT_ORDER = StateKey.of("sortOrder", SortOrder.class);

public class ShopGUI extends StatefulGUI<ShopItem> {

    public ShopGUI(UnifiedPlayer viewer) {
        super(viewer, "Shop", 54);

        // Set initial state
        setState(SELECTED_PAGE, 0);
        setState(SEARCH_QUERY, "");
        setState(SORT_ORDER, SortOrder.NAME_ASC);

        // React to state changes
        onStateChange(SEARCH_QUERY, this::applySearch);
        onStateChange(SORT_ORDER, this::applySorting);
    }

    private void applySearch(String query) {
        if (query.isEmpty()) {
            clearFilter();
        } else {
            setFilter(item -> item.getName().toLowerCase()
                .contains(query.toLowerCase()));
        }
    }

    private void applySorting(SortOrder order) {
        switch (order) {
            case NAME_ASC -> setSorter(Comparator.comparing(ShopItem::getName));
            case NAME_DESC -> setSorter(Comparator.comparing(ShopItem::getName).reversed());
            case PRICE_ASC -> setSorter(Comparator.comparing(ShopItem::getPrice));
            case PRICE_DESC -> setSorter(Comparator.comparing(ShopItem::getPrice).reversed());
        }
    }

    // Search button
    Button searchButton = Button.builder()
        .item(ItemBuilder.of("minecraft:compass")
            .name(Component.text("Search"))
            .build())
        .onClick(ctx -> {
            AnvilInput.builder()
                .title(Component.text("Search Items"))
                .defaultText(getState(SEARCH_QUERY))
                .onComplete(result -> {
                    if (result.isConfirmed()) {
                        setState(SEARCH_QUERY, result.getText().orElse(""));
                    }
                })
                .previousGui(() -> this)
                .open(ctx.player());
        })
        .build();
}
```

---

## Filters and Sorting

### Applying Filters

```java
import sh.pcx.unified.gui.pagination.Filter;
import sh.pcx.unified.gui.pagination.FilterBuilder;

// Simple predicate filter
gui.setFilter(item -> item.getPrice() <= maxPrice);

// Clear filter
gui.clearFilter();

// Using Filter builder
Filter<ShopItem> categoryFilter = FilterBuilder.<ShopItem>create()
    .where(item -> item.getCategory() == selectedCategory)
    .and(item -> item.isAvailable())
    .build();

gui.setFilter(categoryFilter);
```

### Search Filter

```java
import sh.pcx.unified.gui.pagination.SearchFilter;

// Text-based search
SearchFilter<ShopItem> search = SearchFilter.<ShopItem>builder()
    .searchFields(
        ShopItem::getName,
        ShopItem::getDescription,
        item -> item.getCategory().name()
    )
    .caseSensitive(false)
    .build();

// Apply search
search.setQuery("diamond");
gui.setFilter(search);
```

### Sorting

```java
// Simple comparator
gui.setSorter(Comparator.comparing(ShopItem::getName));

// Toggle direction
gui.toggleSortDirection();  // ASCENDING <-> DESCENDING

// Set direction explicitly
gui.setSortDirection(SortDirection.DESCENDING);

// Combined sort (primary and secondary)
Comparator<ShopItem> sorter = Comparator
    .comparing(ShopItem::getCategory)
    .thenComparing(ShopItem::getPrice);
gui.setSorter(sorter);
```

### Sort Button

```java
Button sortButton = Button.builder()
    .item(() -> ItemBuilder.of("minecraft:hopper")
        .name(Component.text("Sort: " + getCurrentSortName()))
        .lore(Component.text("Click to change sort order"))
        .build())
    .onClick(ctx -> {
        // Cycle through sort options
        SortOption next = getNextSortOption();
        gui.setSorter(next.getComparator());
        gui.refresh();
    })
    .build();
```

---

## Complete Examples

### Shop GUI

```java
public class ShopGUI extends PaginatedGUI<ShopItem> {

    private final ShopManager shopManager;
    private final EconomyService economy;

    public ShopGUI(UnifiedPlayer viewer, ShopManager shopManager, EconomyService economy) {
        super(viewer, "Shop", 54);
        this.shopManager = shopManager;
        this.economy = economy;

        // Configure layout
        setContentSlots(ContentSlots.rows(0, 4, 9));
        setPaginationConfig(createPaginationConfig());

        // Render items
        setItemRenderer(this::renderShopItem);

        // Empty state
        setEmptyState(EmptyState.of(
            ItemBuilder.of("minecraft:barrier")
                .name(Component.text("No items available"))
                .build()
        ));

        // Load items
        setItems(shopManager.getAllItems());

        // Add category filter buttons (row 5)
        addCategoryButtons();
    }

    private UnifiedItemStack renderShopItem(ShopItem item, int slot, PageContext<ShopItem> context) {
        double balance = economy.getBalance(viewer.getUniqueId());
        boolean canAfford = balance >= item.getPrice();

        return ItemBuilder.of(item.getMaterial())
            .name(Component.text(item.getName())
                .color(canAfford ? NamedTextColor.GREEN : NamedTextColor.RED))
            .lore(
                Component.text("Price: $" + item.getPrice()),
                Component.text("Your balance: $" + balance),
                Component.empty(),
                canAfford
                    ? Component.text("Click to purchase", NamedTextColor.GREEN)
                    : Component.text("Not enough money!", NamedTextColor.RED)
            )
            .build();
    }

    @Override
    protected void handleItemClick(ShopItem item, int slot, Object clickType) {
        double balance = economy.getBalance(viewer.getUniqueId());

        if (balance < item.getPrice()) {
            viewer.sendMessage(Component.text("Not enough money!", NamedTextColor.RED));
            return;
        }

        // Confirm purchase
        AnvilInput.builder()
            .title(Component.text("Enter quantity"))
            .defaultText("1")
            .validator(text -> {
                try {
                    int qty = Integer.parseInt(text);
                    return qty > 0 && qty * item.getPrice() <= balance;
                } catch (NumberFormatException e) {
                    return false;
                }
            })
            .onComplete(result -> {
                if (result.isConfirmed()) {
                    int quantity = Integer.parseInt(result.getTextOrDefault("1"));
                    completePurchase(item, quantity);
                }
            })
            .previousGui(() -> this)
            .open(viewer);
    }

    private void completePurchase(ShopItem item, int quantity) {
        double total = item.getPrice() * quantity;
        economy.withdraw(viewer.getUniqueId(), total);
        shopManager.giveItem(viewer, item, quantity);
        viewer.sendMessage(Component.text(
            "Purchased " + quantity + "x " + item.getName() + " for $" + total,
            NamedTextColor.GREEN
        ));
        refresh();
    }

    private void addCategoryButtons() {
        int slot = 46;
        for (Category category : Category.values()) {
            setSlot(slot++, Button.builder()
                .item(ItemBuilder.of(category.getIcon())
                    .name(Component.text(category.getDisplayName()))
                    .build())
                .onClick(ctx -> {
                    setFilter(item -> item.getCategory() == category);
                    firstPage();
                })
                .build()
                .getItem());
        }

        // All items button
        setSlot(52, Button.builder()
            .item(ItemBuilder.of("minecraft:chest")
                .name(Component.text("All Items"))
                .build())
            .onClick(ctx -> {
                clearFilter();
                firstPage();
            })
            .build()
            .getItem());
    }
}
```

### Confirmation Dialog

```java
public class ConfirmationGUI {

    public static void show(UnifiedPlayer player, String message, Runnable onConfirm, Runnable onCancel) {
        // Create a simple 27-slot inventory
        // ... platform-specific inventory creation

        // Confirm button (slot 11)
        Button confirmButton = Button.builder()
            .item(ItemBuilder.of("minecraft:lime_wool")
                .name(Component.text("Confirm", NamedTextColor.GREEN))
                .lore(Component.text(message))
                .build())
            .onClick(ctx -> {
                ctx.closeInventory();
                onConfirm.run();
            })
            .build();

        // Cancel button (slot 15)
        Button cancelButton = Button.builder()
            .item(ItemBuilder.of("minecraft:red_wool")
                .name(Component.text("Cancel", NamedTextColor.RED))
                .build())
            .onClick(ctx -> {
                ctx.closeInventory();
                onCancel.run();
            })
            .build();

        // Set items and open
    }
}

// Usage
ConfirmationGUI.show(
    player,
    "Delete all your homes?",
    () -> {
        homeManager.deleteAllHomes(player.getUniqueId());
        player.sendMessage("All homes deleted!");
    },
    () -> player.sendMessage("Cancelled!")
);
```

---

## Next Steps

- [Testing Guide](testing.md) - Test GUIs with MockServer
- [Commands Guide](commands.md) - Open GUIs from commands
- [Modules Guide](modules.md) - Organize GUI code in modules
