package org.teatrove.teaadmin;

public class Constants {
    public static final Long SECOND = Long.valueOf(1000L);
    public static final Long MINUTE = Long.valueOf(60L * SECOND.longValue());
    public static final Long HOUR = Long.valueOf(60L * MINUTE.longValue());
    public static final Long DAY = Long.valueOf(24L * HOUR.longValue());
    public static final Long WEEK = Long.valueOf(7L * DAY.longValue());
    public static final Long MONTH = Long.valueOf(30L * DAY.longValue());
    public static final Long YEAR = Long.valueOf(365L * DAY.longValue());
}
