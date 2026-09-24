package io.nightbeam.donutleaderboard.period;

import io.nightbeam.donutleaderboard.model.PeriodType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class PeriodBounds {

    private final ZoneId zoneId;

    public PeriodBounds(ZoneId zoneId) {
        this.zoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;
    }

    public Instant periodStart(PeriodType periodType, Instant now) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(now, zoneId);
        return switch (periodType) {
            case DAILY -> zdt.truncatedTo(ChronoUnit.DAYS).toInstant();
            case WEEKLY -> zdt.with(java.time.DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS).toInstant();
            case MONTHLY -> zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
            case ALL_TIME -> Instant.EPOCH;
        };
    }

    public Instant nextReset(PeriodType periodType, Instant now) {
        if (periodType == PeriodType.ALL_TIME) {
            return Instant.MAX;
        }
        ZonedDateTime zdt = ZonedDateTime.ofInstant(now, zoneId);
        return switch (periodType) {
            case DAILY -> zdt.truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant();
            case WEEKLY -> zdt.with(java.time.DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS).plusWeeks(1).toInstant();
            case MONTHLY -> zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).plusMonths(1).toInstant();
            case ALL_TIME -> Instant.MAX;
        };
    }
}
