package io.nightbeam.donutleaderboard.util;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;

public final class StatisticHelper {

    private StatisticHelper() {
    }

    public static double read(OfflinePlayer player, Statistic statistic) {
        if (player == null || statistic == null) {
            return 0D;
        }
        try {
            if (statistic == Statistic.MINE_BLOCK) {
                return sumMineBlock(player);
            }
            if (statistic == Statistic.DAMAGE_DEALT) {
                return sumDamageDealt(player);
            }
            if (statistic.getType() == Statistic.Type.UNTYPED) {
                return player.getStatistic(statistic);
            }
            return player.getStatistic(statistic);
        } catch (IllegalArgumentException ex) {
            return 0D;
        }
    }

    private static int sumMineBlock(OfflinePlayer player) {
        int total = 0;
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }
            try {
                total += player.getStatistic(Statistic.MINE_BLOCK, material);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return total;
    }

    private static int sumDamageDealt(OfflinePlayer player) {
        int total = 0;
        for (EntityType type : EntityType.values()) {
            if (!type.isAlive()) {
                continue;
            }
            try {
                total += player.getStatistic(Statistic.DAMAGE_DEALT, type);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return total;
    }
}
