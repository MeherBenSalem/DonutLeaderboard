package io.nightbeam.donutleaderboard.listener;

import io.nightbeam.donutleaderboard.gui.BaseGui;
import io.nightbeam.donutleaderboard.gui.GuiManager;
import io.nightbeam.donutleaderboard.util.InventoryViews;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuiListener implements Listener {

    private final GuiManager guiManager;

    public GuiListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        guiManager.handleInventoryClick(player, event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (InventoryViews.topHolder(event.getView()) instanceof BaseGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            guiManager.handleClose(player);
        }
    }
}
