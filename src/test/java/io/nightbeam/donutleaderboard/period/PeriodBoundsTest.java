package io.nightbeam.donutleaderboard.period;

import io.nightbeam.donutleaderboard.model.PeriodType;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodBoundsTest {

    @Test
    void dailyResetIsAfterStart() {
        PeriodBounds bounds = new PeriodBounds(ZoneId.of("UTC"));
        Instant now = Instant.parse("2026-09-24T15:00:00Z");
        Instant start = bounds.periodStart(PeriodType.DAILY, now);
        Instant next = bounds.nextReset(PeriodType.DAILY, now);
        assertTrue(next.isAfter(start));
        assertTrue(next.isAfter(now));
    }
}
