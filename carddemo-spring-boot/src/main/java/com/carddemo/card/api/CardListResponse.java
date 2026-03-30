package com.carddemo.card.api;

import java.util.List;

public record CardListResponse(
        List<CardRow> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record CardRow(
            String cardNumber,
            Long accountId,
            String embossedName,
            java.time.LocalDate expirationDate,
            String activeStatus
    ) {
    }
}
