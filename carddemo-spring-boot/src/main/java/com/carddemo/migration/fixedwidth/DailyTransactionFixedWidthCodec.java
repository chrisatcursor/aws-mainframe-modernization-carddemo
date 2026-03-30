package com.carddemo.migration.fixedwidth;

import com.carddemo.transaction.model.DailyTransactionRecord;

public class DailyTransactionFixedWidthCodec {

    private DailyTransactionFixedWidthCodec() {
    }

    public static DailyTransactionRecord decode(String line) {
        String normalized = FixedWidthSupport.ensureLength(line, 350);
        DailyTransactionRecord record = new DailyTransactionRecord();
        record.setTransactionId(FixedWidthSupport.slice(normalized, 0, 16).trim());
        record.setTransactionTypeCode(FixedWidthSupport.slice(normalized, 16, 2).trim());
        record.setTransactionCategoryCode(FieldParsers.parseInt(FixedWidthSupport.slice(normalized, 18, 4)));
        record.setSource(FixedWidthSupport.slice(normalized, 22, 10).trim());
        record.setDescription(FixedWidthSupport.slice(normalized, 32, 100).trim());
        record.setAmount(FieldParsers.parseSignedDecimal(FixedWidthSupport.slice(normalized, 132, 11), 2));
        record.setMerchantId(FieldParsers.parseLong(FixedWidthSupport.slice(normalized, 143, 9)));
        record.setMerchantName(FixedWidthSupport.slice(normalized, 152, 50).trim());
        record.setMerchantCity(FixedWidthSupport.slice(normalized, 202, 50).trim());
        record.setMerchantZip(FixedWidthSupport.slice(normalized, 252, 10).trim());
        record.setCardNumber(FixedWidthSupport.slice(normalized, 262, 16).trim());
        record.setOriginalTimestamp(FieldParsers.parseDateTime26(FixedWidthSupport.slice(normalized, 278, 26)));
        record.setProcessedTimestamp(FieldParsers.parseDateTime26(FixedWidthSupport.slice(normalized, 304, 26)));
        return record;
    }
}
