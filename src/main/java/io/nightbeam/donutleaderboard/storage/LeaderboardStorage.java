package io.nightbeam.donutleaderboard.storage;

import io.nightbeam.donutleaderboard.model.PeriodType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

public final class LeaderboardStorage {

    private final DataSource dataSource;

    public LeaderboardStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<UUID, Double> loadScores(String categoryId, PeriodType periodType, long periodStart) throws SQLException {
        Map<UUID, Double> scores = new HashMap<>();
        String sql = """
                SELECT player_uuid, value FROM dl_scores
                WHERE category_id = ? AND period_type = ? AND period_start = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, periodType.name());
            ps.setLong(3, periodStart);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    scores.put(UUID.fromString(rs.getString(1)), rs.getDouble(2));
                }
            }
        }
        return scores;
    }

    public void upsertScore(UUID uuid, String categoryId, PeriodType periodType, long periodStart, double value) throws SQLException {
        String sql = """
                INSERT INTO dl_scores (player_uuid, category_id, period_type, period_start, value, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid, category_id, period_type, period_start) DO UPDATE SET
                    value = excluded.value,
                    updated_at = excluded.updated_at
                """;
        try (Connection connection = dataSource.getConnection()) {
            upsert(connection, sql, uuid, categoryId, periodType, periodStart, value);
        } catch (SQLException ex) {
            String mysql = """
                    INSERT INTO dl_scores (player_uuid, category_id, period_type, period_start, value, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE value = VALUES(value), updated_at = VALUES(updated_at)
                    """;
            try (Connection connection = dataSource.getConnection()) {
                upsert(connection, mysql, uuid, categoryId, periodType, periodStart, value);
            }
        }
    }

    private void upsert(
            Connection connection,
            String sql,
            UUID uuid,
            String categoryId,
            PeriodType periodType,
            long periodStart,
            double value)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            ps.setString(1, uuid.toString());
            ps.setString(2, categoryId);
            ps.setString(3, periodType.name());
            ps.setLong(4, periodStart);
            ps.setDouble(5, value);
            ps.setLong(6, now);
            ps.executeUpdate();
        }
    }

    public void resetCategoryPeriod(String categoryId, PeriodType periodType, long periodStart) throws SQLException {
        String sql = "DELETE FROM dl_scores WHERE category_id = ? AND period_type = ? AND period_start = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, periodType.name());
            ps.setLong(3, periodStart);
            ps.executeUpdate();
        }
        recordReset(categoryId, periodType, System.currentTimeMillis());
    }

    public void recordReset(String categoryId, PeriodType periodType, long when) throws SQLException {
        String sql = """
                INSERT INTO dl_period_resets (category_id, period_type, last_reset)
                VALUES (?, ?, ?)
                ON CONFLICT(category_id, period_type) DO UPDATE SET last_reset = excluded.last_reset
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, periodType.name());
            ps.setLong(3, when);
            ps.executeUpdate();
        } catch (SQLException ex) {
            String mysql = """
                    INSERT INTO dl_period_resets (category_id, period_type, last_reset)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE last_reset = VALUES(last_reset)
                    """;
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement ps = connection.prepareStatement(mysql)) {
                ps.setString(1, categoryId);
                ps.setString(2, periodType.name());
                ps.setLong(3, when);
                ps.executeUpdate();
            }
        }
    }
}
