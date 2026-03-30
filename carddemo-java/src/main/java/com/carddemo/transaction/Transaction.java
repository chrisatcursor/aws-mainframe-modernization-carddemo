package com.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * JPA entity mapped from COBOL copybook CVTRA05Y (TRAN-RECORD, RECLN 350).
 * Backed by VSAM file TRANSACT.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "tran_id", nullable = false, length = 16)
    private String transactionId;

    @Column(name = "type_cd", length = 2)
    private String typeCode;

    @Column(name = "cat_cd")
    private Integer categoryCode;

    @Column(name = "source", length = 10)
    private String source;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "amount", precision = 11, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "card_num", length = 16)
    private String cardNumber;

    @Column(name = "orig_ts", length = 26)
    private String originTimestamp;

    @Column(name = "proc_ts", length = 26)
    private String processedTimestamp;

    protected Transaction() {}

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public Integer getCategoryCode() { return categoryCode; }
    public void setCategoryCode(Integer categoryCode) { this.categoryCode = categoryCode; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getOriginTimestamp() { return originTimestamp; }
    public void setOriginTimestamp(String originTimestamp) { this.originTimestamp = originTimestamp; }
    public String getProcessedTimestamp() { return processedTimestamp; }
    public void setProcessedTimestamp(String processedTimestamp) { this.processedTimestamp = processedTimestamp; }
}
