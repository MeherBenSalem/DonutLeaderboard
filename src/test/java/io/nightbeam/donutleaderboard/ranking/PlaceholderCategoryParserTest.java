package io.nightbeam.donutleaderboard.ranking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceholderCategoryParserTest {

    @Test
    void parsesPlaceholderToken() {
        PlaceholderCategoryParser.ParsedPlaceholder parsed =
                PlaceholderCategoryParser.parse("%donutcore_shards%");
        assertEquals("%donutcore_shards%", parsed.expression());
        assertEquals("donutcore_shards", parsed.token());
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> PlaceholderCategoryParser.parse("   "));
    }
}
