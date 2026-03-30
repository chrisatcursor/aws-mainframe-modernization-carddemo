package com.carddemo.batch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "daily_transaction_rejections")
public class DailyTransactionRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rejection_id")
    private Long rejectionId;

    @Column(name = "transaction_id", nullable = false, length = 16)
    private String transactionId;

    @Column(name = "reason_code", nullable = false)
    private Integer reasonCode;

    @Column(name = "reason_description", nullable = false, length = 200)
    private String reasonDescription;

    public DailyTransactionRejection() {
    }

    public DailyTransactionRejection(String transactionId, Integer reasonCode, String reasonDescription) {
        this.transactionId = transactionId;
        this.reasonCode = reasonCode;
        this.reasonDescription = reasonDescription;
    }

    public Long getRejectionId() {
        return rejectionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(Integer reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public void setReasonDescription(String reasonDescription) {
        this.reasonDescription = reasonDescription;
    }
}
