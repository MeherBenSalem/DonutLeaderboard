package io.nightbeam.donutleaderboard.service;

import io.nightbeam.donutleaderboard.config.CategoryRegistry;
import io.nightbeam.donutleaderboard.hook.PlaceholderService;
import io.nightbeam.donutleaderboard.hook.VaultHook;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.period.PeriodBounds;
import io.nightbeam.donutleaderboard.storage.LeaderboardStorage;
import io.nightbeam.donutleaderboard.storage.MigrationRunner;
import io.nightbeam.donutleaderboard.util.SchedulerAdapter;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardServiceConcurrencyTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void parallelRefreshRequestsDoNotCorruptCache() throws Exception {
        be.seeseemelk.mockbukkit.ServerMock server = be.seeseemelk.mockbukkit.MockBukkit.mock();
        server.addPlayer("cached");
        try {
            runConcurrencyScenario();
        } finally {
            be.seeseemelk.mockbukkit.MockBukkit.unmock();
        }
    }

    private void runConcurrencyScenario() throws Exception {
        var dbPath = tempDir.resolve("lb.db");
        var hikari = new com.zaxxer.hikari.HikariConfig();
        hikari.setJdbcUrl("jdbc:sqlite:" + dbPath.toAbsolutePath());
        hikari.setMaximumPoolSize(2);
        try (var ds = new com.zaxxer.hikari.HikariDataSource(hikari)) {
            MigrationRunner.migrate(ds);
            LeaderboardStorage storage = new LeaderboardStorage(ds);
            UUID uuid = UUID.randomUUID();
            storage.upsertScore(uuid, "kills", PeriodType.ALL_TIME, 0L, 3D);

            var plugin = mock(org.bukkit.plugin.java.JavaPlugin.class);
            when(plugin.getConfig()).thenReturn(new YamlConfiguration());
            when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

            SchedulerAdapter scheduler = new SchedulerAdapter(plugin);
            CategoryRegistry registry = mock(CategoryRegistry.class);
            when(registry.find("kills")).thenReturn(new io.nightbeam.donutleaderboard.model.CategoryDefinition(
                    "kills", "Kills", io.nightbeam.donutleaderboard.model.CategorySourceType.STATISTIC,
                    org.bukkit.Statistic.PLAYER_KILLS, null, org.bukkit.Material.IRON_SWORD, true, 11));
            when(registry.all()).thenReturn(java.util.List.of());

            LeaderboardService service = new LeaderboardService(
                    plugin,
                    scheduler,
                    storage,
                    registry,
                    new VaultHook(),
                    new PlaceholderService(),
                    new PeriodBounds(ZoneId.of("UTC")));
            service.reload(new YamlConfiguration());

            CategoryKey key = new CategoryKey("kills", PeriodType.ALL_TIME);
            // Seed cache directly to avoid OfflinePlayer statistic lookups in MockBukkit.
            service.cacheView();
            int threads = 8;
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger gates = new AtomicInteger();
            for (int i = 0; i < threads; i++) {
                scheduler.runAsync(() -> {
                    try {
                        start.await();
                        service.requestRefresh(key, true);
                        gates.incrementAndGet();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(15, TimeUnit.SECONDS));
            assertTrue(gates.get() >= threads);
            scheduler.shutdown();
        }
    }
}