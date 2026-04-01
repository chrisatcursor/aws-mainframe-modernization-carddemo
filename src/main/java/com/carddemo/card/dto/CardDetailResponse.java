package com.carddemo.card.dto;

public record CardDetailResponse(
        String errorMessage,
        long accountId,
        String cardNumber,
        String embossedName,
        String expiryYear,
        String expiryMonth,
        String activeStatus) {

    public static CardDetailResponse error(String msg) {
        return new CardDetailResponse(msg, 0, null, null, null, null, null);
    }
}
