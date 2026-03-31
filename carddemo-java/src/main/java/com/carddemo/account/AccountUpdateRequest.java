package com.carddemo.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Form backing object for coordinated account + customer update (COACTUPC).
 */
public class AccountUpdateRequest {

    @NotNull
    private Long acctId;

    @NotBlank
    @Size(max = 1)
    private String activeStatus;

    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;

    @Size(max = 10)
    private String openDate;

    @Size(max = 10)
    private String expirationDate;

    @Size(max = 10)
    private String reissueDate;

    @Size(max = 10)
    private String groupId;

    @Size(max = 10)
    private String addressZip;

    @NotNull
    private Long accountVersion;

    @NotBlank
    @Size(max = 25)
    private String firstName;

    @NotBlank
    @Size(max = 25)
    private String lastName;

    @Size(max = 50)
    private String addressLine1;

    @Size(max = 50)
    private String addressLine2;

    @Size(max = 50)
    private String addressLine3;

    @Size(max = 2)
    private String addressStateCode;

    @Size(max = 3)
    private String addressCountryCode;

    @Size(max = 10)
    private String customerAddressZip;

    @Size(max = 15)
    private String phoneNumber1;

    @Size(max = 15)
    private String phoneNumber2;

    private Long ssn;

    @Size(max = 20)
    private String govtIssuedId;

    @Size(max = 10)
    private String dateOfBirth;

    @Size(max = 10)
    private String eftAccountId;

    private Integer ficoCreditScore;

    @NotNull
    private Long customerVersion;

    public static AccountUpdateRequest fromDetail(AccountDetailDto detail) {
        AccountUpdateRequest r = new AccountUpdateRequest();
        r.setAcctId(detail.acctId());
        r.setActiveStatus(detail.activeStatus());
        r.setCreditLimit(detail.creditLimit());
        r.setCashCreditLimit(detail.cashCreditLimit());
        r.setOpenDate(detail.openDate());
        r.setExpirationDate(detail.expirationDate());
        r.setReissueDate(detail.reissueDate());
        r.setGroupId(detail.groupId());
        r.setAddressZip(detail.accountAddressZip());
        r.setAccountVersion(detail.accountVersion());
        r.setFirstName(detail.firstName());
        r.setLastName(detail.lastName());
        r.setAddressLine1(detail.addressLine1());
        r.setAddressLine2(detail.addressLine2());
        r.setAddressLine3(detail.addressLine3());
        r.setAddressStateCode(detail.addressStateCode());
        r.setAddressCountryCode(detail.addressCountryCode());
        r.setCustomerAddressZip(detail.customerAddressZip());
        r.setPhoneNumber1(detail.phoneNumber1());
        r.setPhoneNumber2(detail.phoneNumber2());
        r.setSsn(detail.ssn());
        r.setGovtIssuedId(detail.govtIssuedId());
        r.setDateOfBirth(detail.dateOfBirth());
        r.setEftAccountId(detail.eftAccountId());
        r.setFicoCreditScore(detail.ficoCreditScore());
        r.setCustomerVersion(detail.customerVersion());
        return r;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    public String getOpenDate() {
        return openDate;
    }

    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public Long getAccountVersion() {
        return accountVersion;
    }

    public void setAccountVersion(Long accountVersion) {
        this.accountVersion = accountVersion;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    public String getAddressStateCode() {
        return addressStateCode;
    }

    public void setAddressStateCode(String addressStateCode) {
        this.addressStateCode = addressStateCode;
    }

    public String getAddressCountryCode() {
        return addressCountryCode;
    }

    public void setAddressCountryCode(String addressCountryCode) {
        this.addressCountryCode = addressCountryCode;
    }

    public String getCustomerAddressZip() {
        return customerAddressZip;
    }

    public void setCustomerAddressZip(String customerAddressZip) {
        this.customerAddressZip = customerAddressZip;
    }

    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    public Long getSsn() {
        return ssn;
    }

    public void setSsn(Long ssn) {
        this.ssn = ssn;
    }

    public String getGovtIssuedId() {
        return govtIssuedId;
    }

    public void setGovtIssuedId(String govtIssuedId) {
        this.govtIssuedId = govtIssuedId;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEftAccountId() {
        return eftAccountId;
    }

    public void setEftAccountId(String eftAccountId) {
        this.eftAccountId = eftAccountId;
    }

    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    public Long getCustomerVersion() {
        return customerVersion;
    }

    public void setCustomerVersion(Long customerVersion) {
        this.customerVersion = customerVersion;
    }
}
