package io.nightbeam.donutleaderboard.gui;

import io.nightbeam.donutleaderboard.DonutLeaderboardPlugin;
import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.util.ItemBuilder;
import io.nightbeam.donutleaderboard.util.MessageUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class CategoryMenuGui extends BaseGui {

    public static final int PERIOD_SLOT = 4;

    private final GuiManager guiManager;
    private final Component title;

    public CategoryMenuGui(GuiManager guiManager, String titleRaw) {
        this.guiManager = guiManager;
        MessageUtil messages = guiManager.plugin().messages();
        this.title = messages.component(titleRaw);
    }

    @Override
    public Inventory render(Player player) {
        DonutLeaderboardPlugin plugin = guiManager.plugin();
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        Inventory inventory = GuiInventoryFactory.create(this, rows * 9, title);
        Material fillerMaterial = Material.matchMaterial(plugin.getConfig().getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMaterial == null) {
            fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        Component fillerName = plugin.messages().component(plugin.getConfig().getString("gui.filler.name", " "));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, ItemBuilder.filler(fillerMaterial, fillerName));
        }
        for (CategoryDefinition category : plugin.categories().all()) {
            if (category.menuSlot() < 0 || category.menuSlot() >= inventory.getSize()) {
                continue;
            }
            Map<String, String> placeholders = Map.of("category", category.displayName());
            inventory.setItem(
                    category.menuSlot(),
                    ItemBuilder.of(
                            category.menuIcon(),
                            plugin.messages().component("<yellow>" + category.displayName()),
                            plugin.messages().lore("gui.category-lore", placeholders)));
        }
        inventory.setItem(PERIOD_SLOT, ItemBuilder.of(
                Material.CLOCK,
                plugin.messages().component("<aqua>All-Time"),
                plugin.messages().lore("gui.period-hint", Map.of())));
        return attach(inventory);
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        DonutLeaderboardPlugin plugin = guiManager.plugin();
        int slot = event.getRawSlot();
        for (CategoryDefinition category : plugin.categories().all()) {
            if (category.menuSlot() == slot) {
                guiManager.openLeaderboard(player, category, PeriodType.ALL_TIME, 0);
                return;
            }
        }
    }
}
