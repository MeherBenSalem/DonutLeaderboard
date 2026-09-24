package io.nightbeam.donutleaderboard.service;

import io.nightbeam.donutleaderboard.config.CategoryRegistry;
import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.period.PeriodBounds;
import io.nightbeam.donutleaderboard.util.SchedulerAdapter;
import java.time.Instant;
import org.bukkit.plugin.java.JavaPlugin;

public final class PeriodResetScheduler {

    private final JavaPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final LeaderboardService leaderboardService;
    private final CategoryRegistry categories;
    private final PeriodBounds periodBounds;
    private SchedulerAdapter.CancellableTask task;

    public PeriodResetScheduler(
            JavaPlugin plugin,
            SchedulerAdapter scheduler,
            LeaderboardService leaderboardService,
            CategoryRegistry categories,
            PeriodBounds periodBounds) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.leaderboardService = leaderboardService;
        this.categories = categories;
        this.periodBounds = periodBounds;
    }

    public void start() {
        stop();
        task = scheduler.runGlobalRepeating(this::tickResets, 20L * 60L, 20L * 60L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tickResets() {
        Instant now = Instant.now();
        for (CategoryDefinition category : categories.all()) {
            for (PeriodType period : PeriodType.values()) {
                if (period == PeriodType.ALL_TIME) {
                    continue;
                }
                Instant next = periodBounds.nextReset(period, now);
                if (next.toEpochMilli() - now.toEpochMilli() > 60_000L) {
                    continue;
                }
                CategoryKey key = new CategoryKey(category.id(), period);
                scheduler.runAsync(() -> {
                    try {
                        leaderboardService.resetCategory(key);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Period reset failed for " + key.cacheKey() + ": " + ex.getMessage());
                    }
                });
            }
        }
    }
}
