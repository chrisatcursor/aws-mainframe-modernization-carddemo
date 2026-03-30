package com.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * JPA entity mapped from COBOL copybook CVTRA02Y (DIS-GROUP-RECORD, RECLN 50).
 * Backed by VSAM file DISCGRP.
 * Composite key: (accountGroupId, transactionTypeCode, transactionCategoryCode).
 */
@Entity
@Table(name = "disclosure_groups")
public class DisclosureGroup {

    @EmbeddedId
    private DisclosureGroupKey id;

    @Column(name = "interest_rate", precision = 6, scale = 2)
    private BigDecimal interestRate;

    protected DisclosureGroup() {}

    public DisclosureGroupKey getId() { return id; }
    public void setId(DisclosureGroupKey id) { this.id = id; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    @Embeddable
    public static class DisclosureGroupKey implements Serializable {

        @Column(name = "acct_group_id", length = 10)
        private String accountGroupId;

        @Column(name = "tran_type_cd", length = 2)
        private String transactionTypeCode;

        @Column(name = "tran_cat_cd")
        private Integer transactionCategoryCode;

        protected DisclosureGroupKey() {}

        public DisclosureGroupKey(String accountGroupId, String transactionTypeCode, Integer transactionCategoryCode) {
            this.accountGroupId = accountGroupId;
            this.transactionTypeCode = transactionTypeCode;
            this.transactionCategoryCode = transactionCategoryCode;
        }

        public String getAccountGroupId() { return accountGroupId; }
        public String getTransactionTypeCode() { return transactionTypeCode; }
        public Integer getTransactionCategoryCode() { return transactionCategoryCode; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DisclosureGroupKey that)) return false;
            return Objects.equals(accountGroupId, that.accountGroupId)
                    && Objects.equals(transactionTypeCode, that.transactionTypeCode)
                    && Objects.equals(transactionCategoryCode, that.transactionCategoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountGroupId, transactionTypeCode, transactionCategoryCode);
        }
    }
}
