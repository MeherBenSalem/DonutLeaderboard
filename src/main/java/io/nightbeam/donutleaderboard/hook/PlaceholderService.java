package io.nightbeam.donutleaderboard.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class PlaceholderService {

    private volatile boolean enabled;

    public void hook() {
        enabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double parseDouble(OfflinePlayer player, String expression) {
        if (!enabled || player == null || expression == null) {
            return 0D;
        }
        String replaced = PlaceholderAPI.setPlaceholders(player, expression);
        return parseNumber(replaced);
    }

    public String parseString(OfflinePlayer player, String expression) {
        if (!enabled || expression == null) {
            return expression;
        }
        return PlaceholderAPI.setPlaceholders(player, expression);
    }

    static double parseNumber(String raw) {
        if (raw == null) {
            return 0D;
        }
        String cleaned = raw.replace(",", "").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return 0D;
        }
    }
}
