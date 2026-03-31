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
 * JPA entity mapped from COBOL copybook CVTRA01Y (TRAN-CAT-BAL-RECORD, RECLN 50).
 * Backed by VSAM file TCATBALF. Composite key: (acctId, typeCode, categoryCode).
 */
@Entity
@Table(name = "transaction_category_balances")
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryBalanceKey id;

    @Column(name = "balance", precision = 11, scale = 2)
    private BigDecimal balance;

    protected TransactionCategoryBalance() {}

    /** For use outside this package (e.g. batch) without exposing a public no-arg constructor. */
    public static TransactionCategoryBalance newWithIdAndBalance(
            TransactionCategoryBalanceKey id, BigDecimal balance) {
        TransactionCategoryBalance row = new TransactionCategoryBalance();
        row.setId(id);
        row.setBalance(balance);
        return row;
    }

    public TransactionCategoryBalanceKey getId() { return id; }
    public void setId(TransactionCategoryBalanceKey id) { this.id = id; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    @Embeddable
    public static class TransactionCategoryBalanceKey implements Serializable {

        @Column(name = "acct_id")
        private Long acctId;

        @Column(name = "type_cd", length = 2)
        private String typeCode;

        @Column(name = "cat_cd")
        private Integer categoryCode;

        protected TransactionCategoryBalanceKey() {}

        public TransactionCategoryBalanceKey(Long acctId, String typeCode, Integer categoryCode) {
            this.acctId = acctId;
            this.typeCode = typeCode;
            this.categoryCode = categoryCode;
        }

        public Long getAcctId() { return acctId; }
        public String getTypeCode() { return typeCode; }
        public Integer getCategoryCode() { return categoryCode; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TransactionCategoryBalanceKey that)) return false;
            return Objects.equals(acctId, that.acctId)
                    && Objects.equals(typeCode, that.typeCode)
                    && Objects.equals(categoryCode, that.categoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(acctId, typeCode, categoryCode);
        }
    }
}
