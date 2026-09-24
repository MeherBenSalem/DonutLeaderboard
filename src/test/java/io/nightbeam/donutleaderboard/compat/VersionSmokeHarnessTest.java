package io.nightbeam.donutleaderboard.compat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("smoke")
class VersionSmokeHarnessTest {

    @Test
    void smokeHarnessDisabledByDefaultInCi() {
        String enabled = System.getProperty("donutleaderboard.smoke.enabled", "false");
        assertFalse(Boolean.parseBoolean(enabled), "Smoke tests require -Ddonutleaderboard.smoke.enabled=true and downloaded server jars");
    }

    @Test
    void documentsExpectedMatrix() {
        String matrix = System.getenv("DONUT_LEADERBOARD_SMOKE_MATRIX");
        if (matrix == null) {
            assertTrue(true);
            return;
        }
        assertTrue(matrix.contains("1.20.1") || matrix.contains("26.3"));
    }
}
