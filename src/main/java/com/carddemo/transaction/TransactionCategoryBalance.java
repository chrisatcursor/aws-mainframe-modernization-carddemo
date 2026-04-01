package com.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "transaction_category_balances")
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryBalanceKey id;

    @Column(name = "balance", precision = 11, scale = 2)
    private BigDecimal balance;

    public TransactionCategoryBalanceKey getId() {
        return id;
    }

    public void setId(TransactionCategoryBalanceKey id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Embeddable
    public static class TransactionCategoryBalanceKey implements Serializable {

        @Column(name = "acct_id", nullable = false)
        private Long accountId;

        @Column(name = "type_cd", length = 2, nullable = false)
        private String typeCode;

        @Column(name = "cat_cd", nullable = false)
        private Integer categoryCode;

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TransactionCategoryBalanceKey that = (TransactionCategoryBalanceKey) o;
            return Objects.equals(accountId, that.accountId)
                    && Objects.equals(typeCode, that.typeCode)
                    && Objects.equals(categoryCode, that.categoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId, typeCode, categoryCode);
        }
    }
}
