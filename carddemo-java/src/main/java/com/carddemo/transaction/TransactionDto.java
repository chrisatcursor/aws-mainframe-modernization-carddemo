package com.carddemo.transaction;

import java.math.BigDecimal;

/**
 * Read model for transaction list and detail screens (COTRN00C).
 */
public record TransactionDto(
        String transactionId,
        String cardNumber,
        String typeCode,
        Integer categoryCode,
        String description,
        BigDecimal amount,
        String originTimestamp
) {
    public static TransactionDto from(Transaction entity) {
        if (entity == null) {
            return null;
        }
        return new TransactionDto(
                entity.getTransactionId(),
                entity.getCardNumber(),
                entity.getTypeCode(),
                entity.getCategoryCode(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getOriginTimestamp()
        );
    }
}
