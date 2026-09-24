package io.nightbeam.donutleaderboard.service;

import io.nightbeam.donutleaderboard.config.CategoryRegistry;
import io.nightbeam.donutleaderboard.hook.PlaceholderService;
import io.nightbeam.donutleaderboard.hook.VaultHook;
import io.nightbeam.donutleaderboard.model.CategoryDefinition;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.CategorySourceType;
import io.nightbeam.donutleaderboard.model.LeaderboardSnapshot;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.model.RankedEntry;
import io.nightbeam.donutleaderboard.period.PeriodBounds;
import io.nightbeam.donutleaderboard.ranking.RankingEngine;
import io.nightbeam.donutleaderboard.storage.LeaderboardStorage;
import io.nightbeam.donutleaderboard.util.SchedulerAdapter;
import io.nightbeam.donutleaderboard.util.StatisticHelper;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeaderboardService {

    private final JavaPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final LeaderboardStorage storage;
    private final CategoryRegistry categories;
    private final VaultHook vaultHook;
    private final PlaceholderService placeholderService;
    private final PeriodBounds periodBounds;
    private final Map<String, LeaderboardSnapshot> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> refreshInFlight = new ConcurrentHashMap<>();
    private volatile int refreshSeconds = 120;

    public LeaderboardService(
            JavaPlugin plugin,
            SchedulerAdapter scheduler,
            LeaderboardStorage storage,
            CategoryRegistry categories,
            VaultHook vaultHook,
            PlaceholderService placeholderService,
            PeriodBounds periodBounds) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.storage = storage;
        this.categories = categories;
        this.vaultHook = vaultHook;
        this.placeholderService = placeholderService;
        this.periodBounds = periodBounds;
    }

    public void reload(FileConfiguration config) {
        refreshSeconds = Math.max(15, config.getInt("cache.refresh-seconds", 120));
    }

    public LeaderboardSnapshot getSnapshot(CategoryKey key) {
        return cache.get(key.cacheKey());
    }

    public void requestRefresh(CategoryKey key, boolean force) {
        LeaderboardSnapshot existing = cache.get(key.cacheKey());
        long now = System.currentTimeMillis();
        if (!force && existing != null && now - existing.generatedAtMillis() < refreshSeconds * 1000L) {
            return;
        }
        AtomicBoolean gate = refreshInFlight.computeIfAbsent(key.cacheKey(), ignored -> new AtomicBoolean(false));
        if (!gate.compareAndSet(false, true)) {
            return;
        }
        scheduler.runAsync(() -> {
            try {
                refreshCategory(key);
            } finally {
                gate.set(false);
            }
        });
    }

    public void refreshAll(boolean force) {
        for (CategoryDefinition category : categories.all()) {
            for (PeriodType period : PeriodType.values()) {
                requestRefresh(new CategoryKey(category.id(), period), force);
            }
        }
    }

    private void refreshCategory(CategoryKey key) {
        CategoryDefinition category = categories.find(key.id());
        if (category == null) {
            return;
        }
        Instant now = Instant.now();
        long periodStart = periodBounds.periodStart(key.period(), now).toEpochMilli();
        Map<UUID, Double> stored;
        try {
            stored = new HashMap<>(storage.loadScores(key.id(), key.period(), periodStart));
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed loading scores for " + key.cacheKey() + ": " + ex.getMessage());
            stored = new HashMap<>();
        }

        Set<UUID> playerIds = new HashSet<>(stored.keySet());
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getUniqueId() != null) {
                playerIds.add(offlinePlayer.getUniqueId());
            }
        }

        Map<UUID, String> names = new HashMap<>();
        Map<UUID, Double> live = new HashMap<>();
        for (UUID uuid : playerIds) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            names.put(uuid, player.getName() == null ? uuid.toString() : player.getName());
            double value = resolveValue(category, key, periodStart, player, stored);
            live.put(uuid, value);
            if (Math.abs(value) > 0.0001D || stored.containsKey(uuid)) {
                try {
                    storage.upsertScore(uuid, key.id(), key.period(), periodStart, value);
                } catch (Exception ex) {
                    plugin.getLogger().fine("Score upsert failed for " + uuid + ": " + ex.getMessage());
                }
            }
        }

        Map<UUID, Double> merged = RankingEngine.mergeScores(stored, live);
        merged.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() <= 0D);
        List<RankedEntry> ranked = RankingEngine.rank(merged, names);
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot(key, ranked, System.currentTimeMillis(), ranked.size());
        cache.put(key.cacheKey(), snapshot);
    }

    private double resolveValue(
            CategoryDefinition category,
            CategoryKey key,
            long periodStart,
            OfflinePlayer player,
            Map<UUID, Double> stored) {
        UUID uuid = player.getUniqueId();
        return switch (category.sourceType()) {
            case VAULT_BALANCE -> vaultHook.balance(player);
            case PLAYTIME -> key.period() == PeriodType.ALL_TIME
                    ? ticksToHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE))
                    : stored.getOrDefault(uuid, 0D);
            case STATISTIC -> key.period() == PeriodType.ALL_TIME
                    ? StatisticHelper.read(player, category.statistic())
                    : stored.getOrDefault(uuid, 0D);
            case PLACEHOLDER -> placeholderService.parseDouble(player, category.placeholder());
        };
    }

    private static double ticksToHours(int ticks) {
        return ticks / 20D / 60D / 60D;
    }

    public void resetCategory(CategoryKey key) throws Exception {
        long periodStart = periodBounds.periodStart(key.period(), Instant.now()).toEpochMilli();
        storage.resetCategoryPeriod(key.id(), key.period(), periodStart);
        cache.remove(key.cacheKey());
        requestRefresh(key, true);
    }

    public Map<String, LeaderboardSnapshot> cacheView() {
        return Map.copyOf(cache);
    }

    /** Injects a cached snapshot (used by tests). */
    public void putSnapshotForTest(CategoryKey key, LeaderboardSnapshot snapshot) {
        cache.put(key.cacheKey(), snapshot);
    }
}
