package com.personblog.common.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter YEAR_MONTH_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String currentYearMonth() {
        return YearMonth.now().format(YEAR_MONTH);
    }

    public static String currentYearMonthDay() {
        return LocalDate.now().format(YEAR_MONTH_DAY);
    }
}
