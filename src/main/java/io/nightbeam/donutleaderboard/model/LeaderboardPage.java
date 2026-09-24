package io.nightbeam.donutleaderboard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record LeaderboardPage(
        List<RankedEntry> entries,
        int pageIndex,
        int totalPages,
        int totalEntries,
        RankedEntry viewerEntry) {

    public static LeaderboardPage fromSnapshot(LeaderboardSnapshot snapshot, int pageIndex, int pageSize) {
        List<RankedEntry> all = snapshot.entries();
        int safePageSize = Math.max(1, pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) safePageSize));
        int safePage = Math.min(Math.max(0, pageIndex), totalPages - 1);
        int from = safePage * safePageSize;
        int to = Math.min(from + safePageSize, all.size());
        List<RankedEntry> slice = from >= all.size() ? List.of() : new ArrayList<>(all.subList(from, to));
        return new LeaderboardPage(slice, safePage, totalPages, all.size(), null);
    }

    public LeaderboardPage withViewer(UUID uuid, LeaderboardSnapshot snapshot) {
        if (uuid == null) {
            return this;
        }
        RankedEntry viewer = snapshot.findPlayer(uuid).orElse(null);
        return new LeaderboardPage(entries, pageIndex, totalPages, totalEntries, viewer);
    }

    public boolean hasPrevious() {
        return pageIndex > 0;
    }

    public boolean hasNext() {
        return pageIndex + 1 < totalPages;
    }
}
