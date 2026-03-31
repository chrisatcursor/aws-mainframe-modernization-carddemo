package com.carddemo.card;

/**
 * Read model for card list and detail screens (COCRDLIC / card browse).
 */
public record CardDto(
        String cardNumber,
        Long accountId,
        String embossedName,
        String expirationDate,
        String activeStatus
) {
    public static CardDto from(Card card) {
        return new CardDto(
                card.getCardNumber(),
                card.getAccountId(),
                card.getEmbossedName(),
                card.getExpirationDate(),
                card.getActiveStatus()
        );
    }
}
