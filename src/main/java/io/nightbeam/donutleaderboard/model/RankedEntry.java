package io.nightbeam.donutleaderboard.model;

import java.util.UUID;

public record RankedEntry(int rank, UUID uuid, String name, double value) {
}
