package com.carddemo.auth.pending;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pending_auth_summary")
public class PendingAuthSummary {

    @Id
    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "auth_status", length = 1)
    private String authStatus;

    @Column(name = "acct_status_1", length = 2)
    private String acctStatus1;
    @Column(name = "acct_status_2", length = 2)
    private String acctStatus2;
    @Column(name = "acct_status_3", length = 2)
    private String acctStatus3;
    @Column(name = "acct_status_4", length = 2)
    private String acctStatus4;
    @Column(name = "acct_status_5", length = 2)
    private String acctStatus5;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;
    @Column(name = "cash_limit", precision = 12, scale = 2)
    private BigDecimal cashLimit;
    @Column(name = "credit_balance", precision = 12, scale = 2)
    private BigDecimal creditBalance;
    @Column(name = "cash_balance", precision = 12, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "approved_auth_count")
    private Integer approvedAuthCount;
    @Column(name = "declined_auth_count")
    private Integer declinedAuthCount;
    @Column(name = "approved_auth_amt", precision = 12, scale = 2)
    private BigDecimal approvedAuthAmt;
    @Column(name = "declined_auth_amt", precision = 12, scale = 2)
    private BigDecimal declinedAuthAmt;

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public String getAcctStatus1() {
        return acctStatus1;
    }

    public void setAcctStatus1(String acctStatus1) {
        this.acctStatus1 = acctStatus1;
    }

    public String getAcctStatus2() {
        return acctStatus2;
    }

    public void setAcctStatus2(String acctStatus2) {
        this.acctStatus2 = acctStatus2;
    }

    public String getAcctStatus3() {
        return acctStatus3;
    }

    public void setAcctStatus3(String acctStatus3) {
        this.acctStatus3 = acctStatus3;
    }

    public String getAcctStatus4() {
        return acctStatus4;
    }

    public void setAcctStatus4(String acctStatus4) {
        this.acctStatus4 = acctStatus4;
    }

    public String getAcctStatus5() {
        return acctStatus5;
    }

    public void setAcctStatus5(String acctStatus5) {
        this.acctStatus5 = acctStatus5;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashLimit() {
        return cashLimit;
    }

    public void setCashLimit(BigDecimal cashLimit) {
        this.cashLimit = cashLimit;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public Integer getApprovedAuthCount() {
        return approvedAuthCount;
    }

    public void setApprovedAuthCount(Integer approvedAuthCount) {
        this.approvedAuthCount = approvedAuthCount;
    }

    public Integer getDeclinedAuthCount() {
        return declinedAuthCount;
    }

    public void setDeclinedAuthCount(Integer declinedAuthCount) {
        this.declinedAuthCount = declinedAuthCount;
    }

    public BigDecimal getApprovedAuthAmt() {
        return approvedAuthAmt;
    }

    public void setApprovedAuthAmt(BigDecimal approvedAuthAmt) {
        this.approvedAuthAmt = approvedAuthAmt;
    }

    public BigDecimal getDeclinedAuthAmt() {
        return declinedAuthAmt;
    }

    public void setDeclinedAuthAmt(BigDecimal declinedAuthAmt) {
        this.declinedAuthAmt = declinedAuthAmt;
    }
}
