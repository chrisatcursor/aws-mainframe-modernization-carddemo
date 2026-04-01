package com.carddemo.account.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountUpdateRequest(
        @NotNull Long accountId,
        @NotNull Long expectedAccountVersion,
        @NotNull Long expectedCustomerVersion,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String openDate,
        String expirationDate,
        String reissueDate,
        String groupId,
        Long customerId,
        String firstName,
        String middleName,
        String lastName,
        String addrLine1,
        String addrLine2,
        String addrLine3,
        String addrStateCd,
        String addrCountryCd,
        String addrZip,
        String phoneNum1,
        String phoneNum2,
        String ssn,
        String govtIssuedId,
        String dobYyyyMmDd,
        String eftAccountId,
        String priCardHolderInd,
        Integer ficoCreditScore) {
}
