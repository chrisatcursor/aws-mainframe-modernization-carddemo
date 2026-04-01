package com.carddemo.batch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Approximates COBOL Z-GET-DB2-FORMAT-TIMESTAMP for TRAN-PROC-TS (26 chars).
 */
public final class Db2FormatTimestamp {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private Db2FormatTimestamp() {
    }

    public static String now() {
        String base = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss"));
        return base + ".000000";
    }

    public static String format(LocalDateTime t) {
        return t.format(FMT);
    }
}
