package com.carddemo.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped from COBOL copybook CVTRA03Y (TRAN-TYPE-RECORD, RECLN 60).
 * Backed by VSAM file TRANTYPE.
 */
@Entity
@Table(name = "transaction_types")
public class TransactionType {

    @Id
    @Column(name = "type_cd", nullable = false, length = 2)
    private String typeCode;

    @Column(name = "type_desc", length = 50)
    private String typeDescription;

    protected TransactionType() {}

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeDescription() { return typeDescription; }
    public void setTypeDescription(String typeDescription) { this.typeDescription = typeDescription; }
}
