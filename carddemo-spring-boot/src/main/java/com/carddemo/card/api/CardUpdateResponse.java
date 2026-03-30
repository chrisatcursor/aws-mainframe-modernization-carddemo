package com.carddemo.card.api;

import java.time.LocalDate;

public record CardUpdateResponse(
        String cardNumber,
        Long accountId,
        String embossedName,
        String activeStatus,
        LocalDate expirationDate,
        String message
) {
}
