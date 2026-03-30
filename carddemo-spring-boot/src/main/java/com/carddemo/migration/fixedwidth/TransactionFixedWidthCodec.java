package com.carddemo.migration.fixedwidth;

import com.carddemo.transaction.model.TransactionRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TransactionFixedWidthCodec {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private TransactionFixedWidthCodec() {
    }

    public static TransactionRecord parseTransaction(String line) {
        String normalized = FixedWidthSupport.normalize(line, 350);
        TransactionRecord record = new TransactionRecord();
        record.setTransactionId(FixedWidthSupport.slice(normalized, 0, 16).trim());
        record.setTransactionTypeCode(FixedWidthSupport.slice(normalized, 16, 2).trim());
        record.setTransactionCategoryCode(FieldParsers.numericInt(normalized, 18, 22));
        record.setSource(FixedWidthSupport.slice(normalized, 22, 10).trim());
        record.setDescription(FixedWidthSupport.slice(normalized, 32, 100).trim());
        record.setAmount(FieldParsers.signedDecimalOverpunch(normalized, 132, 143, 2));
        record.setMerchantId(FieldParsers.numericLong(normalized, 143, 152));
        record.setMerchantName(FixedWidthSupport.slice(normalized, 152, 50).trim());
        record.setMerchantCity(FixedWidthSupport.slice(normalized, 202, 50).trim());
        record.setMerchantZip(FixedWidthSupport.slice(normalized, 252, 10).trim());
        record.setCardNumber(FixedWidthSupport.slice(normalized, 262, 16).trim());
        record.setOriginalTimestamp(FieldParsers.timestamp(normalized, 278, 304));
        record.setProcessedTimestamp(FieldParsers.timestamp(normalized, 304, 330));
        return record;
    }

    public static String formatTransaction(TransactionRecord record) {
        return new StringBuilder(350)
                .append(FixedWidthSupport.leftPadZero(record.getTransactionId(), 16))
                .append(FixedWidthSupport.leftPadZero(record.getTransactionTypeCode(), 2))
                .append(FixedWidthSupport.leftPadZero(
                        record.getTransactionCategoryCode() == null ? null : record.getTransactionCategoryCode().toString(), 4))
                .append(FixedWidthSupport.rightPad(record.getSource(), 10))
                .append(FixedWidthSupport.rightPad(record.getDescription(), 100))
                .append(FixedWidthSupport.formatSignedDecimal(record.getAmount(), 11, 2))
                .append(FixedWidthSupport.leftPadZero(record.getMerchantId() == null ? null : record.getMerchantId().toString(), 9))
                .append(FixedWidthSupport.rightPad(record.getMerchantName(), 50))
                .append(FixedWidthSupport.rightPad(record.getMerchantCity(), 50))
                .append(FixedWidthSupport.rightPad(record.getMerchantZip(), 10))
                .append(FixedWidthSupport.leftPadZero(record.getCardNumber(), 16))
                .append(formatTs(record.getOriginalTimestamp()))
                .append(formatTs(record.getProcessedTimestamp()))
                .append(" ".repeat(20))
                .toString();
    }

    private static LocalDateTime parseTs(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value, TS_FORMAT);
    }

    private static String formatTs(LocalDateTime value) {
        return value == null
                ? " ".repeat(26)
                : FixedWidthSupport.rightPad(TS_FORMAT.format(value), 26);
    }
}
