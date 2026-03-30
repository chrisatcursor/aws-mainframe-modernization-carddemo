package com.carddemo.account.api;

public record AccountUpdateResponse(
        Long accountId,
        Long customerId,
        Long version,
        String message
) {
}
