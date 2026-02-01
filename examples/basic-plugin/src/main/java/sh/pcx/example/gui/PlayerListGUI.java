/*
 * Basic Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sh.pcx.example.BasicPlugin;
import sh.pcx.example.config.BasicPluginConfig;
import sh.pcx.unified.gui.pagination.ContentSlots;
import sh.pcx.unified.gui.pagination.PageContext;
import sh.pcx.unified.gui.pagination.PaginatedGUI;
import sh.pcx.unified.gui.pagination.PaginationConfig;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Paginated GUI displaying players with filtering and sorting.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class PlayerListGUI extends PaginatedGUI<PlayerListGUI.PlayerEntry> {

    private final BasicPlugin plugin;
    private final BasicPluginConfig.GuiConfig guiConfig;
    private final UnifiedItemStack[] inventory;

    public PlayerListGUI(@NotNull UnifiedPlayer viewer, @NotNull BasicPlugin plugin) {
        super(viewer, "Online Players", 54);
        this.plugin = plugin;
        this.guiConfig = plugin.getPluginConfig().getGui();
        this.inventory = new UnifiedItemStack[54];

        setContentSlots(ContentSlots.rectangle(9, 7, 4, 54));
        configurePagination();
        configureItemRenderer();
        loadPlayers();
    }

    private void configurePagination() {
        setPaginationConfig(PaginationConfig.builder()
            .previousButtonSlot(45).nextButtonSlot(53).pageInfoSlot(49)
            .previousButton(navItem("minecraft:arrow", "Previous Page", NamedTextColor.YELLOW))
            .nextButton(navItem("minecraft:arrow", "Next Page", NamedTextColor.YELLOW))
            .disabledPreviousButton(navItem("minecraft:barrier", "Previous Page", NamedTextColor.GRAY))
            .disabledNextButton(navItem("minecraft:barrier", "Next Page", NamedTextColor.GRAY))
            .build());
    }

    private void configureItemRenderer() {
        setItemRenderer((entry, slot, ctx) -> ItemBuilder.skull()
            .skullOwner(entry.uuid())
            .name(Component.text(entry.name(), NamedTextColor.YELLOW))
            .lore(Component.empty(),
                Component.text().append(Component.text("Rank: ", NamedTextColor.GRAY))
                    .append(Component.text(entry.rank(), NamedTextColor.GOLD)).build(),
                Component.text().append(Component.text("Playtime: ", NamedTextColor.GRAY))
                    .append(Component.text(formatTime(entry.playtime()), NamedTextColor.WHITE)).build(),
                Component.empty(),
                Component.text("Click to interact", NamedTextColor.GREEN))
            .build());
    }

    private void loadPlayers() {
        List<PlayerEntry> list = new ArrayList<>();
        list.add(new PlayerEntry(UUID.randomUUID(), "Notch", "Owner", 10000));
        list.add(new PlayerEntry(UUID.randomUUID(), "jeb_", "Admin", 8000));
        for (int i = 1; i <= 50; i++)
            list.add(new PlayerEntry(UUID.randomUUID(), "Player" + i, "Member", (long)(Math.random() * 5000)));
        setItems(list);
        setSorter(Comparator.comparingLong(PlayerEntry::playtime));
        setSortDirection(sh.pcx.unified.gui.pagination.SortDirection.DESCENDING);
    }

    private UnifiedItemStack navItem(String mat, String name, NamedTextColor color) {
        return ItemBuilder.of(mat).name(Component.text(name, color)).build();
    }

    private String formatTime(long min) {
        return min < 60 ? min + "m" : (min / 60) + "h " + (min % 60) + "m";
    }

    @Override protected void setSlot(int slot, @NotNull UnifiedItemStack item) {
        if (slot >= 0 && slot < 54) inventory[slot] = item;
    }

    @Override protected void clearSlots() {
        for (int i = 0; i < 54; i++) inventory[i] = null;
    }

    @Override public void open() {
        renderBorder();
        if (guiConfig.getCloseButtonSlot() >= 0)
            setSlot(guiConfig.getCloseButtonSlot(), ItemBuilder.of("minecraft:barrier")
                .name(Component.text("Close", NamedTextColor.RED)).build());
        refresh();
        if (plugin.getPluginConfig().isDebug())
            plugin.getLogger().info("Opened GUI for " + viewer.getName());
    }

    @Override public void close() {
        if (plugin.getPluginConfig().isDebug())
            plugin.getLogger().info("Closed GUI for " + viewer.getName());
    }

    @Override protected void handleItemClick(@NotNull PlayerEntry entry, int slot, @NotNull Object click) {
        viewer.sendMessage(Component.text("Clicked: " + entry.name(), NamedTextColor.YELLOW));
    }

    @Override protected void onPageChange(@NotNull PageContext<PlayerEntry> ctx) {
        if (plugin.getPluginConfig().isDebug())
            plugin.getLogger().info("Page: " + ctx.getDisplayPage());
    }

    private void renderBorder() {
        UnifiedItemStack border = ItemBuilder.of("minecraft:gray_stained_glass_pane").name(Component.empty()).build();
        for (int i = 0; i < 9; i++) if (i != 4) setSlot(i, border);
        setSlot(4, ItemBuilder.of("minecraft:player_head")
            .name(Component.text("Online Players", NamedTextColor.GOLD, TextDecoration.BOLD)).build());
        for (int r = 1; r < 5; r++) setSlot(r * 9 + 8, border);
        for (int i = 45; i < 54; i++)
            if (i != 45 && i != 49 && i != 53 && i != guiConfig.getCloseButtonSlot()) setSlot(i, border);
    }

    public void filterByName(@NotNull String q) {
        if (q.isEmpty()) clearFilter();
        else setFilter((java.util.function.Predicate<PlayerEntry>) e -> e.name().toLowerCase().contains(q.toLowerCase()));
    }

    public void filterByRank(@NotNull String rank) { setFilter((java.util.function.Predicate<PlayerEntry>) e -> e.rank().equalsIgnoreCase(rank)); }

    public void sortByPlaytime(boolean asc) {
        setSorter(Comparator.comparingLong(PlayerEntry::playtime));
        setSortDirection(asc ? sh.pcx.unified.gui.pagination.SortDirection.ASCENDING : sh.pcx.unified.gui.pagination.SortDirection.DESCENDING);
    }

    public void sortByName(boolean asc) {
        setSorter(Comparator.comparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER));
        setSortDirection(asc ? sh.pcx.unified.gui.pagination.SortDirection.ASCENDING : sh.pcx.unified.gui.pagination.SortDirection.DESCENDING);
    }

    public record PlayerEntry(@NotNull UUID uuid, @NotNull String name, @NotNull String rank, long playtime) {}
}
