package com.carddemo.transaction;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form backing bean for manual transaction entry (COTRN02C).
 */
public class TransactionCreateRequest {

    @NotBlank
    @Size(max = 16)
    private String cardNumber;

    @NotBlank
    @Size(max = 2)
    private String typeCode;

    @NotNull
    private Integer categoryCode;

    @Size(max = 100)
    private String description;

    @NotNull
    private BigDecimal amount;

    @Size(max = 50)
    private String merchantName;

    @Size(max = 50)
    private String merchantCity;

    @Size(max = 10)
    private String merchantZip;

    @NotBlank
    private String originTimestamp;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public Integer getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(Integer categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    public String getOriginTimestamp() {
        return originTimestamp;
    }

    public void setOriginTimestamp(String originTimestamp) {
        this.originTimestamp = originTimestamp;
    }
}
