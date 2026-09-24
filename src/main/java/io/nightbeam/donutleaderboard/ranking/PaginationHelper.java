package io.nightbeam.donutleaderboard.ranking;

public final class PaginationHelper {

    private PaginationHelper() {
    }

    public static int clampPage(int requestedPage, int totalPages) {
        int pages = Math.max(1, totalPages);
        if (requestedPage < 0) {
            return 0;
        }
        return Math.min(requestedPage, pages - 1);
    }

    public static int pageAfterNavigation(int currentPage, int totalPages, NavigationAction action) {
        return switch (action) {
            case PREVIOUS -> clampPage(currentPage - 1, totalPages);
            case NEXT -> clampPage(currentPage + 1, totalPages);
            case NONE -> clampPage(currentPage, totalPages);
        };
    }

    public enum NavigationAction {
        PREVIOUS,
        NEXT,
        NONE
    }
}
