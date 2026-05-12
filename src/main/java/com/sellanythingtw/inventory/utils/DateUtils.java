package com.sellanythingtw.inventory.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FILE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    public static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MMdd");

    public static String nowFileText() {
        return LocalDateTime.now().format(DATETIME_FILE);
    }

    public static String toMmdd(LocalDate date) {
        if (date == null) return "";
        return date.format(MMDD);
    }
}
