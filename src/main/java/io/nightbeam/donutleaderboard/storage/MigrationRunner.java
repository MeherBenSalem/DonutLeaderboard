package io.nightbeam.donutleaderboard.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class MigrationRunner {

    public static final int SCHEMA_VERSION = 1;

    private MigrationRunner() {
    }

    public static void migrate(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            ensureMeta(connection);
            int current = readVersion(connection);
            if (current < 1) {
                applyV1(connection);
                writeVersion(connection, 1);
            }
        }
    }

    private static void ensureMeta(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS dl_schema (
                        id INTEGER PRIMARY KEY,
                        version INTEGER NOT NULL
                    )
                    """);
        }
    }

    private static int readVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT version FROM dl_schema WHERE id = 1")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static void writeVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO dl_schema (id, version) VALUES (1, ?)")) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }

    private static void applyV1(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS dl_scores (
                        player_uuid CHAR(36) NOT NULL,
                        category_id VARCHAR(64) NOT NULL,
                        period_type VARCHAR(16) NOT NULL,
                        period_start BIGINT NOT NULL,
                        value DOUBLE NOT NULL,
                        updated_at BIGINT NOT NULL,
                        PRIMARY KEY (player_uuid, category_id, period_type, period_start)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS dl_period_resets (
                        category_id VARCHAR(64) NOT NULL,
                        period_type VARCHAR(16) NOT NULL,
                        last_reset BIGINT NOT NULL,
                        PRIMARY KEY (category_id, period_type)
                    )
                    """);
        }
    }
}
