package com.carddemo.batch;

/**
 * Rejected daily transaction line with a reason for downstream rejection reporting.
 */
public record RejectionRecord(
        String transactionId,
        String cardNumber,
        String reasonCode,
        String reasonMessage) {}
