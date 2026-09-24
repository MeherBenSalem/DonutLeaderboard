package io.nightbeam.donutleaderboard.ranking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderCategoryParser {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%]+)%");

    private PlaceholderCategoryParser() {
    }

    public static ParsedPlaceholder parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Placeholder cannot be empty");
        }
        String trimmed = raw.trim();
        Matcher matcher = PLACEHOLDER.matcher(trimmed);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Expected PlaceholderAPI token like %statistic_mine_block%");
        }
        String token = matcher.group(1);
        return new ParsedPlaceholder(trimmed, token);
    }

    public record ParsedPlaceholder(String expression, String token) {
    }
}
