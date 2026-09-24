package io.nightbeam.donutleaderboard.gui;

import io.nightbeam.donutleaderboard.model.PeriodType;

public final class PeriodToggle {

    private static final PeriodType[] CYCLE = {
        PeriodType.DAILY, PeriodType.WEEKLY, PeriodType.MONTHLY, PeriodType.ALL_TIME
    };

    private PeriodToggle() {
    }

    public static PeriodType next(PeriodType current) {
        if (current == null) {
            return PeriodType.ALL_TIME;
        }
        for (int i = 0; i < CYCLE.length; i++) {
            if (CYCLE[i] == current) {
                return CYCLE[(i + 1) % CYCLE.length];
            }
        }
        return PeriodType.ALL_TIME;
    }

    public static String displayLabel(PeriodType period) {
        return switch (period) {
            case DAILY -> "Daily";
            case WEEKLY -> "Weekly";
            case MONTHLY -> "Monthly";
            case ALL_TIME -> "All-Time";
        };
    }
}
