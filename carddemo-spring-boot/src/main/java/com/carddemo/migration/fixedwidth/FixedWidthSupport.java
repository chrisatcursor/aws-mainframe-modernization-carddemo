package com.carddemo.migration.fixedwidth;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FixedWidthSupport {

    private FixedWidthSupport() {
    }

    public static String normalize(String line, int requiredLength) {
        String value = line == null ? "" : line;
        if (value.length() == requiredLength) {
            return value;
        }
        if (value.length() > requiredLength) {
            return value.substring(0, requiredLength);
        }
        return value + " ".repeat(requiredLength - value.length());
    }

    public static String requireLength(String line, int requiredLength) {
        return normalize(line, requiredLength);
    }

    public static String requireLength(String line, int requiredLength, String ignoredRecordType) {
        return normalize(line, requiredLength);
    }

    public static String ensureLength(String line, int requiredLength) {
        return normalize(line, requiredLength);
    }

    public static String padRight(String value, int size) {
        String source = value == null ? "" : value;
        if (source.length() >= size) {
            return source.substring(0, size);
        }
        return source + " ".repeat(size - source.length());
    }

    public static String rightPad(String value, int size) {
        return padRight(value, size);
    }

    public static String leftPadZero(String value, int size) {
        String source = value == null ? "" : value.trim();
        if (source.length() >= size) {
            return source.substring(source.length() - size);
        }
        return "0".repeat(size - source.length()) + source;
    }

    public static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(c).repeat(count);
    }

    public static String slice(String line, int start, int length) {
        String normalized = normalize(line, Math.max(start + length, 0));
        return normalized.substring(start, Math.min(start + length, normalized.length()));
    }

    public static String formatString(String value, int size) {
        return rightPad(value, size);
    }

    public static String formatLong(Long value, int size) {
        if (value == null) {
            return "0".repeat(size);
        }
        return leftPadZero(Long.toString(value), size);
    }

    public static String formatSignedDecimal(BigDecimal value, int size, int scale) {
        BigDecimal normalized = (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.HALF_UP);
        boolean negative = normalized.signum() < 0;
        String digits = normalized.movePointRight(scale).abs().toBigInteger().toString();
        String body = leftPadZero(digits, size);
        char last = body.charAt(body.length() - 1);
        body = body.substring(0, body.length() - 1) + encodeOverpunch(last, negative);
        return body;
    }

    private static char encodeOverpunch(char lastDigit, boolean negative) {
        return switch (lastDigit) {
            case '0' -> negative ? '}' : '{';
            case '1' -> negative ? 'J' : 'A';
            case '2' -> negative ? 'K' : 'B';
            case '3' -> negative ? 'L' : 'C';
            case '4' -> negative ? 'M' : 'D';
            case '5' -> negative ? 'N' : 'E';
            case '6' -> negative ? 'O' : 'F';
            case '7' -> negative ? 'P' : 'G';
            case '8' -> negative ? 'Q' : 'H';
            case '9' -> negative ? 'R' : 'I';
            default -> lastDigit;
        };
    }
}
