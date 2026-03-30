package com.carddemo.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped from COBOL copybook CVACT03Y (CARD-XREF-RECORD, RECLN 50).
 * Backed by VSAM files CCXREF / CXACAIX (alternate index path).
 */
@Entity
@Table(name = "card_xrefs")
public class CardXref {

    @Id
    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNumber;

    @Column(name = "cust_id")
    private Long customerId;

    @Column(name = "acct_id")
    private Long accountId;

    protected CardXref() {}

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
