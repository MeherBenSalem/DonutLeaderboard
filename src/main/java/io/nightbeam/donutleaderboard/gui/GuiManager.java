package io.nightbeam.donutleaderboard.gui;

import io.nightbeam.donutleaderboard.DonutLeaderboardPlugin;
import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.util.InventoryViews;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GuiManager {

    private final DonutLeaderboardPlugin plugin;
    private final Map<UUID, BaseGui> openGuis = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> navigating = new ConcurrentHashMap<>();

    public GuiManager(DonutLeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public DonutLeaderboardPlugin plugin() {
        return plugin;
    }

    public void openCategoryMenu(Player player) {
        open(player, new CategoryMenuGui(this, plugin.getConfig().getString("gui.menu-title", "<gold>Leaderboards")));
    }

    public void openLeaderboard(Player player, CategoryDefinition category, PeriodType period, int page) {
        CategoryKey key = new CategoryKey(category.id(), period);
        plugin.leaderboardService().requestRefresh(key, false);
        open(player, new LeaderboardGui(this, category, period, page));
    }

    public void open(Player player, BaseGui gui) {
        closeSilently(player);
        navigating.put(player.getUniqueId(), true);
        plugin.scheduler().runEntity(player, () -> {
            player.openInventory(gui.render(player));
            openGuis.put(player.getUniqueId(), gui);
            navigating.remove(player.getUniqueId());
        });
    }

    public void handleInventoryClick(Player player, InventoryClickEvent event) {
        BaseGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) {
            return;
        }
        if (InventoryViews.topHolder(event.getView()) != gui) {
            return;
        }
        event.setCancelled(true);
        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) {
            return;
        }
        if (event.getClickedInventory() == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        gui.handleClick(player, event);
    }

    public void closeAllForReload() {
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            if (openGuis.containsKey(player.getUniqueId())) {
                player.closeInventory();
            }
        }
        openGuis.clear();
        navigating.clear();
    }

    public void handleClose(Player player) {
        if (navigating.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        openGuis.remove(player.getUniqueId());
    }

    public BaseGui getOpenGui(UUID uuid) {
        return openGuis.get(uuid);
    }

    private void closeSilently(Player player) {
        openGuis.remove(player.getUniqueId());
    }
}
