package com.carddemo.common;

/**
 * COBOL-style fixed-width string operations (CSSTRPFY equivalent).
 * <p>
 * COBOL PIC X(n) fields are always exactly n characters, right-padded with spaces.
 * These utilities replicate that behavior for interoperability with legacy data formats
 * and fixed-width file I/O.
 */
public final class StringPaddingUtil {

    private StringPaddingUtil() {}

    /** Right-pad (or truncate) a string to exactly {@code length} characters. */
    public static String rightPad(String value, int length) {
        if (value == null) {
            return " ".repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    /** Left-pad (or truncate from the left) a string to exactly {@code length} characters. */
    public static String leftPad(String value, int length) {
        if (value == null) {
            return " ".repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(value.length() - length);
        }
        return " ".repeat(length - value.length()) + value;
    }

    /** Left-pad a numeric value with zeros to {@code length} digits. */
    public static String zeroPad(long value, int length) {
        String str = Long.toString(value);
        if (str.length() >= length) {
            return str.substring(str.length() - length);
        }
        return "0".repeat(length - str.length()) + str;
    }

    /** Trim trailing spaces (common when reading COBOL-formatted data). */
    public static String trimTrailing(String value) {
        if (value == null) {
            return null;
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }
}
