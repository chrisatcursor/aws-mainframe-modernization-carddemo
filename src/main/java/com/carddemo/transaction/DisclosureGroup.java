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
@Table(name = "disclosure_groups")
public class DisclosureGroup {

    @EmbeddedId
    private DisclosureGroupKey id;

    @Column(name = "int_rate", precision = 6, scale = 2)
    private BigDecimal interestRate;

    public DisclosureGroupKey getId() {
        return id;
    }

    public void setId(DisclosureGroupKey id) {
        this.id = id;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    @Embeddable
    public static class DisclosureGroupKey implements Serializable {

        @Column(name = "acct_group_id", length = 10, nullable = false)
        private String accountGroupId;

        @Column(name = "tran_type_cd", length = 2, nullable = false)
        private String tranTypeCode;

        @Column(name = "tran_cat_cd", nullable = false)
        private Integer tranCategoryCode;

        public String getAccountGroupId() {
            return accountGroupId;
        }

        public void setAccountGroupId(String accountGroupId) {
            this.accountGroupId = accountGroupId;
        }

        public String getTranTypeCode() {
            return tranTypeCode;
        }

        public void setTranTypeCode(String tranTypeCode) {
            this.tranTypeCode = tranTypeCode;
        }

        public Integer getTranCategoryCode() {
            return tranCategoryCode;
        }

        public void setTranCategoryCode(Integer tranCategoryCode) {
            this.tranCategoryCode = tranCategoryCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DisclosureGroupKey that = (DisclosureGroupKey) o;
            return Objects.equals(accountGroupId, that.accountGroupId)
                    && Objects.equals(tranTypeCode, that.tranTypeCode)
                    && Objects.equals(tranCategoryCode, that.tranCategoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountGroupId, tranTypeCode, tranCategoryCode);
        }
    }
}
