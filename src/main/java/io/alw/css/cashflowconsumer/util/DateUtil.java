package io.alw.css.cashflowconsumer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    public static LocalDate formatValueDate(String dateString) {
        return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
    }
}
