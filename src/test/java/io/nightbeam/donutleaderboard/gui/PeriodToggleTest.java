package io.nightbeam.donutleaderboard.gui;

import io.nightbeam.donutleaderboard.model.PeriodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeriodToggleTest {

    @Test
    void cyclesThroughPeriods() {
        assertEquals(PeriodType.WEEKLY, PeriodToggle.next(PeriodType.DAILY));
        assertEquals(PeriodType.MONTHLY, PeriodToggle.next(PeriodType.WEEKLY));
        assertEquals(PeriodType.ALL_TIME, PeriodToggle.next(PeriodType.MONTHLY));
        assertEquals(PeriodType.DAILY, PeriodToggle.next(PeriodType.ALL_TIME));
    }
}
