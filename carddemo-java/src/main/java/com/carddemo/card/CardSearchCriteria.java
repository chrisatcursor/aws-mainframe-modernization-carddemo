package com.carddemo.card;

/**
 * Optional filters for card browse (account id and card number prefix).
 */
public class CardSearchCriteria {

    private Long accountId;
    private String cardNumberPrefix;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getCardNumberPrefix() {
        return cardNumberPrefix;
    }

    public void setCardNumberPrefix(String cardNumberPrefix) {
        this.cardNumberPrefix = cardNumberPrefix;
    }
}
