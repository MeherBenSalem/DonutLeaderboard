package io.nightbeam.donutleaderboard.storage;

import io.nightbeam.donutleaderboard.model.PeriodType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("docker")
class MySqlLeaderboardStorageTest {

    @Test
    void storesScoresOnMysql() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
            mysql.start();
            var config = new com.zaxxer.hikari.HikariConfig();
            config.setJdbcUrl(mysql.getJdbcUrl());
            config.setUsername(mysql.getUsername());
            config.setPassword(mysql.getPassword());
            config.setMaximumPoolSize(2);
            try (var ds = new com.zaxxer.hikari.HikariDataSource(config)) {
                MigrationRunner.migrate(ds);
                LeaderboardStorage storage = new LeaderboardStorage(ds);
                UUID uuid = UUID.randomUUID();
                storage.upsertScore(uuid, "balance", PeriodType.WEEKLY, 123L, 50D);
                Map<UUID, Double> loaded = storage.loadScores("balance", PeriodType.WEEKLY, 123L);
                assertEquals(50D, loaded.get(uuid));
            }
        }
    }
}
