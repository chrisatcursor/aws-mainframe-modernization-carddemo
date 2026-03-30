package com.carddemo.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date/time formatting utility replacing CSDAT01Y working-storage fields
 * and the CSUTLDTC date-validation subroutine.
 * <p>
 * COBOL programs store dates as formatted strings (PIC X(10), PIC 9(8), etc.).
 * This helper converts between {@code java.time} types and those legacy formats.
 */
public final class DateTimeHelper {

    private DateTimeHelper() {}

    private static final DateTimeFormatter COBOL_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    /** Format as {@code yyyyMMdd} (matches WS-CURDATE-N). */
    public static String toCobolDate(LocalDate date) {
        return date.format(COBOL_DATE);
    }

    /** Format as {@code MM/dd/yy} (matches WS-CURDATE-MM-DD-YY). */
    public static String toDisplayDate(LocalDate date) {
        return date.format(DISPLAY_DATE);
    }

    /** Format as {@code yyyy-MM-dd HH:mm:ss.SSSSSS} (matches WS-TIMESTAMP). */
    public static String toTimestamp(LocalDateTime dateTime) {
        return dateTime.format(TIMESTAMP);
    }

    /** Parse a {@code yyyyMMdd} COBOL date string. Returns null if invalid. */
    public static LocalDate parseCobolDate(String cobolDate) {
        if (cobolDate == null || cobolDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(cobolDate.trim(), COBOL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /** Validate that a string is a parseable {@code yyyyMMdd} date. */
    public static boolean isValidCobolDate(String cobolDate) {
        return parseCobolDate(cobolDate) != null;
    }
}
