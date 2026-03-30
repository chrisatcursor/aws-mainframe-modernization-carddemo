package com.carddemo.batch;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * One logical record from the daily transaction sequential file (DALYTRAN / CVTRA06Y, RECLN 350).
 * Used by CBTRN01C validation flow.
 */
public record DailyTransaction(
        String transactionId,
        String typeCode,
        int categoryCode,
        String source,
        String description,
        BigDecimal amount,
        Long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String cardNumber,
        String originTimestamp,
        String processedTimestamp,
        String rawLine) {

    public static final int RECORD_LENGTH = 350;

    private static final int IDX_ID = 0;
    private static final int LEN_ID = 16;
    private static final int IDX_TYPE = 16;
    private static final int LEN_TYPE = 2;
    private static final int IDX_CAT = 18;
    private static final int LEN_CAT = 4;
    private static final int IDX_SOURCE = 22;
    private static final int LEN_SOURCE = 10;
    private static final int IDX_DESC = 32;
    private static final int LEN_DESC = 100;
    private static final int IDX_AMT = 132;
    private static final int LEN_AMT = 11;
    private static final int IDX_MERCHANT_ID = 143;
    private static final int LEN_MERCHANT_ID = 9;
    private static final int IDX_MERCHANT_NAME = 152;
    private static final int LEN_MERCHANT_NAME = 50;
    private static final int IDX_MERCHANT_CITY = 202;
    private static final int LEN_MERCHANT_CITY = 50;
    private static final int IDX_MERCHANT_ZIP = 252;
    private static final int LEN_MERCHANT_ZIP = 10;
    private static final int IDX_CARD = 262;
    private static final int LEN_CARD = 16;
    private static final int IDX_ORIG_TS = 278;
    private static final int LEN_ORIG_TS = 26;
    private static final int IDX_PROC_TS = 304;
    private static final int LEN_PROC_TS = 26;

    /**
     * Parses a single physical line into {@link DailyTransaction}, padding with spaces to
     * {@link #RECORD_LENGTH} when shorter (COBOL blank-fill behavior).
     */
    public static DailyTransaction parse(String line) {
        String normalized = padToRecordLength(line == null ? "" : line);
        return new DailyTransaction(
                sliceTrim(normalized, IDX_ID, LEN_ID),
                sliceTrim(normalized, IDX_TYPE, LEN_TYPE),
                parseCategory(sliceRaw(normalized, IDX_CAT, LEN_CAT)),
                sliceTrim(normalized, IDX_SOURCE, LEN_SOURCE),
                sliceTrim(normalized, IDX_DESC, LEN_DESC),
                parseAmount(sliceRaw(normalized, IDX_AMT, LEN_AMT)),
                parseLongDigits(sliceRaw(normalized, IDX_MERCHANT_ID, LEN_MERCHANT_ID)),
                sliceTrim(normalized, IDX_MERCHANT_NAME, LEN_MERCHANT_NAME),
                sliceTrim(normalized, IDX_MERCHANT_CITY, LEN_MERCHANT_CITY),
                sliceTrim(normalized, IDX_MERCHANT_ZIP, LEN_MERCHANT_ZIP),
                sliceTrim(normalized, IDX_CARD, LEN_CARD),
                sliceTrim(normalized, IDX_ORIG_TS, LEN_ORIG_TS),
                sliceTrim(normalized, IDX_PROC_TS, LEN_PROC_TS),
                normalized);
    }

    public static String padToRecordLength(String line) {
        byte[] raw = line.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= RECORD_LENGTH) {
            return new String(raw, 0, RECORD_LENGTH, StandardCharsets.UTF_8);
        }
        char[] padded = new char[RECORD_LENGTH];
        Arrays.fill(padded, ' ');
        String s = new String(raw, StandardCharsets.UTF_8);
        int copy = Math.min(s.length(), RECORD_LENGTH);
        s.getChars(0, copy, padded, 0);
        return new String(padded);
    }

    private static String sliceRaw(String line, int start, int length) {
        int end = Math.min(line.length(), start + length);
        if (start >= line.length()) {
            return "";
        }
        return line.substring(start, end);
    }

    private static String sliceTrim(String line, int start, int length) {
        return sliceRaw(line, start, length).trim();
    }

    private static int parseCategory(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal parseAmount(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty() || !t.chars().allMatch(c -> Character.isDigit(c) || c == '+' || c == '-')) {
            return null;
        }
        try {
            boolean neg = t.startsWith("-");
            String digits = t.replace("+", "").replace("-", "");
            if (digits.isEmpty()) {
                return null;
            }
            BigDecimal v = new BigDecimal(digits).movePointLeft(2);
            return neg ? v.negate() : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLongDigits(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
