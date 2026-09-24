package io.nightbeam.donutleaderboard.gui;

import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.LeaderboardPage;
import io.nightbeam.donutleaderboard.model.LeaderboardSnapshot;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.model.RankedEntry;
import io.nightbeam.donutleaderboard.ranking.PaginationHelper;
import io.nightbeam.donutleaderboard.util.ItemBuilder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class LeaderboardGui extends BaseGui {

    public static final int PREVIOUS_SLOT = 45;
    public static final int NEXT_SLOT = 53;
    public static final int BACK_SLOT = 49;

    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("#,##0.##");

    private final GuiManager guiManager;
    private final CategoryDefinition category;
    private final PeriodType period;
    private int pageIndex;

    public LeaderboardGui(GuiManager guiManager, CategoryDefinition category, PeriodType period, int pageIndex) {
        this.guiManager = guiManager;
        this.category = category;
        this.period = period;
        this.pageIndex = Math.max(0, pageIndex);
    }

    @Override
    public Inventory render(Player player) {
        var plugin = guiManager.plugin();
        CategoryKey key = new CategoryKey(category.id(), period);
        LeaderboardSnapshot snapshot = plugin.leaderboardService().getSnapshot(key);
        List<Integer> entrySlots = plugin.getConfig().getIntegerList("gui.entry-slots");
        if (entrySlots.isEmpty()) {
            entrySlots = defaultEntrySlots();
        }
        int pageSize = entrySlots.size();
        LeaderboardPage page = snapshot == null
                ? new LeaderboardPage(List.of(), 0, 1, 0, null)
                : snapshot.page(pageIndex, pageSize).withViewer(player.getUniqueId(), snapshot);

        String titleTemplate = plugin.getConfig().getString("gui.board-title", "<gold>%category% <gray>(%period%)");
        Component title = plugin.messages().component(titleTemplate
                .replace("%category%", category.displayName())
                .replace("%period%", period.name()));
        Inventory inventory = GuiInventoryFactory.create(this, plugin.getConfig().getInt("gui.rows", 6) * 9, title);

        Material fillerMaterial = Material.matchMaterial(plugin.getConfig().getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMaterial == null) {
            fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        Component fillerName = plugin.messages().component(" ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, ItemBuilder.filler(fillerMaterial, fillerName));
        }

        for (int i = 0; i < page.entries().size() && i < entrySlots.size(); i++) {
            RankedEntry entry = page.entries().get(i);
            int slot = entrySlots.get(i);
            inventory.setItem(slot, createEntryItem(plugin, entry));
        }

        inventory.setItem(PREVIOUS_SLOT, ItemBuilder.of(Material.ARROW, plugin.messages().component("<yellow>Previous"), List.of()));
        inventory.setItem(NEXT_SLOT, ItemBuilder.of(Material.ARROW, plugin.messages().component("<yellow>Next"), List.of()));
        inventory.setItem(BACK_SLOT, ItemBuilder.of(Material.BARRIER, plugin.messages().component("<red>Back"), List.of()));

        int viewerSlot = plugin.getConfig().getInt("gui.show-viewer-rank-slot", 48);
        if (page.viewerEntry() != null && viewerSlot >= 0 && viewerSlot < inventory.getSize()) {
            inventory.setItem(viewerSlot, createEntryItem(plugin, page.viewerEntry()));
        }

        this.pageIndex = page.pageIndex();
        return attach(inventory);
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        CategoryKey key = new CategoryKey(category.id(), period);
        LeaderboardSnapshot snapshot = guiManager.plugin().leaderboardService().getSnapshot(key);
        int totalPages = snapshot == null ? 1 : snapshot.page(pageIndex, defaultEntrySlots().size()).totalPages();

        if (slot == PREVIOUS_SLOT) {
            pageIndex = PaginationHelper.pageAfterNavigation(pageIndex, totalPages, PaginationHelper.NavigationAction.PREVIOUS);
            guiManager.open(player, new LeaderboardGui(guiManager, category, period, pageIndex));
            return;
        }
        if (slot == NEXT_SLOT) {
            pageIndex = PaginationHelper.pageAfterNavigation(pageIndex, totalPages, PaginationHelper.NavigationAction.NEXT);
            guiManager.open(player, new LeaderboardGui(guiManager, category, period, pageIndex));
            return;
        }
        if (slot == BACK_SLOT) {
            guiManager.openCategoryMenu(player);
        }
    }

    public static int pageAfterClick(int currentPage, int totalPages, int slot) {
        if (slot == PREVIOUS_SLOT) {
            return PaginationHelper.pageAfterNavigation(currentPage, totalPages, PaginationHelper.NavigationAction.PREVIOUS);
        }
        if (slot == NEXT_SLOT) {
            return PaginationHelper.pageAfterNavigation(currentPage, totalPages, PaginationHelper.NavigationAction.NEXT);
        }
        return currentPage;
    }

    private Component rankPrefix(int rank) {
        return switch (rank) {
            case 1 -> guiManager.plugin().messages().component("<gold><bold>#1");
            case 2 -> guiManager.plugin().messages().component("<gray><bold>#2");
            case 3 -> guiManager.plugin().messages().component("<#CD7F32><bold>#3");
            default -> guiManager.plugin().messages().component("<white>#" + rank);
        };
    }

    private org.bukkit.inventory.ItemStack createEntryItem(io.nightbeam.donutleaderboard.DonutLeaderboardPlugin plugin, RankedEntry entry) {
        Map<String, String> placeholders = Map.of(
                "rank", String.valueOf(entry.rank()),
                "player", entry.name(),
                "value", VALUE_FORMAT.format(entry.value()));
        List<Component> lore = plugin.messages().lore("gui.entry-lore", placeholders);
        return ItemBuilder.playerHead(entry.name(), rankPrefix(entry.rank()).append(Component.text(" " + entry.name())), lore);
    }

    private static List<Integer> defaultEntrySlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }
}
