package com.carddemo.card.api;

import java.time.LocalDate;

public record CardDetailResponse(
        String cardNumber,
        Long accountId,
        Long customerId,
        Integer cvv,
        String embossedName,
        LocalDate expirationDate,
        String activeStatus
) {
}
