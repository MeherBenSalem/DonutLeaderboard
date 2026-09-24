package io.nightbeam.donutleaderboard.papi;

import io.nightbeam.donutleaderboard.DonutLeaderboardPlugin;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.LeaderboardSnapshot;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.model.RankedEntry;
import java.util.List;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class DonutLeaderboardExpansion extends PlaceholderExpansion {

    private final DonutLeaderboardPlugin plugin;

    public DonutLeaderboardExpansion(DonutLeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "donutleaderboard";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NAIZO";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.toLowerCase(Locale.ROOT).split("_");
        if (parts.length < 2) {
            return "";
        }
        if (parts[0].equals("rank") && parts.length >= 3) {
            String category = parts[1];
            PeriodType period = parts.length >= 4 ? PeriodType.fromString(parts[3]) : PeriodType.ALL_TIME;
            if (player == null) {
                return "0";
            }
            LeaderboardSnapshot snapshot = plugin.leaderboardService().getSnapshot(new CategoryKey(category, period));
            if (snapshot == null) {
                return "0";
            }
            return snapshot.findPlayer(player.getUniqueId()).map(entry -> String.valueOf(entry.rank())).orElse("0");
        }
        if (parts[0].equals("top") && parts.length >= 4) {
            int position = parseInt(parts[1], 0);
            String field = parts[2];
            String category = parts[3];
            PeriodType period = parts.length >= 5 ? PeriodType.fromString(parts[4]) : PeriodType.ALL_TIME;
            LeaderboardSnapshot snapshot = plugin.leaderboardService().getSnapshot(new CategoryKey(category, period));
            if (snapshot == null || position <= 0) {
                return "";
            }
            List<RankedEntry> entries = snapshot.entries();
            if (position > entries.size()) {
                return "";
            }
            RankedEntry entry = entries.get(position - 1);
            return switch (field) {
                case "name" -> entry.name();
                case "value" -> String.valueOf(entry.value());
                case "rank" -> String.valueOf(entry.rank());
                default -> "";
            };
        }
        return "";
    }

    private static int parseInt(String raw, int def) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
