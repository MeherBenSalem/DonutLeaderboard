package io.nightbeam.donutleaderboard.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

public final class InventoryViews {

    private InventoryViews() {
    }

    public static InventoryHolder topHolder(InventoryView view) {
        if (view == null) {
            return null;
        }
        Inventory top = view.getTopInventory();
        if (top == null) {
            return null;
        }
        try {
            var method = Inventory.class.getMethod("getHolder", boolean.class);
            Object holder = method.invoke(top, false);
            if (holder instanceof InventoryHolder inventoryHolder) {
                return inventoryHolder;
            }
        } catch (ReflectiveOperationException ignored) {
            // Spigot fallback
        }
        return top.getHolder();
    }
}
