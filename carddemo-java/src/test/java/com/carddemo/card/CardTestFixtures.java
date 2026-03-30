package com.carddemo.card;

/** Test helpers that need access to {@link Card}'s protected constructor. */
public final class CardTestFixtures {

    public static Card minimalCard(String cardNumber, Long accountId) {
        Card c = new Card();
        c.setCardNumber(cardNumber);
        c.setAccountId(accountId);
        c.setActiveStatus("Y");
        return c;
    }

    private CardTestFixtures() {}
}
