package io.nightbeam.donutleaderboard.util;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class ItemBuilder {

    private ItemBuilder() {
    }

    public static ItemStack of(Material material, net.kyori.adventure.text.Component name, List<net.kyori.adventure.text.Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(line)).toList());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack playerHead(String ownerName, net.kyori.adventure.text.Component name, List<net.kyori.adventure.text.Component> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            meta.setOwner(ownerName);
            if (name != null) {
                meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));
            }
            if (lore != null) {
                meta.setLore(lore.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(line)).toList());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack filler(Material material, net.kyori.adventure.text.Component name) {
        return of(material, name, List.of());
    }
}
