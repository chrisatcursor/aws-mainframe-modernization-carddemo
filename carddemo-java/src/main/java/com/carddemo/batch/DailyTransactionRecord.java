package com.carddemo.batch;

import java.math.BigDecimal;

/**
 * One row from the daily transaction input file (CBTRN02C input).
 */
public record DailyTransactionRecord(
        String transactionId,
        String cardNumber,
        String typeCode,
        int categoryCode,
        BigDecimal amount,
        Long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String originTimestamp) {}
