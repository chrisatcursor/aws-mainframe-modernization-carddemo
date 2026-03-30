package com.carddemo.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped from COBOL copybook CVACT02Y (CARD-RECORD, RECLN 150).
 * Backed by VSAM file CARDDAT.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNumber;

    @Column(name = "acct_id")
    private Long accountId;

    @Column(name = "cvv_cd")
    private Integer cvvCode;

    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "expiration_date", length = 10)
    private String expirationDate;

    @Column(name = "active_status", length = 1)
    private String activeStatus;

    protected Card() {}

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Integer getCvvCode() { return cvvCode; }
    public void setCvvCode(Integer cvvCode) { this.cvvCode = cvvCode; }
    public String getEmbossedName() { return embossedName; }
    public void setEmbossedName(String embossedName) { this.embossedName = embossedName; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }
}
