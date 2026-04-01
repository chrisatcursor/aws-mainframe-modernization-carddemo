package com.carddemo.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_xrefs")
public class CardXref {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "cust_id", nullable = false)
    private Long customerId;

    @Column(name = "acct_id", nullable = false)
    private Long accountId;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
