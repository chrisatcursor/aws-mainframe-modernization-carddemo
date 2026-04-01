package com.carddemo.card.dto;

import java.util.List;

public record CardListResponse(
        String errorMessage,
        boolean hasNextPage,
        String nextCursorCardNumber,
        List<CardRowDto> rows) {

    public static CardListResponse error(String msg) {
        return new CardListResponse(msg, false, null, List.of());
    }
}
