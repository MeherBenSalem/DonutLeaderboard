package io.nightbeam.donutleaderboard.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GuiInventoryFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private GuiInventoryFactory() {
    }

    public static Inventory create(InventoryHolder holder, int size, Component title) {
        String legacy = LEGACY.serialize(title);
        return Bukkit.createInventory(holder, size, legacy);
    }
}
