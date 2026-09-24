package io.nightbeam.donutleaderboard.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardGuiPaginationTest {

    @Test
    void slotConstantsMatchDonutLayout() {
        assertEquals(45, LeaderboardGui.PREVIOUS_SLOT);
        assertEquals(53, LeaderboardGui.NEXT_SLOT);
    }

    @Test
    void pageAfterClickNavigates() {
        assertEquals(1, LeaderboardGui.pageAfterClick(0, 3, LeaderboardGui.NEXT_SLOT));
        assertEquals(0, LeaderboardGui.pageAfterClick(0, 3, LeaderboardGui.PREVIOUS_SLOT));
    }
}
