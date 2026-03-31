package com.carddemo.transaction;

import java.math.BigDecimal;

/**
 * Outcome of a bill payment attempt (COBIL00C confirm path).
 */
public record PaymentResult(boolean success, String message, String transactionId, BigDecimal paidAmount) {

    public static PaymentResult success(String transactionId, BigDecimal paidAmount) {
        return new PaymentResult(true, null, transactionId, paidAmount);
    }

    public static PaymentResult error(String message) {
        return new PaymentResult(false, message, null, null);
    }
}
