package io.nightbeam.donutleaderboard.model;

public enum PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME;

    public static PeriodType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL_TIME;
        }
        return PeriodType.valueOf(raw.trim().toUpperCase());
    }
}
