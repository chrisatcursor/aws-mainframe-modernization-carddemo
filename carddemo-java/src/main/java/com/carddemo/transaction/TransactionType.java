package com.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_types")
public class TransactionType {

    @Id
    @Column(name = "tr_type", length = 2)
    private String trType;

    @Column(name = "tr_description", nullable = false, length = 50)
    private String description;

    public String getTrType() {
        return trType;
    }

    public void setTrType(String trType) {
        this.trType = trType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
