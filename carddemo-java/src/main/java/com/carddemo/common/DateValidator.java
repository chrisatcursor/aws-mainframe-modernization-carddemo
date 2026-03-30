package com.carddemo.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class DateValidator {

    public enum DateValidationCode {
        VALID("Date is valid"),
        INSUFFICIENT_DATA("Insufficient"),
        BAD_DATE_VALUE("Datevalue error"),
        INVALID_ERA("Invalid Era"),
        UNSUPPORTED_RANGE("Unsupp. Range"),
        INVALID_MONTH("Invalid month"),
        BAD_FORMAT_STRING("Bad Pic String"),
        NON_NUMERIC_DATA("Nonnumeric data"),
        YEAR_IN_ERA_ZERO("YearInEra is 0"),
        INVALID("Date is invalid");

        private final String message;

        DateValidationCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public record ValidationResult(int severity, String message, String dateValue, String formatUsed) {}

    private DateValidator() {}

    public static ValidationResult validate(String date, String format) {
        String rawDate = date == null ? "" : date;
        String rawFormat = format == null ? "" : format;

        if (date == null || date.isBlank()) {
            return new ValidationResult(
                    1, DateValidationCode.INSUFFICIENT_DATA.getMessage(), rawDate, rawFormat);
        }

        if (format == null || format.isBlank()) {
            return new ValidationResult(
                    1, DateValidationCode.BAD_FORMAT_STRING.getMessage(), date.trim(), rawFormat);
        }

        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(normalizePatternForStrictYear(format), Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);
        } catch (IllegalArgumentException e) {
            return new ValidationResult(
                    1, DateValidationCode.BAD_FORMAT_STRING.getMessage(), date.trim(), format);
        }

        String trimmedDate = date.trim();
        try {
            LocalDate.parse(trimmedDate, formatter);
            return new ValidationResult(0, DateValidationCode.VALID.getMessage(), trimmedDate, format);
        } catch (DateTimeParseException e) {
            DateValidationCode code = mapParseException(e, trimmedDate);
            return new ValidationResult(1, code.getMessage(), trimmedDate, format);
        }
    }

    private static String normalizePatternForStrictYear(String pattern) {
        return pattern.replace("yyyy", "uuuu").replace("yy", "uu");
    }

    private static DateValidationCode mapParseException(DateTimeParseException e, String text) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);

        if (msg.contains("unable to obtain localdate") || msg.contains("invalid date")) {
            return DateValidationCode.BAD_DATE_VALUE;
        }
        if (msg.contains("invalid value for monthofyear") || msg.contains("invalid value for month of year")) {
            return DateValidationCode.INVALID_MONTH;
        }
        if (msg.contains("era")) {
            if (msg.contains("year") && (msg.contains("zero") || msg.contains(" 0"))) {
                return DateValidationCode.YEAR_IN_ERA_ZERO;
            }
            return DateValidationCode.INVALID_ERA;
        }
        if (msg.contains("exceed") || msg.contains("out of range") || msg.contains("unsupported")) {
            return DateValidationCode.UNSUPPORTED_RANGE;
        }
        if (text.chars().anyMatch(Character::isLetter)) {
            return DateValidationCode.NON_NUMERIC_DATA;
        }
        return DateValidationCode.INVALID;
    }
}
