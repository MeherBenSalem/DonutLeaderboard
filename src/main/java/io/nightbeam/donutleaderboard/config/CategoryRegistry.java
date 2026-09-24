package io.nightbeam.donutleaderboard.config;

import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.CategorySourceType;
import io.nightbeam.donutleaderboard.ranking.PlaceholderCategoryParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class CategoryRegistry {

    private final List<CategoryDefinition> categories = new ArrayList<>();

    public CategoryRegistry(JavaPlugin plugin) {
        reload(plugin.getConfig());
    }

    public void reload(FileConfiguration config) {
        categories.clear();
        registerBuiltins(config.getConfigurationSection("categories.builtin"));
        registerCustom(config.getConfigurationSection("categories.custom"));
    }

    public List<CategoryDefinition> all() {
        return List.copyOf(categories);
    }

    public CategoryDefinition find(String id) {
        if (id == null) {
            return null;
        }
        String key = id.toLowerCase(Locale.ROOT);
        for (CategoryDefinition category : categories) {
            if (category.id().equals(key)) {
                return category;
            }
        }
        return null;
    }

    private void registerBuiltins(ConfigurationSection section) {
        register("balance", "Balance", CategorySourceType.VAULT_BALANCE, null, null, Material.GOLD_INGOT, section, 10);
        register("kills", "Kills", CategorySourceType.STATISTIC, Statistic.PLAYER_KILLS, null, Material.IRON_SWORD, section, 11);
        register("deaths", "Deaths", CategorySourceType.STATISTIC, Statistic.DEATHS, null, Material.SKELETON_SKULL, section, 12);
        register("playtime", "Playtime", CategorySourceType.PLAYTIME, Statistic.PLAY_ONE_MINUTE, null, Material.CLOCK, section, 13);
        register("blocks_mined", "Blocks Mined", CategorySourceType.STATISTIC, Statistic.MINE_BLOCK, null, Material.DIAMOND_PICKAXE, section, 14);
        register("mob_kills", "Mob Kills", CategorySourceType.STATISTIC, Statistic.MOB_KILLS, null, Material.ZOMBIE_HEAD, section, 15);
        register("animals_bred", "Animals Bred", CategorySourceType.STATISTIC, Statistic.ANIMALS_BRED, null, Material.WHEAT, section, 16);
        register("fish_caught", "Fish Caught", CategorySourceType.STATISTIC, Statistic.FISH_CAUGHT, null, Material.FISHING_ROD, section, 19);
        register("damage_dealt", "Damage Dealt", CategorySourceType.STATISTIC, Statistic.DAMAGE_DEALT, null, Material.NETHERITE_AXE, section, 20);
    }

    private void register(
            String id,
            String defaultName,
            CategorySourceType type,
            Statistic statistic,
            String placeholder,
            Material icon,
            ConfigurationSection section,
            int defaultSlot) {
        ConfigurationSection entry = section == null ? null : section.getConfigurationSection(id);
        boolean enabled = entry == null || entry.getBoolean("enabled", true);
        if (!enabled) {
            return;
        }
        String name = entry != null ? entry.getString("name", defaultName) : defaultName;
        int slot = entry != null ? entry.getInt("slot", defaultSlot) : defaultSlot;
        Material material = icon;
        if (entry != null && entry.getString("icon") != null) {
            Material parsed = Material.matchMaterial(entry.getString("icon", icon.name()));
            if (parsed != null) {
                material = parsed;
            }
        }
        categories.add(new CategoryDefinition(id, name, type, statistic, placeholder, material, true, slot));
    }

    private void registerCustom(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null || !entry.getBoolean("enabled", true)) {
                continue;
            }
            String placeholder = entry.getString("placeholder");
            if (placeholder == null || placeholder.isBlank()) {
                continue;
            }
            PlaceholderCategoryParser.parse(placeholder);
            Material icon = Material.matchMaterial(entry.getString("icon", "PAPER"));
            if (icon == null) {
                icon = Material.PAPER;
            }
            categories.add(new CategoryDefinition(
                    key.toLowerCase(Locale.ROOT),
                    entry.getString("name", key),
                    CategorySourceType.PLACEHOLDER,
                    null,
                    placeholder,
                    icon,
                    true,
                    entry.getInt("slot", 22)));
        }
    }
}
