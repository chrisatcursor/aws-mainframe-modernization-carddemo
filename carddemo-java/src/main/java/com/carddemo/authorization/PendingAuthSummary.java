package com.carddemo.authorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/**
 * IMS PAUTSUM0 / CIPAUSMY mapped relationally (one row per account).
 */
@Entity
@Table(name = "pending_auth_summaries")
public class PendingAuthSummary {

    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "auth_status", length = 1)
    private String authStatus;

    @Column(name = "account_status_1", length = 2)
    private String accountStatus1;

    @Column(name = "account_status_2", length = 2)
    private String accountStatus2;

    @Column(name = "account_status_3", length = 2)
    private String accountStatus3;

    @Column(name = "account_status_4", length = 2)
    private String accountStatus4;

    @Column(name = "account_status_5", length = 2)
    private String accountStatus5;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "cash_limit", precision = 12, scale = 2)
    private BigDecimal cashLimit;

    @Column(name = "credit_balance", precision = 12, scale = 2)
    private BigDecimal creditBalance;

    @Column(name = "cash_balance", precision = 12, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "approved_auth_cnt", nullable = false)
    private int approvedAuthCnt;

    @Column(name = "declined_auth_cnt", nullable = false)
    private int declinedAuthCnt;

    @Column(name = "approved_auth_amt", precision = 12, scale = 2, nullable = false)
    private BigDecimal approvedAuthAmt = BigDecimal.ZERO;

    @Column(name = "declined_auth_amt", precision = 12, scale = 2, nullable = false)
    private BigDecimal declinedAuthAmt = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PendingAuthSummary() {}

    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }
    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }
    public String getAccountStatus1() { return accountStatus1; }
    public void setAccountStatus1(String accountStatus1) { this.accountStatus1 = accountStatus1; }
    public String getAccountStatus2() { return accountStatus2; }
    public void setAccountStatus2(String accountStatus2) { this.accountStatus2 = accountStatus2; }
    public String getAccountStatus3() { return accountStatus3; }
    public void setAccountStatus3(String accountStatus3) { this.accountStatus3 = accountStatus3; }
    public String getAccountStatus4() { return accountStatus4; }
    public void setAccountStatus4(String accountStatus4) { this.accountStatus4 = accountStatus4; }
    public String getAccountStatus5() { return accountStatus5; }
    public void setAccountStatus5(String accountStatus5) { this.accountStatus5 = accountStatus5; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public BigDecimal getCashLimit() { return cashLimit; }
    public void setCashLimit(BigDecimal cashLimit) { this.cashLimit = cashLimit; }
    public BigDecimal getCreditBalance() { return creditBalance; }
    public void setCreditBalance(BigDecimal creditBalance) { this.creditBalance = creditBalance; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public int getApprovedAuthCnt() { return approvedAuthCnt; }
    public void setApprovedAuthCnt(int approvedAuthCnt) { this.approvedAuthCnt = approvedAuthCnt; }
    public int getDeclinedAuthCnt() { return declinedAuthCnt; }
    public void setDeclinedAuthCnt(int declinedAuthCnt) { this.declinedAuthCnt = declinedAuthCnt; }
    public BigDecimal getApprovedAuthAmt() { return approvedAuthAmt; }
    public void setApprovedAuthAmt(BigDecimal approvedAuthAmt) { this.approvedAuthAmt = approvedAuthAmt; }
    public BigDecimal getDeclinedAuthAmt() { return declinedAuthAmt; }
    public void setDeclinedAuthAmt(BigDecimal declinedAuthAmt) { this.declinedAuthAmt = declinedAuthAmt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
