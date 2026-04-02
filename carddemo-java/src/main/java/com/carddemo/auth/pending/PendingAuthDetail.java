package com.carddemo.auth.pending;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "pending_auth_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"acct_id", "transaction_id"}))
public class PendingAuthDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "auth_ts", nullable = false)
    private LocalDateTime authTs;

    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    @Column(name = "auth_type", length = 4)
    private String authType;
    @Column(name = "card_expiry_date", length = 4)
    private String cardExpiryDate;
    @Column(name = "message_type", length = 6)
    private String messageType;
    @Column(name = "message_source", length = 6)
    private String messageSource;
    @Column(name = "auth_id_code", length = 6)
    private String authIdCode;
    @Column(name = "auth_resp_code", length = 2)
    private String authRespCode;
    @Column(name = "auth_resp_reason", length = 4)
    private String authRespReason;
    @Column(name = "processing_code", length = 6)
    private String processingCode;
    @Column(name = "transaction_amt", precision = 12, scale = 2)
    private BigDecimal transactionAmt;
    @Column(name = "approved_amt", precision = 12, scale = 2)
    private BigDecimal approvedAmt;
    @Column(name = "merchant_category_cd", length = 4)
    private String merchantCategoryCd;
    @Column(name = "acqr_country_code", length = 3)
    private String acqrCountryCode;
    @Column(name = "pos_entry_mode")
    private Integer posEntryMode;
    @Column(name = "merchant_id", length = 15)
    private String merchantId;
    @Column(name = "merchant_name", length = 22)
    private String merchantName;
    @Column(name = "merchant_city", length = 13)
    private String merchantCity;
    @Column(name = "merchant_state", length = 2)
    private String merchantState;
    @Column(name = "merchant_zip", length = 9)
    private String merchantZip;
    @Column(name = "transaction_id", nullable = false, length = 15)
    private String transactionId;
    @Column(name = "match_status", length = 1)
    private String matchStatus;
    @Column(name = "fraud_flag", length = 1)
    private String fraudFlag;
    @Column(name = "fraud_report_date", length = 8)
    private String fraudReportDate;

    public Long getId() {
        return id;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public LocalDateTime getAuthTs() {
        return authTs;
    }

    public void setAuthTs(LocalDateTime authTs) {
        this.authTs = authTs;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getCardExpiryDate() {
        return cardExpiryDate;
    }

    public void setCardExpiryDate(String cardExpiryDate) {
        this.cardExpiryDate = cardExpiryDate;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageSource() {
        return messageSource;
    }

    public void setMessageSource(String messageSource) {
        this.messageSource = messageSource;
    }

    public String getAuthIdCode() {
        return authIdCode;
    }

    public void setAuthIdCode(String authIdCode) {
        this.authIdCode = authIdCode;
    }

    public String getAuthRespCode() {
        return authRespCode;
    }

    public void setAuthRespCode(String authRespCode) {
        this.authRespCode = authRespCode;
    }

    public String getAuthRespReason() {
        return authRespReason;
    }

    public void setAuthRespReason(String authRespReason) {
        this.authRespReason = authRespReason;
    }

    public String getProcessingCode() {
        return processingCode;
    }

    public void setProcessingCode(String processingCode) {
        this.processingCode = processingCode;
    }

    public BigDecimal getTransactionAmt() {
        return transactionAmt;
    }

    public void setTransactionAmt(BigDecimal transactionAmt) {
        this.transactionAmt = transactionAmt;
    }

    public BigDecimal getApprovedAmt() {
        return approvedAmt;
    }

    public void setApprovedAmt(BigDecimal approvedAmt) {
        this.approvedAmt = approvedAmt;
    }

    public String getMerchantCategoryCd() {
        return merchantCategoryCd;
    }

    public void setMerchantCategoryCd(String merchantCategoryCd) {
        this.merchantCategoryCd = merchantCategoryCd;
    }

    public String getAcqrCountryCode() {
        return acqrCountryCode;
    }

    public void setAcqrCountryCode(String acqrCountryCode) {
        this.acqrCountryCode = acqrCountryCode;
    }

    public Integer getPosEntryMode() {
        return posEntryMode;
    }

    public void setPosEntryMode(Integer posEntryMode) {
        this.posEntryMode = posEntryMode;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
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

    public String getMerchantState() {
        return merchantState;
    }

    public void setMerchantState(String merchantState) {
        this.merchantState = merchantState;
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getFraudFlag() {
        return fraudFlag;
    }

    public void setFraudFlag(String fraudFlag) {
        this.fraudFlag = fraudFlag;
    }

    public String getFraudReportDate() {
        return fraudReportDate;
    }

    public void setFraudReportDate(String fraudReportDate) {
        this.fraudReportDate = fraudReportDate;
    }
}
