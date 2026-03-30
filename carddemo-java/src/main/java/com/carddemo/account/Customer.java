package com.carddemo.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped from COBOL copybook CVCUS01Y (CUSTOMER-RECORD, RECLN 500).
 * Backed by VSAM file CUSTDAT.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "first_name", length = 25)
    private String firstName;

    @Column(name = "middle_name", length = 25)
    private String middleName;

    @Column(name = "last_name", length = 25)
    private String lastName;

    @Column(name = "addr_line_1", length = 50)
    private String addressLine1;

    @Column(name = "addr_line_2", length = 50)
    private String addressLine2;

    @Column(name = "addr_line_3", length = 50)
    private String addressLine3;

    @Column(name = "addr_state_cd", length = 2)
    private String addressStateCode;

    @Column(name = "addr_country_cd", length = 3)
    private String addressCountryCode;

    @Column(name = "addr_zip", length = 10)
    private String addressZip;

    @Column(name = "phone_num_1", length = 15)
    private String phoneNumber1;

    @Column(name = "phone_num_2", length = 15)
    private String phoneNumber2;

    @Column(name = "ssn")
    private Long ssn;

    @Column(name = "govt_issued_id", length = 20)
    private String govtIssuedId;

    @Column(name = "dob", length = 10)
    private String dateOfBirth;

    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;

    @Column(name = "pri_card_holder_ind", length = 1)
    private String primaryCardHolderIndicator;

    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;

    protected Customer() {}

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getAddressLine3() { return addressLine3; }
    public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }
    public String getAddressStateCode() { return addressStateCode; }
    public void setAddressStateCode(String addressStateCode) { this.addressStateCode = addressStateCode; }
    public String getAddressCountryCode() { return addressCountryCode; }
    public void setAddressCountryCode(String addressCountryCode) { this.addressCountryCode = addressCountryCode; }
    public String getAddressZip() { return addressZip; }
    public void setAddressZip(String addressZip) { this.addressZip = addressZip; }
    public String getPhoneNumber1() { return phoneNumber1; }
    public void setPhoneNumber1(String phoneNumber1) { this.phoneNumber1 = phoneNumber1; }
    public String getPhoneNumber2() { return phoneNumber2; }
    public void setPhoneNumber2(String phoneNumber2) { this.phoneNumber2 = phoneNumber2; }
    public Long getSsn() { return ssn; }
    public void setSsn(Long ssn) { this.ssn = ssn; }
    public String getGovtIssuedId() { return govtIssuedId; }
    public void setGovtIssuedId(String govtIssuedId) { this.govtIssuedId = govtIssuedId; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEftAccountId() { return eftAccountId; }
    public void setEftAccountId(String eftAccountId) { this.eftAccountId = eftAccountId; }
    public String getPrimaryCardHolderIndicator() { return primaryCardHolderIndicator; }
    public void setPrimaryCardHolderIndicator(String primaryCardHolderIndicator) { this.primaryCardHolderIndicator = primaryCardHolderIndicator; }
    public Integer getFicoCreditScore() { return ficoCreditScore; }
    public void setFicoCreditScore(Integer ficoCreditScore) { this.ficoCreditScore = ficoCreditScore; }
}
