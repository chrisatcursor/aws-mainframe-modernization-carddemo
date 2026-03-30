package com.carddemo.migration.fixedwidth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class FieldParsers {

    private static final Map<Character, Character> POSITIVE_OVERPUNCH = Map.ofEntries(
            Map.entry('{', '0'),
            Map.entry('A', '1'),
            Map.entry('B', '2'),
            Map.entry('C', '3'),
            Map.entry('D', '4'),
            Map.entry('E', '5'),
            Map.entry('F', '6'),
            Map.entry('G', '7'),
            Map.entry('H', '8'),
            Map.entry('I', '9')
    );

    private static final Map<Character, Character> NEGATIVE_OVERPUNCH = Map.ofEntries(
            Map.entry('}', '0'),
            Map.entry('J', '1'),
            Map.entry('K', '2'),
            Map.entry('L', '3'),
            Map.entry('M', '4'),
            Map.entry('N', '5'),
            Map.entry('O', '6'),
            Map.entry('P', '7'),
            Map.entry('Q', '8'),
            Map.entry('R', '9')
    );

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private FieldParsers() {
    }

    public static String text(String line, int fromInclusive, int toExclusive) {
        return line.substring(fromInclusive, toExclusive).trim();
    }

    public static String string(String line, int fromInclusive, int toExclusive) {
        return text(line, fromInclusive, toExclusive);
    }

    public static Long numericLong(String line, int fromInclusive, int toExclusive) {
        String value = text(line, fromInclusive, toExclusive);
        if (value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    public static Long parseLong(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    public static Long parseLong(String line, int fromInclusive, int toExclusive) {
        return numericLong(line, fromInclusive, toExclusive);
    }

    public static Long longNumber(String line, int fromInclusive, int length) {
        return numericLong(line, fromInclusive, fromInclusive + length);
    }

    public static Integer numericInt(String line, int fromInclusive, int toExclusive) {
        String value = text(line, fromInclusive, toExclusive);
        if (value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    public static Integer parseInt(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    public static Integer parseInt(String line, int fromInclusive, int toExclusive) {
        return numericInt(line, fromInclusive, toExclusive);
    }

    public static Integer parseInteger(String value) {
        return parseInt(value);
    }

    public static Integer parseInteger(String line, int fromInclusive, int toExclusive) {
        return numericInt(line, fromInclusive, toExclusive);
    }

    public static Integer integer(String line, int fromInclusive, int toExclusive) {
        return numericInt(line, fromInclusive, toExclusive);
    }

    public static String parseString(String line, int fromInclusive, int length) {
        return text(line, fromInclusive, fromInclusive + length);
    }

    public static BigDecimal signedDecimalOverpunch(String line, int fromInclusive, int toExclusive, int scale) {
        String raw = line.substring(fromInclusive, toExclusive);
        if (raw.isBlank()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        char last = raw.charAt(raw.length() - 1);
        boolean negative = NEGATIVE_OVERPUNCH.containsKey(last);
        char normalizedLast = POSITIVE_OVERPUNCH.getOrDefault(last, NEGATIVE_OVERPUNCH.getOrDefault(last, last));
        String normalized = raw.substring(0, raw.length() - 1) + normalizedLast;
        long abs = Long.parseLong(normalized);
        if (negative) {
            abs = -abs;
        }
        return BigDecimal.valueOf(abs, scale);
    }

    public static BigDecimal signed(String line, int fromInclusive, int toExclusive, int scale) {
        return signedDecimalOverpunch(line, fromInclusive, toExclusive, scale);
    }

    public static BigDecimal parseSignedDecimalWithOverpunch(String line, int fromInclusive, int toExclusive, int scale) {
        return signedDecimalOverpunch(line, fromInclusive, toExclusive, scale);
    }

    public static BigDecimal parseSignedDecimal(String raw, int scale) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        char last = raw.charAt(raw.length() - 1);
        boolean negative = NEGATIVE_OVERPUNCH.containsKey(last);
        char normalizedLast = POSITIVE_OVERPUNCH.getOrDefault(last, NEGATIVE_OVERPUNCH.getOrDefault(last, last));
        String normalized = raw.substring(0, raw.length() - 1) + normalizedLast;
        long abs = Long.parseLong(normalized.trim());
        if (negative) {
            abs = -abs;
        }
        return BigDecimal.valueOf(abs, scale);
    }

    public static BigDecimal parseSignedDecimal(String line, int fromInclusive, int toExclusive, int scale) {
        return parseSignedDecimal(line.substring(fromInclusive, toExclusive), scale);
    }

    public static BigDecimal parseSignedImplied2(String value) {
        return parseSignedDecimal(value, 2);
    }

    public static LocalDate date(String line, int fromInclusive, int toExclusive) {
        String value = text(line, fromInclusive, toExclusive);
        if (value.isBlank() || value.equals("0000-00-00")) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date value: " + value, ex);
        }
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date value: " + value, ex);
        }
    }

    public static LocalDate parseDate(String line, int fromInclusive, int toExclusive) {
        return date(line, fromInclusive, toExclusive);
    }

    public static LocalDateTime timestamp(String line, int fromInclusive, int toExclusive) {
        String value = text(line, fromInclusive, toExclusive);
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid timestamp value: " + value, ex);
        }
    }

    public static LocalDateTime parseDateTime26(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid timestamp value: " + value, ex);
        }
    }

    public static String formatLong(Long value) {
        return value == null ? "0" : Long.toString(value);
    }

    public static String formatInt(Integer value) {
        return value == null ? "0" : Integer.toString(value);
    }

    public static String formatDate(LocalDate value) {
        return value == null ? "" : value.format(DATE_FORMAT);
    }
}
