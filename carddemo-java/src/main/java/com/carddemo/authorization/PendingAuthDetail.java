package com.carddemo.authorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * IMS PAUTDTL1 / CIPAUDTY mapped relationally.
 */
@Entity
@Table(name = "pending_auth_details")
public class PendingAuthDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "auth_date_9c", nullable = false)
    private int authDate9c;

    @Column(name = "auth_time_9c", nullable = false)
    private int authTime9c;

    @Column(name = "auth_orig_date", length = 6)
    private String authOrigDate;

    @Column(name = "auth_orig_time", length = 6)
    private String authOrigTime;

    @Column(name = "card_num", length = 16, nullable = false)
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

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "acqr_country_code", length = 3)
    private String acqrCountryCode;

    @Column(name = "pos_entry_mode")
    private Short posEntryMode;

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

    @Column(name = "transaction_id", length = 15)
    private String transactionId;

    @Column(name = "match_status", length = 1)
    private String matchStatus;

    @Column(name = "auth_fraud", length = 1)
    private String authFraud;

    @Column(name = "fraud_rpt_date", length = 8)
    private String fraudRptDate;

    public PendingAuthDetail() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }
    public int getAuthDate9c() { return authDate9c; }
    public void setAuthDate9c(int authDate9c) { this.authDate9c = authDate9c; }
    public int getAuthTime9c() { return authTime9c; }
    public void setAuthTime9c(int authTime9c) { this.authTime9c = authTime9c; }
    public String getAuthOrigDate() { return authOrigDate; }
    public void setAuthOrigDate(String authOrigDate) { this.authOrigDate = authOrigDate; }
    public String getAuthOrigTime() { return authOrigTime; }
    public void setAuthOrigTime(String authOrigTime) { this.authOrigTime = authOrigTime; }
    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getCardExpiryDate() { return cardExpiryDate; }
    public void setCardExpiryDate(String cardExpiryDate) { this.cardExpiryDate = cardExpiryDate; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getMessageSource() { return messageSource; }
    public void setMessageSource(String messageSource) { this.messageSource = messageSource; }
    public String getAuthIdCode() { return authIdCode; }
    public void setAuthIdCode(String authIdCode) { this.authIdCode = authIdCode; }
    public String getAuthRespCode() { return authRespCode; }
    public void setAuthRespCode(String authRespCode) { this.authRespCode = authRespCode; }
    public String getAuthRespReason() { return authRespReason; }
    public void setAuthRespReason(String authRespReason) { this.authRespReason = authRespReason; }
    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }
    public BigDecimal getTransactionAmt() { return transactionAmt; }
    public void setTransactionAmt(BigDecimal transactionAmt) { this.transactionAmt = transactionAmt; }
    public BigDecimal getApprovedAmt() { return approvedAmt; }
    public void setApprovedAmt(BigDecimal approvedAmt) { this.approvedAmt = approvedAmt; }
    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    public String getAcqrCountryCode() { return acqrCountryCode; }
    public void setAcqrCountryCode(String acqrCountryCode) { this.acqrCountryCode = acqrCountryCode; }
    public Short getPosEntryMode() { return posEntryMode; }
    public void setPosEntryMode(Short posEntryMode) { this.posEntryMode = posEntryMode; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
    public String getMerchantState() { return merchantState; }
    public void setMerchantState(String merchantState) { this.merchantState = merchantState; }
    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
    public String getAuthFraud() { return authFraud; }
    public void setAuthFraud(String authFraud) { this.authFraud = authFraud; }
    public String getFraudRptDate() { return fraudRptDate; }
    public void setFraudRptDate(String fraudRptDate) { this.fraudRptDate = fraudRptDate; }
}
