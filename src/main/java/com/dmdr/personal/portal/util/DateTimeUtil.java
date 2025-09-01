package com.dmdr.personal.portal.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
    private DateTimeUtil() {}
    public static String format(Instant instant) {
        return instant == null ? "" : ISO.format(instant);
    }
}
