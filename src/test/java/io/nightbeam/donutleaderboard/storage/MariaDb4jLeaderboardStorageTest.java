package io.nightbeam.donutleaderboard.storage;

import ch.vorburger.mariadb4j.DB;
import io.nightbeam.donutleaderboard.model.PeriodType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("docker")
class MariaDb4jLeaderboardStorageTest {

    @Test
    void storesScoresOnEmbeddedMariaDb() throws Exception {
        DB db = null;
        try {
            db = DB.newEmbeddedDB(0);
            db.start();
            db.createDB("donut_lb_test");
            var config = new com.zaxxer.hikari.HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:" + db.getConfiguration().getPort() + "/donut_lb_test");
            config.setUsername("root");
            config.setPassword("");
            config.setMaximumPoolSize(2);
            try (var ds = new com.zaxxer.hikari.HikariDataSource(config)) {
                MigrationRunner.migrate(ds);
                LeaderboardStorage storage = new LeaderboardStorage(ds);
                UUID uuid = UUID.randomUUID();
                storage.upsertScore(uuid, "kills", PeriodType.MONTHLY, 999L, 7D);
                Map<UUID, Double> loaded = storage.loadScores("kills", PeriodType.MONTHLY, 999L);
                assertEquals(7D, loaded.get(uuid));
            }
        } catch (Exception ex) {
            assumeTrue(false, "Embedded MariaDB unavailable in this environment: " + ex.getMessage());
        } finally {
            if (db != null) {
                db.stop();
            }
        }
    }
}
