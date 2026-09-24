package io.nightbeam.donutleaderboard.model;

import java.util.Locale;
import java.util.Objects;

public record CategoryKey(String id, PeriodType period) {

    public CategoryKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(period, "period");
        id = id.toLowerCase(Locale.ROOT);
    }

    public String cacheKey() {
        return id + ":" + period.name();
    }
}
