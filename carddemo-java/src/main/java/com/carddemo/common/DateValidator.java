package com.carddemo.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates date strings in the spirit of IBM LE {@code CEEDAYS} (see {@code CSUTLDTC.cbl}):
 * COBOL callers pass PIC {@code X(10)} values such as {@code YYYY-MM-DD} or {@code YYYYMMDD};
 * severity {@code 0} matches LE severity {@code 0000} / return-code {@code 0}, {@code 1} matches invalid input.
 * <p>
 * CEEDAYS feedback tokens (hex) are documented on {@link DateValidationCode}; messages align with CSUTLDTC's
 * {@code WS-RESULT} literals.
 */
public final class DateValidator {

    /** First day of the IBM Lilian calendar (Lilian day 1). */
    public static final LocalDate LILIAN_EPOCH = LocalDate.of(1582, 10, 15);

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

        /**
         * IBM Language Environment condition tokens referenced by {@code CSUTLDTC.cbl} 88-levels (hex, 8 bytes).
         * A zero token indicates normal completion (valid date).
         */
        public String ceedaysFeedbackHex() {
            return switch (this) {
                case VALID -> "0000000000000000";
                case INSUFFICIENT_DATA -> "000309CB59C3C5C5";
                case BAD_DATE_VALUE -> "000309CC59C3C5C5";
                case INVALID_ERA -> "000309CD59C3C5C5";
                case UNSUPPORTED_RANGE -> "000309D159C3C5C5";
                case INVALID_MONTH -> "000309D559C3C5C5";
                case BAD_FORMAT_STRING -> "000309D659C3C5C5";
                case NON_NUMERIC_DATA -> "000309D859C3C5C5";
                case YEAR_IN_ERA_ZERO -> "000309D959C3C5C5";
                case INVALID -> "";
            };
        }
    }

    public record ValidationResult(int severity, String message, String dateValue, String formatUsed) {}

    private static final Pattern CEEDAYS_YYYY = Pattern.compile("(?i)YYYY");
    private static final Pattern CEEDAYS_YY = Pattern.compile("(?i)YY");
    private static final Pattern CEEDAYS_DD = Pattern.compile("(?i)DD");

    private DateValidator() {}

    /**
     * IBM Lilian day number: 1 = {@link #LILIAN_EPOCH}. Matches {@code OUTPUT-LILLIAN} from {@code CEEDAYS}.
     */
    public static long toLilianDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date");
        }
        return ChronoUnit.DAYS.between(LILIAN_EPOCH, date) + 1;
    }

    /** Inverse of {@link #toLilianDay(LocalDate)} for Lilian values {@code >= 1}. */
    public static LocalDate fromLilianDay(long lilianDay) {
        if (lilianDay < 1) {
            throw new IllegalArgumentException("lilianDay must be >= 1");
        }
        return LILIAN_EPOCH.plusDays(lilianDay - 1);
    }

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

        String javaPattern = mapCeedaysPictureToJavaPattern(format.trim());

        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(javaPattern, Locale.US).withResolverStyle(ResolverStyle.STRICT);
        } catch (IllegalArgumentException e) {
            return new ValidationResult(
                    1, DateValidationCode.BAD_FORMAT_STRING.getMessage(), date.trim(), format);
        }

        String trimmedDate = date.trim();
        int minLen = minimumCharsForPattern(javaPattern);
        if (trimmedDate.length() < minLen) {
            return new ValidationResult(
                    1, DateValidationCode.INSUFFICIENT_DATA.getMessage(), trimmedDate, format);
        }

        final LocalDate parsed;
        try {
            parsed = LocalDate.parse(trimmedDate, formatter);
        } catch (DateTimeParseException e) {
            DateValidationCode code = mapParseException(e, trimmedDate);
            return new ValidationResult(1, code.getMessage(), trimmedDate, format);
        }

        if (parsed.getYear() == 0) {
            return new ValidationResult(1, DateValidationCode.YEAR_IN_ERA_ZERO.getMessage(), trimmedDate, format);
        }
        if (parsed.isBefore(LILIAN_EPOCH)) {
            return new ValidationResult(1, DateValidationCode.INVALID_ERA.getMessage(), trimmedDate, format);
        }

        return new ValidationResult(0, DateValidationCode.VALID.getMessage(), trimmedDate, format);
    }

    /**
     * Maps COBOL / CEEDAYS picture strings to {@link DateTimeFormatter} patterns.
     * <ul>
     *   <li>{@code YYYY} / {@code yy} style year → {@code uuuu} / {@code uu} (STRICT, proleptic)</li>
     *   <li>{@code DD} (CEEDAYS day-of-month) → {@code dd} (Java {@code DD} is day-of-year)</li>
     * </ul>
     */
    static String mapCeedaysPictureToJavaPattern(String picture) {
        String p = picture.trim();
        p = replaceOutsideQuotes(p, CEEDAYS_YYYY, "uuuu");
        p = replaceOutsideQuotes(p, CEEDAYS_YY, "uu");
        p = replaceOutsideQuotes(p, CEEDAYS_DD, "dd");
        return normalizePatternForStrictYear(p);
    }

    /** Replace {@code token} with {@code replacement} only outside single-quoted literals. */
    private static String replaceOutsideQuotes(String input, Pattern token, String replacement) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean inQuote = false;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\'') {
                if (inQuote && i + 1 < input.length() && input.charAt(i + 1) == '\'') {
                    out.append("''");
                    i += 2;
                    continue;
                }
                inQuote = !inQuote;
                out.append(c);
                i++;
                continue;
            }
            if (inQuote) {
                out.append(c);
                i++;
                continue;
            }
            Matcher m = token.matcher(input);
            m.region(i, input.length());
            if (m.lookingAt()) {
                out.append(replacement);
                i = m.end();
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Minimum character count the input must provide for the pattern (excludes trailing padding). */
    static int minimumCharsForPattern(String javaPattern) {
        int n = 0;
        int i = 0;
        boolean inQuote = false;
        while (i < javaPattern.length()) {
            char c = javaPattern.charAt(i);
            if (c == '\'') {
                if (inQuote && i + 1 < javaPattern.length() && javaPattern.charAt(i + 1) == '\'') {
                    n++;
                    i += 2;
                    continue;
                }
                inQuote = !inQuote;
                i++;
                continue;
            }
            if (inQuote) {
                n++;
                i++;
                continue;
            }
            if (Character.isLetter(c)) {
                int j = i + 1;
                while (j < javaPattern.length() && javaPattern.charAt(j) == c) {
                    j++;
                }
                int run = j - i;
                char cl = Character.toLowerCase(c);
                int fieldMin =
                        switch (cl) {
                            case 'u', 'y' -> run <= 2 ? 2 : 4;
                            case 'm' -> run >= 3 ? 3 : 2;
                            case 'd' -> run >= 3 ? 3 : 2;
                            case 'g' -> 2;
                            default -> run;
                        };
                n += fieldMin;
                i = j;
            } else {
                n++;
                i++;
            }
        }
        return n;
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
