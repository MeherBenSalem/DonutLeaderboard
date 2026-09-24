package io.nightbeam.donutleaderboard.storage;

import io.nightbeam.donutleaderboard.model.PeriodType;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteLeaderboardStorageTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void migratesAndStoresScores() throws Exception {
        var dbPath = tempDir.resolve("test.db");
        var config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath.toAbsolutePath());
        config.setMaximumPoolSize(1);
        try (var ds = new com.zaxxer.hikari.HikariDataSource(config)) {
            MigrationRunner.migrate(ds);
            LeaderboardStorage storage = new LeaderboardStorage(ds);
            UUID uuid = UUID.randomUUID();
            storage.upsertScore(uuid, "kills", PeriodType.ALL_TIME, 0L, 12D);
            Map<UUID, Double> loaded = storage.loadScores("kills", PeriodType.ALL_TIME, 0L);
            assertEquals(12D, loaded.get(uuid));
            storage.resetCategoryPeriod("kills", PeriodType.ALL_TIME, 0L);
            loaded = storage.loadScores("kills", PeriodType.ALL_TIME, 0L);
            assertTrue(loaded.isEmpty());
        }
        assertTrue(Files.exists(dbPath));
    }
}
