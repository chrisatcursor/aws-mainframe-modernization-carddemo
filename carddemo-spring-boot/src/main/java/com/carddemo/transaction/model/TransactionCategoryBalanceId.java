package com.carddemo.transaction.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TransactionCategoryBalanceId implements Serializable {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "transaction_type_code", nullable = false, length = 2)
    private String transactionTypeCode;

    @Column(name = "transaction_category_code", nullable = false)
    private Integer transactionCategoryCode;

    protected TransactionCategoryBalanceId() {
    }

    public TransactionCategoryBalanceId(Long accountId, String transactionTypeCode, Integer transactionCategoryCode) {
        this.accountId = accountId;
        this.transactionTypeCode = transactionTypeCode;
        this.transactionCategoryCode = transactionCategoryCode;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public Integer getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionCategoryBalanceId that)) {
            return false;
        }
        return Objects.equals(accountId, that.accountId)
                && Objects.equals(transactionTypeCode, that.transactionTypeCode)
                && Objects.equals(transactionCategoryCode, that.transactionCategoryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, transactionTypeCode, transactionCategoryCode);
    }
}
