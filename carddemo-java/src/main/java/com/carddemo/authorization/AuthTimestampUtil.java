package com.carddemo.authorization;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Maps COBOL PA-AUTH-DATE-9C / PA-AUTH-TIME-9C and PA-AUTH-ORIG-DATE to {@link Instant}
 * per COPAUS2C (inverted time: {@code 999999999 - authTime9c}).
 */
public final class AuthTimestampUtil {

    private AuthTimestampUtil() {}

    /**
     * @param authOrigDate YYMMDD (6 chars, from COBOL display)
     * @param authTime9c packed inverted time from IMS detail segment
     */
    public static Instant toInstant(String authOrigDate, int authTime9c) {
        if (authOrigDate == null || authOrigDate.length() < 6) {
            return Instant.EPOCH;
        }
        int yy = Integer.parseInt(authOrigDate.substring(0, 2));
        int mm = Integer.parseInt(authOrigDate.substring(2, 4));
        int dd = Integer.parseInt(authOrigDate.substring(4, 6));
        int year = yy >= 70 ? 1900 + yy : 2000 + yy;
        LocalDate date = LocalDate.of(year, mm, dd);

        int inv = 999_999_999 - authTime9c;
        String digits = String.format("%09d", inv);
        int hh = Integer.parseInt(digits.substring(0, 2));
        int mi = Integer.parseInt(digits.substring(2, 4));
        int ss = Integer.parseInt(digits.substring(4, 6));
        int nano = Integer.parseInt(digits.substring(6, 9)) * 1_000_000;
        LocalTime time = LocalTime.of(hh, mi, ss, nano);

        return date.atTime(time).toInstant(ZoneOffset.UTC);
    }
}
