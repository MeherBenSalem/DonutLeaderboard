package io.nightbeam.donutleaderboard.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record LeaderboardSnapshot(
        CategoryKey category,
        List<RankedEntry> entries,
        long generatedAtMillis,
        int totalPlayers) {

    public Optional<RankedEntry> findPlayer(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return entries.stream().filter(entry -> uuid.equals(entry.uuid())).findFirst();
    }

    public LeaderboardPage page(int pageIndex, int pageSize) {
        return LeaderboardPage.fromSnapshot(this, pageIndex, pageSize);
    }
}
