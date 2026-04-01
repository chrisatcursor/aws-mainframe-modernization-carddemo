package com.carddemo.batch;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Parses a 350-byte logical DALYTRAN record (CVTRA06Y field order) as ASCII for tests and batch input.
 */
public final class DalyTranRecordParser {

    private DalyTranRecordParser() {
    }

    public static ParsedDalyTran parse(String line) {
        byte[] raw = line.getBytes(StandardCharsets.US_ASCII);
        if (raw.length < 350) {
            byte[] padded = new byte[350];
            System.arraycopy(raw, 0, padded, 0, raw.length);
            raw = padded;
        }
        String s = new String(raw, 0, 350, StandardCharsets.US_ASCII);
        int p = 0;
        String id = trim(s.substring(p, p + 16));
        p += 16;
        String typeCd = trim(s.substring(p, p + 2));
        p += 2;
        int catCd = Integer.parseInt(trim(s.substring(p, p + 4)));
        p += 4;
        String source = trim(s.substring(p, p + 10));
        p += 10;
        String desc = trim(s.substring(p, p + 100));
        p += 100;
        String amtStr = trim(s.substring(p, p + 11));
        p += 11;
        BigDecimal amount = new BigDecimal(amtStr);
        long merchantId = Long.parseLong(trim(s.substring(p, p + 9)));
        p += 9;
        String merchantName = trim(s.substring(p, p + 50));
        p += 50;
        String merchantCity = trim(s.substring(p, p + 50));
        p += 50;
        String merchantZip = trim(s.substring(p, p + 10));
        p += 10;
        String cardNum = trim(s.substring(p, p + 16));
        p += 16;
        String origTs = trim(s.substring(p, p + 26));
        p += 26;
        String procTs = trim(s.substring(p, p + 26));
        return new ParsedDalyTran(id, typeCd, catCd, source, desc, amount, merchantId, merchantName,
                merchantCity, merchantZip, cardNum, origTs, procTs);
    }

    private static String trim(String x) {
        return x == null ? "" : x.trim();
    }

    public record ParsedDalyTran(
            String id,
            String typeCode,
            int categoryCode,
            String source,
            String description,
            BigDecimal amount,
            long merchantId,
            String merchantName,
            String merchantCity,
            String merchantZip,
            String cardNumber,
            String origTimestamp,
            String procTimestamp) {
    }
}
