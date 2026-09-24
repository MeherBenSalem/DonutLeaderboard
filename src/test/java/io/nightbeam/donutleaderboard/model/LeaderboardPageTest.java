package io.nightbeam.donutleaderboard.model;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardPageTest {

    @Test
    void paginatesEntries() {
        UUID id = UUID.randomUUID();
        List<RankedEntry> entries = List.of(new RankedEntry(1, id, "Player", 10));
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot(new CategoryKey("kills", PeriodType.ALL_TIME), entries, 0L, 1);
        LeaderboardPage page = snapshot.page(0, 1);
        assertEquals(1, page.entries().size());
        assertEquals(1, page.totalPages());
    }
}
