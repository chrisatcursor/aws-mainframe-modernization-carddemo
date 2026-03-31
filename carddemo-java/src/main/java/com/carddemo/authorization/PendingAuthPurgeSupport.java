package com.carddemo.authorization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * CBPAUP0C expiry math: {@code dayDiff = currentYyddd - (99999 - authDate9c)}.
 */
public final class PendingAuthPurgeSupport {

    private PendingAuthPurgeSupport() {}

    public static int currentYyddd(LocalDate date) {
        int yy = date.getYear() % 100;
        int ddd = date.getDayOfYear();
        return yy * 1000 + ddd;
    }

    public static int dayDiff(int authDate9c, LocalDate today) {
        int invertedJulian = 99999 - authDate9c;
        return currentYyddd(today) - invertedJulian;
    }

    public static boolean isExpired(int authDate9c, LocalDate today, int expiryDays) {
        return dayDiff(authDate9c, today) >= expiryDays;
    }

    /** Adjust summary tallies when deleting a detail (mirrors CBPAUP0C 4000). */
    public static void adjustSummaryForDeletedDetail(PendingAuthSummary s, PendingAuthDetail d) {
        if ("00".equals(trim(d.getAuthRespCode()))) {
            s.setApprovedAuthCnt(Math.max(0, s.getApprovedAuthCnt() - 1));
            BigDecimal amt = d.getApprovedAmt() != null ? d.getApprovedAmt() : BigDecimal.ZERO;
            s.setApprovedAuthAmt(s.getApprovedAuthAmt().subtract(amt).max(BigDecimal.ZERO));
        } else {
            s.setDeclinedAuthCnt(Math.max(0, s.getDeclinedAuthCnt() - 1));
            BigDecimal amt = d.getTransactionAmt() != null ? d.getTransactionAmt() : BigDecimal.ZERO;
            s.setDeclinedAuthAmt(s.getDeclinedAuthAmt().subtract(amt).max(BigDecimal.ZERO));
        }
    }

    public static boolean shouldDeleteSummary(PendingAuthSummary s) {
        return s.getApprovedAuthCnt() <= 0 && s.getDeclinedAuthCnt() <= 0;
    }

    private static String trim(String x) {
        return x == null ? "" : x.trim();
    }

    public static BigDecimal parseAuthRequestAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String t = raw.trim().replace("+", "").replace(",", "");
        try {
            return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
