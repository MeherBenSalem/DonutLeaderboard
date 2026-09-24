package io.nightbeam.donutleaderboard.listener;

import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.period.PeriodBounds;
import io.nightbeam.donutleaderboard.service.LeaderboardService;
import io.nightbeam.donutleaderboard.storage.LeaderboardStorage;
import io.nightbeam.donutleaderboard.util.SchedulerAdapter;
import java.time.Instant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class StatListener implements Listener {

    private final LeaderboardStorage storage;
    private final LeaderboardService leaderboardService;
    private final PeriodBounds periodBounds;
    private final SchedulerAdapter scheduler;

    public StatListener(
            LeaderboardStorage storage,
            LeaderboardService leaderboardService,
            PeriodBounds periodBounds,
            SchedulerAdapter scheduler) {
        this.storage = storage;
        this.leaderboardService = leaderboardService;
        this.periodBounds = periodBounds;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        bump(victim, "deaths", 1D);
        Player killer = victim.getKiller();
        if (killer != null) {
            bump(killer, "kills", 1D);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduler.runAsync(() -> leaderboardService.refreshAll(false));
    }

    private void bump(Player player, String categoryId, double delta) {
        scheduler.runAsync(() -> {
            for (PeriodType period : PeriodType.values()) {
                if (period == PeriodType.ALL_TIME) {
                    continue;
                }
                long start = periodBounds.periodStart(period, Instant.now()).toEpochMilli();
                try {
                    double current = storage.loadScores(categoryId, period, start).getOrDefault(player.getUniqueId(), 0D);
                    storage.upsertScore(player.getUniqueId(), categoryId, period, start, current + delta);
                } catch (Exception ignored) {
                }
            }
        });
    }
}
