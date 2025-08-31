package yagu.yagu.game.cache;

import java.time.YearMonth;

public final class CalendarCacheKeys {
    private CalendarCacheKeys() {}
    public static final String YEARS_SET = "games:calendar:years";
    public static String monthKey(YearMonth ym) { return String.format("games:calendar:%04d-%02d", ym.getYear(), ym.getMonthValue()); }
    public static String yearIndexKey(int year) { return String.format("games:calendar:index:%04d", year); }
}
