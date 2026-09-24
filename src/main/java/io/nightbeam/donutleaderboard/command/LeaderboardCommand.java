package io.nightbeam.donutleaderboard.command;

import io.nightbeam.donutleaderboard.DonutLeaderboardPlugin;
import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.LeaderboardSnapshot;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.model.RankedEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final DonutLeaderboardPlugin plugin;

    public LeaderboardCommand(DonutLeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "errors.player-only", "Only players can open the GUI.");
                return true;
            }
            if (!sender.hasPermission("donutleaderboard.use")) {
                plugin.messages().send(sender, "errors.no-permission", "You cannot do that.");
                return true;
            }
            plugin.guiManager().openCategoryMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "open" -> handleOpen(sender, args);
            case "rank" -> handleRank(sender, args);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "refresh" -> handleRefresh(sender, args);
            default -> {
                plugin.messages().send(sender, "errors.unknown-subcommand", "Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "errors.player-only", "Only players can open the GUI.");
            return true;
        }
        if (!sender.hasPermission("donutleaderboard.use")) {
            plugin.messages().send(sender, "errors.no-permission", "You cannot do that.");
            return true;
        }
        if (args.length < 2) {
            plugin.guiManager().openCategoryMenu(player);
            return true;
        }
        CategoryDefinition category = plugin.categories().find(args[1]);
        if (category == null) {
            plugin.messages().send(sender, "errors.unknown-category", "Unknown category.", "category", args[1]);
            return true;
        }
        PeriodType period = args.length >= 3 ? PeriodType.fromString(args[2]) : PeriodType.ALL_TIME;
        plugin.guiManager().openLeaderboard(player, category, period, 0);
        return true;
    }

    private boolean handleRank(CommandSender sender, String[] args) {
        if (!sender.hasPermission("donutleaderboard.rank")) {
            plugin.messages().send(sender, "errors.no-permission", "You cannot do that.");
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "errors.usage-rank", "Usage: /lb rank <player> <category> [period]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "errors.player-not-found", "Player not found.", "player", args[1]);
            return true;
        }
        CategoryDefinition category = plugin.categories().find(args[2]);
        if (category == null) {
            plugin.messages().send(sender, "errors.unknown-category", "Unknown category.", "category", args[2]);
            return true;
        }
        PeriodType period = args.length >= 4 ? PeriodType.fromString(args[3]) : PeriodType.ALL_TIME;
        CategoryKey key = new CategoryKey(category.id(), period);
        plugin.leaderboardService().requestRefresh(key, false);
        LeaderboardSnapshot snapshot = plugin.leaderboardService().getSnapshot(key);
        RankedEntry entry = snapshot == null ? null : snapshot.findPlayer(target.getUniqueId()).orElse(null);
        if (entry == null) {
            plugin.messages().send(sender, "info.rank-unranked", "Player is unranked.", "player", target.getName(), "category", category.displayName());
            return true;
        }
        plugin.messages().send(
                sender,
                "info.rank",
                "%player% is #%rank% in %category% with %value%.",
                "player",
                target.getName(),
                "rank",
                String.valueOf(entry.rank()),
                "category",
                category.displayName(),
                "value",
                String.valueOf(entry.value()));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("donutleaderboard.admin.reload")) {
            plugin.messages().send(sender, "errors.no-permission", "You cannot do that.");
            return true;
        }
        plugin.reloadPlugin();
        plugin.messages().send(sender, "info.reloaded", "DonutLeaderboard reloaded.");
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("donutleaderboard.admin.reset")) {
            plugin.messages().send(sender, "errors.no-permission", "You cannot do that.");
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "errors.usage-reset", "Usage: /lb reset <category> <period>");
            return true;
        }
        CategoryDefinition category = plugin.categories().find(args[1]);
        if (category == null) {
            plugin.messages().send(sender, "errors.unknown-category", "Unknown category.", "category", args[1]);
            return true;
        }
        PeriodType period = PeriodType.fromString(args[2]);
        CategoryKey key = new CategoryKey(category.id(), period);
        plugin.scheduler().runAsync(() -> {
            try {
                plugin.leaderboardService().resetCategory(key);
                plugin.scheduler().runGlobal(() -> plugin.messages().send(sender, "info.reset", "Reset complete.", "category", category.id(), "period", period.name()));
            } catch (Exception ex) {
                plugin.scheduler().runGlobal(() -> plugin.messages().send(sender, "errors.reset-failed", "Reset failed: %error%", "error", ex.getMessage()));
            }
        });
        return true;
    }

    private boolean handleRefresh(CommandSender sender, String[] args) {
        if (!sender.hasPermission("donutleaderboard.admin.refresh")) {
            plugin.messages().send(sender, "errors.no-permission", "You cannot do that.");
            return true;
        }
        if (args.length >= 2) {
            CategoryDefinition category = plugin.categories().find(args[1]);
            if (category == null) {
                plugin.messages().send(sender, "errors.unknown-category", "Unknown category.", "category", args[1]);
                return true;
            }
            PeriodType period = args.length >= 3 ? PeriodType.fromString(args[2]) : PeriodType.ALL_TIME;
            plugin.leaderboardService().requestRefresh(new CategoryKey(category.id(), period), true);
        } else {
            plugin.leaderboardService().refreshAll(true);
        }
        plugin.messages().send(sender, "info.refresh-started", "Refresh queued.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("open", "rank", "reload", "reset", "refresh"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return filter(plugin.categories().all().stream().map(CategoryDefinition::id).toList(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("refresh"))) {
            return filter(plugin.categories().all().stream().map(CategoryDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rank")) {
            return filter(plugin.categories().all().stream().map(CategoryDefinition::id).toList(), args[2]);
        }
        if (args.length == 3 && List.of("open", "reset", "refresh").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Arrays.stream(PeriodType.values()).map(Enum::name).map(String::toLowerCase).toList(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("rank")) {
            return filter(Arrays.stream(PeriodType.values()).map(Enum::name).map(String::toLowerCase).toList(), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rank")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toCollection(ArrayList::new));
    }
}
