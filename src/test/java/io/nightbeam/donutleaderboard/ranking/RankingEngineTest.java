package io.nightbeam.donutleaderboard.ranking;

import io.nightbeam.donutleaderboard.model.RankedEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingEngineTest {

    @Test
    void sortsDescendingAndCompetitionRanksTies() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Map<UUID, Double> scores = new LinkedHashMap<>();
        scores.put(a, 10D);
        scores.put(b, 10D);
        scores.put(c, 5D);
        Map<UUID, String> names = Map.of(a, "A", b, "B", c, "C");

        List<RankedEntry> ranked = RankingEngine.rank(scores, names);
        assertEquals(3, ranked.size());
        assertEquals(1, ranked.get(0).rank());
        assertEquals(1, ranked.get(1).rank());
        assertEquals(3, ranked.get(2).rank());
        assertEquals(10D, ranked.get(0).value());
        assertEquals(10D, ranked.get(1).value());
    }

    @Test
    void mergeScoresOverlayWins() {
        UUID id = UUID.randomUUID();
        Map<UUID, Double> base = Map.of(id, 1D);
        Map<UUID, Double> overlay = Map.of(id, 9D);
        Map<UUID, Double> merged = RankingEngine.mergeScores(base, overlay);
        assertEquals(9D, merged.get(id));
    }
}
