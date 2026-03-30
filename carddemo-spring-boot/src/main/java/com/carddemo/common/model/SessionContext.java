package com.carddemo.common.model;

public record SessionContext(
        String fromProgram,
        String fromTransactionId,
        String toProgram,
        String toTransactionId,
        String userId,
        String userType,
        Long currentAccountId,
        String currentCardNumber
) {
}
