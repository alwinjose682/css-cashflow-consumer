package io.alw.css.cashflowconsumer.util;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class DateUtil {
    public static LocalTime toLocalTime(Time time) {
        return time.toLocalTime();
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }
}
