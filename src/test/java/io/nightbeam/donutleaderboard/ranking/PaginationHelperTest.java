package io.nightbeam.donutleaderboard.ranking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationHelperTest {

    @Test
    void clampsAndNavigatesPages() {
        assertEquals(0, PaginationHelper.pageAfterNavigation(0, 3, PaginationHelper.NavigationAction.PREVIOUS));
        assertEquals(1, PaginationHelper.pageAfterNavigation(0, 3, PaginationHelper.NavigationAction.NEXT));
        assertEquals(2, PaginationHelper.pageAfterNavigation(2, 3, PaginationHelper.NavigationAction.NEXT));
    }
}
