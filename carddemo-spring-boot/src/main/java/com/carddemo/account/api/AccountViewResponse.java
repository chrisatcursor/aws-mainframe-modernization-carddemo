package com.carddemo.account.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountViewResponse(
        Long accountId,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        LocalDate openDate,
        LocalDate expirationDate,
        LocalDate reissueDate,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String addressZip,
        String groupId,
        Long customerId,
        String firstName,
        String middleName,
        String lastName,
        String stateCode,
        String countryCode,
        String zip,
        String phone1,
        String phone2,
        String ssn,
        String govtIssuedId,
        LocalDate dateOfBirth,
        Integer ficoScore
) {
    public static AccountViewResponse from(
            com.carddemo.account.model.Account account,
            com.carddemo.account.model.Customer customer,
            String cardNumber
    ) {
        return new AccountViewResponse(
                account.getAccountId(),
                account.getActiveStatus(),
                account.getCurrentBalance(),
                account.getCreditLimit(),
                account.getCashCreditLimit(),
                account.getOpenDate(),
                account.getExpirationDate(),
                account.getReissueDate(),
                account.getCurrentCycleCredit(),
                account.getCurrentCycleDebit(),
                account.getAddressZip(),
                account.getGroupId(),
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getMiddleName(),
                customer.getLastName(),
                customer.getStateCode(),
                customer.getCountryCode(),
                customer.getZip(),
                customer.getPhone1(),
                customer.getPhone2(),
                customer.getSsn(),
                customer.getGovtIssuedId(),
                customer.getDateOfBirth(),
                customer.getFicoScore());
    }
}
