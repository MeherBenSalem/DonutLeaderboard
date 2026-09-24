package io.nightbeam.donutleaderboard.ranking;

import io.nightbeam.donutleaderboard.model.RankedEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RankingEngine {

    private RankingEngine() {
    }

    public static List<RankedEntry> rank(Map<UUID, Double> rawScores, Map<UUID, String> names) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(rawScores.entrySet());
        sorted.sort(Comparator.<Map.Entry<UUID, Double>>comparingDouble(Map.Entry::getValue).reversed()
                .thenComparing(entry -> names.getOrDefault(entry.getKey(), entry.getKey().toString())));

        List<RankedEntry> ranked = new ArrayList<>(sorted.size());
        int index = 0;
        int lastRank = 0;
        Double lastValue = null;
        for (Map.Entry<UUID, Double> entry : sorted) {
            index++;
            double value = entry.getValue() == null ? 0D : entry.getValue();
            int rank = index;
            if (lastValue != null && Double.compare(lastValue, value) == 0) {
                rank = lastRank;
            } else {
                lastRank = index;
            }
            lastValue = value;
            String name = names.getOrDefault(entry.getKey(), entry.getKey().toString());
            ranked.add(new RankedEntry(rank, entry.getKey(), name, value));
        }
        return List.copyOf(ranked);
    }

    public static Map<UUID, Double> mergeScores(Map<UUID, Double> base, Map<UUID, Double> overlay) {
        Map<UUID, Double> merged = new LinkedHashMap<>(base);
        for (Map.Entry<UUID, Double> entry : overlay.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }
        return merged;
    }
}
