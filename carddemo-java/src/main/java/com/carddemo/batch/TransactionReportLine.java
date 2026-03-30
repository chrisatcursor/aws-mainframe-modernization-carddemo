package com.carddemo.batch;

import java.math.BigDecimal;

/**
 * One detail line for the CBTRN03C / TRANREPT transaction report. {@code transactionType} and
 * {@code transactionCategory} carry {@code '\u001e'}-separated code and description for fixed-width
 * formatting in {@link TransactionReportWriter}.
 */
public record TransactionReportLine(
        String transactionId,
        String cardNumber,
        String accountId,
        String transactionType,
        String transactionCategory,
        BigDecimal amount,
        String date,
        String description) {}
